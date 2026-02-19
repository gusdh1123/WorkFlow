import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [
    react(), // React 플러그인: Fast Refresh, JSX 지원 등
  ],
  server: {
    host: 'localhost', // 개발 서버를 로컬에서만 열기 (외부 접근 차단)
    proxy: {
      '/api': {
        target: 'http://localhost:8081', // API 요청을 로컬 백엔드로 프록시
        changeOrigin: true,               // Host 헤더를 타겟에 맞춰 변경
        secure: false,                    // HTTPS 인증서 문제 무시 (HTTP라 영향 없음)
      },
    },
  },
})
