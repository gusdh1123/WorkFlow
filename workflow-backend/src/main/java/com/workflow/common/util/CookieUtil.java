package com.workflow.common.util;

import org.springframework.http.ResponseCookie;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// HTTP 쿠키 생성/삭제/읽기용 유틸 클래스
public class CookieUtil {

    private CookieUtil() {} // 유틸 클래스이므로 인스턴스화 방지

    // HttpOnly 쿠키 생성
    public static void addHttpOnlyCookie(HttpServletResponse res, String name, String value, int maxMin) {
        // ResponseCookie 사용 → 더 안전하게 쿠키 옵션 지정 가능
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)      // JS에서 접근 불가 → XSS 공격 방어
                .secure(false)       // HTTPS 환경이면 true로 변경 필요
                .sameSite("Lax")     // CSRF 공격 방어
                .path("/")           // 모든 경로에서 접근 가능
                .maxAge(maxMin * 60L) // 만료 시간: 분 단위 → 초 단위로 변환
                .build();

        // 응답 헤더에 Set-Cookie 추가
        res.addHeader("Set-Cookie", cookie.toString());
    }

    // 쿠키 삭제
    public static void deleteCookie(HttpServletResponse res, String name) {
        // 동일한 이름으로 maxAge=0 설정 → 브라우저가 쿠키 삭제
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        res.addHeader("Set-Cookie", cookie.toString());
    }

    // 요청에서 쿠키 읽기
    public static String readCookie(HttpServletRequest req, String name) {
        if (req.getCookies() == null) return null; // 쿠키가 없으면 null 반환

        // 이름이 일치하는 쿠키 탐색
        for (Cookie c : req.getCookies()) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null; // 해당 이름의 쿠키 없으면 null
    }
}
