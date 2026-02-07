package com.workflow.auth.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workflow.auth.dto.Tokens;
import com.workflow.auth.service.AuthService;
import com.workflow.user.entity.UserEntity;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AuthController {
	
//	@Autowired
//	AuthRepository lr;	
//	
//	@PostMapping("/login")
//	public void insterUsers(@RequestBody AuthEntity le) {
//		System.out.println("컨트롤러 진입");
//		System.out.println("출력" + le.getPasswordHash());
//	}
	
	private final AuthService authService;
	
	// 쿠키 생성
	private Cookie httpCookie(String name, String value, int maxMin) {
		
		Cookie cookie = new Cookie(name, value);
		cookie.setHttpOnly(true); // JS 접근 불가(XSS 방어)
		cookie.setSecure(false); // HTTPS면 true
		cookie.setPath("/"); // 모든 요청에 응답
		cookie.setMaxAge(maxMin * 60); // 1시간 (초)
		
		return cookie;
	}
	
	// 쿠키 읽기
	private String readCookie(HttpServletRequest req, String name) {
		
		if(req.getCookies()==null) return null;
		
		for(Cookie c : req.getCookies()) {
			if(name.equals(c.getName())) return c.getValue();
		}
		
		return null;
	}
	
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody UserEntity userEntity, HttpServletResponse res){
		
		Tokens tokens = authService.login(userEntity.getEmail(), userEntity.getPassword());
		
		// 확인용
		System.out.println("Access 토큰: " + tokens.getAccessToken());
		System.out.println("Refresh 토큰: " + tokens.getRefreshToken());
		
		res.addCookie(httpCookie("refreshToken", tokens.getRefreshToken(), 10080));
		
		return ResponseEntity.ok(Map.of("accessToken", tokens.getAccessToken()));
		
	}
	
	@PostMapping("/refresh")
	public ResponseEntity<?> refresh(HttpServletRequest req, HttpServletResponse res){
		
		String refreshToken = readCookie(req, "refreshToken");
		String newAccessToken = authService.refresh(refreshToken);// 서비스에서 다시 발급 코드
		
		return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
		
	}
	
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletResponse res, HttpServletRequest req){
		
		res.addCookie(httpCookie("refreshToken", "", 0));
		
		// DB에 리프래쉬 토큰 제거하기
		
		System.out.println("토큰" + req.getCookies());
		
		return ResponseEntity.ok().build();
	}
	
	

}
