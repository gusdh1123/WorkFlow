import { useEffect, useState } from "react";
import { AuthCtx } from "./AuthContext";
import { refreshOnce, setApiAccessToken } from "../api/api";
import { jwtDecode } from "jwt-decode";

export default function AuthProvider({ children }) {
  const [accessToken, setAccessToken] = useState(null);
  const [user, setUser] = useState(null);
  const [authReady, setAuthReady] = useState(false);

  // accessToken이 바뀔 때마다 api에 주입 + user 디코딩
  useEffect(() => {
    setApiAccessToken(accessToken);

    if (accessToken) {
      const decoded = jwtDecode(accessToken);
      setUser({
        id: decoded.sub,
        email: decoded.email,
        role: decoded.role,
        name: decoded.name,
      });
    } else {
      setUser(null);
    }
  }, [accessToken]);

  // 앱 시작 시 1회 refresh로 로그인 복구
  useEffect(() => {
    let mounted = true;

    const boot = async () => {
try {
  const token = await refreshOnce();
  if (!mounted) return;

  // token 있으면 로그인 복구
  if (token) setAccessToken(token);

  // token 없으면 비로그인 상태
} catch {
  if (mounted) {
    setAccessToken(null);
    setUser(null);
  }
} finally {
  if (mounted) setAuthReady(true);
}
    };

    boot();

    return () => {
      mounted = false;
    };
  }, []);

  if (!authReady) return null;

  return (
    <AuthCtx.Provider value={{ accessToken, setAccessToken, user }}>
      {children}
    </AuthCtx.Provider>
  );
}


// AuthProvider 작성된 코드 역할
// 앱 시작 시 /api/refresh로 로그인 상태를 복구하고 accessToken을 전역 상태로 관리하며,
// accessToken이 바뀔 때마다 API 클라이언트에 자동 반영하고 인증 준비가 끝나기 전에는 화면을 렌더링하지 않는다.
