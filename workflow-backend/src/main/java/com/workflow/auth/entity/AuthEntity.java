package com.workflow.auth.entity;

import java.time.LocalDateTime;

import com.workflow.user.entity.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_token_hash", columnList = "tokenHash"),
    // tokenHash 단건 조회 성능용 인덱스
    // refresh 시 hash로 바로 찾기 때문에 거의 PK급 중요도
    @Index(name = "idx_user_revoked", columnList = "user_id, revoked_at")
    // 유저의 "활성 토큰" 조회용 복합 인덱스
    // where user_id = ? and revoked_at is null
    // 로그아웃 시 전체 토큰 무효화할 때도 성능에 도움
})
@AllArgsConstructor(access = AccessLevel.PROTECTED)
// 파라미터 없는 기본 생성자를 protected 접근제어자로 만들겠다. 
// JPA 기본 생성자 강제 조건 만족 + 외부에서 new 못하게 막음
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class AuthEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // PK는 단순 식별용
    // refresh 토큰은 tokenHash 기준으로 조회

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // FK
    private UserEntity user;
    // fetch = FetchType.LAZY -> AuthEntity만 조회할 때 UserEntity를 안가져오기 위함
    // UserEntity를 가져오고 싶을 땐 auth.getUser()를 하면 SELECT문 실행해서 가져옴
    // 불필요한 조인 방지 = 성능 최적화

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;
    // refreshToken을 그대로 저장하지 않고 HMAC_SHA_256 해시값만 저장
    // DB 털려도 원본 토큰 복원 불가
    // length=64는 SHA-256 hex 길이 (32byte → 64문자)

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt; 
    // refresh 토큰 만료 시간
    // 만료 검사 시 DB + JWT exp 둘 다 검증하면 더 안전

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt; 
    // 로그아웃/강제 폐기 시각
    // null이면 활성 토큰
    // 값 있으면 폐기된 토큰 (재사용 공격 방지용)

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt; 
    // 생성 시각
    // 최초 발급 시점 기록
    // 보안 감사 / 토큰 이상 탐지 시 활용 가능

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.expiresAt == null) {
            this.expiresAt = LocalDateTime.now().plusDays(7);
        }
        // expiresAt 기본값 7일
        // JWT refresh 만료 기간과 반드시 동일하게 맞춰야 함
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
        // refresh rotation 시 hash 교체용
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
        // rotation 시 만료 갱신
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
        // 로그아웃 / 재사용 감지 시 즉시 무효화
    }
}
