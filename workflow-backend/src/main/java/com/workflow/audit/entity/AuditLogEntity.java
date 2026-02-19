package com.workflow.audit.entity;

import java.time.LocalDateTime;

import com.workflow.tasks.entity.TaskEntity;
import com.workflow.user.entity.UserEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
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

    @Column(name="action_type", nullable = false, length = 30)
    private String actionType; // 동작 종류, 예: CREATE, UPDATE, DELETE

    @Column(name="field_name", length = 50)
    private String fieldName; // 수정된 컬럼명 (null 가능, 전체 삭제 등에는 null)

    @Column(name="before_value", columnDefinition = "TEXT")
    private String beforeValue; // 변경 전 값, 큰 텍스트 저장 가능

    @Column(name="after_value", columnDefinition = "TEXT")
    private String afterValue; // 변경 후 값, 큰 텍스트 저장 가능

    @Column(columnDefinition = "TEXT")
    private String reason; // 변경 사유, 필요 시 작성

    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now(); // 생성 시점 기록, 기본값 현재 시각

    // 엔티티가 처음 저장될 때 자동으로 생성 시각 세팅
    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
