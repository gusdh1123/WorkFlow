package com.workflow.favorite.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.workflow.favorite.entity.FavoriteEntity;
import com.workflow.tasks.entity.TaskEntity;
import com.workflow.tasks.enums.TaskStatus;
import com.workflow.user.entity.UserEntity;

public interface FavoriteRepository extends JpaRepository<FavoriteEntity, Long> {
    
    // 즐겨찾기 조회 (status 무관 + 부서 필터, 등록일 최신순)
    @Query("""
        select t from TaskEntity t
        join FavoriteEntity f on f.task.id = t.id
        where f.user.id = :userId
          and t.isDeleted = false
          and (:deptId IS NULL OR t.workDepartment.id = :deptId)
        order by f.createdAt desc
    """)
    Page<TaskEntity> findFavoriteTasksOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("deptId") Long deptId,
            Pageable pageable
    );

    // 즐겨찾기 조회 (status 무관 + 부서 필터, 등록일 오래된순)
    @Query("""
        select t from TaskEntity t
        join FavoriteEntity f on f.task.id = t.id
        where f.user.id = :userId
          and t.isDeleted = false
          and (:deptId IS NULL OR t.workDepartment.id = :deptId)
        order by f.createdAt asc
    """)
    Page<TaskEntity> findFavoriteTasksOrderByCreatedAtAsc(
            @Param("userId") Long userId,
            @Param("deptId") Long deptId,
            Pageable pageable
    );

    // 즐겨찾기 조회 (status 포함 + 부서 필터, 등록일 최신순)
    @Query("""
        select t from TaskEntity t
        join FavoriteEntity f on f.task.id = t.id
        where f.user.id = :userId
          and t.isDeleted = false
          and t.status = :status
          and (:deptId IS NULL OR t.workDepartment.id = :deptId)
        order by f.createdAt desc
    """)
    Page<TaskEntity> findFavoriteTasksByStatusOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("status") TaskStatus status,
            @Param("deptId") Long deptId,
            Pageable pageable
    );

    // 즐겨찾기 조회 (status 포함 + 부서 필터, 등록일 오래된순)
    @Query("""
        select t from TaskEntity t
        join FavoriteEntity f on f.task.id = t.id
        where f.user.id = :userId
          and t.isDeleted = false
          and t.status = :status
          and (:deptId IS NULL OR t.workDepartment.id = :deptId)
        order by f.createdAt asc
    """)
    Page<TaskEntity> findFavoriteTasksByStatusOrderByCreatedAtAsc(
            @Param("userId") Long userId,
            @Param("status") TaskStatus status,
            @Param("deptId") Long deptId,
            Pageable pageable
    );

    // 즐겨찾기 조회 (status 무관 + 부서 필터, dueDate 오름차순 = 마감 임박순)
    @Query("""
        select t from TaskEntity t
        join FavoriteEntity f on f.task.id = t.id
        where f.user.id = :userId
          and t.isDeleted = false
          and (:deptId IS NULL OR t.workDepartment.id = :deptId)
        order by t.dueDate asc, t.id asc
    """)
    Page<TaskEntity> findFavoriteTasksOrderByDueDateAsc(
            @Param("userId") Long userId,
            @Param("deptId") Long deptId,
            Pageable pageable
    );

    // 즐겨찾기 조회 (status 무관 + 부서 필터, dueDate 내림차순 = 마감 늦은순)
    @Query("""
        select t from TaskEntity t
        join FavoriteEntity f on f.task.id = t.id
        where f.user.id = :userId
          and t.isDeleted = false
          and (:deptId IS NULL OR t.workDepartment.id = :deptId)
        order by t.dueDate desc, t.id desc
    """)
    Page<TaskEntity> findFavoriteTasksOrderByDueDateDesc(
            @Param("userId") Long userId,
            @Param("deptId") Long deptId,
            Pageable pageable
    );

    // 즐겨찾기 조회 (status 포함 + 부서 필터, dueDate 오름차순 = 마감 임박순)
    @Query("""
        select t from TaskEntity t
        join FavoriteEntity f on f.task.id = t.id
        where f.user.id = :userId
          and t.isDeleted = false
          and t.status = :status
          and (:deptId IS NULL OR t.workDepartment.id = :deptId)
        order by t.dueDate asc, t.id asc
    """)
    Page<TaskEntity> findFavoriteTasksByStatusOrderByDueDateAsc(
            @Param("userId") Long userId,
            @Param("status") TaskStatus status,
            @Param("deptId") Long deptId,
            Pageable pageable
    );

    // 즐겨찾기 조회 (status 포함 + 부서 필터, dueDate 내림차순 = 마감 늦은순)
    @Query("""
        select t from TaskEntity t
        join FavoriteEntity f on f.task.id = t.id
        where f.user.id = :userId
          and t.isDeleted = false
          and t.status = :status
          and (:deptId IS NULL OR t.workDepartment.id = :deptId)
        order by t.dueDate desc, t.id desc
    """)
    Page<TaskEntity> findFavoriteTasksByStatusOrderByDueDateDesc(
            @Param("userId") Long userId,
            @Param("status") TaskStatus status,
            @Param("deptId") Long deptId,
            Pageable pageable
    );

    // 즐겨찾기 priority 오름차순 + status, 부서 필터
    @Query("""
        SELECT t
        FROM TaskEntity t
        JOIN FavoriteEntity f ON f.task.id = t.id AND f.user.id = :userId
        WHERE t.isDeleted = false
          AND (:status IS NULL OR t.status = :status)
          AND (:deptId IS NULL OR t.workDepartment.id = :deptId)
        ORDER BY
          CASE t.priority
            WHEN 'LOW' THEN 1
            WHEN 'MEDIUM' THEN 2
            WHEN 'HIGH' THEN 3
            ELSE 0
          END ASC,
          t.createdAt ASC,
          t.id ASC
    """)
    Page<TaskEntity> findFavoriteTasksPriorityAsc(
            @Param("status") TaskStatus status,
            @Param("userId") Long userId,
            @Param("deptId") Long deptId,
            Pageable pageable
    );

    // 즐겨찾기 priority 내림차순 + status, 부서 필터
    @Query("""
        SELECT t
        FROM TaskEntity t
        JOIN FavoriteEntity f ON f.task.id = t.id AND f.user.id = :userId
        WHERE t.isDeleted = false
          AND (:status IS NULL OR t.status = :status)
          AND (:deptId IS NULL OR t.workDepartment.id = :deptId)
        ORDER BY
          CASE t.priority
            WHEN 'LOW' THEN 1
            WHEN 'MEDIUM' THEN 2
            WHEN 'HIGH' THEN 3
            ELSE 0
          END DESC,
          t.createdAt DESC,
          t.id DESC
    """)
    Page<TaskEntity> findFavoriteTasksPriorityDesc(
            @Param("status") TaskStatus status,
            @Param("userId") Long userId,
            @Param("deptId") Long deptId,
            Pageable pageable
    );
    
    // 삭제 시 즐겨찾기 해제
    void deleteByTaskId(Long taskId);
        
    // 중복 체크(즐겨찾기 등록)
    boolean existsByUserAndTask(UserEntity user, TaskEntity task);
        
    // 업무 확인(즐겨찾기 해제)
    Optional<FavoriteEntity> findByUserAndTask(UserEntity user, TaskEntity task);
    
    // KPI 조회
    @Query("SELECT COUNT(f) FROM FavoriteEntity f " +
    	       "WHERE f.user.id = :userId " + 
    	       "AND f.task.status = :status " +
    	       "AND f.task.isDeleted = false")
    	long countByUserIdAndStatus(@Param("userId") Long userId,
    	                            @Param("status") TaskStatus status);
}