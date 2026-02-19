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
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 프록시용
@Builder
public class TaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK, 업무 고유 ID

    @NotBlank(message = "제목은 필수입니다.")
    @Column(nullable = false, length = 200)
    private String title; // 업무 제목

    @Column(columnDefinition = "TEXT")
    private String description; // 업무 상세 설명

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TaskStatus status = TaskStatus.TODO; // 업무 상태 (TODO, IN_PROGRESS, DONE 등)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TaskPriority priority = TaskPriority.MEDIUM; // 업무 우선순위

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TaskVisibility visibility = TaskVisibility.DEPARTMENT; // 공개 범위

    @Column(name = "due_date")
    private LocalDate dueDate; // 마감일

    @Column(name = "hold_reason", columnDefinition = "TEXT")
    private String holdReason; // ON_HOLD 상태일 때 사유

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason; // CANCELED 상태일 때 사유

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false; // soft delete 여부

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt; // 삭제 시점 (soft delete)

    // 업무 작성자
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private UserEntity createdBy;

    // 담당자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private UserEntity assignee;

    // 업무 소유 부서 (작성자 소속 부서)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_department_id", nullable = false)
    private DepartmentEntity ownerDepartment;

    // 실제 업무 처리 부서 (담당자 또는 다른 부서)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_department_id", nullable = false)
    private DepartmentEntity workDepartment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // 생성 시점

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // 최종 수정 시점

    // 엔티티 저장 직전 기본값 및 부서 세팅
    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;

        // null 방어용 기본값 세팅
        if (status == null) status = TaskStatus.TODO;
        if (priority == null) priority = TaskPriority.MEDIUM;
        if (visibility == null) visibility = TaskVisibility.DEPARTMENT;

        // owner/work 부서 기본 세팅
        if (createdBy != null && ownerDepartment == null) {
            ownerDepartment = createdBy.getDepartment();
        }
        if (workDepartment == null) {
            workDepartment = (assignee != null) ? assignee.getDepartment() : ownerDepartment;
        }
    }

    // 엔티티 수정 직전 updatedAt 갱신
    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now(); // 수정 시 updatedAt 갱신
    }
}
