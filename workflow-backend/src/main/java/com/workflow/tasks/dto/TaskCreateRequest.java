package com.workflow.tasks.dto;

import java.time.LocalDate;

import com.workflow.tasks.enums.TaskPriority;
import com.workflow.tasks.enums.TaskVisibility;

public record TaskCreateRequest(
    String title,
    String description,
    TaskPriority priority,
    TaskVisibility visibility,
    Long assigneeId,
    LocalDate dueDate
) {
    public String titleTrimmed() {
        return title == null ? null : title.trim();
    }

    public TaskPriority priorityOrDefault() {
        return priority != null ? priority : TaskPriority.MEDIUM;
    }

    public TaskVisibility visibilityOrDefault() {
        return visibility != null ? visibility : TaskVisibility.DEPARTMENT;
    }
}
