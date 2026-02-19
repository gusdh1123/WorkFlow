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
import com.workflow.common.exception.ApiException;
import com.workflow.common.exception.ErrorCode;
import com.workflow.user.entity.UserEntity;
import com.workflow.user.enums.UserStatus;
import com.workflow.user.repository.UserRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final AuthRepository authRepository;
    private final TokenHashProvider tokenHashProvider;

    // 로그인
    public Tokens login(String email, String password) {

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 다릅니다."));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 다릅니다.");
        }

        LocalDateTime now = LocalDateTime.now();

        // Single Session 정책: 기존 활성 토큰 모두 폐기
        authRepository.revokeAllActiveByUser(user, now);

        // 유저 상태 업데이트
        user.setStatus(UserStatus.ONLINE);
        user.setLastLoginAt(now);

        String accessToken = jwtProvider.createAccessToken(user);
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        String hashToken = tokenHashProvider.hashRefreshToken(refreshToken);

        // DB에 upsert
        authRepository.upsertToken(
            user.getId(),
            hashToken,
            now,
            now.plusDays(7),
            null
        );

        return new Tokens(accessToken, refreshToken);
    }

    // 리프래쉬 토큰으로 액세스 토큰 발급
    @Transactional
    public Tokens refresh(String refreshToken) {

        if (refreshToken == null)
            throw new ApiException(ErrorCode.UNAUTHORIZED, "인증 정보가 유효하지 않습니다.");

        try {
            Claims claims = jwtProvider.parseAndValidate(refreshToken);

            if (!jwtProvider.isRefreshToken(claims)) {
                throw new ApiException(ErrorCode.UNAUTHORIZED, "리프레시 토큰 타입이 아닙니다.");
            }

            Long userId = jwtProvider.getUserId(claims);
            String oldHash = tokenHashProvider.hashRefreshToken(refreshToken);

            // 기존 refresh token 조회
            AuthEntity auth = authRepository.findByTokenHashAndRevokedAtIsNull(oldHash)
                    .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "로그아웃 또는 폐기된 토큰입니다."));

            LocalDateTime now = LocalDateTime.now();

            if (auth.getExpiresAt() != null && auth.getExpiresAt().isBefore(now)) {
                throw new ApiException(ErrorCode.UNAUTHORIZED, "리프레시 토큰이 만료되었습니다.");
            }

            if (!auth.getUser().getId().equals(userId)) {
                throw new ApiException(ErrorCode.UNAUTHORIZED, "토큰 사용자가 불일치합니다.");
            }

            UserEntity user = auth.getUser();

            // 기존 refresh 토큰 폐기
            authRepository.revokeAllActiveByUser(user, now);

            // 새 refresh 발급
            String newRefreshToken = jwtProvider.createRefreshToken(user.getId());
            String newHash = tokenHashProvider.hashRefreshToken(newRefreshToken);

            // DB upsert
            authRepository.upsertToken(
                user.getId(),
                newHash,
                now,
                now.plusDays(7),
                null
            );

            String newAccessToken = jwtProvider.createAccessToken(user);

            return new Tokens(newAccessToken, newRefreshToken);

        } catch (JwtException | IllegalArgumentException e) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "리프레시 토큰이 유효하지 않습니다.");
        }
    }

    // 로그아웃
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null) return;

        LocalDateTime now = LocalDateTime.now();

        try {
            String hashToken = tokenHashProvider.hashRefreshToken(refreshToken);

            AuthEntity auth = authRepository
                    .findByTokenHashAndRevokedAtIsNull(hashToken)
                    .orElse(null);

            if (auth == null) return;

            UserEntity user = auth.getUser();

            authRepository.revokeAllActiveByUser(user, now);

            user.setStatus(UserStatus.OFFLINE);

        } catch (IllegalArgumentException e) {
            return;
        }
    }
}
