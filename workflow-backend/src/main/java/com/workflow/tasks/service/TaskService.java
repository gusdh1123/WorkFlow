package com.workflow.tasks.service;

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
import com.workflow.tasks.dto.TaskCreateRequest;
import com.workflow.tasks.dto.TaskResponse;
import com.workflow.tasks.entity.TaskEntity;
import com.workflow.tasks.enums.TaskStatus;
import com.workflow.tasks.repasitory.TaskRepository;
import com.workflow.user.entity.UserEntity;
import com.workflow.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskResponse create(TaskCreateRequest req, Long loginUserId) {

        if (loginUserId == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        UserEntity creator = userRepository.findById(loginUserId)
                .orElseThrow(() -> new UnauthorizedException("사용자가 존재하지 않습니다."));

        UserEntity assignee = (req.assigneeId() == null)
                ? null
                : userRepository.findById(req.assigneeId())
                    .orElseThrow(() -> new NotFoundException("담당자를 찾을 수 없습니다."));
        
        TaskStatus status = (req.status() == null) ? TaskStatus.TODO : req.status();

        TaskEntity task = TaskEntity.builder()
                .title(req.title() == null ? null : req.title().trim()) // 앞뒤 공백 제거
                .description(req.description())
                .status(status)
                .priority(req.priority()) // null OK
                .dueDate(req.dueDate())
                .createdBy(creator)
                .assignee(assignee)
                .build();

        return TaskResponse.from(taskRepository.save(task));
    }


    
    @Transactional(readOnly = true)
    public Page<TaskResponse> list(String scope, TaskStatus status, Long userId, int page, int size) {
        if (scope == null || scope.isBlank()) scope = "all";

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<TaskEntity> result;

        switch (scope) {
            case "created" -> result = (status == null)
                    ? taskRepository.findByIsDeletedFalseAndCreatedBy_Id(userId, pageable)
                    : taskRepository.findByIsDeletedFalseAndCreatedBy_IdAndStatus(userId, status, pageable);

            case "assigned" -> result = (status == null)
                    ? taskRepository.findByIsDeletedFalseAndAssignee_Id(userId, pageable)
                    : taskRepository.findByIsDeletedFalseAndAssignee_IdAndStatus(userId, status, pageable);

            case "all" -> result = (status == null)
                    ? taskRepository.findByIsDeletedFalse(pageable)
                    : taskRepository.findByIsDeletedFalseAndStatus(status, pageable);

            default -> throw new BadRequestException("scope 값이 올바르지 않습니다. (all|created|assigned)");
        }

        return result.map(TaskResponse::from);
    }
    
    @Transactional(readOnly = true)
    public Map<String, Long> kpi(Long userId) {

        return Map.of(
                "TODO", taskRepository.countByIsDeletedFalseAndAssignee_IdAndStatus(userId, TaskStatus.TODO),
                "IN_PROGRESS", taskRepository.countByIsDeletedFalseAndAssignee_IdAndStatus(userId, TaskStatus.IN_PROGRESS),
                "REVIEW", taskRepository.countByIsDeletedFalseAndAssignee_IdAndStatus(userId, TaskStatus.REVIEW),
                "DONE", taskRepository.countByIsDeletedFalseAndAssignee_IdAndStatus(userId, TaskStatus.DONE),
                "ON_HOLD", taskRepository.countByIsDeletedFalseAndAssignee_IdAndStatus(userId, TaskStatus.ON_HOLD),
                "CANCELED", taskRepository.countByIsDeletedFalseAndAssignee_IdAndStatus(userId, TaskStatus.CANCELED)
        );
    }
}
