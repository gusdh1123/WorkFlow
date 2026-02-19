import { useContext } from "react";
import { AuthCtx } from "../context/AuthContext";

// AuthContext 접근용 커스텀 훅
// - AuthProvider 내부에서 전역 인증 상태(accessToken, user) 접근
export const useAuth = () => {

  // Context 가져오기
  const ctx = useContext(AuthCtx);

  // Provider 없이 사용 방지
  // - AuthProvider 밖에서 호출 시 즉시 에러 발생
  if (!ctx) {
    throw new Error("AuthProvider 밖에서 useAuth 사용 불가");
  }

  // Context 반환 (accessToken, setAccessToken, user 포함)
  return ctx;
};

// useAuth 역할 요약
// - AuthContext 안전 접근
// - Provider 누락 시 즉시 오류로 감지
// - 전역 인증 상태 표준 인터페이스 제공
