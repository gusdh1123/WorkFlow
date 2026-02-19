package com.workflow.attachment.job;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.workflow.attachment.entity.AttachmentEntity;
import com.workflow.attachment.repository.AttachmentRepository;
import com.workflow.common.file.FileStorageService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AttachmentCleanupJob {

    private final AttachmentRepository attachmentRepository; // DB 접근용
    private final FileStorageService fileStorageService; // 파일 시스템 접근용

    @Value("${app.attachment-cleanup.enabled:true}")
    private boolean enabled; // 청소 기능 활성 여부

    @Value("${app.attachment-cleanup.retention-days:1}")
    private int retentionDays; // soft delete 후 보관 일수

    // 매일 새벽 3시 실행(cron = "0 0 3 * * *")
    // fixedDelay = 10000 10초마다 실행
    // fixedDelay = 60000 1분
    // fixedDelay = 600000 10분
    @Scheduled(fixedDelay = 600000) // 10분마다 실행
    @Transactional
    public void cleanupDeletedAttachments() {

        if (!enabled) return; // 기능 비활성화 시 바로 종료

        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays); 
        // retentionDays 이전 삭제된 첨부만 대상

        List<AttachmentEntity> targets = attachmentRepository.findCleanupTargets(cutoff); 
        // soft delete 된 파일 중 retentionDays 지난 파일 조회

        for (AttachmentEntity a : targets) {

            // storagePath: "/uploads/...." 형태
            try {
                Path p = fileStorageService.resolveUploadPath(a.getStoragePath()); 
                // 실제 파일 시스템 경로 변환

                Files.deleteIfExists(p); 
                // 파일 존재하면 삭제, 없으면 그냥 넘어감

                // (선택) 폴더 비었으면 정리하고 싶다면 여기에 디렉토리 정리 로직 추가 가능

            } catch (Exception ignored) {
                // 파일 삭제 실패해도 DB는 지우지 않는 편이 안전함
                // (원하면 로그 찍기)
                continue;
            }

            // 파일 삭제 성공/파일 없음 → DB row 물리 삭제
            attachmentRepository.hardDeleteById(a.getId()); 
            // DB에서 실제 row 제거
        }
    }
}
