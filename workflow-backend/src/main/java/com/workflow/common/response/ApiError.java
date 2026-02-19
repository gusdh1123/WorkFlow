package com.workflow.common.response;

import java.time.Instant;

// API 예외/오류 응답 DTO
// 클라이언트에 전달될 오류 정보를 담음
public record ApiError(
        String code,     // 내부 오류 코드: "AUTH_INVALID", "TOKEN_EXPIRED" 등
        String message,  // 사용자/프론트에 보여줄 메시지
        Instant timestamp // 오류 발생 시각 (서버 시간 기준)
) {
    // 간편 팩토리 메서드: timestamp 자동 설정
    public static ApiError of(String code, String message) {
        return new ApiError(code, message, Instant.now());
    }
}
