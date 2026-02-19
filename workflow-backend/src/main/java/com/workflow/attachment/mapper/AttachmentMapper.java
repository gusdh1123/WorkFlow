package com.workflow.attachment.mapper;

import com.workflow.attachment.dto.AttachmentResponse;
import com.workflow.attachment.entity.AttachmentEntity;

// Attachment Entity → DTO 변환용 Mapper
// Service 내부 private 메서드에서 분리
// 재사용 가능, 다른 Service/Controller에서도 활용 가능
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
