package com.workflow.tasks.dto;

import java.time.LocalDate;

import com.workflow.tasks.enums.TaskPriority;
import com.workflow.tasks.enums.TaskVisibility;

public record TaskCreateRequest(
    String title,               // 업무 제목, 필수 입력
    String description,         // 업무 상세 내용, 선택 입력
    TaskPriority priority,      // 우선순위, null일 경우 기본 MEDIUM
    TaskVisibility visibility,  // 공개 범위, null일 경우 기본 DEPARTMENT
    Long assigneeId,            // 담당자 사용자 ID, null 가능
    LocalDate dueDate           // 마감일, null 가능
) {

    // 제목 앞뒤 공백 제거
    public String titleTrimmed() {
        return title == null ? null : title.trim();
    }

    // 우선순위가 null이면 MEDIUM으로 반환
    public TaskPriority priorityOrDefault() {
        return priority != null ? priority : TaskPriority.MEDIUM;
    }

    // 공개 범위가 null이면 DEPARTMENT으로 반환
    public TaskVisibility visibilityOrDefault() {
        return visibility != null ? visibility : TaskVisibility.DEPARTMENT;
    }
}
