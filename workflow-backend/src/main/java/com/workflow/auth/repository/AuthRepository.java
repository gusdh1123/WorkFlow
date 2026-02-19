package com.workflow.auth.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.workflow.auth.entity.AuthEntity;
import com.workflow.user.entity.UserEntity;

import jakarta.persistence.LockModeType;

public interface AuthRepository extends JpaRepository<AuthEntity, Long> {

    // 토큰 검증 시 동시성 방지용
    // SELECT로 읽은 row를 지금 트랜잭션이 끝날 때까지
    // 다른 트랜잭션이 수정/삭제/잠금 못 하게 막는 역할
    // → Refresh 토큰 탈취 동시 재사용 방지
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AuthEntity> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    // 특정 유저의 토큰 조회
    Optional<AuthEntity> findByUser(UserEntity user);

    // 아직 revoke 되지 않은 활성 토큰만 조회
    Optional<AuthEntity> findByUserAndRevokedAtIsNull(UserEntity user);

    // 한 사람당 리프래시 토큰 1개 정책
    // @Modifying: UPDATE / DELETE 실행 시 필수
    // JPQL UPDATE는 영속성 컨텍스트를 우회하고 DB를 직접 수정함
    // → 실행 후 영속성 컨텍스트와 DB 상태가 달라질 수 있음
    @Modifying
    @Query("""
        update AuthEntity a
           set a.revokedAt = :now
         where a.user = :user
           and a.revokedAt is null
    """)
    int revokeAllActiveByUser(@Param("user") UserEntity user,
                              @Param("now") LocalDateTime now);

    // 스케줄러용 토큰 정리 (로그 정리 목적)
    // revoke 된 토큰 중 오래된 것 삭제
    @Modifying
    @Query("""
        delete from AuthEntity a
         where a.revokedAt is not null
           and a.revokedAt < :cutoff
    """)
    int deleteRevokedBefore(@Param("cutoff") LocalDateTime cutoff);

    // Postgres upsert용 native query
    // 중복 token_hash 발생 시 기존 row update
    @Modifying
    @Query(value = """
        INSERT INTO refresh_tokens (user_id, token_hash, created_at, expires_at, revoked_at)
        VALUES (:userId, :tokenHash, :createdAt, :expiresAt, :revokedAt)
        ON CONFLICT (token_hash)
        DO UPDATE SET
            expires_at = EXCLUDED.expires_at,
            revoked_at = EXCLUDED.revoked_at
        """, nativeQuery = true)
    void upsertToken(@Param("userId") Long userId,
                     @Param("tokenHash") String tokenHash,
                     @Param("createdAt") LocalDateTime createdAt,
                     @Param("expiresAt") LocalDateTime expiresAt,
                     @Param("revokedAt") LocalDateTime revokedAt);
}
