const API_BASE = "http://localhost:8081";

export async function apiFetch(path, accessToken, setAccessToken, options = {}) {
  // 1) 최초 요청
  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      ...(options.headers || {}),
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
    },
    credentials: "include", // ✅ refresh 쿠키 포함/수신
  });

  // 성공이면 그대로 반환
  if (res.status !== 401) return res;

  // 2) 401 -> refresh 시도
  const refreshRes = await fetch(`${API_BASE}/api/refresh`, {
    method: "POST",
    credentials: "include",
  });

  if (!refreshRes.ok) {
    // refresh 실패 -> 로그아웃 처리
    setAccessToken(null);
    throw new Error("Refresh failed (need login)");
  }

  // 3) 새 accessToken 받기 (서버가 body로 준다는 가정)
  const data = await refreshRes.json();
  const newToken = data.accessToken;

  if (!newToken) {
    setAccessToken(null);
    throw new Error("No accessToken in refresh response");
  }

  // 4) Context 업데이트
  setAccessToken(newToken);

  // 5) 원래 요청 재시도
  const retry = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      ...(options.headers || {}),
      Authorization: `Bearer ${newToken}`,
    },
    credentials: "include",
  });

  return retry;
}
