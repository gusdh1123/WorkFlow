package com.workflow.attachment.dto;

import java.nio.file.Path;

// Attachment 다운로드 정보 DTO
public record DownloadInfo(
        String originalFilename,  // 원본 파일명
        String contentType,       // MIME 타입
        Path filePath             // 실제 서버 경로
) {}
