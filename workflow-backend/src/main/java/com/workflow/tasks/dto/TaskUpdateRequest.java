package com.workflow.tasks.dto;

import java.time.LocalDate;

import com.workflow.tasks.enums.TaskPriority;
import com.workflow.tasks.enums.TaskVisibility;

public record TaskUpdateRequest(
    String title,
    String description,
    TaskPriority priority,
    TaskVisibility visibility,
    Long assigneeId,
    LocalDate dueDate,
    String reason
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