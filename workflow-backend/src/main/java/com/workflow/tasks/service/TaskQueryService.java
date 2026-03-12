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
    public Page<TaskResponse> list(String scope, TaskStatus status, Long userId, Long deptId, int page, int size) {

        if (userId == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        // 로그인 사용자 확인

        if (scope == null || scope.isBlank()) scope = "all";
        // 기본 scope 설정

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        // 페이지네이션, 최소 1~최대 100 제한, 최신순 정렬

        UserEntity me = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "사용자가 존재하지 않습니다."));
        Long myDeptId = me.getDepartment().getId();
        // 로그인 사용자 정보 + 부서 ID

        Page<TaskEntity> result;

        if (me.getRole() == Role.ADMIN) {
        	Long targetDeptId = deptId; // URL에서 받은 부서 필터
            switch(scope) {
                case "all" -> {
                    if (deptId != null) {
                        result = (status == null)
                            ? taskRepository.findByWorkDepartment_IdAndIsDeletedFalse(deptId, pageable)
                            : taskRepository.findByWorkDepartment_IdAndStatusAndIsDeletedFalse(deptId, status, pageable);
                    } else {
                        result = (status == null)
                            ? taskRepository.findByIsDeletedFalse(pageable)
                            : taskRepository.findByIsDeletedFalseAndStatus(status, pageable);
                    }
                }
                case "public" -> {
                    if (targetDeptId != null) {
                        // 부서 + public만 조회
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
                        // 부서 필터 적용
                        result = (status == null)
                            ? taskRepository.findByIsDeletedFalseAndVisibilityAndWorkDepartment_Id(TaskVisibility.PRIVATE, targetDeptId, pageable)
                            : taskRepository.findByIsDeletedFalseAndVisibilityAndWorkDepartment_IdAndStatus(TaskVisibility.PRIVATE, targetDeptId, status, pageable);
                    } else {
                        // 부서 필터 없이 전체 조회
                        result = (status == null)
                            ? taskRepository.findByIsDeletedFalseAndVisibility(TaskVisibility.PRIVATE, pageable)
                            : taskRepository.findByIsDeletedFalseAndVisibilityAndStatus(TaskVisibility.PRIVATE, status, pageable);
                    }
                }
                default -> throw new ApiException(ErrorCode.BAD_REQUEST,
                        "scope 값이 올바르지 않습니다. (all|public|team|created|assigned)");
            }
        
            
        } else {
            // 자바 14 이상부터 switch expression 사용
            switch (scope) {

                // 전체 업무: 내가 볼 수 있는 모든 업무
                case "all" -> result = (status == null)
                        ? taskRepository.findAllVisibleForUser(userId, myDeptId, pageable)
                        : taskRepository.findAllVisibleForUserByStatus(userId, myDeptId, status, pageable);

                // 전사 업무: PUBLIC만
                case "public" -> result = (status == null)
                        ? taskRepository.findPublicOnly(pageable)
                        : taskRepository.findPublicOnlyByStatus(status, pageable);

                // 우리팀 업무: 우리 팀만 + PRIVATE는 (작성자/담당자=나)만 예외 허용
                case "team" -> result = (status == null)
                        ? taskRepository.findTeamVisibleForUser(userId, myDeptId, pageable)
                        : taskRepository.findTeamVisibleForUserByStatus(userId, myDeptId, status, pageable);

                // 내가 만든 업무: 내가 작성자인 것만
                case "created" -> result = (status == null)
                        ? taskRepository.findByIsDeletedFalseAndCreatedBy_Id(userId, pageable)
                        : taskRepository.findByIsDeletedFalseAndCreatedBy_IdAndStatus(userId, status, pageable);

                // 담당 업무: 내가 담당자인 것만
                case "assigned" -> result = (status == null)
                        ? taskRepository.findByIsDeletedFalseAndAssignee_Id(userId, pageable)
                        : taskRepository.findByIsDeletedFalseAndAssignee_IdAndStatus(userId, status, pageable);
                
                case "private" ->
                    // 본인만 조회
                    result = (status == null)
                        ? taskRepository.findVisiblePrivateForUser(TaskVisibility.PRIVATE, userId, userId, pageable)
                        : taskRepository.findVisiblePrivateForUserByStatus(TaskVisibility.PRIVATE, status, userId, userId, pageable);

                default -> throw new ApiException(ErrorCode.BAD_REQUEST, "scope 값이 올바르지 않습니다. (all|public|team|created|assigned)");
                // 범위 값 검증
            }
        }

        return result.map(t -> {
            long cnt = attachmentService.countActiveByTask(t.getId());
            return TaskResponse.from(t, cnt); // 목록은 attachments 비우고 count만
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
