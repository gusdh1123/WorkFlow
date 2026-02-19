import { jwtDecode } from "jwt-decode";

// JWT 토큰을 User 객체로 변환
// - accessToken payload 구조를 기반으로 변환
// - 토큰 구조가 바뀌면 여기만 수정하면 됨
export function userFromToken(token) {

  try {

    // JWT 디코딩
    const decoded = jwtDecode(token);

    // 디코딩된 페이로드에서 User 정보 추출
    return {
      id: decoded.sub,     // 사용자 ID (sub claim)
      email: decoded.email, // 이메일
      role: decoded.role,   // 권한/역할
      name: decoded.name,   // 사용자 이름
    };

  } catch (e) {

    // 디코딩 실패 시 null 반환 + 콘솔 기록
    console.error("JWT 디코딩 실패", e);
    return null;

  }
}

// userFromToken 역할 요약
// - JWT accessToken을 디코딩해서 앱 내에서 사용할 User 객체 생성
// - 오류 발생 시 안전하게 null 반환
