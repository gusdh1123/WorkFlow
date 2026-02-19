package com.workflow.comment.entity;

import java.time.LocalDateTime;

import com.workflow.tasks.entity.TaskEntity;
import com.workflow.user.entity.UserEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
public class CommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private TaskEntity task; // 댓글이 속한 업무

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private UserEntity author; // 작성자

    @Column(name="parent_id")
    private Long parentId; // 대댓글 참조용 ID

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content; // 댓글 내용

    @Column(name="is_deleted", nullable = false)
    private boolean isDeleted = false; // soft delete 여부

    @Column(name="deleted_at")
    private LocalDateTime deletedAt; // 삭제 시각

    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name="updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
