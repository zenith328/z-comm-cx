import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 15173,
    // 같은 와이파이의 다른 기기(핸드폰 등)에서 PC의 LAN IP로 접속해 테스트할 수 있도록 개방.
    host: true,
    // Google 로그인(Authorized JavaScript origins)이 순수 IP를 허용하지 않아, ngrok의 고정
    // 무료 dev domain으로 임시 HTTPS 접속할 때 Vite의 Host 헤더 검증(DNS 리바인딩 방지)을 통과시켜야 한다.
    allowedHosts: ['quiet-yard-mushroom.ngrok-free.dev'],
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
