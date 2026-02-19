package com.workflow.attachment.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "attachments")
@Getter
@Setter
@NoArgsConstructor
public class AttachmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  
    // 첨부파일 고유 ID, DB PK

    @Column(name = "task_id", nullable = false)
    private Long taskId;  
    // 연관 업무(task) ID
    // tasks 테이블 ID(FK 역할)
    // 어떤 업무에 속하는 첨부인지 구분

    @Column(name = "uploader_id", nullable = false)
    private Long uploaderId;  
    // 업로더(user) ID
    // 첨부파일을 업로드한 사용자 식별용

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;  
    // 사용자가 업로드한 원본 파일명

    @Column(name = "stored_filename", nullable = false, length = 255)
    private String storedFilename;  
    // 서버 저장용 UUID 포함 이름
    // 실제 파일 시스템에서 저장할 때 중복 방지용

    @Column(name = "content_type", length = 100)
    private String contentType;  
    // MIME 타입 (예: image/png, application/pdf)

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;  
    // 파일 크기(바이트)

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;  
    // 실제 저장 경로 또는 접근 URL
    // 예: /uploads/tasks/123/attachments/uuid_name.pdf

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;  
    // soft delete 여부
    // true면 사용자 화면/조회에서 제외

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;  
    // soft delete 시점
    // cleanup 작업 시 참조용

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();  
    // 생성 시점
    // 업로드 날짜/시간 기록
}
