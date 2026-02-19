package com.workflow.common.upload;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.workflow.common.file.FileStorageService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

// 이미지 업로드용 REST 컨트롤러
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/uploads")
public class UploadController {

    private final FileStorageService fileStorageService; // 실제 파일 저장/처리 서비스

    // 에디터에서 업로드한 이미지를 tmp 폴더에 저장
    @PostMapping("/images")
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file, // 업로드 파일
            HttpServletRequest req // 요청 정보 (호스트/포트 등)
    ) {

        // tmp 폴더에 파일 저장 후 상대 경로 반환
        String path = fileStorageService.storeEditorImageToTmp(file, "tasks");

        // 클라이언트에서 접근 가능한 전체 URL 생성
        String base = req.getScheme() + "://" + req.getServerName() +
                ((req.getServerPort() == 80 || req.getServerPort() == 443)
                        ? "" // 기본 HTTP/HTTPS 포트면 생략
                        : ":" + req.getServerPort());

        // {"url": "http://host:port/uploads/tasks/tmp/파일명"} 형식으로 반환
        return ResponseEntity.ok(Map.of("url", base + path));
    }
}
