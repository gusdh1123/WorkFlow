package com.workflow.user.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name="users")
@AllArgsConstructor
// (access = AccessLevel.PROTECTED) =
// 파리미터 없는 기본 생성자를 protected 접근제어자로 만들겠다. 
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity {
	
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	private String email;
	
	@Column(name="password_hash")
	private String password;
	
	private String name;
	private String department;
	private String position;
	private String role;
	private String status;
	
	@Column(name="last_login_at")
	private LocalDateTime lastLoginAt;
	
	// 업데이트 적용 안되게
	@Column(name="created_at", updatable = false)
	private LocalDateTime createdAt;
	
	// 일단 막아 둠
	@Column(name="updated_at")
	private LocalDateTime updatedAt;
	
	
	// 자동 시간 세팅
	// 처음 생성 시 : INSERT 직전
	@PrePersist
	void prePersist(){
		this.createdAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
	}
	
	// 수정될 때 : UPDATE 직전
	@PreUpdate
	void preUpdate(){
		this.updatedAt = LocalDateTime.now();
	}

	public void setLastLoginAt(LocalDateTime lastLoginAt) {
		this.lastLoginAt = lastLoginAt;
	}

}