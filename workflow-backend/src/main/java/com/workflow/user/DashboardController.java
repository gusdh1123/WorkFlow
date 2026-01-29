package com.workflow.user;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DashboardController {

  // 확인용
	@GetMapping("/health")
  public Map<String, String> health() {
    return Map.of("status", "ok");
  }

  // KPI 카드 값 내려주기
  @GetMapping("/kpi")
  public Map<String, Integer> kpi() {
    return Map.of(
      "TODO", 3,
      "IN_PROGRESS", 2,
      "REVIEW", 1,
      "DONE", 10,
      "ON_HOLD", 0,
      "CANCELED", 0
    );
  }
}
