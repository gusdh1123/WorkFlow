package com.workflow.auth.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workflow.auth.dto.LoginRequest;
import com.workflow.auth.dto.Tokens;
import com.workflow.auth.service.AuthService;
import com.workflow.common.exception.ApiException;
import com.workflow.common.exception.ErrorCode;
import com.workflow.common.util.CookieUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletResponse res) {
        try {
            // 로그인 수행 후 access + refresh 토큰 발급
            Tokens tokens = authService.login(request.email(), request.password());

            // refreshToken 쿠키 저장 (HttpOnly + Secure + SameSite 필요)
            CookieUtil.addHttpOnlyCookie(res, "refreshToken", tokens.refreshToken(), 10080);

            // accessToken 반환
            return ResponseEntity.ok(Map.of("accessToken", tokens.accessToken()));

        } catch (ApiException e) {
            // ApiException 발생 시 ErrorCode 기반으로 상태와 메시지 반환
            return ResponseEntity
                    .status(e.getErrorCode().getStatus())
                    .body(Map.of(
                        "error", e.getErrorCode().name(),
                        "message", e.getErrorCode().getDefaultMessage()
                    ));
        }
    }

    // 토큰 갱신
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest req, HttpServletResponse res) {

        // 요청 쿠키에서 refreshToken 읽기
        String refreshToken = CookieUtil.readCookie(req, "refreshToken");

        // 쿠키 자체가 없으면 = 비로그인 상태
        if (refreshToken == null) {
            // UNAUTHORIZED 상태와 메시지 반환
            ErrorCode error = ErrorCode.UNAUTHORIZED;
            return ResponseEntity
                    .status(error.getStatus())
                    .body(Map.of(
                        "error", error.name(),
                        "message", error.getDefaultMessage()
                    ));
        }

        try {
            // refreshToken 유효하면 회전된 access + refresh 발급
            Tokens tokens = authService.refresh(refreshToken);

            // 새 refresh 쿠키 재설정
            CookieUtil.addHttpOnlyCookie(res, "refreshToken", tokens.refreshToken(), 10080);

            // 새로운 accessToken 반환
            return ResponseEntity.ok(Map.of("accessToken", tokens.accessToken()));

        } catch (ApiException e) {
            // refresh 실패 시 쿠키 제거 (탈취된 토큰이 계속 시도되는 것 방지)
            CookieUtil.deleteCookie(res, "refreshToken");

            // ErrorCode 기반 상태와 메시지 반환
            return ResponseEntity
                    .status(e.getErrorCode().getStatus())
                    .body(Map.of(
                        "error", e.getErrorCode().name(),
                        "message", e.getErrorCode().getDefaultMessage()
                    ));
        }
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse res, HttpServletRequest req) {

        // 요청 쿠키에서 refreshToken 읽기
        String refreshToken = CookieUtil.readCookie(req, "refreshToken");

        // 쿠키 삭제 (클라이언트 쪽 제거)
        CookieUtil.deleteCookie(res, "refreshToken");

        // DB에서 refreshToken 논리적 제거
        if (refreshToken != null) {
            try {
                authService.logout(refreshToken);
            } catch (ApiException e) {
                // 로그아웃 실패 시 ErrorCode 기반으로 상태와 메시지 반환
                return ResponseEntity
                        .status(e.getErrorCode().getStatus())
                        .body(Map.of(
                            "error", e.getErrorCode().name(),
                            "message", e.getErrorCode().getDefaultMessage()
                        ));
            }
        }

        // 204 넘겨줄 값이 없으므로 noContent 반환
        return ResponseEntity.noContent().build();
    }
}
