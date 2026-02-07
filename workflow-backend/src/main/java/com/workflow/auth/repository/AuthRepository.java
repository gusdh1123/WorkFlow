package com.workflow.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workflow.auth.entity.AuthEntity;

public interface AuthRepository extends JpaRepository<AuthEntity, Long>{
	
	AuthEntity save(AuthEntity authEntity);
	Optional<AuthEntity> findByTokenHashAndRevokedAtIsNull(String tokenHash);

}
