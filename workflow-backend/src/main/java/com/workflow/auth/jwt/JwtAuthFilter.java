package com.workflow.auth.jwt;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import com.workflow.auth.service.AuthService;
import com.workflow.auth.service.CustomUserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
// OncePerRequestFilter: 요청 1번당 필터도 1번만 실행되게 보장하는 편한 필터 베이스 클래스
public class JwtAuthFilter extends OncePerRequestFilter {

	// jwtProvider: 토큰 검증(validate), 토큰에서 username 추출(getEmail) 등을 하는 애
    private final JwtProvider jwtProvider;
    // userDetailsService: email으로 UserDetails(권한 포함) 를 로딩하는 애
    // ex) 너는 InMemoryUserDetailsManager로 “user/1234/ROLE_USER” 들어있지
    private final UserDetailsService userDetailsService;

//    // 생성자
//    public JwtAuthFilter(JwtProvider jwtProvider, UserDetailsService userDetailsService) {
//        this.jwtProvider = jwtProvider;
//        this.userDetailsService = userDetailsService;
//    }

    // 모든 요청이 들어올 때마다 여기 실행됨
    // request: 들어온 HTTP 요청
    // response: 나갈 HTTP 응답
    // filterChain: “다음 필터/컨트롤러로 넘겨라” 할 때 사용
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

    	// 헤더에서 Authorization 값을 꺼냄.
        String auth = request.getHeader("Authorization");

        // Bearer 토큰 형식인지 체크
        // 헤더가 존재하고, "Bearer "로 시작하면 JWT가 있다고 판단.
        // 이 조건을 통과 못하면 통이 없는 요청이거나 형식이 잘못된 요청이니까 인증 처리 안 하고 그냥 다음으로 넘김.
        if (auth != null && auth.startsWith("Bearer ")) {
        	
        	// 실제 토큰 문자열만 뽑기
        	// "Bearer "는 7글자 (B e a r e r + 공백), 앞의 "Bearer " 제거하고 JWT만 남김.
            String token = auth.substring(7);

            // 토큰 유효성 검증
            // validate()가 보통 하는 일
            // 서명(Signature) 검증: 위조/변조 여부 확인, 만료(exp) 확인: 시간이 지났는지, 토큰 형식이 정상인지
            if (jwtProvider.validate(token)) {
            	// 토큰에서 email 꺼내기
            	// JWT payload의 sub(subject) 같은 곳에 email을 넣어두는 경우가 흔함. getUsername()는 거기서 값을 꺼내는 메서드.
                String email = jwtProvider.getEmail(token);
                // email으로 사용자 정보 로딩
                // 여기서 DB든 메모리든 사용자 정보를 가져옴.
                // 중요 포인트: 권한(authorities)을 얻으려고 이걸 함(ROLE_USER, ROLE_ADMIN 같은 권한 정보)
                // 즉, 토큰에 role을 넣지 않았더라도 서버가 UserDetailsService로 다시 가져와서 권한을 채울 수 있음.
                UserDetails user = userDetailsService.loadUserByUsername(email);

                // 인증된 사요자 객체 만들기
                // sernamePasswordAuthenticationToken의 두 가지 용도
                // 1. 로그인 시도할 때: email/password 넣어서 authenticate()로 보냄
                // 2. 로그인 완료 상태 만들 때: principal(UserDetails) + authorities 넣어서 “이미 인증됨”으로 만듦
                // user : principal(누구냐) → UserDetails
                // null : credentials(비밀번호) → 이미 인증 끝났으니 굳이 안 넣음
                // user.getAuthorities() : 권한 목록
                var authentication =
                        new UsernamePasswordAuthenticationToken(
                                user, null, user.getAuthorities()
                        );

                // SecurityContext에 인증 정보 세팅
                // Spring Security가 “이 요청은 인증된 요청이다”라고 판단하는 근거가 여기서 만들어짐.
                // 이걸 해두면 이후에:.anyRequest().authenticated() 통과
                // 컨트롤러 파라미터로 Authentication 주입 가능
                // @AuthenticationPrincipal 사용 가능
                // 즉 /me에서 public String me(Authentication authentication) 이게 값이 들어오는 이유가 바로 여기임.
                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
            }
        }

        // 다음 필터/컨트롤러로 넘기기
        // 이 줄을 호출해야 요청이 계속 진행돼서 컨트롤러까지 감.
        // 이 줄이 없으면 요청이 여기서 멈춤.
        filterChain.doFilter(request, response);
    }
}
