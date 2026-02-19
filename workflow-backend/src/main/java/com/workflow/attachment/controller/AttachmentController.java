package com.workflow.attachment.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.workflow.attachment.service.AttachmentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AttachmentController {

    private final AttachmentService attachmentService;

    // 첨부파일 업로드 (특정 taskId에 귀속)
    @PostMapping(value = "/tasks/{taskId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@PathVariable("taskId") Long taskId,
                                    @RequestParam("files") List<MultipartFile> files,
                                    Authentication auth) {

        // 현재 로그인한 사용자의 ID 추출
        Long uploaderId = Long.valueOf(auth.getName());

        // AttachmentService에서 실제 파일 저장 처리 후 결과 반환
        return ResponseEntity.ok(attachmentService.uploadToTask(taskId, uploaderId, files));
    }

    // 첨부파일 삭제 (soft delete)
    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<?> delete(@PathVariable("attachmentId") Long attachmentId,
                                    Authentication auth) {

        Long requesterId = Long.valueOf(auth.getName());

        // 실제 삭제 로직은 softDelete 메서드에서 처리 (DB flag 변경)
        attachmentService.softDelete(attachmentId, requesterId);

        return ResponseEntity.ok().build();
    }

    // 첨부파일 다운로드
    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> download(@PathVariable("attachmentId") Long attachmentId,
                                             Authentication auth) throws Exception {

        Long requesterId = Long.valueOf(auth.getName());

        // 다운로드 정보 가져오기 (경로, 원본 이름, MIME 타입 등)
        var info = attachmentService.getDownloadInfo(attachmentId, requesterId);

        Resource resource = new UrlResource(info.filePath().toUri());

        if (!resource.exists()) {
            // 파일이 존재하지 않으면 404 반환
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String filename = info.originalFilename();
        if (filename == null || filename.isBlank()) filename = "file";

        // 구형 브라우저용 안전한 파일명 처리
        String fallback = filename
                .replaceAll("[\\\\/]+", "_")           // 경로 문자를 _로 대체
                .replaceAll("[\\p{Cntrl}\"]", "")      // 제어 문자 제거
                .replaceAll("[^\\x20-\\x7E]", "_");    // ASCII 아닌 문자는 _로
        if (fallback.isBlank()) fallback = "file";

        // UTF-8로 인코딩 (스페이스는 %20으로 변환)
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        // Content-Disposition 헤더 설정 (RFC 5987 지원)
        String disposition =
                "attachment; filename=\"" + fallback + "\"; filename*=UTF-8''" + encoded;

        // MIME 타입 기본값 설정
        MediaType ct = MediaType.APPLICATION_OCTET_STREAM;
        if (info.contentType() != null && !info.contentType().isBlank()) {
            try { 
                ct = MediaType.parseMediaType(info.contentType()); 
            } catch (Exception ignored) {
                // 잘못된 MIME 타입일 경우 무시하고 octet-stream 유지
            }
        }

        // ResponseEntity 반환: 파일 바디 + Content-Disposition + Content-Type
        return ResponseEntity.ok()
                .contentType(ct)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(resource);
    }
}
