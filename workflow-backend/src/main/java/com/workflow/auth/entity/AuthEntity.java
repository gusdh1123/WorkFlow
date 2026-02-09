package com.workflow.auth.entity;

import java.time.LocalDateTime;

import com.workflow.user.entity.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name="refresh_tokens")
@AllArgsConstructor(access = AccessLevel.PROTECTED)
// 파리미터 없는 기본 생성자를 protected 접근제어자로 만들겠다. 
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class AuthEntity {
	
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	// fetch = FetchType.LAZY -> AuthEntity만 조회할 때 UserEntity를 안가져오기 위함
	// UserEntity를 가져오고 싶을 땐 auth.getUser()를 하면 SELET문 실행해서 가져옴
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false) // FK
    private UserEntity user;
	
	@Column(nullable = false)
    private String tokenHash;
    
    @Column(name="expires_at", nullable = false)
    private LocalDateTime expiresAt; // 유효 시각
    
    @Column(name="revoked_at")
    private LocalDateTime revokedAt; // 로그아웃/강제 폐기 시각
    
    @Column(name="created_at", updatable=false, nullable = false)
    private LocalDateTime createdAt; // 생성 시각
    
	// 자동 시간 세팅
	// 처음 생성 시 : INSERT 직전
	@PrePersist
	void prePersist(){
		this.createdAt = LocalDateTime.now();
		if (this.expiresAt == null) {
	        this.expiresAt = LocalDateTime.now().plusDays(7);
	    }
	}

	public void setTokenHash(String tokenHash) {
		this.tokenHash = tokenHash;
	}

	public void setExpiresAt(LocalDateTime expiresAt) {
		this.expiresAt = expiresAt;
	}

	public void setRevokedAt(LocalDateTime revokedAt) {
		this.revokedAt = revokedAt;
	}


}
