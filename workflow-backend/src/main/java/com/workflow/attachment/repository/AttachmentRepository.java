package com.workflow.attachment.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.workflow.attachment.entity.AttachmentEntity;

public interface AttachmentRepository extends JpaRepository<AttachmentEntity, Long> {

    // taskId 기준 살아있는 첨부 목록 조회 (최신 순)
    List<AttachmentEntity> findByTaskIdAndIsDeletedFalseOrderByIdDesc(Long taskId);
    // - 업무별로 soft delete 되지 않은 파일만 반환
    // - OrderByIdDesc: 최근 업로드 순서대로
    // - Task 상세 조회 시 첨부파일 리스트 제공용

    // 다운로드/삭제 시 살아있는 파일만 조회
    java.util.Optional<AttachmentEntity> findByIdAndIsDeletedFalse(Long id);
    // - ID 기준, soft delete 되지 않은 경우만 조회
    // - 권한 체크 후 다운로드/삭제 로직에서 사용

    // 일정 기간 지난 soft-deleted 첨부 목록 조회
    @Query("""
        select a
          from AttachmentEntity a
         where a.isDeleted = true
           and a.deletedAt is not null
           and a.deletedAt < :cutoff
         order by a.deletedAt asc
    """)
    List<AttachmentEntity> findCleanupTargets(@Param("cutoff") LocalDateTime cutoff);
    // - AttachmentCleanupJob 등에서 사용
    // - retentionDays 이전에 soft delete 된 파일만 대상
    // - deletedAt 오름차순 정렬 (오래된 것부터 처리)
    // - 서버 디스크에서 실제 파일 삭제 시 DB row 참조용

    // 물리 삭제 (DB row 삭제)
    @Modifying
    @Query("""
        delete from AttachmentEntity a
         where a.id = :id
    """)
    int hardDeleteById(@Param("id") Long id);
    // - 파일 삭제 후 DB row 실제 제거
    // - 반환값: 삭제된 row 수(0 또는 1)
    // - AttachmentCleanupJob에서 cleanup 대상 파일 삭제 후 호출

    // taskId 기준 활성(soft delete 되지 않은) 첨부 개수 조회
    @Query("""
        select count(a)
          from AttachmentEntity a
         where a.taskId = :taskId
           and a.isDeleted = false
    """)
    long countActiveByTaskId(@Param("taskId") Long taskId);
    // - 업무 내 남아있는 첨부 파일 수 확인
    // - UI에서 파일 아이콘/카운트 표시 등에 사용
}
