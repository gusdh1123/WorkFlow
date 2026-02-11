package com.workflow.tasks.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.workflow.tasks.entity.TaskEntity;
import com.workflow.tasks.enums.TaskPriority;
import com.workflow.tasks.enums.TaskStatus;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        OffsetDateTime createdAt,

        Long createdById,
        String createdByName,
        String createdByDepartment,

        Long assigneeId,
        String assigneeName,
        String assigneeDepartment
) {
    public static TaskResponse from(TaskEntity t) {
    	
        return new TaskResponse(
                t.getId(),
                t.getTitle(),
                t.getDescription(),
                t.getStatus(),
                t.getPriority(),
                t.getDueDate(),
                t.getCreatedAt(),

                // createdBy
                t.getCreatedBy() != null ? t.getCreatedBy().getId() : null,
                t.getCreatedBy() != null ? t.getCreatedBy().getName() : null,
                t.getCreatedBy() != null ? t.getCreatedBy().getDepartment() : null,

                // assignee
                t.getAssignee() != null ? t.getAssignee().getId() : null,
                t.getAssignee() != null ? t.getAssignee().getName() : null,
                t.getAssignee() != null ? t.getAssignee().getDepartment() : null
        );
    }
}
