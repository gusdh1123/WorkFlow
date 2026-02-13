package com.workflow.user.entity;

import java.time.LocalDateTime;

import com.workflow.department.entity.DepartmentEntity;
import com.workflow.user.enums.Role;
import com.workflow.user.enums.UserStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
	
	@Column(nullable = false, unique = true)
	private String email;
	
	@Column(name="password_hash", nullable = false)
	private String password;
	
	@Column(nullable = false)
	private String name;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "department_id", nullable = false)
	private DepartmentEntity department;
	
	@Column(nullable = false)
	private String position;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Role role;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UserStatus status;
	
	@Column(name="last_login_at")
	private LocalDateTime lastLoginAt;
	
	// 업데이트 적용 안되게
	@Column(name="created_at", updatable = false, nullable = false)
	private LocalDateTime createdAt;
	
	
	@Column(name="updated_at", nullable = false)
	private LocalDateTime updatedAt;
	
	
	// 자동 시간 세팅
	// 처음 생성 시 : INSERT 직전
	@PrePersist
	void prePersist(){
		this.createdAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
		// this.lastLoginAt = LocalDateTime.now();
	}
	
	// 회원 정보 수정할때만 바끼도록 수정만들때 거기에 직접 대입하기.
//	// 수정될 때 : UPDATE 직전
//	@PreUpdate
//	void preUpdate(){
//		this.updatedAt = LocalDateTime.now();
//	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
	
	public void setLastLoginAt(LocalDateTime lastLoginAt) {
		this.lastLoginAt = lastLoginAt;
	}

	public void setStatus(UserStatus status) {
		this.status = status;
	}

}