package com.workflow.common.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.workflow.auth.jwt.JwtAuthFilter;
import com.workflow.auth.jwt.JwtProvider;

// 스프링 설정 클래스, 클래스 안의 @Bean들을 스프링이 등록함.
@Configuration
// 스프링 시큐리티 기능을 활성화하고 아래의 설정들을 적용
@EnableWebSecurity
public class SecurityConfig {

	// 핵심
	// SecurityFilterChain: 보안 필터 규칙 세트
	// HttpSecurity로 어떤 요청을 허용/차단할지, 어떤 인증 방식을 쓸지 설정
	// JwtAuthFilter를 여기에 끼워 넣는 구조
	@Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        return http
                // JWT 기반이므로 CSRF 비활성화 (세션 기반 아님)
                .csrf(csrf -> csrf.disable())

                // CORS 설정 사용 (아래 Bean과 연결됨)
                .cors(cors -> {})

                // 세션을 사용하지 않는 완전 Stateless 구조
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // React 연동 중엔 폼로그인/기본인증 끄기 (리다이렉트 방지)
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                
                // 인증 실패시 302 말고 401/403
                // REST API는 리다이렉트가 아니라 상태코드 반환이 맞음
		        .exceptionHandling(ex -> ex
				        .authenticationEntryPoint((req, res, e) -> res.sendError(401))
				        .accessDeniedHandler((req, res, e) -> res.sendError(403))
                )

				// 지금은 일단: auth API만 열고, 나머지 API 보호
		        .authorizeHttpRequests(auth -> auth

		        	// /api/health만 임시로 열어서 200뜨게 하기(연결 확인용) 추가
		        	// .requestMatchers("/api/health").permitAll() 

                	// 로그인은 누구나 가능
                	.requestMatchers(HttpMethod.POST, "/api/login").permitAll()

                	// 로그아웃은 refreshToken 기반이라 일단 열어둠
                	.requestMatchers(HttpMethod.POST, "/api/logout").permitAll()

                	// refresh도 access 만료 시 재발급용이라 permit
                	.requestMatchers(HttpMethod.POST, "/api/refresh").permitAll()
                	
                	// 로그인 안해도 이미지 보이게
                	.requestMatchers("/uploads/**").permitAll()

                    // 관리자 API는 ADMIN 권한 필요
                    // hasRole("ADMIN") → 내부적으로 ROLE_ADMIN 비교
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")

                    // 로그인 후에만 api접근 가능
                    .requestMatchers("/api/**").authenticated()

                    // 그 외 요청은 허용 (프론트 라우팅 등)
                    .anyRequest().permitAll()
                )

                // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 삽입
                // 즉, 아이디/비번 로그인 필터 전에 JWT 먼저 검사
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }

	@Bean
	CorsConfigurationSource corsConfigurationSource() {

		// CORS 정책 설정 객체
		CorsConfiguration config = new CorsConfiguration();

		// 허용할 프론트 주소
		// 배포 시 도메인 추가 필요
		config.setAllowedOrigins(List.of("http://localhost:5173"));

		// 허용 HTTP 메서드
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

		// 모든 헤더 허용
		config.setAllowedHeaders(List.of("*"));

		// 브라우저에서 접근 가능하게 노출할 헤더
		// Authorization → JWT 전달용
		// Content-Disposition → 파일 다운로드용
		config.setExposedHeaders(List.of("Authorization", "Content-Disposition"));

		// credentials = 쿠키 / 인증 정보
		// JWT를 헤더로만 쓸 거면 false 권장
		config.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

		// 모든 경로에 CORS 정책 적용
		source.registerCorsConfiguration("/**", config);

		return source;
	}

//	// DB 없이 메모리 유저
//	// UserDetailsService 시큐리티가 이 username의 사용자 정보 가져와 할때 쓰이는 인터페이스
//	// DB가 있다면 보통 userRepositiry로 조회해서 반환
//	@Bean
//	public UserDetailsService userDetailsService(PasswordEncoder encoder) {
//		UserDetails user = User.builder().username("user").password(encoder.encode("1234")).roles("USER").build();
//		return new InMemoryUserDetailsManager(user);
//	}

	// 비밀번호를 평문으로 비교하면 위험하니 해시로 비교
	// BCryptPasswordEncoder가 가장 흔한 표준
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	// /login에서 authenticate() 쓸 때 필요
	// ex) authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(id, pw))
	// 이걸로 시큐리티 방식대로 인증 수행
	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	// 필터도 Bean으로 등록 (순환참조 끊기)
	// JwtAuthFilter를 스프링 빈으로 등록해서 위의 .addFilterBefore(jwtAuthFilter, ...)에 주입되게 함.
	@Bean
	public JwtAuthFilter jwtAuthFilter(JwtProvider jwtProvider, UserDetailsService userDetailsService) {
		return new JwtAuthFilter(jwtProvider, userDetailsService);
	}
}
