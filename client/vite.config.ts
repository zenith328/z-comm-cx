import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 15173,
    // 같은 와이파이의 다른 기기(핸드폰 등)에서 PC의 LAN IP로 접속해 테스트할 수 있도록 개방.
    host: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
