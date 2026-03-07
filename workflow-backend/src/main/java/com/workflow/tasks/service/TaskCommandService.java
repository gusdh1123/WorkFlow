package com.workflow.tasks.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workflow.attachment.repository.AttachmentRepository;
import com.workflow.attachment.service.AttachmentService;
import com.workflow.audit.enums.AuditActionType;
import com.workflow.audit.service.AuditLogService;
import com.workflow.common.exception.ApiException;
import com.workflow.common.exception.ErrorCode;
import com.workflow.common.file.FileStorageService;
import com.workflow.department.entity.DepartmentEntity;
import com.workflow.tasks.dto.TaskCreateRequest;
import com.workflow.tasks.dto.TaskResponse;
import com.workflow.tasks.dto.TaskUpdateRequest;
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
public class TaskCommandService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final AttachmentRepository attachmentRepository;    
    private final AuditLogService auditLogService;
    private final FileStorageService fileStorageService;
    private final AttachmentService attachmentService;

    // 업무 작성
    public TaskResponse create(TaskCreateRequest req, Long loginUserId) {

        if (loginUserId == null) throw new ApiException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        // 로그인 사용자 체크

        String title = req.titleTrimmed();
        // 제목 공백 제거

        if (title == null || title.isEmpty()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "제목은 필수입니다.");
        }
        // 제목 필수 체크

        UserEntity creator = userRepository.findById(loginUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "사용자가 존재하지 않습니다."));
        // 작성자 정보 조회

        UserEntity assignee = (req.assigneeId() == null) ? null
                : userRepository.findById(req.assigneeId())
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "담당자를 찾을 수 없습니다."));
        // 담당자 존재 여부 확인

        TaskVisibility visibility = req.visibilityOrDefault();
        TaskPriority priority = req.priorityOrDefault();
        // 가시성/우선순위 기본값 처리

        if (visibility == TaskVisibility.PUBLIC) {
            Role role = creator.getRole();
            if (role == null || (role != Role.MANAGER && role != Role.ADMIN)) {
                throw new ApiException(ErrorCode.UNAUTHORIZED, "PUBLIC 업무는 매니저/관리자만 생성할 수 있습니다.");
            }
        }
        // PUBLIC 업무 생성 권한 체크

        DepartmentEntity ownerDept = creator.getDepartment();
        DepartmentEntity workDept = (assignee != null) ? assignee.getDepartment() : ownerDept;
        // 소유 부서 vs 업무 부서 결정

        // 저장해서 아이디 생성
        TaskEntity task = TaskEntity.builder()
                .title(title)
                .description(req.description())   // 초기 템프 내용
                .status(TaskStatus.TODO)
                .priority(priority)
                .visibility(visibility)
                .dueDate(req.dueDate())
                .createdBy(creator)
                .assignee(assignee)
                .ownerDepartment(ownerDept)
                .workDepartment(workDept)
                .build();

        taskRepository.save(task); // DB에 저장 후 ID 확보

        // 아이디 기반으로 tmp → final/{taskId} 이동 + 본문 URL 치환
        String descriptionFinal = fileStorageService.commitEditorImagesInContent(task.getDescription(), "tasks", task.getId());
        task.setDescription(descriptionFinal);

        taskRepository.save(task);
        // 수정된 description 재저장

        return TaskResponse.from(task);
        // DTO로 변환 후 반환
    }

    // 업무 수정
    public TaskResponse update(Long taskId, TaskUpdateRequest req, Long loginUserId) {
        if (loginUserId == null) throw new ApiException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");

        TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "업무를 찾을 수 없습니다. id=" + taskId));

        UserEntity loginUser = userRepository.findById(loginUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "사용자가 존재하지 않습니다."));

        // 수정 권한 체크: 작성자, 담당자, 관리자/매니저 등
        if (!task.canEdit(loginUser)) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "수정 권한이 없습니다.");
        }

        // 제목 공백 제거
        String title = req.titleTrimmed();
        if (title == null || title.isEmpty()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "제목은 필수입니다.");
        }

        // 담당자 존재 여부 확인
        UserEntity assignee = (req.assigneeId() == null) ? null
                : userRepository.findById(req.assigneeId())
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "담당자를 찾을 수 없습니다."));


        if (req.visibility() == TaskVisibility.PUBLIC) {
            Role role = loginUser.getRole();
            if (role == null || (role != Role.MANAGER && role != Role.ADMIN)) {
                throw new ApiException(ErrorCode.UNAUTHORIZED, "PUBLIC 업무는 매니저/관리자만 수정할 수 있습니다.");
            }
        }
        
        // 수정 사유 확인
        if (req.reason() == null || req.reason().isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "수정 사유는 필수입니다.");
        }
        
        // 변경 전 값 저장 (AUDIT_LOGS)
        String oldTitle = task.getTitle();
        Long oldAssigneeId = task.getAssignee() != null ? task.getAssignee().getId() : null;
        LocalDate oldDueDate = task.getDueDate();
        String oldDescription = task.getDescription();
        TaskVisibility oldVisibility = task.getVisibility();
        TaskPriority oldPriority = task.getPriority();
        TaskStatus oldStatus = task.getStatus();

        // 본문 description 처리
        String descriptionFinal = fileStorageService.commitEditorImagesInContent(req.description(), "tasks", task.getId());
        
        // TaskEntity 업데이트
        task.setTitle(title);
        task.setPriority(req.priority());
        task.setVisibility(req.visibility());
        task.setAssignee(assignee);
        task.setDueDate(req.dueDate());

        task.setDescription(descriptionFinal);

        taskRepository.save(task); // 업데이트 반영
        
        // 변경 이력 인서트
        auditLogService.saveTaskUpdateLogs(
                task,
                loginUser,
                req,
                oldTitle,
                oldAssigneeId,
                oldDueDate,
                oldDescription,
                oldVisibility,
                oldPriority,
                oldStatus,
                AuditActionType.TASK_UPDATE,
                req.reason() 
        );
        
        return TaskResponse.from(task);
    }

    // 소프트 삭제 메서드
    public TaskResponse softDeleteTask(Long taskId, Long loginUserId, String deleteReason) {

        if (loginUserId == null)
            throw new ApiException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");

        // Task 조회
        TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "업무를 찾을 수 없습니다. id=" + taskId));

        // 로그인 유저 조회
        UserEntity loginUser = userRepository.findById(loginUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "사용자가 존재하지 않습니다."));

        // 삭제 권한 체크
        boolean canDelete = false;
        Role role = loginUser.getRole();
        if (role == Role.ADMIN) {
            canDelete = true;
        } else if (task.getCreatedBy().getId().equals(loginUserId)) {
            canDelete = true;
        } else if (task.getAssignee() != null && task.getAssignee().getId().equals(loginUserId)) {
            canDelete = true;
        } else if (role == Role.MANAGER 
                   && loginUser.getDepartment().getId().equals(task.getWorkDepartment().getId())) {
            canDelete = true;
        }

        if (!canDelete)
            throw new ApiException(ErrorCode.UNAUTHORIZED, "삭제 권한이 없습니다.");

        // Soft delete
        task.setDeleted(true);
        task.setDeletedAt(LocalDateTime.now());
        taskRepository.save(task);

        // Audit log 기록 (삭제 사유 포함)
        auditLogService.saveTaskUpdateLogs(
                task,
                loginUser,
                null, // 수정 DTO 없음
                task.getTitle(),
                task.getAssignee() != null ? task.getAssignee().getId() : null,
                task.getDueDate(),
                task.getDescription(),
                task.getVisibility(),
                task.getPriority(),
                task.getStatus(),
                AuditActionType.TASK_DELETE,
                deleteReason // 삭제 사유
        );

        // Attachment soft delete
        attachmentRepository.findByTaskIdAndIsDeletedFalse(taskId)
                .forEach(a -> attachmentService.softDelete(a.getId(), loginUserId));

        return TaskResponse.from(task);
    }

}
