package com.workflow.auth.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workflow.auth.dto.Tokens;
import com.workflow.auth.service.AuthService;
import com.workflow.common.exception.UnauthorizedException;
import com.workflow.common.util.CookieUtil;
import com.workflow.user.entity.UserEntity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserEntity userEntity, HttpServletResponse res){
    	
        Tokens tokens = authService.login(userEntity);

        // 확인용
        // System.out.println("Access 토큰: " + tokens.getAccessToken());
        // System.out.println("Refresh 토큰: " + tokens.getRefreshToken());

        // refreshToken 쿠키 저장 (HttpOnly)
        CookieUtil.addHttpOnlyCookie(res, "refreshToken", tokens.refreshToken(), 10080);

        return ResponseEntity.ok(Map.of("accessToken", tokens.accessToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest req, HttpServletResponse res) {

        String refreshToken = CookieUtil.readCookie(req, "refreshToken");

        // 쿠키 자체가 없으면 = 비로그인 상태
        if (refreshToken == null) {
            return ResponseEntity.noContent().build();
        }

        try {
            // 회전된 access + refresh
            Tokens tokens = authService.refresh(refreshToken);

            // 새 refresh 쿠키 재설정
            CookieUtil.addHttpOnlyCookie(res, "refreshToken", tokens.refreshToken(), 10080);
            
            return ResponseEntity.ok(Map.of("accessToken", tokens.accessToken()));

        } catch (UnauthorizedException e) {

            // refresh 실패 시 쿠키 제거
        	// 잘못되서 이상하게 작동함 없애는 게 맞고 로그아웃에서 삭제하기.
            // CookieUtil.deleteCookie(res,"refreshToken");
            
            return ResponseEntity.status(401).build();
        }
    }

 
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse res, HttpServletRequest req){

        // 먼저 요청에 들어온 refreshToken 읽기
        String refreshToken = CookieUtil.readCookie(req, "refreshToken");

        // 쿠키 삭제
        CookieUtil.deleteCookie(res, "refreshToken");

        // DB에 리프래쉬 토큰 논리적 제거하기
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }

        // 204 넘겨줄 값이 없으니까
        return ResponseEntity.noContent().build();
    }
}
