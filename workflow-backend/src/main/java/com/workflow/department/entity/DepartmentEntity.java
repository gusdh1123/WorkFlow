package com.workflow.department.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@Table(name = "department")
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA가 프록시 생성할 때 필요
@AllArgsConstructor
@Builder
public class DepartmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB에서 자동 증가
    private Long id;

    // 부서 이름, 예: "Development", "Design", "Operations"
    @Column(nullable = false, length = 100, unique = true) // null 불가, 중복 금지, 길이 제한
    private String name;

    // 부서 코드, 예: "DEV", "DSG", "OPS"
    @Column(nullable = false, length = 20, unique = true) // null 불가, 중복 금지, 길이 제한
    private String code;

    // 생성 시간, 저장 시 자동 입력, 수정 불가
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 마지막 수정 시간, 저장/수정 시 업데이트
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 엔티티가 처음 저장될 때 실행
    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now(); // 생성 시간 기록
        this.updatedAt = LocalDateTime.now(); // 업데이트 시간 초기화
    }

    // 업데이트 시간 변경용 메서드, 수정 시 호출
    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    // 이름 변경 메서드, 변경 시 touch() 호출
    public void setName(String name) {
        this.name = name;
        touch();
    }

    // 코드 변경 메서드, 변경 시 touch() 호출
    public void setCode(String code) {
        this.code = code;
        touch();
    }
}
