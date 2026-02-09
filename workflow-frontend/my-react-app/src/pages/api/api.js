import axios from "axios";

// refresh는 refreshOnce로만 호출할 것

/**
 * 1) axios 기본 인스턴스
 * - 모든 일반 API 요청에 사용
 * - accessToken은 Authorization 헤더로 전달
 * - refreshToken은 HttpOnly 쿠키로 자동 전송
 */
export const api = axios.create({
  baseURL: "http://localhost:8081",
  withCredentials: true, // refreshToken 쿠키 주고받기
});

/**
 * refresh 전용 axios 인스턴스
 * - refresh 요청에는 interceptor를 타지 않게 분리
 * - 무한 루프(refresh → 401 → refresh)를 방지
 */
export const refreshClient = axios.create({
  baseURL: "http://localhost:8081",
  withCredentials: true,
});

/**
 * 2) accessToken 메모리 보관
 * - localStorage/sessionStorage 미사용 (XSS 대응)
 * - 페이지 새로고침 시 사라지는 것이 정상
 * - refresh 성공 시 다시 메모리에 주입
 */
let accessToken = null;

export const setApiAccessToken = (token) => {
  accessToken = token;
};

/**
 * 요청 인터셉터
 * - accessToken이 있으면 Authorization 헤더 자동 추가
 * - 토큰이 없으면 헤더를 붙이지 않음
 */
api.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers = config.headers || {};
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

/**
 * 3) refresh 단일화 (핵심 로직)
 * - 동시에 여러 요청에서 refresh가 필요해도
 *   실제 refresh 요청은 1번만 발생
 * - 나머지는 같은 Promise를 공유
 */
let refreshPromise = null;

export function refreshOnce() {
  if (!refreshPromise) {
    refreshPromise = refreshClient
      .post("/api/refresh")
      .then((res) => {
        // 204: 비로그인 상태 (정상)
        if (res.status === 204) return null;

        // 200: accessToken 재발급 성공
        return res.data?.accessToken ?? null;
      })
      .catch((e) => {
        // 401: refreshToken 만료/폐기 → 조용히 로그아웃 처리
        if (e.response?.status === 401) return null;
        throw e; // 그 외 에러는 상위로 전달
      })
      .finally(() => {
        // 다음 refresh를 위해 Promise 초기화
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

/**
 * 4) 401 처리 로직 (queue + 재시도)
 * - accessToken 만료로 401 발생 시 refresh 시도
 * - refresh 중이면 요청을 queue에 대기
 * - refresh 성공 시 대기 중 요청 재실행
 */
let isRefreshing = false;
let queue = [];

// refresh 완료 후 대기 중이던 요청들 처리
const runQueue = (error, token = null) => {
  queue.forEach((p) => (error ? p.reject(error) : p.resolve(token)));
  queue = [];
};

api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config;

    // 401 아니면 그대로 에러 반환
    if (error.response?.status !== 401) {
      return Promise.reject(error);
    }

    // refresh 요청 자체에서 401 → 재시도 금지
    if (original?.url?.includes("/api/refresh")) {
      return Promise.reject(error);
    }

    // 이미 재시도한 요청이면 중단
    if (original._retry) {
      return Promise.reject(error);
    }
    original._retry = true;

    // refresh 진행 중이면 queue에 대기
    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        queue.push({
          resolve: (token) => {
            if (!token) return reject(error);
            original.headers = original.headers || {};
            original.headers.Authorization = `Bearer ${token}`;
            resolve(api(original));
          },
          reject,
        });
      });
    }

    isRefreshing = true;

    try {
      const newToken = await refreshOnce();

      // refresh 실패 → 인증 종료
      if (!newToken) throw error;

      // 새 토큰 메모리에 반영
      setApiAccessToken(newToken);

      // 대기 중 요청들 재개
      runQueue(null, newToken);

      // 원래 요청 재시도
      original.headers = original.headers || {};
      original.headers.Authorization = `Bearer ${newToken}`;
      return api(original);

    } catch (e) {
      // refresh 실패 시 모든 대기 요청 실패 처리
      runQueue(e, null);
      setApiAccessToken(null);
      return Promise.reject(e);
    } finally {
      isRefreshing = false;
    }
  }
);