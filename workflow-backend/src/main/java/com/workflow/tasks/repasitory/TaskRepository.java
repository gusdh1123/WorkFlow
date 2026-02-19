package com.workflow.tasks.repasitory;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.workflow.tasks.entity.TaskEntity;
import com.workflow.tasks.enums.TaskStatus;

public interface TaskRepository extends JpaRepository<TaskEntity, Long> {

	// @EntityGraph: 연관 엔티티를 한 번에 같이 조회하라고 강제하는 옵션(N+1 문제 방지용 + Lazy 로딩 최적화)

	// 기본 목록/필터
	@EntityGraph(attributePaths = { "createdBy", "assignee" })
	Page<TaskEntity> findByIsDeletedFalse(Pageable pageable); 
    // 삭제되지 않은 모든 Task를 페이지 단위로 조회, 작성자/담당자 정보를 즉시 가져옴

	@EntityGraph(attributePaths = { "createdBy", "assignee" })
	Page<TaskEntity> findByIsDeletedFalseAndStatus(TaskStatus status, Pageable pageable);
    // 삭제되지 않은 Task 중 특정 상태(status)를 가진 것만 조회

	// created 탭: 내가 만든 것만
	@EntityGraph(attributePaths = { "createdBy", "assignee" })
	Page<TaskEntity> findByIsDeletedFalseAndCreatedBy_Id(Long createdById, Pageable pageable);
    // 특정 사용자가 생성한 삭제되지 않은 Task 조회

	@EntityGraph(attributePaths = { "createdBy", "assignee" })
	Page<TaskEntity> findByIsDeletedFalseAndCreatedBy_IdAndStatus(Long createdById, TaskStatus status, Pageable pageable);
    // 특정 사용자가 생성한 Task 중 상태가 특정 값인 것만 조회

	// assigned 탭: 내가 담당인 것만
	@EntityGraph(attributePaths = { "createdBy", "assignee" })
	Page<TaskEntity> findByIsDeletedFalseAndAssignee_Id(Long assigneeId, Pageable pageable);
    // 특정 사용자가 담당인 삭제되지 않은 Task 조회

	@EntityGraph(attributePaths = { "createdBy", "assignee" })
	Page<TaskEntity> findByIsDeletedFalseAndAssignee_IdAndStatus(Long assigneeId, TaskStatus status, Pageable pageable);
    // 특정 사용자가 담당인 Task 중 상태가 특정 값인 것만 조회

	// count 계열은 EntityGraph 붙이면 손해
	long countByIsDeletedFalseAndAssignee_IdAndStatus(Long userId, TaskStatus status);
    // 특정 사용자가 담당한 특정 상태의 Task 개수만 계산 (EntityGraph 미사용)

	// created KPI (내가 만든 업무)
    long countByIsDeletedFalseAndCreatedBy_IdAndStatus(Long userId, TaskStatus status);
    // 특정 사용자가 생성한 특정 상태 Task 개수

	// 전사 업무: PUBLIC만
	@EntityGraph(attributePaths = { "createdBy", "assignee" })
	@Query("""
		select t from TaskEntity t
		where t.isDeleted = false
		  and t.visibility = 'PUBLIC'
	""")
	Page<TaskEntity> findPublicOnly(Pageable pageable);
    // 모든 사용자가 볼 수 있는 공개 Task 조회

	@EntityGraph(attributePaths = { "createdBy", "assignee" })
	@Query("""
		select t from TaskEntity t
		where t.isDeleted = false
		  and t.visibility = 'PUBLIC'
		  and t.status = :status
	""")
	Page<TaskEntity> findPublicOnlyByStatus(@Param("status") TaskStatus status, Pageable pageable);
    // 공개 Task 중 특정 상태만 조회

