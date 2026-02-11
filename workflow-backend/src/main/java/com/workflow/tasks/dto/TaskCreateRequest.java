package com.workflow.tasks.dto;

import java.time.LocalDate;

import com.workflow.tasks.enums.TaskPriority;
import com.workflow.tasks.enums.TaskStatus;

public record TaskCreateRequest (
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        Long assigneeId
		) {}
