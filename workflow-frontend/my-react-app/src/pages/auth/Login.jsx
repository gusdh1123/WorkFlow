import './Login.css'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from './useAuth';

function Login()  {

    const navigate = useNavigate();

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const {setAccessToken} = useAuth();

    const loginHandle = async () => {
        try {
            const response = await fetch("http://localhost:8081/api/login", {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                credentials: "include",
                body: JSON.stringify({
                    email,
                    password
                })
            });

            // 로그 출력
            // const log = await response.text();
            // console.log(response.status);
            // console.log(log);

            if (!response.ok){
                throw new Error("로그인 실패");
            }

            // 엑세스 토큰 받기
            const data = await response.json();
            setAccessToken(data.accessToken);

            // 로그인 후 이동
            navigate("/")

        } catch (error){
            console.error(error);
            alert("아이디 또는 비밀번호가 틀렸습니다.");
        }
    };

    return (
        <div>
            <h2>로그인</h2>

            <input
            type='email'
            placeholder='이메일'
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            />

            <input
            type='password'
            placeholder='비밀번호'
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            />

            <button onClick={loginHandle}>로그인</button>
        </div>
    ); 
}

export default Login;