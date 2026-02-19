package com.workflow.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.workflow.user.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
	
	// 이메일로 사용자 조회
	// Optional: null일 수도 있는 값을 감싸서, null 처리 강제
	// 예: userRepository.findByEmail(email).orElseThrow(...)
	Optional<UserEntity> findByEmail(String email);

	// 모든 사용자 조회 시 부서 정보 함께 로딩 (N+1 방지)
	@Query("""
			  select u
			  from UserEntity u
			  join fetch u.department
			""")
	List<UserEntity> findAllWithDepartment();
}
