package com.workflow.attachment.dto;

// 첨부파일 응답 DTO
// DB나 서비스에서 Attachment 정보를 클라이언트로 전달할 때 사용
public record AttachmentResponse(
        Long id,                  // 첨부파일 고유 ID
        String originalFilename,  // 사용자가 업로드한 원본 파일명
        String contentType,       // MIME 타입 (ex: image/png, application/pdf)
        Long sizeBytes,           // 파일 크기 (바이트 단위)
        String storagePath        // 서버 저장 경로 또는 접근 가능한 URL (/uploads/...) 
) {}
