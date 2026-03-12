package com.workflow.audit.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.workflow.audit.dto.AuditLogGroupedResponse;
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


    // 이력 저장
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

    // 이력 저장 빌더
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
    
    // 이력 조회
    // 특정 Task의 Audit Log를 최신 순으로 그룹핑해서 반환
    // 같은 사용자 + 같은 시각(초 단위)로 묶음
    // 최신 3개는 isRecent=true, 나머지는 isRecent=false
    public List<AuditLogGroupedResponse> getGroupedAuditLogs(Long taskId) {
    	
    	// 특정 Task의 Audit Log를 최신 순으로 조회
        List<AuditLogEntity> logs = auditLogRepository.findByTaskIdWithActor(taskId);

        // 이력이 없는 경우, 빈 리스트 반환
        if (logs == null || logs.isEmpty()) {
            return List.of();
        }

        // actorId + 초 단위 timestamp로 그룹핑
        Map<String, AuditLogGroupedResponse> grouped = new LinkedHashMap<>();

        for (AuditLogEntity log : logs) {
            // 안전 체크: actor나 생성 시각이 없으면 건너뛰기
            if (log.getActor() == null || log.getCreatedAt() == null) continue;

            // 키 생성: 사용자ID + 초 단위 생성 시각
            String key = log.getActor().getId() + "_" + log.getCreatedAt().truncatedTo(ChronoUnit.SECONDS);

            // 그룹에 없으면 새 DTO 생성 (Builder 사용)
            grouped.computeIfAbsent(key, k -> AuditLogGroupedResponse.builder()
                    .actorName(log.getActor().getName())   // 수정자 이름
                    .modifiedAt(log.getCreatedAt())        // 수정 시각
                    .changedFields(new ArrayList<>())      // 변경된 필드 목록
                    .reason(log.getReason())               // 수정/삭제 사유
                    .build()
            );

            AuditLogGroupedResponse dto = grouped.get(key);

            // 변경된 필드 기록
            dto.getChangedFields().add(log.getFieldName());

            // 상태 변경 기록 (status 필드인 경우만)
            if ("status".equals(log.getFieldName())) {
                dto.setStatusBefore(log.getBeforeValue());
                dto.setStatusAfter(log.getAfterValue());
            }
        }

        // Map -> List로 변환
        List<AuditLogGroupedResponse> result = new ArrayList<>(grouped.values());

        // 최신 3개는 isRecent = true, 나머지는 false
        for (int i = 0; i < result.size(); i++) {
            result.get(i).setRecent(i < 3);
        }

        return result;
    }
}