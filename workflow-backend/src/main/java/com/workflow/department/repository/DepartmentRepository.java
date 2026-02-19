package com.workflow.department.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workflow.department.entity.DepartmentEntity;

// 부서 테이블 접근용 JPA 레포지토리
// JpaRepository<T, ID> 상속: 기본 CRUD 메서드 제공
public interface DepartmentRepository extends JpaRepository<DepartmentEntity, Long> {

    // 코드로 부서 조회, 결과 Optional로 반환 (없을 수 있음)
    Optional<DepartmentEntity> findByCode(String code);

    // 이름으로 부서 조회, 결과 Optional로 반환 (없을 수 있음)
    Optional<DepartmentEntity> findByName(String name);
}
