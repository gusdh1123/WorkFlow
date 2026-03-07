package com.workflow.audit.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.workflow.audit.entity.AuditLogEntity;
import com.workflow.audit.enums.AuditActionType;
import com.workflow.audit.repository.AuditLogRepository;
import com.workflow.tasks.dto.TaskUpdateRequest;
import com.workflow.tasks.entity.TaskEntity;
import com.workflow.tasks.enums.TaskPriority;
import com.workflow.tasks.enums.TaskStatus;
import com.workflow.tasks.enums.TaskVisibility;
import com.workflow.user.entity.UserEntity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;


    public void saveTaskUpdateLogs(
            TaskEntity task,
            UserEntity loginUser,
            TaskUpdateRequest req,  // 삭제 시 null
            String oldTitle,
            Long oldAssigneeId,
            LocalDate oldDueDate,
            String oldDescription,
            TaskVisibility oldVisibility,
            TaskPriority oldPriority,
            TaskStatus oldStatus,
            AuditActionType actionType,
            String deleteReason // 삭제 사유
    ) {
        List<AuditLogEntity> logs = new ArrayList<>();

        String finalReason = (req != null ? req.reasonTrimmed() : deleteReason);

        if (actionType == AuditActionType.TASK_DELETE) {
            // 삭제 시 한 줄로 기록
            String summary = String.format("제목: '%s' 삭제됨. 내용: %s", oldTitle, oldDescription);
            addLog(logs, task, loginUser, actionType, "task", summary, null, finalReason);
        } else {
            // 업데이트 기존 로직 그대로
            if (!Objects.equals(oldTitle, task.getTitle())) {
                addLog(logs, task, loginUser, actionType, "title", oldTitle, task.getTitle(), finalReason);
            }
            Long newAssigneeId = task.getAssignee() != null ? task.getAssignee().getId() : null;
            if (!Objects.equals(oldAssigneeId, newAssigneeId)) {
                addLog(logs, task, loginUser, actionType, "assignee_id",
                        String.valueOf(oldAssigneeId), String.valueOf(newAssigneeId), finalReason);
            }
            if (!Objects.equals(oldDueDate, task.getDueDate())) {
                addLog(logs, task, loginUser, actionType, "due_date",
                        String.valueOf(oldDueDate), String.valueOf(task.getDueDate()), finalReason);
            }
            if (!Objects.equals(oldDescription, task.getDescription())) {
                addLog(logs, task, loginUser, actionType, "description",
                        oldDescription, task.getDescription(), finalReason);
            }
            if (oldVisibility != task.getVisibility()) {
                addLog(logs, task, loginUser, actionType, "visibility",
                        String.valueOf(oldVisibility), String.valueOf(task.getVisibility()), finalReason);
            }
            if (oldPriority != task.getPriority()) {
                addLog(logs, task, loginUser, actionType, "priority",
                        String.valueOf(oldPriority), String.valueOf(task.getPriority()), finalReason);
            }
            if (oldStatus != task.getStatus()) {
                addLog(logs, task, loginUser, actionType, "status",
                        String.valueOf(oldStatus), String.valueOf(task.getStatus()), finalReason);
            }
        }

        if (!logs.isEmpty()) {
            auditLogRepository.saveAll(logs);
        }
    }

    private void addLog(List<AuditLogEntity> logs,
                        TaskEntity task,
                        UserEntity user,
                        AuditActionType actionType,
                        String field,
                        String oldValue,
                        String newValue,
                        String reason) {

        AuditLogEntity log = AuditLogEntity.builder()
                .task(task)
                .actor(user)
                .actionType(actionType)
                .fieldName(field)
                .beforeValue(oldValue)
                .afterValue(newValue)
                .reason(reason)
                .build();

        logs.add(log);
    }
}