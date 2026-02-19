import '../../css/login/Login.css'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/hooks/useAuth';
import { api } from "../../api/api";
import LoginLogo from "../../assets/images/Logo.png";

function Login() {
  const navigate = useNavigate(); // 페이지 이동 훅
  const { setAccessToken } = useAuth(); // 로그인 후 토큰 저장

  // 입력 상태 관리
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  // 로그인 처리 함수
  const loginHandle = async () => {
    try {
      // API 호출: 이메일/비밀번호 전송
      const res = await api.post("/api/login", { email, password });

      // accessToken 저장
      setAccessToken(res.data.accessToken);

      // 로그인 성공 시 대시보드로 이동
      navigate("/");
    } catch (err) {
      console.error(err);
      alert("아이디 또는 비밀번호가 틀렸습니다."); // 로그인 실패 안내
    }
  };

  // 폼 제출 처리 (엔터 또는 버튼 클릭)
  const submitHandle = (e) => {
    e.preventDefault(); // 기본 새로고침 방지
    loginHandle();       // 로그인 실행
  };

  return (
    <div className='login__page'>
      <div className='login__box'>
        {/* 로고 */}
        <img className="login__logo" src={LoginLogo} alt="LoginLogo"/>

        {/* 로그인 폼 */}
        <form onSubmit={submitHandle} className='login__form'>
          {/* 이메일 입력 */}
          <input
            className='email'
            type='email'
            placeholder='이메일'
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />

          {/* 비밀번호 입력 */}
          <input
            className='password'
            type='password'
            placeholder='비밀번호'
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />

          {/* 로그인 버튼 */}
          <button type="submit" className='login__btn'>
            로그인
          </button>
        </form>
      </div>
    </div>
  );
}

export default Login;
