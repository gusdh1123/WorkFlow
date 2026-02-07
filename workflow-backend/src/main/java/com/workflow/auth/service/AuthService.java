package com.workflow.auth.service;

import java.time.LocalDateTime;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workflow.auth.dto.Tokens;
import com.workflow.auth.entity.AuthEntity;
import com.workflow.auth.jwt.JwtProvider;
import com.workflow.auth.repository.AuthRepository;
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
	private final AuthRepository authRepository;
	
	// 해시 secret 키 -> properties에 있음
	@Value("${hasher.secret}")
	private String hashSecret;
	
	
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
		
		// 해시값 변경
		String hashToken = hash(refresToken);
		// DB에 리프래쉬 토큰 저장하기
		AuthEntity authEntity = AuthEntity.builder()
				.user(user)
				.tokenHash(hashToken)
				.build();
		authRepository.save(authEntity);
		
		return new Tokens(accessToken, refresToken);	
		
	}
	
	// 리프래쉬 토큰으로 액세스 토큰 발급
	@Transactional // (readOnly = true)
	public String refresh(String refreshToken) {
		
		if(refreshToken == null || !jwtProvider.validate(refreshToken) || !jwtProvider.isRefreshToken(refreshToken)) {
			throw new RuntimeException("리프레시 토큰 유효 X");
		}
		
		String email = jwtProvider.getClaims(refreshToken).getSubject();
		
		// DB에 이메일 있는지 비교 및 값 가져오기
//		UserEntity user = userRepository.findByEmail(email)
//				.orElseThrow(() -> new RuntimeException("사용자 못 찾음"));
		
		// RefreshToken 토큰 해시값 변경
		String hashToken = hash(refreshToken);
		// RefreshToken 토큰 찾기
		AuthEntity auth = authRepository.findByTokenHashAndRevokedAtIsNull(hashToken)
				.orElseThrow(() -> new RuntimeException("로그아웃 또는 폐기된 토큰"));
		
		// AuthEntity의 FK덕분에 위 상황이 맞다면 알아서 참조해서 가져옴
		UserEntity user = auth.getUser();
		
		return jwtProvider.createAccessToken(user.getEmail(), user.getRole());
	}
	
	@Transactional 
	public void logout(String logoutRefreshToken) {
		
		// RefreshToken 가져와서 해시값으로 변경
		String hashToken = hash(logoutRefreshToken);
		
		// 해시값으로 변경한 RefreshToken 찾기
		AuthEntity auth = authRepository.findByTokenHashAndRevokedAtIsNull(hashToken)
		.orElseThrow(() -> new RuntimeException("이미 로그아웃"));
		
		// 로그아웃 누른 현 시점 시각 넣음
		auth.setRevokedAt(LocalDateTime.now());
	}
	
	
	// 해시값 변경 로직
	// SHA_256 -> 시크릿키 없이도 입력 = 같은 결과
	// HMAC_SHA_256 -> 시크릿키 없으면 해시만들기 불가
	private String hash(String refreshToken) {
		return new HmacUtils(HmacAlgorithms.HMAC_SHA_256, hashSecret)
                .hmacHex(refreshToken);
	}
	

}
