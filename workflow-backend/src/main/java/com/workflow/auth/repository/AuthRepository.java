package com.workflow.auth.repository;

import org.springframework.data.repository.CrudRepository;

import com.workflow.auth.entity.AuthEntity;

public interface AuthRepository extends CrudRepository<AuthEntity, Object> {
	
//	@Query("INSERT INTO users (id, email, password_hash, name, department, position, role, status) VALUES "
//			+ "(a.getId(), a.getEmail()")
//	public List<Object> insterUser(LoginEntity a);
	
}
