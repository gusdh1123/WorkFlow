package com.workflow.common.file;

// 업로드/첨부파일 정보를 담는 DTO
// DB 저장용이 아니라, 서버에서 파일 처리 후 반환할 때 사용
public record StoredAttachment(
        String originalFilename, // 클라이언트에서 올라온 원본 파일명
        String storedFilename,   // 서버에 실제 저장된 이름(UUID + 원본)
        String contentType,      // MIME 타입 (ex: image/png, application/pdf)
        long sizeBytes,          // 파일 용량(bytes)
        String storagePath       // 서버/클라이언트에서 접근 가능한 URL 경로 (/uploads/... )
) {}
