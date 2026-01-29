import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    // 호스트 노출 방지
    host: 'localhost',
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        // 백엔드가 Host 체크하는 경우 대비
        changeOrigin: true,
        // HTTPS 타겟일 때 인증서 문제 회피용(지금은 http라 영향 거의 없음)
        secure: false,
      },
    },
  },
})
