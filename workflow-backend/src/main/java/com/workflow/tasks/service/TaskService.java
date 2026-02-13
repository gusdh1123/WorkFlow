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

import com.workflow.common.exception.BadRequestException;
import com.workflow.common.exception.NotFoundException;
import com.workflow.common.exception.UnauthorizedException;
import com.workflow.department.entity.DepartmentEntity;
import com.workflow.tasks.dto.TaskCreateRequest;
import com.workflow.tasks.dto.TaskResponse;
import com.workflow.tasks.entity.TaskEntity;
import com.workflow.tasks.enums.TaskPriority;
import com.workflow.tasks.enums.TaskStatus;
import com.workflow.tasks.enums.TaskVisibility;
import com.workflow.tasks.repasitory.TaskRepository;
import com.workflow.user.entity.UserEntity;
import com.workflow.user.enums.Role;
import com.workflow.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskResponse create(TaskCreateRequest req, Long loginUserId) {

        if (loginUserId == null) throw new UnauthorizedException("로그인이 필요합니다.");

        String title = req.titleTrimmed();

        if (title == null || title.isEmpty()) {
            throw new BadRequestException("제목은 필수입니다.");
        }

        UserEntity creator = userRepository.findById(loginUserId)
                .orElseThrow(() -> new UnauthorizedException("사용자가 존재하지 않습니다."));

        UserEntity assignee = (req.assigneeId() == null) ? null
                : userRepository.findById(req.assigneeId())
                    .orElseThrow(() -> new NotFoundException("담당자를 찾을 수 없습니다."));

        TaskVisibility visibility = req.visibilityOrDefault();
        TaskPriority priority = req.priorityOrDefault();

        if (visibility == TaskVisibility.PUBLIC) {
        	Role role = creator.getRole();
        	if (role == null || (role != Role.MANAGER && role != Role.ADMIN)) {
        		throw new UnauthorizedException("PUBLIC 업무는 매니저/관리자만 생성할 수 있습니다.");
        	}
        }

        DepartmentEntity ownerDept = creator.getDepartment();
        DepartmentEntity workDept = (assignee != null) ? assignee.getDepartment() : ownerDept;

        TaskEntity task = TaskEntity.builder()
                .title(title)
                .description(req.description())
                .status(TaskStatus.TODO)
                .priority(priority)
                .visibility(visibility)
                .dueDate(req.dueDate())
                .createdBy(creator)
                .assignee(assignee)
                .ownerDepartment(ownerDept)
                .workDepartment(workDept)
                .build();

        return TaskResponse.from(taskRepository.save(task));
    }

    // 업무 목록
    @Transactional(readOnly = true)
    public Page<TaskResponse> list(String scope, TaskStatus status, Long userId, int page, int size) {

        if (userId == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        if (scope == null || scope.isBlank()) scope = "all";

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        UserEntity me = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("사용자가 존재하지 않습니다."));

        Long myDeptId = me.getDepartment().getId();

        Page<TaskEntity> result;

        // 자바 14이상부터 추가된 switch expression
        // 예전 스위치와 달리 브레이크를 쓸 필요없음
        // ex)
        // switch (result) {
        // case "1" -> 코드 작성
        // }
        switch (scope) {

            // 전체 업무: 내가 볼 수 있는 모든 업무
            // PUBLIC + (DEPARTMENT면 내 부서) + (PRIVATE면 내가 작성자/담당자)
            case "all" -> result = (status == null)
                    ? taskRepository.findAllVisibleForUser(userId, myDeptId, pageable)
                    : taskRepository.findAllVisibleForUserByStatus(userId, myDeptId, status, pageable);

            // 전사 업무: PUBLIC만
            case "public" -> result = (status == null)
                    ? taskRepository.findPublicOnly(pageable)
                    : taskRepository.findPublicOnlyByStatus(status, pageable);

            // 우리팀 업무: 우리 팀만 + PRIVATE는 (작성자/담당자=나)만 예외 허용 (B안)
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

            default -> throw new BadRequestException("scope 값이 올바르지 않습니다. (all|public|team|created|assigned)");
        }

        return result.map(TaskResponse::from);
    }
    
    @Transactional(readOnly = true)
    public TaskResponse detail(Long taskId, Long userId) {
    	
    	if(userId == null) {
    		throw new UnauthorizedException("로그인이 필요합니다.");
    	}
    	
    	UserEntity me = userRepository.findById(userId)
    			.orElseThrow(() -> new UnauthorizedException("사용자가 존재하지 않습니다."));
    	
    	Long myDeptId = me.getDepartment().getId();
    	
    	TaskEntity task = taskRepository.findDetailVisibleForUser(taskId, userId, myDeptId)
    			.orElseThrow(() -> new UnauthorizedException("업무를 찾을 수 없습니다."));
    	
    	return TaskResponse.from(task);
    }
    
    // kpi 출력
    @Transactional(readOnly = true)
    public Map<String, Map<String, Long>> kpi(Long userId) {

    	// HashMap: 순서 의미 없음
    	// LinkedHashMap: 삽입 순서 대로
    	// TreeHashMap: 알파벳 순서 대로
        Map<String, Long> assigned = new LinkedHashMap<>();
        Map<String, Long> created = new LinkedHashMap<>();

        // EnumSet: 이넘 반복할때 사용
        // allOf: 해당 이넘의 모든 값 가져옴.
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
