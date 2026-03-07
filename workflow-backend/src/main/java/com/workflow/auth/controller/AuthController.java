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
        // 로그인 수행 후 access + refresh 토큰 발급
        Tokens tokens = authService.login(request.email(), request.password());

        // refreshToken 쿠키 저장 (HttpOnly + Secure + SameSite 필요)
        CookieUtil.addHttpOnlyCookie(res, "refreshToken", tokens.refreshToken(), 20160);

        // accessToken 반환
        return ResponseEntity.ok(Map.of("accessToken", tokens.accessToken()));
    }

    // 토큰 갱신
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest req, HttpServletResponse res) {

        // 요청 쿠키에서 refreshToken 읽기
        String refreshToken = CookieUtil.readCookie(req, "refreshToken");

        // 쿠키 자체가 없으면 = 비로그인 상태
        if (refreshToken == null) {
            // ApiException을 던져서 글로벌 핸들러에서 처리
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }

        // refreshToken 유효하면 회전된 access + refresh 발급
        Tokens tokens = authService.refresh(refreshToken);

        // 새 refresh 쿠키 재설정
        CookieUtil.addHttpOnlyCookie(res, "refreshToken", tokens.refreshToken(), 10080);

        // 새로운 accessToken 반환
        return ResponseEntity.ok(Map.of("accessToken", tokens.accessToken()));
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
            authService.logout(refreshToken);
        }

        // 204 넘겨줄 값이 없으므로 noContent 반환
        return ResponseEntity.noContent().build();
    }
}