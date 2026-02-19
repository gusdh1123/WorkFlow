import { useNavigate } from "react-router-dom";
import { useAuth } from "./useAuth";
import { api, setApiAccessToken } from "../../api/api";

// 로그아웃 처리용 커스텀 훅
// - 서버 로그아웃 + 클라이언트 인증 상태 초기화
export function useLogout() {

  const navigate = useNavigate();          // 페이지 이동용
  const { setAccessToken } = useAuth();    // 전역 accessToken 상태 수정

  // 실제 로그아웃 함수 반환
  return async function logout() {

    try {

      // 서버 로그아웃 API 호출
      await api.post("/api/logout");

    } catch (e) {

      // 서버 로그아웃 실패 시 콘솔만 기록
      console.error("로그아웃 실패", e);

    } finally {

      // 클라이언트 인증 상태 초기화
      setApiAccessToken(null);  // axios 인스턴스에서 Authorization 헤더 제거
      setAccessToken(null);     // React 전역 상태 초기화

      // 로그인 페이지로 강제 이동
      navigate("/login", { replace: true });

    }
  };
}

// useLogout 역할 요약
// - 서버 로그아웃 호출
// - accessToken 메모리 + 상태 초기화
// - 로그인 페이지로 리디렉션
