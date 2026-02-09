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

public interface AuthRepository extends JpaRepository<AuthEntity, Long>{
	
	// SELECT로 읽은 row를 지금 트랜잭션이 끝날 때까지 다른 트랜잭션이 수정/삭제/잠금 못 하게 막는 역할
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<AuthEntity> findByTokenHashAndRevokedAtIsNull(String tokenHash);
	
	Optional<AuthEntity> findByUser(UserEntity user);
	
	Optional<AuthEntity> findByUserAndRevokedAtIsNull(UserEntity user);
	
	// 한 사람당 리프래시 토큰 1개씩
	@Modifying
    @Query("""
        update AuthEntity a
           set a.revokedAt = :now
         where a.user = :user
           and a.revokedAt is null
    """)
    int revokeAllActiveByUser(@Param("user") UserEntity user,
                              @Param("now") LocalDateTime now);
	
	// 스케줄러 토큰 로그 삭제
	@Modifying
	@Query("""
	  delete from AuthEntity a
	   where a.revokedAt is not null
	     and a.revokedAt < :cutoff
	""")
	int deleteRevokedBefore(@Param("cutoff") LocalDateTime cutoff);
}
