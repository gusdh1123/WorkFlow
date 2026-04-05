package com.workflow.attachment.dto;

public record AttachmentResponse(
        Long id,
        String originalFilename,
        String contentType,
        Long sizeBytes,
        String storagePath
) {}