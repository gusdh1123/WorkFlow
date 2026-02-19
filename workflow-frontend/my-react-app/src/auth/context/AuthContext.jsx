import { createContext } from "react";

// 인증 전용 Context 생성
// - accessToken, 사용자 정보 등 인증 관련 상태를 전역에서 공유
// - Provider로 감싸야 하위 컴포넌트에서 사용 가능
// - 초기값 null: Provider 없으면 기본적으로 값 없음
export const AuthCtx = createContext(null);

// 사용 예시:
// <AuthCtx.Provider value={{ accessToken, setAccessToken }}>
//   <App />
// </AuthCtx.Provider>
