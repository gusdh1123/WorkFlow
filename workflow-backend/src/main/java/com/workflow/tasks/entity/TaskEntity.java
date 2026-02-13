package com.workflow.tasks.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.workflow.department.entity.DepartmentEntity;
import com.workflow.tasks.enums.TaskPriority;
import com.workflow.tasks.enums.TaskStatus;
import com.workflow.tasks.enums.TaskVisibility;
import com.workflow.user.entity.UserEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tasks")
@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class TaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "제목은 필수입니다.")
    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TaskStatus status = TaskStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TaskVisibility visibility = TaskVisibility.DEPARTMENT;

    @Column(name="due_date")
    private LocalDate dueDate;

    @Column(name="hold_reason", columnDefinition = "TEXT")
    private String holdReason;

    @Column(name="cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name="is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @Column(name="deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private UserEntity createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private UserEntity assignee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_department_id", nullable = false)
    private DepartmentEntity ownerDepartment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_department_id", nullable = false)
    private DepartmentEntity workDepartment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;

        // 혹시라도 null로 들어오면 기본값 방어
        if (status == null) status = TaskStatus.TODO;
        if (priority == null) priority = TaskPriority.MEDIUM;
        if (visibility == null) visibility = TaskVisibility.DEPARTMENT;

        // 서비스에서 이미 owner/work 부서 세팅해서 덮어쓰지 않게
        if (createdBy != null && ownerDepartment == null) {
            ownerDepartment = createdBy.getDepartment();
        }
        if (workDepartment == null) {
            workDepartment = (assignee != null) ? assignee.getDepartment() : ownerDepartment;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
