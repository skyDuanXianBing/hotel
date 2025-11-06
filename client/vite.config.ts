import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  server: {
    port: 8091,
    host: true, // 允许外部访问
    allowedHosts: [
      '.cpolar.cn', // 允许所有 cpolar 子域名
      '.cpolar.top', // 允许所有 cpolar.top 子域名
      'localhost',
      '127.0.0.1'
    ]
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
})
