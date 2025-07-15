import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/login': 'http://localhost:8080',
      '/register': 'http://localhost:8080',
      '/api': 'http://localhost:8080',
    }
  }
})
