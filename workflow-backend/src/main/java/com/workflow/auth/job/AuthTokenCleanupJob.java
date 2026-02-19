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

    // application.yml 또는 properties에서
    // 토큰 정리 기능 활성화 여부를 설정
    // 기본값 true
    @Value("${app.token-cleanup.enabled:true}")
    private boolean enabled;

    // revoked 토큰을 몇 일 보관 후 삭제할지 설정
    // 기본 1일
    @Value("${app.token-cleanup.days:1}")
    private int days;

    public AuthTokenCleanupJob(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    // 매일 새벽 3시 실행
    // @Scheduled(cron = "0 0 3 * * ?")
    // 10분
    // fixedDelay는 "이전 작업이 끝난 후" 지정 시간 뒤에 다시 실행
    // 지금 설정은 600000ms = 10분마다 실행
    // 절대 시간 기준이 아니라 작업 완료 기준임
    @Scheduled(fixedDelay = 600000)
    @Transactional
    public void cleanupRevokedTokens() {

        // enabled=false면 배치 아예 실행 안함
        // 로컬/개발 환경에서 끄기 좋음
        if (!enabled) return;

        // 현재 시간 기준으로 days 만큼 이전 시점 계산
        // 예: days=1이면 하루 전
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);

        // revokedAt이 존재하고
        // cutoff 이전에 폐기된 토큰을 물리 삭제
        // DB 용량 증가 방지 + 인덱스 성능 유지 목적
        authRepository.deleteRevokedBefore(cutoff);
    }
}
