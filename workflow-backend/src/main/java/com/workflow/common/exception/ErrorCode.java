package com.workflow.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // 400 Bad Request 관련
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."), // 일반적인 잘못된 요청
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "입력값을 확인해주세요."), // 입력값 검증 실패 시 사용

    // 401 Unauthorized 관련
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."), // 로그인 필요
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "토큰이 유효하지 않습니다."), // JWT 등 인증 토큰 오류

    // 403 Forbidden 관련
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."), // 인증은 되었지만 권한 없음

    // 404 Not Found 관련
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."), // 데이터 존재하지 않을 때

    // 500 Internal Server Error 관련
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."); // 서버 내부 문제

    private final HttpStatus status; // HTTP 상태 코드
    private final String defaultMessage; // 기본 사용자 메시지 (커스텀 메시지 없을 때 사용)

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus() {
        return status; // 글로벌 예외 처리에서 ResponseEntity.status에 사용
    }

    public String getDefaultMessage() {
        return defaultMessage; // ApiException에서 기본 메시지로 사용
    }

    // 주의:
    // ErrorCode 자체는 예외가 아니며, 단순 코드+메시지+HTTP 상태 묶음
    // 실제 예외 발생 시 ApiException과 함께 사용하여 글로벌 핸들러에서 처리
}
