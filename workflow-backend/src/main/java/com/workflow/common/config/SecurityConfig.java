package com.workflow.common.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {}) // 아래 CORS Bean 필요

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
            	.requestMatchers("/api/kpi").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                // 로그인 후에만 api접근 가능
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            );

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(false); // JWT를 헤더로만 쓸 거면 false 권장

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
