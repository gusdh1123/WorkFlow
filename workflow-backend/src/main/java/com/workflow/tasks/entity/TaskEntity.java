package com.workflow.tasks.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import com.workflow.tasks.enums.TaskPriority;
import com.workflow.tasks.enums.TaskStatus;
import com.workflow.user.entity.UserEntity;
import com.workflow.user.enums.Role;
import com.workflow.user.enums.UserStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.var;

@Entity
@Table(name = "tasks")
@Getter @Setter
@AllArgsConstructor
//(access = AccessLevel.PROTECTED) =
//파리미터 없는 기본 생성자를 protected 접근제어자로 만들겠다. 
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class TaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // BIGSERIAL

    @NotBlank(message = "제목은 필수입니다.")
    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "상태(status)는 필수입니다.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TaskPriority priority;

    @Column(name="due_date")
    private LocalDate dueDate;

    @Column(name="hold_reason", columnDefinition = "TEXT")
    private String holdReason;

    @Column(name="cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name="is_deleted", nullable = false)
    private boolean isDeleted;

    @Column(name="deleted_at")
    private OffsetDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private UserEntity createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private UserEntity assignee;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        var now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
    	updatedAt = OffsetDateTime.now();
    }
}
