package com.workflow.auth.job;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.workflow.auth.repository.AuthRepository;

@Component
public class AuthTokenCleanupJob {

    private final AuthRepository authRepository;

    @Value("${app.token-cleanup.enabled:false}")
    private boolean enabled;

    @Value("${app.token-cleanup.days:30}")
    private int days;

    public AuthTokenCleanupJob(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    // 매일 새벽 3시 실행
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupRevokedTokens() {

        // 로컬/개발 환경에서는 즉시 종료 (기본 OFF)
        if (!enabled) return;

        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        authRepository.deleteRevokedBefore(cutoff);
    }
}
