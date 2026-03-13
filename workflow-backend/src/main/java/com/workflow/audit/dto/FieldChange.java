package com.workflow.audit.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FieldChange {

    private String field;       // 변경된 필드
    private String beforeValue; // 변경 전
    private String afterValue;  // 변경 후

}