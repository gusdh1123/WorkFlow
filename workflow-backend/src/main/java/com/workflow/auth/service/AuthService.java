package com.workflow.auth.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workflow.auth.crypto.TokenHashProvider;
import com.workflow.auth.dto.Tokens;
import com.workflow.auth.entity.AuthEntity;
import com.workflow.auth.jwt.JwtProvider;
import com.workflow.auth.repository.AuthRepository;
import com.workflow.common.exception.UnauthorizedException;
import com.workflow.user.entity.UserEntity;
import com.workflow.user.enums.UserStatus;
import com.workflow.user.repository.UserRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
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
	private final TokenHashProvider tokenHashProvider;

	public Tokens login(UserEntity userEntity) {

		UserEntity user = userRepository.findByEmail(userEntity.getEmail())
				.orElseThrow(() -> new UnauthorizedException("이메일 또는 비밀번호가 다릅니다."));

		if (!passwordEncoder.matches(userEntity.getPassword(), user.getPassword())) {
			// throw new RuntimeException("비밀번호 다름"); = 애가 실행되면 메서드가 종료
			throw new UnauthorizedException("이메일 또는 비밀번호가 다릅니다.");
		}

		LocalDateTime now = LocalDateTime.now();
		
		// 유저 상태 및 마지막 로그인 시간 업데이트
		user.setStatus(UserStatus.ONLINE);
		user.setLastLoginAt(now);

		String accessToken = jwtProvider.createAccessToken(user);
		String refreshToken = jwtProvider.createRefreshToken(user.getId());

		// 해시값 변경
		String hashToken = tokenHashProvider.hashRefreshToken(refreshToken);

		// DB에 리프래쉬 토큰 저장하기
		// 있으면 갱신, 없으면 생성
		// 현재 유효한 토큰 열을 찾음
		AuthEntity auth = authRepository.findByUserAndRevokedAtIsNull(user)
				// 있으면 그 열을 쓰고 없으면 새로 만듬
				.orElseGet(() -> AuthEntity.builder().user(user).build());
		auth.setTokenHash(hashToken);
		auth.setRevokedAt(null);
		auth.setExpiresAt(now.plusDays(7)); // 만료 갱신
		authRepository.save(auth);

		return new Tokens(accessToken, refreshToken);

	}

	// 리프래쉬 토큰으로 액세스 토큰 발급
	@Transactional
	public Tokens refresh(String refreshToken) {

		if (refreshToken == null)
			throw new UnauthorizedException("인증 정보가 유효하지 않습니다.");

		try {

			// 서명/만료 검증 + 파싱
			Claims claims = jwtProvider.parseAndValidate(refreshToken);

			// refresh 토큰인지 확인
			if (!jwtProvider.isRefreshToken(claims)) {
				throw new UnauthorizedException("리프레시 토큰 타입이 아닙니다.");
			}

			// sub(subject) = userId
			Long userId = jwtProvider.getUserId(claims);

			String oldHash = tokenHashProvider.hashRefreshToken(refreshToken);

			// RefreshToken 토큰 찾기(폐기 안 된 것만)
			AuthEntity auth = authRepository.findByTokenHashAndRevokedAtIsNull(oldHash)
					.orElseThrow(() -> new UnauthorizedException("로그아웃 또는 폐기된 토큰입니다."));

			LocalDateTime now = LocalDateTime.now();

			// DB 만료(expiresAt) 체크
			if (auth.getExpiresAt() != null && auth.getExpiresAt().isBefore(now)) {
				throw new UnauthorizedException("리프레시 토큰이 만료되었습니다.");
			}

			// 토큰과 유저 매칭 검증
			// 토큰은 위조가 아니더라도, DB에 있는 토큰의 user와 claims의 userId를 비교해서 확인
			if (!auth.getUser().getId().equals(userId)) {
				throw new UnauthorizedException("토큰 사용자가 불일치합니다.");
			}

			UserEntity user = auth.getUser();

			// System.out.println("액세스 토큰 발급 완료");

			// 리프래시 회전

			// 기존 refresh 폐기
			authRepository.revokeAllActiveByUser(user, now);

			// 새 refresh 발급
			String newRefreshToken = jwtProvider.createRefreshToken(user.getId());

			// 새 refresh DB 저장
			AuthEntity newAuth = AuthEntity.builder().user(user)
					.tokenHash(tokenHashProvider.hashRefreshToken(newRefreshToken)).expiresAt(now.plusDays(7)).build();
			authRepository.save(newAuth);

			// 새 access 발급
			String newAccessToken = jwtProvider.createAccessToken(user);

			return new Tokens(newAccessToken, newRefreshToken);

			// JwtException: 토큰 자체가 유효하지 않은 경우 처리
			// IllegalArgumentException: 토큰 값/형식이 아예 잘못된 경우라 둘 다 인증 실패로 묶어 처리
		} catch (JwtException | IllegalArgumentException e) {
			throw new UnauthorizedException("리프레시 토큰이 유효하지 않습니다.");
		}
	}

	@Transactional
	public void logout(String refreshToken) {

		// 로그아웃은 없어도 성공
		if (refreshToken == null)
			return;

		LocalDateTime now = LocalDateTime.now();

		try {
			String hashToken = tokenHashProvider.hashRefreshToken(refreshToken);

			AuthEntity auth = authRepository.findByTokenHashAndRevokedAtIsNull(hashToken).orElse(null);

			if (auth == null)
				return;

			UserEntity user = auth.getUser();

			// 토큰 1개 로그아웃도 로그인이랑 똑같이 활성 토큰 전부 폐기
			authRepository.revokeAllActiveByUser(user, now);

			// OFFLINE 처리
			user.setStatus(UserStatus.OFFLINE);

		} catch (IllegalArgumentException e) {
			// 해시/입력 문제 → 그냥 종료
			return;
		}
	}

}
