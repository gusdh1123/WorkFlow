package com.workflow.audit.entity;

import java.time.LocalDateTime;

import com.workflow.audit.enums.AuditActionType;
import com.workflow.tasks.entity.TaskEntity;
import com.workflow.user.entity.UserEntity;

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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@Builder	
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK, 단순 식별용

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private TaskEntity task; // 변경된 업무(Task) 연관, lazy로 불필요한 조인 방지

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private UserEntity actor; // 변경을 수행한 사용자, lazy 로딩

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    private AuditActionType actionType;

    @Column(name="field_name", length = 50)
    private String fieldName; // 수정된 컬럼명 (null 가능, 전체 삭제 등에는 null)

    @Column(name="before_value", columnDefinition = "TEXT")
    private String beforeValue; // 변경 전 값, 큰 텍스트 저장 가능

    @Column(name="after_value", columnDefinition = "TEXT")
    private String afterValue; // 변경 후 값, 큰 텍스트 저장 가능

    @Column(columnDefinition = "TEXT")
    private String reason; // 변경 사유, 필요 시 작성

    @Builder.Default
    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now(); // 생성 시점 기록, 기본값 현재 시각

    // 엔티티가 처음 저장될 때 자동으로 생성 시각 세팅
    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
