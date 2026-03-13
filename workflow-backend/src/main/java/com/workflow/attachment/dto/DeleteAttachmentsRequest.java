package com.workflow.attachment.dto;

import java.util.List;

// 첨부파일 삭제 요청용 DTO
// - 클라이언트에서 삭제할 첨부파일 ID 리스트를 서버로 전달할 때 사용
public record DeleteAttachmentsRequest(
        List<Long> ids // 삭제할 첨부파일 ID
) {}