	// 전체 업무: 내가 볼 수 있는 모든 업무
	// PUBLIC + (DEPARTMENT면 내 부서) + (PRIVATE면 내가 작성/담당)
	@EntityGraph(attributePaths = { "createdBy", "assignee" })
	@Query("""
	  select t from TaskEntity t
	  where t.isDeleted = false
	    and (
	          t.createdBy.id = :userId
	       or t.assignee.id = :userId
	       or t.visibility = 'PUBLIC'
	       or (t.visibility = 'DEPARTMENT' and t.workDepartment.id = :deptId)
	    )
	""")
	Page<TaskEntity> findAllVisibleForUser(@Param("userId") Long userId,
	                                      @Param("deptId") Long deptId,
	                                      Pageable pageable);
    // 로그인 사용자가 볼 수 있는 모든 Task 조회: 공개/내부부서/내가 작성하거나 담당

	@EntityGraph(attributePaths = { "createdBy", "assignee" })
	@Query("""
	  select t from TaskEntity t
	  where t.isDeleted = false
	    and t.status = :status
	    and (
	          t.createdBy.id = :userId
	       or t.assignee.id = :userId
	       or t.visibility = 'PUBLIC'
	       or (t.visibility = 'DEPARTMENT' and t.workDepartment.id = :deptId)
	    )
	""")
	Page<TaskEntity> findAllVisibleForUserByStatus(@Param("userId") Long userId,
	                                              @Param("deptId") Long deptId,
	                                              @Param("status") TaskStatus status,
	                                              Pageable pageable);
    // 로그인 사용자가 볼 수 있는 Task 중 특정 상태인 것만 조회

	// 우리팀 업무: 우리 팀만 + PRIVATE는 (작성자/담당자=나)만 예외 허용
	@EntityGraph(attributePaths = { "createdBy", "assignee" })
	@Query("""
		select t from TaskEntity t
		where t.isDeleted = false
		  and t.workDepartment.id = :deptId
		  and (
		        t.visibility <> 'PRIVATE'
		     or (t.visibility = 'PRIVATE' and (t.createdBy.id = :userId or t.assignee.id = :userId))
		  )
	""")
	Page<TaskEntity> findTeamVisibleForUser(@Param("userId") Long userId,
	                                       @Param("deptId") Long deptId,
	                                       Pageable pageable);
    // 내 부서 팀 Task 조회, PRIVATE인 경우 작성자/담당자만 예외 허용

	@EntityGraph(attributePaths = { "createdBy", "assignee" })
	@Query("""
		select t from TaskEntity t
		where t.isDeleted = false
		  and t.status = :status
		  and t.workDepartment.id = :deptId
		  and (
		        t.visibility <> 'PRIVATE'
		     or (t.visibility = 'PRIVATE' and (t.createdBy.id = :userId or t.assignee.id = :userId))
		  )
	""")
	Page<TaskEntity> findTeamVisibleForUserByStatus(@Param("userId") Long userId,
	                                               @Param("deptId") Long deptId,
	                                               @Param("status") TaskStatus status,
	                                               Pageable pageable);
    // 내 부서 팀 Task 중 특정 상태만 조회

	@EntityGraph(attributePaths = {
	        "createdBy", "createdBy.department",
	        "assignee", "assignee.department",
	        "ownerDepartment",
	        "workDepartment"
	})
	@Query("""
		    select t from TaskEntity t
		    where t.isDeleted = false
		      and t.id = :taskId
		      and (
		            t.createdBy.id = :userId
		         or t.assignee.id = :userId
		         or t.visibility = 'PUBLIC'
		         or (t.visibility = 'DEPARTMENT' and t.workDepartment.id = :deptId)
		      )
		""")
	Optional<TaskEntity> findDetailVisibleForUser(@Param("taskId") Long taskId,
												  @Param("userId") Long	userId,
												  @Param("deptId") Long	deptId);
    // 특정 Task 상세 조회: 로그인 사용자가 접근 가능한 Task만, 작성자/담당자/공개/부서 포함
}
