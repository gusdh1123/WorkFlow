package com.workflow.tasks.repasitory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.workflow.tasks.entity.TaskEntity;
import com.workflow.tasks.enums.TaskStatus;

public interface TaskRepository extends JpaRepository<TaskEntity, Long> {

	// N + 1 방지
    @EntityGraph(attributePaths = {"createdBy", "assignee"})
    Page<TaskEntity> findByIsDeletedFalse(Pageable pageable);

    @EntityGraph(attributePaths = {"createdBy", "assignee"})
    Page<TaskEntity> findByIsDeletedFalseAndStatus(TaskStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"createdBy", "assignee"})
    Page<TaskEntity> findByIsDeletedFalseAndCreatedBy_Id(Long createdById, Pageable pageable);

    @EntityGraph(attributePaths = {"createdBy", "assignee"})
    Page<TaskEntity> findByIsDeletedFalseAndCreatedBy_IdAndStatus( Long createdById, TaskStatus status, Pageable pageable );

    @EntityGraph(attributePaths = {"createdBy", "assignee"})
    Page<TaskEntity> findByIsDeletedFalseAndAssignee_Id(Long assigneeId, Pageable pageable);

    @EntityGraph(attributePaths = {"createdBy", "assignee"})
    Page<TaskEntity> findByIsDeletedFalseAndAssignee_IdAndStatus( Long assigneeId, TaskStatus status, Pageable pageable );

    long countByIsDeletedFalseAndAssignee_IdAndStatus(Long assigneeId, TaskStatus status);
}
