package com.workflow.user;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workflow.tasks.service.TaskQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class DashboardController {
	
	private final TaskQueryService taskQueryService; // KPI 계산/업무 서비스 의존성

    // 시스템 상태 확인용 엔드포인트
    @GetMapping("/health")
    public Map<String, String> health() {
        // 단순 확인용, 서비스/컨트롤러 정상 여부 확인
        return Map.of("status", "ok");
    }

    // 로그인 사용자 KPI 조회
    @GetMapping("/kpi")
    public Map<String, Map<String, Long>> kpi(@AuthenticationPrincipal UserDetails principal) {
        // AuthenticationPrincipal: Spring Security에서 로그인 사용자 정보 주입
        // principal.getUsername() → 사용자 ID (문자열), Long 변환 필요
        return taskQueryService.kpi(Long.parseLong(principal.getUsername()));
        // 담당/작성별 업무 상태별 개수를 Map으로 반환
    }
}
