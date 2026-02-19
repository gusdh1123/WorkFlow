package com.workflow.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;

// SecretKey : 서명/검증에 사용할 키 타입
// StandardCharsets.UTF_8 : 문자열을 바이트로 변환할 때 인코딩 고정
// Date : issuedAt / expiration 설정용
import javax.crypto.SecretKey;

// @Component로 빈 등록
// @Value로 application.properties/yml 값 주입
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.workflow.user.entity.UserEntity;

import io.jsonwebtoken.Claims;
// jjwt 라이브러리(0.12.x)의 핵심 클래스들
// Jwts.builder() : 토큰 만들기
// Jwts.parser() : 토큰 파싱/검증하기
// Keys.hmacShaKeyFor() : HMAC 서명용 키 만들기
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

// JWT를 “발급(createToken)”, “검증(validate)”, “토큰에서 username 뽑기(getUsername)” 하는 유틸 클래스
// 이 클래스를 스프링이 자동으로 Bean으로 등록
// 다른 곳에서 JwtProvider를 주입받아 사용할 수 있게 됨
@Component
public class JwtProvider {

	// key: JWT 서명(Sign)과 검증(Verify)에 쓰는 비밀키
	// accessExpMillis : Access 토큰 만료 시간(밀리초 단위)
	// refreshExpMillis : Refresh 토큰 만료 시간(밀리초 단위)
	private final SecretKey key;
	private final long accessExpMillis;
	private final long refreshExpMillis;

	// 생성자: 설정값 주입 + 가공
	// @Value("${jwt.secret}"): application.properties에 있는 jwt.secret 값을 문자열로 주입
	// @Value("${jwt.access-exp-min}"): access 토큰 만료시간(분)
	// @Value("${jwt.refresh-exp-day}"): refresh 토큰 만료시간(일)
	public JwtProvider(@Value("${jwt.secret}") String secret,
	                   @Value("${jwt.access-exp-min}") long accessExpMin,
	                   @Value("${jwt.refresh-exp-day}") long refreshExpDay) {

		// 보안 핵심 부분
		// secret 문자열을 UTF-8 바이트 배열로 변환
		// HMAC 서명용 SecretKey 생성
		// HS256 기준 최소 32바이트 이상 권장 (짧으면 보안 취약)
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

		// 분 → 밀리초 변환
		this.accessExpMillis = accessExpMin * 60_000L;

		// 일 → 밀리초 변환
		this.refreshExpMillis = refreshExpDay * 24L * 60L * 60_000L;
	}

	// Access 토큰 생성
	public String createAccessToken(UserEntity userEntity) {

		Date now = new Date();
		Date exp = new Date(now.getTime() + accessExpMillis);

		return Jwts.builder()
				// subject(sub) 에 사용자 고유값 저장 (여기선 userId)
				.subject(String.valueOf(userEntity.getId()))

				// 권한 정보 (ROLE_USER / ROLE_ADMIN 등)
				.claim("role", userEntity.getRole().name())

				// 토큰 타입 구분 (access)
				.claim("typ", "access")

				// 추가 사용자 정보 (프론트에서 활용 가능)
				.claim("name", userEntity.getName())
				.claim("email", userEntity.getEmail())

				// 발급 시간
				.issuedAt(now)

				// 만료 시간
				.expiration(exp)

				// 서명 (위변조 방지 핵심)
				.signWith(key)

				// 최종 JWT 문자열 생성
				.compact();
	}

	// Refresh 토큰 생성
	public String createRefreshToken(Long userId) {

		Date now = new Date();
		Date exp = new Date(now.getTime() + refreshExpMillis);

		return Jwts.builder()
				.subject(String.valueOf(userId))

				// 토큰 타입 구분 (refresh)
				.claim("typ", "refresh")

				.issuedAt(now)
				.expiration(exp)
				.signWith(key)
				.compact();
	}

	// 서명 + 만료 검증 파싱
	public Claims parseAndValidate(String token) {

		// 이 과정에서 자동으로 검증되는 것:
		// 1. 서명 위변조 여부
		// 2. 만료(exp) 여부
		// 3. 토큰 형식 유효성
		// 하나라도 실패하면 예외 발생

		return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	// 토큰 타입 체크
	public boolean isRefreshToken(Claims claims) {
		return "refresh".equals(claims.get("typ", String.class));
	}

	public boolean isAccessToken(Claims claims) {
		return "access".equals(claims.get("typ", String.class));
	}

	// Claims 정보 추출
	public String getRole(Claims claims) {
		return claims.get("role", String.class);
	}

	public Long getUserId(Claims claims) {
		return Long.parseLong(claims.getSubject());
	}
}
