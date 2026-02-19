package com.workflow.user.entity;

import java.time.LocalDateTime;

import com.workflow.department.entity.DepartmentEntity;
import com.workflow.user.enums.Role;
import com.workflow.user.enums.UserStatus;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name="users")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 기본 생성자
public class UserEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id; // PK, 사용자 고유 ID

	@Column(nullable = false, unique = true)
	private String email; // 로그인용 이메일, 유일 제약

	@Column(name="password_hash", nullable = false)
	private String password; // bcrypt 등으로 암호화된 비밀번호

	@Column(nullable = false)
	private String name; // 사용자 이름

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "department_id", nullable = false)
	private DepartmentEntity department; // 소속 부서, 다대일 관계

	@Column(nullable = false)
	private String position; // 직책

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Role role; // ADMIN, MANAGER, USER 등 권한

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UserStatus status; // ONLINE, OFFLINE 등 사용자 상태

	@Column(name="last_login_at")
	private LocalDateTime lastLoginAt; // 마지막 로그인 시간

	@Column(name="created_at", updatable = false, nullable = false)
	private LocalDateTime createdAt; // 가입 시각, 최초 저장 시점 고정

	@Column(name="updated_at", nullable = false)
	private LocalDateTime updatedAt; // 마지막 수정 시각, 매번 갱신

	// 엔티티 최초 저장 전 자동 호출
	@PrePersist
	void prePersist(){
		this.createdAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
	}

	// 수정 시 직접 호출
	public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
	public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
	public void setStatus(UserStatus status) { this.status = status; }
}
