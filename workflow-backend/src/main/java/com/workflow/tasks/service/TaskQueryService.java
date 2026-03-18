package com.workflow.tasks.service;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workflow.attachment.service.AttachmentService;
import com.workflow.common.exception.ApiException;
import com.workflow.common.exception.ErrorCode;
import com.workflow.tasks.dto.TaskResponse;
import com.workflow.tasks.entity.TaskEntity;
import com.workflow.tasks.enums.TaskStatus;
import com.workflow.tasks.enums.TaskVisibility;
import com.workflow.tasks.repasitory.TaskRepository;
import com.workflow.user.entity.UserEntity;
import com.workflow.user.enums.Role;
import com.workflow.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskQueryService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final AttachmentService attachmentService;

    // 업무 목록 조회
    public Page<TaskResponse> list(String scope, TaskStatus status, Long userId, Long deptId, int page, int size, String sort) {

        // 로그인 사용자 확인
        if (userId == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        // 기본 scope 설정
        if (scope == null || scope.isBlank()) scope = "all";

        // priorityDesc 여부 확인
        boolean usePrioritySort = "priorityDesc".equals(sort);

        
        // Pageable 객체 생성
        Pageable pageable;
        
        // "priorityDesc" 정렬일 경우, DB 쿼리에서 우선순위 정렬을 처리하기 때문에
        // Pageable에는 Sort를 따로 지정하지 않고 페이지 번호와 사이즈만 설정
        // 그 외의 sort 조건일 경우, Sort 객체를 생성해 PageRequest에 적용
        if ("priorityDesc".equals(sort)) {
            pageable = PageRequest.of(
                    Math.max(page, 0),
                    Math.min(Math.max(size, 1), 100)
            );
        } else {
            Sort sortObj = switch (sort) {
                case "createdAtDesc" -> Sort.by(Sort.Direction.DESC, "createdAt");
                case "dueDateAsc" -> Sort.by(Sort.Direction.ASC, "dueDate");
                case "dueDateDesc" -> Sort.by(Sort.Direction.DESC, "dueDate");
                default -> Sort.by(Sort.Direction.DESC, "createdAt");
            };
            pageable = PageRequest.of(
                    Math.max(page, 0),
                    Math.min(Math.max(size, 1), 100),
                    sortObj
            );
        }

        // 로그인 사용자 정보
        UserEntity me = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "사용자가 존재하지 않습니다."));
        Long myDeptId = me.getDepartment().getId();

        Page<TaskEntity> result;

        if (usePrioritySort) {
            // 우선순위 정렬
            if (me.getRole() == Role.ADMIN) {
                // ADMIN → 전체 업무 우선순위 정렬
                result = taskRepository.findAllByPriorityMappedDesc(scope, status, userId, deptId, myDeptId, pageable);
            } else {
                // 일반 사용자/팀장 → 자신이 볼 수 있는 업무만 우선순위 정렬
                result = taskRepository.findVisibleTasksPriorityDesc(scope, status, userId, deptId, pageable);
            }

        } else {
            // 일반 scope 기반 조회
            if (me.getRole() == Role.ADMIN) {
                Long targetDeptId = deptId;
                switch(scope) {
                    case "all" -> {
                        if (targetDeptId != null) {
                            result = (status == null)
                                ? taskRepository.findByWorkDepartment_IdAndIsDeletedFalse(targetDeptId, pageable)
                                : taskRepository.findByWorkDepartment_IdAndStatusAndIsDeletedFalse(targetDeptId, status, pageable);
                        } else {
                            result = (status == null)
                                ? taskRepository.findByIsDeletedFalse(pageable)
                                : taskRepository.findByIsDeletedFalseAndStatus(status, pageable);
                        }
                    }
                    case "public" -> {
                        if (targetDeptId != null) {
                            result = (status == null)
                                ? taskRepository.findPublicByDepartmentId(targetDeptId, pageable)
                                : taskRepository.findPublicByDepartmentIdAndStatus(targetDeptId, status, pageable);
                        } else {
                            result = (status == null)
                                ? taskRepository.findPublicOnly(pageable)
                                : taskRepository.findPublicOnlyByStatus(status, pageable);
                        }
                    }
                    case "team" -> {
                        result = (status == null)
                            ? taskRepository.findByWorkDepartment_IdAndIsDeletedFalse(myDeptId, pageable)
                            : taskRepository.findByWorkDepartment_IdAndStatusAndIsDeletedFalse(myDeptId, status, pageable);
                    }
                    case "created" -> {
                        result = (status == null)
                            ? taskRepository.findByIsDeletedFalseAndCreatedBy_Id(userId, pageable)
                            : taskRepository.findByIsDeletedFalseAndCreatedBy_IdAndStatus(userId, status, pageable);
                    }
                    case "assigned" -> {
                        result = (status == null)
                            ? taskRepository.findByIsDeletedFalseAndAssignee_Id(userId, pageable)
                            : taskRepository.findByIsDeletedFalseAndAssignee_IdAndStatus(userId, status, pageable);
                    }
                    case "private" -> {
                        if (targetDeptId != null) {
                            result = (status == null)
                                ? taskRepository.findByIsDeletedFalseAndVisibilityAndWorkDepartment_Id(TaskVisibility.PRIVATE, targetDeptId, pageable)
                                : taskRepository.findByIsDeletedFalseAndVisibilityAndWorkDepartment_IdAndStatus(TaskVisibility.PRIVATE, targetDeptId, status, pageable);
                        } else {
                            result = (status == null)
                                ? taskRepository.findByIsDeletedFalseAndVisibility(TaskVisibility.PRIVATE, pageable)
                                : taskRepository.findByIsDeletedFalseAndVisibilityAndStatus(TaskVisibility.PRIVATE, status, pageable);
                        }
                    }
                    default -> throw new ApiException(ErrorCode.BAD_REQUEST,
                            "scope 값이 올바르지 않습니다. (all|public|team|created|assigned)");
                }
            } else {
                // 일반 사용자/팀장
                switch (scope) {
                    case "all" -> result = (status == null)
                            ? taskRepository.findAllVisibleForUser(userId, myDeptId, pageable)
                            : taskRepository.findAllVisibleForUserByStatus(userId, myDeptId, status, pageable);
                    case "public" -> result = (status == null)
                            ? taskRepository.findPublicOnly(pageable)
                            : taskRepository.findPublicOnlyByStatus(status, pageable);
                    case "team" -> result = (status == null)
                            ? taskRepository.findTeamVisibleForUser(userId, myDeptId, pageable)
                            : taskRepository.findTeamVisibleForUserByStatus(userId, myDeptId, status, pageable);
                    case "created" -> result = (status == null)
                            ? taskRepository.findByIsDeletedFalseAndCreatedBy_Id(userId, pageable)
                            : taskRepository.findByIsDeletedFalseAndCreatedBy_IdAndStatus(userId, status, pageable);
                    case "assigned" -> result = (status == null)
                            ? taskRepository.findByIsDeletedFalseAndAssignee_Id(userId, pageable)
                            : taskRepository.findByIsDeletedFalseAndAssignee_IdAndStatus(userId, status, pageable);
                    case "private" -> result = (status == null)
                            ? taskRepository.findVisiblePrivateForUser(TaskVisibility.PRIVATE, userId, userId, pageable)
                            : taskRepository.findVisiblePrivateForUserByStatus(TaskVisibility.PRIVATE, status, userId, userId, pageable);
                    default -> throw new ApiException(ErrorCode.BAD_REQUEST,
                            "scope 값이 올바르지 않습니다. (all|public|team|created|assigned)");
                }
            }
        }

        // TaskEntity → TaskResponse 매핑, attachments count만 포함
        return result.map(t -> {
            long cnt = attachmentService.countActiveByTask(t.getId());
            return TaskResponse.from(t, cnt);
        });
    }

    // 업무 상세 조회
    public TaskResponse detail(Long taskId, Long userId) {

    	// 로그인 체크
        if (userId == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        // 사용자 부서 ID
        UserEntity me = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "사용자가 존재하지 않습니다."));
        Long myDeptId = me.getDepartment().getId();
        
        
        // Task 조회: 어드민 체크
        TaskEntity task;
        
        if(me.getRole() == Role.ADMIN) {
        task = taskRepository.findByIdAndIsDeletedFalse(taskId)
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "업무를 찾을 수 없습니다."));
        }else {
        	
        // 접근 가능한 Task 상세 조회
        task = taskRepository.findDetailVisibleForUser(taskId, userId, myDeptId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "업무를 찾을 수 없습니다."));
        }
        
        // 첨부 목록 로딩
        var attachments = attachmentService.listByTask(taskId);

        // TaskResponse DTO 반환, 첨부 포함
        return TaskResponse.from(task, attachments);
    }

    // KPI 출력
    public Map<String, Map<String, Long>> kpi(Long userId) {

        Map<String, Long> assigned = new LinkedHashMap<>();
        Map<String, Long> created = new LinkedHashMap<>();
        // LinkedHashMap: 삽입 순서 유지 (출력 순서 일관성)

        for (TaskStatus status : EnumSet.allOf(TaskStatus.class)) {
            assigned.put(status.name(),
                    taskRepository.countByIsDeletedFalseAndAssignee_IdAndStatus(userId, status));
            created.put(status.name(),
                    taskRepository.countByIsDeletedFalseAndCreatedBy_IdAndStatus(userId, status));
        }

        return Map.of(
                "assigned", assigned,
                "created", created
        );
    }

}
