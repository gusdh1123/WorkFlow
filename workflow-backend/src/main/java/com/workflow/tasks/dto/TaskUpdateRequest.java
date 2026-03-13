package com.workflow.tasks.dto;

import java.time.LocalDate;
import java.util.List;

import com.workflow.tasks.enums.TaskPriority;
import com.workflow.tasks.enums.TaskStatus;
import com.workflow.tasks.enums.TaskVisibility;

public record TaskUpdateRequest(
    String title,
    String description,
    TaskPriority priority,
    TaskVisibility visibility,
    TaskStatus status,
    Long assigneeId,
    LocalDate dueDate,
    String reason,
    List<Long> addedAttachmentIds,   // 새로 추가된 첨부
    List<Long> deletedAttachmentIds,  // 삭제된 첨부
    Long version // 서버와 비교할 version
) {

    public String titleTrimmed() {
        return title == null ? null : title.trim();
    }

    public String descriptionTrimmed() {
        return description == null ? null : description.trim();
    }

    public String reasonTrimmed() {
        return reason == null ? null : reason.trim();
    }

}