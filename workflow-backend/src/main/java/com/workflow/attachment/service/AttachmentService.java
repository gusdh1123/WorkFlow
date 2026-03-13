package com.workflow.attachment.service;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.workflow.attachment.dto.AttachmentResponse;
import com.workflow.attachment.dto.DownloadInfo;
import com.workflow.attachment.entity.AttachmentEntity;
import com.workflow.attachment.mapper.AttachmentMapper; // 새로 추가
import com.workflow.attachment.repository.AttachmentRepository;
import com.workflow.common.exception.ApiException;
import com.workflow.common.exception.ErrorCode;
import com.workflow.common.file.FileStorageService;
import com.workflow.common.file.StoredAttachment;
import com.workflow.tasks.entity.TaskEntity;
import com.workflow.tasks.repasitory.TaskRepository;
import com.workflow.user.entity.UserEntity;
import com.workflow.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    
    // 첨부파일 제한 상수
    private static final int MAX_FILES = 10;
    private static final long MAX_TOTAL_SIZE = 50L * 1024 * 1024; // 50MB

    // Task 상세 조회 시 첨부 포함용
    @Transactional(readOnly = true)
    public List<AttachmentResponse> listByTask(Long taskId) {
        // taskId 기준 soft delete 안 된 첨부만 조회
        return attachmentRepository.findByTaskIdAndIsDeletedFalseOrderByIdDesc(taskId)
                .stream()
                .map(AttachmentMapper::toResponse) // Mapper로 변환
                .toList();
    }

    // 첨부 업로드 (taskId에 귀속)
    @Transactional
    public List<AttachmentResponse> uploadToTask(Long taskId, Long uploaderId, List<MultipartFile> files) {

        if (taskId == null || taskId <= 0) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "잘못된 taskId");
        }

        if (uploaderId == null || uploaderId <= 0) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        if (files == null || files.isEmpty()) return List.of(); // 업로드 파일 없으면 빈 리스트

        if (files.size() > MAX_FILES) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "첨부파일은 최대 " + MAX_FILES + "개까지 가능합니다.");
        }

        long total = 0;
        for (MultipartFile f : files) if (f != null) total += f.getSize();
        if (total > MAX_TOTAL_SIZE) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "첨부파일 총합은 50MB 이하만 가능합니다.");
        }
        
        // 권한 체크
        UserEntity loginUser = userRepository.findById(uploaderId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "사용자가 존재하지 않습니다."));

        TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "업무를 찾을 수 없습니다."));

        if (!task.canEdit(loginUser)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "업로드 권한이 없습니다.");
        }

        List<AttachmentResponse> out = new ArrayList<>();

        for (MultipartFile f : files) {
            // 실제 저장 (task별 폴더)
            StoredAttachment stored =
                    fileStorageService.storeTaskAttachmentToTaskDir(f, "tasks", taskId);

            // DB 엔티티 생성
            AttachmentEntity a = new AttachmentEntity();
            a.setTaskId(taskId);
            a.setUploaderId(uploaderId);
            a.setOriginalFilename(stored.originalFilename());
            a.setStoredFilename(stored.storedFilename());
            a.setContentType(stored.contentType());
            a.setSizeBytes(stored.sizeBytes());
            a.setStoragePath(stored.storagePath());
            a.setDeleted(false);
            a.setCreatedAt(LocalDateTime.now());

            // DB 저장 후 응답 변환
            AttachmentEntity saved = attachmentRepository.save(a);
            out.add(AttachmentMapper.toResponse(saved)); // Mapper 사용
        }

        return out;
    }

    // soft delete 처리
    @Transactional
    public void softDelete(Long taskId, List<Long> attachmentIds, Long requesterId) {

        // 요청자 정보 가져오기
        UserEntity loginUser = userRepository.findById(requesterId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "사용자가 존재하지 않습니다."));

        for (Long attachmentId : attachmentIds) {
            // 첨부 파일 조회
            AttachmentEntity a = attachmentRepository.findByIdAndIsDeletedFalse(attachmentId)
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "첨부파일이 없습니다."));

            // Task 조회 (컨트롤러에서 받은 taskId 사용)
            TaskEntity task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "업무를 찾을 수 없습니다."));

            // 권한 체크
            if (!task.canEdit(loginUser)) {
                throw new ApiException(ErrorCode.UNAUTHORIZED, "삭제 권한이 없습니다.");
            }

            // soft delete 처리
            a.setDeleted(true);
            a.setDeletedAt(LocalDateTime.now());

            attachmentRepository.save(a);
        }
    }
    
    // 다운로드용: 엔티티 + 실제 디스크 경로 반환
    // 파일 정보 반환
    @Transactional(readOnly = true)
    public DownloadInfo getDownloadInfo(Long attachmentId, Long requesterId) {

        AttachmentEntity a = attachmentRepository.findByIdAndIsDeletedFalse(attachmentId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "첨부파일이 없습니다."));
        
        // 권한 체크
        UserEntity loginUser = userRepository.findById(requesterId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "사용자가 존재하지 않습니다."));

        TaskEntity task = taskRepository.findById(a.getTaskId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "업무를 찾을 수 없습니다."));

        if (!task.canEdit(loginUser)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "다운로드 권한이 없습니다.");
        }

        Path filePath = fileStorageService.resolveUploadPath(a.getStoragePath());

        return new DownloadInfo(
                a.getOriginalFilename(),
                a.getContentType(),
                filePath
        );
    }

    // Task별 남아있는 활성 첨부 파일 수 조회
    @Transactional(readOnly = true)
    public long countActiveByTask(Long taskId) {
        return attachmentRepository.countActiveByTaskId(taskId);
    }

    // === 기존 Service 내부 private 메서드 toResponse 삭제됨 ===
    // Entity → DTO 변환은 AttachmentMapper로 통일됨
}
