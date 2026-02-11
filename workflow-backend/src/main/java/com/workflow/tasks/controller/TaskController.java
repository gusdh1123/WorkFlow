package com.workflow.tasks.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.workflow.tasks.dto.TaskCreateRequest;
import com.workflow.tasks.dto.TaskResponse;
import com.workflow.tasks.enums.TaskStatus;
import com.workflow.tasks.service.TaskService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tasks")
public class TaskController {
	
	private final TaskService taskService;
	
	
	@GetMapping
	public ResponseEntity<?> list(
	    @RequestParam(name = "scope", required = false, defaultValue = "all") String scope,
	    @RequestParam(name = "status", required = false) TaskStatus status,
	    @RequestParam(name = "page", required = false, defaultValue = "0") int page,
	    @RequestParam(name = "size", required = false, defaultValue = "10") int size,
	    @AuthenticationPrincipal UserDetails principal
	) {
		Long userId = Long.parseLong(principal.getUsername());
	    return ResponseEntity.ok(taskService.list(scope, status, userId, page, size));
	}
	
	@PostMapping
	public ResponseEntity<TaskResponse> create(
	        @Valid @RequestBody TaskCreateRequest req,
	        @AuthenticationPrincipal UserDetails principal
	) {
	    if (principal == null) return ResponseEntity.status(401).build();

	    Long userId = Long.parseLong(principal.getUsername()); // username에 id 들어있음
	    return ResponseEntity.ok(taskService.create(req, userId));
	}
	
//	@PostMapping("/upload")
//	public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) throws IOException {
//		
//		String original = Optional.ofNullable(file.getOriginalFilename()).orElse("file");
//		String fileName = UUID.randomUUID() + "_" + original.replaceAll("[\\\\/]", "_");
//	    Path filePath = Paths.get("uploads/" + fileName);
//
//	    Files.createDirectories(filePath.getParent());
//	    Files.write(filePath, file.getBytes());
//
//	    String url = "http://localhost:8081/uploads/" + fileName;
//	    return ResponseEntity.ok(Map.of("url", url));
//	}

}
