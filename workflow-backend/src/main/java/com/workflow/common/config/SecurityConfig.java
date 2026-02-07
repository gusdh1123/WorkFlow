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
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
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
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {}) // 아래 CORS Bean 필요
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // React 연동 중엔 폼로그인/기본인증 끄기 (리다이렉트 방지)
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                
                // 인증 실패시 302 말고 401/403
		            .exceptionHandling(ex -> ex
				                .authenticationEntryPoint((req, res, e) -> res.sendError(401))
				                .accessDeniedHandler((req, res, e) -> res.sendError(403))
            )
								// 지금은 일단: auth API만 열고, 나머지 API 보호
		            .authorizeHttpRequests(auth -> auth
		            	// /api/health만 임시로 열어서 200뜨게 하기(연결 확인용) 추가
		            	// .requestMatchers("/api/health").permitAll() 
            	.requestMatchers(HttpMethod.POST, "/api/login").permitAll()
            	.requestMatchers(HttpMethod.POST, "/api/logout").permitAll()
            	 .requestMatchers(HttpMethod.POST, "/api/refresh").permitAll()
                // .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // 로그인 후에만 api접근 가능
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(List.of("http://localhost:5173"));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setExposedHeaders(List.of("Authorization"));
		// credentials = 쿠키 / 인증 정보
		config.setAllowCredentials(true); // JWT를 헤더로만 쓸 거면 false 권장

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
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
	// ex) uthenticationManager.authenticate(new
	// UsernamePasswordAuthenticationToken(id, pw)) 이걸로 시큐리티 방식대로 인증 수행
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
