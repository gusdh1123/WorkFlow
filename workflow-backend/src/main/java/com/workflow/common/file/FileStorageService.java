package com.workflow.common.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.workflow.common.exception.ApiException;
import com.workflow.common.exception.ErrorCode;

@Service
public class FileStorageService {

    private final Path root;

    public FileStorageService(@Value("${app.upload-dir}") String uploadDir) {
        this.root = Paths.get(uploadDir).normalize(); // 업로드 루트 디렉토리 설정, 안전하게 절대 경로로 변환
    }

    private String editorDir(String module) {

        if (module == null || module.isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "module이 비었습니다."); // module 필수
        }

        String m = module.trim().toLowerCase();

        if (!m.matches("^[a-z0-9_-]{2,30}$")) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "module 형식 오류"); // module 규칙 확인
        }

        return m; // 예: "tasks", "profile", 안전하게 소문자+문자 제한
    }

    // 작성 중 업로드 → tmp 저장
    public String storeEditorImageToTmp(MultipartFile file, String module) {

        String dirName = editorDir(module);

        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "파일이 비었습니다."); // 업로드 파일 필수
        }

        // 이미지 용량 제한 5MB
        long maxImageBytes = 5L * 1024 * 1024;
        if (file.getSize() > maxImageBytes) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "이미지는 5MB 이하만 업로드 가능합니다."); // 용량 초과
        }

        String ct = file.getContentType();
        if (ct == null) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "파일 형식이 올바르지 않습니다."); // ContentType 확인
        }

        // 확장자 확인
        Set<String> allowed = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
        );
        if (!allowed.contains(ct)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "허용되지 않는 이미지 형식입니다. (jpg/png/webp/gif)"); // 허용된 형식 체크
        }

        try {
            String ext = switch (ct) {
                case "image/png" -> ".png";
                case "image/webp" -> ".webp";
                case "image/gif" -> ".gif";
                case "image/jpeg" -> ".jpg";
                default -> throw new ApiException(ErrorCode.BAD_REQUEST, "허용되지 않는 이미지 형식입니다."); // fallback 안전장치
            };

            String storedName = UUID.randomUUID() + ext; // 파일명 랜덤 UUID + 확장자

            Path tmpDir = root.resolve(Paths.get(dirName, "tmp")).normalize(); // tmp 폴더 경로
            Files.createDirectories(tmpDir); // 폴더 없으면 생성

            Path target = tmpDir.resolve(storedName).normalize();

            // 안전장치: root 밖으로 못 나가게
            if (!target.startsWith(tmpDir)) {
                throw new ApiException(ErrorCode.BAD_REQUEST, "잘못된 파일 경로");
            }

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING); // 파일 저장, 기존 파일 덮어쓰기
            }

            return "/uploads/" + dirName + "/tmp/" + storedName; // URL 반환

        } catch (IOException e) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "이미지 저장 실패"); // IO 에러
        }
    }

    // content에서 tmp 이미지 path 추출
    public List<String> extractTmpEditorImagePaths(String content, String module) {

        if (content == null || content.isBlank()) {
            return List.of(); // 내용 없으면 빈 리스트 반환
        }

        String dirName = editorDir(module);

        Pattern p = Pattern.compile(
                "(?:https?://[^\"']+)?(/uploads/" + Pattern.quote(dirName) + "/tmp/[^\"'\\s>]+)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher m = p.matcher(content);

        LinkedHashSet<String> set = new LinkedHashSet<>();
        while (m.find()) set.add(m.group(1)); // 중복 제거 + 순서 유지

        return new ArrayList<>(set); // 리스트로 변환하여 반환
    }

    // tmp → {module}/{ownerId}/ 이동
    public String moveTmpToOwnerDir(String tmpPath, String module, Long ownerId) {

        if (ownerId == null || ownerId <= 0) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "잘못된 ID"); // ownerId 검증
        }

        String dirName = editorDir(module);

        try {
            String fileName = Paths.get(tmpPath).getFileName().toString();

            Path src = root.resolve(Paths.get(dirName, "tmp", fileName)).normalize(); // tmp 경로

            // 최종 폴더 = {module}/{ownerId}/
            Path ownerDir = root.resolve(Paths.get(dirName, String.valueOf(ownerId))).normalize();
            Files.createDirectories(ownerDir); // 폴더 없으면 생성

            Path dst = ownerDir.resolve(fileName).normalize();

            // 안전장치: root 밖 이동 방지
            if (!dst.startsWith(ownerDir)) {
                throw new ApiException(ErrorCode.BAD_REQUEST, "잘못된 파일 경로");
            }

            if (Files.exists(src)) {
                Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING); // 이동
            }

            return "/uploads/" + dirName + "/" + ownerId + "/" + fileName; // URL 반환

        } catch (InvalidPathException e) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "잘못된 파일 경로"); // Path 형식 오류
        } catch (IOException e) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "파일 이동 실패"); // IO 오류
        }
    }

    // tmp 이미지들 확정 폴더로 이동 + URL 치환
    public String commitEditorImagesInContent(String content, String module, Long ownerId) {

        if (content == null || content.isBlank()) return content;

        List<String> tmpPaths = extractTmpEditorImagePaths(content, module);

        String updated = content;

        for (String tmpPath : tmpPaths) {
            String finalPath = moveTmpToOwnerDir(tmpPath, module, ownerId); // tmp → ownerDir 이동
            updated = updated.replace(tmpPath, finalPath); // URL 치환
        }

        // 유튜브 iframe 태그 감싼 a 태그 제거
        updated = normalizeQuillHtml(updated);

        return updated;
    }

    // 유튜브 iframe을 링크(a)로 감싸서 깨진 HTML 정리
    private String normalizeQuillHtml(String html) {
        if (html == null) return null;

        String out = html;

        // href 안에 iframe HTML이 통째로 들어가버렸을때
        out = out.replaceAll(
            "(?is)<a\\s+[^>]*href\\s*=\\s*\"<iframe[\\s\\S]*?</iframe>\"[^>]*>\\s*(<iframe[\\s\\S]*?</iframe>)\\s*</a>",
            "$1"
        );

        // iframe을 <a>로 감싼 경우 제거
        out = out.replaceAll(
            "(?is)<a[^>]*>\\s*(<iframe[\\s\\S]*?</iframe>)\\s*</a>",
            "$1"
        );

//        // 깨진 a 태그 찌꺼기 제거
//        out = out.replaceAll("(?is)\"\\s*>\\s*(?=<iframe)", "");
        
        // 깨진 <a> 태그 제거 (iframe 아닌 빈 <a>)
        out = out.replaceAll("(?is)<a[^>]*></a>", "");

        // 필요하면 공백/엔터 정리
        out = out.trim();

        return out;
    }

    // /uploads/... -> {app.upload-dir}/... 로 변환
    public Path resolveUploadPath(String storagePath) {

        if (storagePath == null || !storagePath.startsWith("/uploads/")) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "잘못된 storagePath"); // URL 검증
        }

        // "/uploads/" 제거 후 root에 붙임
        String relative = storagePath.substring("/uploads/".length());
        Path p = root.resolve(relative).normalize();

        // 안전장치: root 밖으로 못 나가게
        if (!p.startsWith(root)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "잘못된 파일 경로");
        }

        return p; // 실제 경로 반환
    }

    // 첨부파일 최종 저장: /uploads/{module}/{taskId}/attachments/{uuid}_{original}
    public StoredAttachment storeTaskAttachmentToTaskDir(MultipartFile file, String module, Long taskId) {

        // module은 "tasks"로 들어올 예정 (에디터 이미지랑 동일 모듈 톤)
        String dirName = editorDir(module);

        if (taskId == null || taskId <= 0) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "잘못된 taskId"); // taskId 검증
        }

        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "파일이 비었습니다."); // 파일 필수
        }

        // 개별 용량 제한 20MB
        long maxFileBytes = 20L * 1024 * 1024;
        if (file.getSize() > maxFileBytes) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "파일은 개별 20MB 이하만 가능합니다."); // 용량 체크
        }

        // 확장자 검사 (프론트 정책과 맞춤)
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null) {
            int idx = original.lastIndexOf('.');
            if (idx > 0 && idx < original.length() - 1) ext = original.substring(idx + 1).toLowerCase();
        }

        Set<String> allowedExts = Set.of(
            "pdf","doc","docx","xls","xlsx","ppt","pptx","hwp","txt","csv","zip",
            "png","jpg","jpeg","webp"
        );

        if (ext.isBlank() || !allowedExts.contains(ext)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "허용되지 않는 확장자입니다: " + (original == null ? "" : original)); // 확장자 체크
        }

        // 안전한 원본명
        String safeOriginal = (original == null || original.isBlank()) ? ("file." + ext) : original;

        // 경로 문자/제어문자 제거
        safeOriginal = safeOriginal.replaceAll("[\\\\/]+", "_");
        safeOriginal = safeOriginal.replaceAll("[\\p{Cntrl}]", "");
        safeOriginal = safeOriginal.replace("\"", ""); // 다운로드 헤더 안전

        // 너무 길면 뒤쪽만 남김(확장자 유지)
        if (safeOriginal.length() > 120) safeOriginal = safeOriginal.substring(safeOriginal.length() - 120);

        try {
            String storedName = UUID.randomUUID() + "_" + safeOriginal;

            // 최종 폴더: {uploadDir}/{module}/{taskId}/attachments/
            Path attachDir = root.resolve(Paths.get(dirName, String.valueOf(taskId), "attachments")).normalize();
            Files.createDirectories(attachDir);

            Path target = attachDir.resolve(storedName).normalize();

            // 안전장치
            if (!target.startsWith(attachDir)) {
                throw new ApiException(ErrorCode.BAD_REQUEST, "잘못된 파일 경로");
            }

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING); // 파일 저장
            }

            // DB에 저장할 경로: /uploads/{module}/{taskId}/attachments/{storedName}
            String storagePath = "/uploads/" + dirName + "/" + taskId + "/attachments/" + storedName;

            return new StoredAttachment(
                safeOriginal,
                storedName,
                file.getContentType(),
                file.getSize(),
                storagePath
            );

        } catch (IOException e) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "첨부 저장 실패"); // IO 오류
        }
    }
}
