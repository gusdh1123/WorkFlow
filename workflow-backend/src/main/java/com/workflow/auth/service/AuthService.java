package com.workflow.auth.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workflow.auth.dto.Tokens;
import com.workflow.auth.jwt.JwtProvider;
import com.workflow.user.entity.UserEntity;
import com.workflow.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
// final이거나 @NonNull이 붙은 필드만 파라미터로 받는 생성자를 자동 생성
@RequiredArgsConstructor
@Transactional
public class AuthService {
	
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtProvider jwtProvider;
	
	
	public Tokens login(String email, String rawPassword) {
		
		UserEntity user = userRepository.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("이메일 다름"));
		
		if(!passwordEncoder.matches(rawPassword, user.getPassword())) {
			// throw new RuntimeException("비밀번호 다름"); = 애가 실행되면 메서드가 종료
			throw new RuntimeException("비밀번호 다름");
		}
		
		user.setLastLoginAt(LocalDateTime.now());
		
		String accessToken = jwtProvider.createAccessToken(user.getEmail(), user.getRole());
		String refresToken = jwtProvider.createRefreshToken(user.getEmail());
		
		// DB에 리프래쉬 토큰 저장하기
		
		return new Tokens(accessToken, refresToken);	
		
	}
	
	// 리프래쉬 토큰으로 액세스 토큰 발급
	@Transactional(readOnly = true)
	public String refresh(String refreshToken) {
		if(refreshToken == null || !jwtProvider.validate(refreshToken) || !jwtProvider.isRefreshToken(refreshToken)) {
			throw new RuntimeException("리프레시 토큰 유효 X");
		}
		
		String email = jwtProvider.getClaims(refreshToken).getSubject();
		
		// DB에 이메일 있는지 비교 및 값 가져오기
		UserEntity user = userRepository.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("사용자 못 찾음"));
		
		return jwtProvider.createAccessToken(user.getEmail(), user.getRole());
	}

}
