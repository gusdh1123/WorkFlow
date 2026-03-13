package com.workflow.audit.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AuditLogGroupedResponse {

    private String actorName;           // 수정자
    private LocalDateTime modifiedAt;   // 수정 시각
    private List<String> changedFields; // 변경된 필드 목록 (status 포함)
    private String reason;              // 수정/삭제 사유

    private List<FieldChange> changes;  // 변경 필드 리스트   

    private boolean isRecent;           // 최신 3개 표시용 플래그

}