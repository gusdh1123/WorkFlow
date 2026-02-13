package com.workflow.tasks.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.workflow.department.entity.DepartmentEntity;
import com.workflow.tasks.entity.TaskEntity;
import com.workflow.tasks.enums.TaskPriority;
import com.workflow.tasks.enums.TaskStatus;
import com.workflow.tasks.enums.TaskVisibility;
import com.workflow.user.entity.UserEntity;

public record TaskResponse(Long id, String title, String description, TaskStatus status, TaskPriority priority,
		TaskVisibility visibility, LocalDate dueDate, LocalDateTime createdAt,

		Long createdById, String createdByName, String createdByDepartmentName, String createdByDepartmentCode,

		Long assigneeId, String assigneeName, String assigneeDepartmentName, String assigneeDepartmentCode,

		String ownerDepartmentName, String ownerDepartmentCode, String workDepartmentName, String workDepartmentCode) {
	public static TaskResponse from(TaskEntity t) {

        UserEntity cb = t.getCreatedBy();
        UserEntity as = t.getAssignee();

        DepartmentEntity cbDept = (cb == null) ? null : cb.getDepartment();
        DepartmentEntity asDept = (as == null) ? null : as.getDepartment();

        DepartmentEntity ownerDept = t.getOwnerDepartment();
        DepartmentEntity workDept = t.getWorkDepartment();

        return new TaskResponse(
                t.getId(),
                t.getTitle(),
                t.getDescription(),
                t.getStatus(),
                t.getPriority(),
                t.getVisibility(),
                t.getDueDate(),
                t.getCreatedAt(),

                cb != null ? cb.getId() : null,
                cb != null ? cb.getName() : null,
                cbDept != null ? cbDept.getName() : null,
                cbDept != null ? cbDept.getCode() : null,

                as != null ? as.getId() : null,
                as != null ? as.getName() : null,
                asDept != null ? asDept.getName() : null,
                asDept != null ? asDept.getCode() : null,

                ownerDept != null ? ownerDept.getName() : null,
                ownerDept != null ? ownerDept.getCode() : null,
                workDept != null ? workDept.getName() : null,
                workDept != null ? workDept.getCode() : null
                		
                // 위와 같이 하는 이유는 아무 값도 안들어가는 순간 500이 터지기 때문에 값이 없으면 널을 넣는 것
                // 값이 없다는 걸 명시적으로 표현
                // Optional 방식으로 해도 됨
//              // 작성자
//              Optional.ofNullable(cb)
//              .map(UserEntity::getId)
//              .orElse(null),
//
//              Optional.ofNullable(cb)
//              .map(UserEntity::getName)
//              .orElse(null),
//
//              Optional.ofNullable(cb)
//      		.map(UserEntity::getDepartment)
//				.map(DepartmentEntity::getName)
//              .orElse(null),
//
//              Optional.ofNullable(cb)
//              .map(UserEntity::getDepartment)
//              .map(DepartmentEntity::getCode)
//              .orElse(null),
//
//              // 담당자
//              Optional.ofNullable(as)
//              .map(UserEntity::getId)
//              .orElse(null),
//
//              Optional.ofNullable(as)
//              .map(UserEntity::getName)
//              .orElse(null),
//
//              Optional.ofNullable(as)
//              .map(UserEntity::getDepartment)
//              .map(DepartmentEntity::getName)
//              .orElse(null),
//
//              Optional.ofNullable(as)
//              .map(UserEntity::getDepartment)
//              .map(DepartmentEntity::getCode)
//              .orElse(null),
//
//              // 소유 부서
//              Optional.ofNullable(t.getOwnerDepartment())
//              .map(DepartmentEntity::getName)
//              .orElse(null),
//
//              Optional.ofNullable(t.getOwnerDepartment())
//              .map(DepartmentEntity::getCode)
//              .orElse(null),
//
//              // 수행 부서
//              Optional.ofNullable(t.getWorkDepartment())
//              .map(DepartmentEntity::getName)
//              .orElse(null),
//
//              Optional.ofNullable(t.getWorkDepartment())
//              .map(DepartmentEntity::getCode)
//              .orElse(null)
        );
    }
}
