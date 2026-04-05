package com.workflow.attachment.mapper;

import com.workflow.attachment.dto.AttachmentResponse;
import com.workflow.attachment.entity.AttachmentEntity;

public class AttachmentMapper {
    public static AttachmentResponse toResponse(AttachmentEntity a) {
        return new AttachmentResponse(
                a.getId(),
                a.getOriginalFilename(),
                a.getContentType(),
                a.getSizeBytes(),
                a.getStoragePath()
        );
    }
}