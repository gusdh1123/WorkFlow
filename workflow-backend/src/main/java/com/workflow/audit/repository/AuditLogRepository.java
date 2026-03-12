package com.workflow.audit.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.workflow.audit.entity.AuditLogEntity;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long>{
	
	// 특정 Task ID에 대한 AuditLog를 최신순(desc)으로 조회
	@Query("SELECT a FROM AuditLogEntity a JOIN FETCH a.actor WHERE a.task.id = :taskId ORDER BY a.createdAt DESC")
	List<AuditLogEntity> findByTaskIdWithActor(@Param("taskId") Long taskId);
	
}
