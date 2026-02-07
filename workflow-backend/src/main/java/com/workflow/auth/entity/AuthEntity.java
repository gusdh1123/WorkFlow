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
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name="refresh_tokens")
@AllArgsConstructor(access = AccessLevel.PROTECTED)
// 파리미터 없는 기본 생성자를 protected 접근제어자로 만들겠다. 
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthEntity {
	
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	// fetch = FetchType.LAZY -> AuthEntity만 조회할 때 UserEntity를 안가져오기 위함
	// UserEntity를 가져오고 싶을 땐 auth.getUser()를 하면 SELET문 실행해서 가져옴
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false) // FK
    private UserEntity user;
	
    private String tokenHash;
    
    @Column(name="expires_at", updatable = false)
    private LocalDateTime expiresAt; // 유효 시각
    @Column(name="revoked_at")
    private LocalDateTime revokedAt; // 죽인 시각
    @Column(name="created_at")
    private LocalDateTime createdAt; // 생성 시각
    
	// 자동 시간 세팅
	// 처음 생성 시 : INSERT 직전
	@PrePersist
	void prePersist(){
		this.createdAt = LocalDateTime.now();
		this.expiresAt = LocalDateTime.now().plusDays(7);
	}
	
	@Builder
    private AuthEntity(UserEntity user, String tokenHash) {
        this.user = user;
        this.tokenHash = tokenHash;
    }


}
