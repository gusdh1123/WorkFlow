package com.workflow.auth.jwt;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
//OncePerRequestFilter: 요청 1번당 필터도 1번만 실행되게 보장하는 편한 필터 베이스 클래스
//일반 Filter는 forward/include 시 여러 번 실행될 수 있는데
//OncePerRequestFilter는 그런 중복 실행을 방지해줌
public class JwtAuthFilter extends OncePerRequestFilter {

 // jwtProvider: JWT 파싱 + 서명/만료 검증 + claims 추출 담당
 private final JwtProvider jwtProvider;

 // userDetailsService:
 // userId를 기준으로 UserDetails(권한 포함)를 로딩하는 서비스
 // 메서드 이름은 loadUserByUsername지만 실제로는 "식별자 문자열"을 받는 용도
 // 여기서는 username 대신 userId를 문자열로 전달해서 사용
 private final UserDetailsService userDetailsService;

 @Override
 protected void doFilterInternal(
         HttpServletRequest request,
         HttpServletResponse response,
         FilterChain filterChain
 ) throws ServletException, IOException {

     // 나중에 차단, 삭제 이런 계정 상태 추가 할거면.
     // if (u.getStatus() == UserStatus.DELETED || u.getStatus() == UserStatus.BLOCKED) {
     //     throw new UsernameNotFoundException("inactive user");
     // }

     // 이미 인증된 상태면 스킵
     // (다른 필터에서 Authentication을 이미 세팅했을 수도 있음)
     // 중복 인증 세팅 방지 목적
     if (SecurityContextHolder.getContext().getAuthentication() != null) {
         filterChain.doFilter(request, response);
         return;
     }

     // 헤더에서 Authorization 값을 꺼냄
     String auth = request.getHeader("Authorization");

     // Bearer 토큰 형식인지 체크
     // 헤더가 없거나 "Bearer "로 시작하지 않으면
     // JWT 인증 대상이 아니므로 그냥 다음 필터로 넘김
     // (로그인, 정적 리소스, 공개 API 등)
     if (auth == null || !auth.startsWith("Bearer ")) {
         filterChain.doFilter(request, response);
         return;
     }

     // 실제 토큰 문자열만 추출
     // "Bearer "는 7글자 (B e a r e r + 공백)
     String token = auth.substring(7);

     try {
         // 1. JWT 파싱 + 서명/만료 검증 (한 번만)
         // 여기서 실패하면 예외 발생 (만료, 위조, 형식 오류 등)
         Claims claims = jwtProvider.parseAndValidate(token);

         // 2. access 토큰인지 확인
         // refresh 토큰이 Authorization 헤더로 오는 걸 방지
         // refresh 토큰은 오직 쿠키 + /refresh 엔드포인트에서만 사용
         if (!jwtProvider.isAccessToken(claims)) {
             filterChain.doFilter(request, response);
             return;
         }

         // 3. sub(subject)에 담아둔 userId 꺼내기
         // JWT 설계상 sub = userId(PK)
         Long userId = jwtProvider.getUserId(claims);

         // 4. userId로 사용자 정보 로딩
         // UserDetailsService의 메서드 이름은 username이지만
         // 실제로는 "식별자 문자열"을 받는 용도라 userId를 문자열로 전달
         // 여기서 권한 정보까지 같이 로딩됨
         UserDetails user = userDetailsService.loadUserByUsername(String.valueOf(userId));

         // 5. 인증 객체 생성
         // UsernamePasswordAuthenticationToken의 두 가지 용도
         // 1) 로그인 시도용 (username/password)
         // 2) 인증 완료 상태 표현용 (principal + authorities)
         // 지금은 2번 케이스
         var authentication =
                 new UsernamePasswordAuthenticationToken(
                         user,           // principal (누구인가)
                         null,           // credentials (이미 인증 완료라 null)
                         user.getAuthorities() // 권한 목록
                 );

         // 6. SecurityContext에 인증 정보 저장
         // 이 이후부터 Spring Security는
         // "이 요청은 인증된 사용자 요청이다"라고 판단
         // Controller에서 @AuthenticationPrincipal 사용 가능
         SecurityContextHolder.getContext()
                 .setAuthentication(authentication);

     } catch (JwtException | IllegalArgumentException e) {
         // 토큰이 만료되었거나, 위조되었거나, 형식이 잘못된 경우
         // 인증 정보 세팅 안 함
         // 보호된 API에서는 SecurityConfig의 EntryPoint가 401 처리
         // 여기서 401을 직접 주지 않는 이유:
         // 필터는 인증만 담당하고, 응답 결정은 Security가 하도록 분리
         SecurityContextHolder.clearContext();
     }

     // 다음 필터 / 컨트롤러로 요청 전달
     filterChain.doFilter(request, response);
 }
}

