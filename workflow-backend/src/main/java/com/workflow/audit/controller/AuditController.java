package com.workflow.audit.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workflow.audit.dto.AuditLogGroupedResponse;
import com.workflow.audit.service.AuditLogService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/audit")
public class AuditController {
	
	private final AuditLogService auditLogService;	
	
	@GetMapping("/{taskId}/audit-logs")
	public List<AuditLogGroupedResponse> getTaskAuditLogs(@PathVariable("taskId") Long taskId) {
		
	    return auditLogService.getGroupedAuditLogs(taskId);
	}

}
