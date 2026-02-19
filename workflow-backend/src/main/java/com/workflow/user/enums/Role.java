package com.workflow.user.enums;

// 권한
public enum Role {
    USER,    // 일반 사용자 권한
    ADMIN,   // 시스템 관리자 권한, 모든 기능 접근 가능
    MANAGER  // 부서 관리자 권한, 부서 관련 관리 기능 접근 가능
}
