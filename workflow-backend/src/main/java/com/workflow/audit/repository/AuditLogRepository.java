package com.workflow.audit.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workflow.audit.entity.AuditLogEntity;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long>{

}
