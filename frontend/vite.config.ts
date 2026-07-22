import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { configDefaults } from 'vitest/config'
export default defineConfig({
  plugins: [vue()],
  server: { port: 5173, proxy: { '/api': 'http://localhost:8080' } },
  test: { environment: 'jsdom', exclude: [...configDefaults.exclude, 'e2e/**'] },
})
