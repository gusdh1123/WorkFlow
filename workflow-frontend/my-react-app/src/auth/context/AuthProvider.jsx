import { useEffect, useState } from "react";
import { AuthCtx } from "./AuthContext";
import { refreshOnce, setApiAccessToken } from "../../api/api";
import { userFromToken } from "../utils/userFromToken";

// 인증 상태 전역 제공자
// - accessToken / user 상태를 Context로 하위 컴포넌트에 전달
// - 앱 시작 시 refreshToken으로 로그인 복구
export default function AuthProvider({ children }) {

  // accessToken 전역 상태
  const [accessToken, setAccessToken] = useState(null);

  // JWT 디코딩된 사용자 정보
  const [user, setUser] = useState(null);

  // 인증 준비 완료 여부 (refresh 복구 완료)
  const [authReady, setAuthReady] = useState(false);


  // accessToken 변경 시 처리
  // - axios 인스턴스에 토큰 반영
  // - JWT 디코딩 → user 상태 생성
  useEffect(() => {

    // API 클라이언트에 accessToken 주입
    setApiAccessToken(accessToken);

    if (accessToken) {
      // 토큰 존재 → JWT 디코딩 후 user 상태 생성
      const parsedUser = userFromToken(accessToken);
      setUser(parsedUser);

    } else {
      // 토큰 없음 → user 초기화
      setUser(null);
    }

  }, [accessToken]);


  // 앱 시작 시 로그인 복구
  // - refreshToken 쿠키 기반 accessToken 재발급
  useEffect(() => {

    let mounted = true;

    const boot = async () => {
      try {
        // 로그인/회원가입 페이지에서는 refresh 시도 금지
        if (window.location.pathname === "/login" || window.location.pathname === "/signup") {
        if (mounted) setAuthReady(true); // 인증 준비 완료 표시
        return;
      }
      
        // refresh 요청 (쿠키 기반)
        const token = await refreshOnce();

        if (!mounted) return;

        if (token) {
          // refresh 성공 → 로그인 상태 복구
          setAccessToken(token);
        }

        // refresh 실패 / 비로그인 → 그냥 통과

      } catch (e) {
        if (mounted) {
          // refresh 중 예외 발생 시 로그 및 상태 초기화
          console.error("인증 복구 실패", e);
          setAccessToken(null);
          setUser(null);
        }

      } finally {
        if (mounted) {
          // 인증 준비 완료 표시 → 렌더 허용
          setAuthReady(true);
        }
      }
    };

    boot();

    // 컴포넌트 언마운트 시 플래그
    return () => {
      mounted = false;
    };

  }, []);


  // 인증 준비 전 렌더 차단
  // - refresh 시 화면 깜빡임 방지
  if (!authReady) return null;


  // Context Provider로 accessToken, setAccessToken, user 전달
  return (
    <AuthCtx.Provider value={{ accessToken, setAccessToken, user }}>
      {children}
    </AuthCtx.Provider>
  );
}

// AuthProvider 요약
// - 앱 시작 시 refreshToken으로 로그인 복구
// - accessToken 전역 상태 관리
// - 토큰 변경 시 API 자동 반영
// - JWT 디코딩 → user 상태 생성
// - 인증 준비 완료 전 화면 렌더 차단
