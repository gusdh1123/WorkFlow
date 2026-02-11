package com.workflow.user;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workflow.tasks.service.TaskService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class DashboardController {
	
	private final TaskService taskService;

  // 확인용
  @GetMapping("/health")
  public Map<String, String> health() {
    return Map.of("status", "ok");
  }

  @GetMapping("/kpi")
  public Map<String, Long> kpi(@AuthenticationPrincipal UserDetails principal) {
      return taskService.kpi(Long.parseLong(principal.getUsername()));
  }
}
