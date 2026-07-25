/// <reference types="vitest" />

import legacy from '@vitejs/plugin-legacy'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import { defineConfig, loadEnv } from 'vite'

const LOCAL_PMS_ORIGIN = 'http://localhost:8092'
const CLOUD_PMS_ORIGIN = 'https://pms.the-host.jp'

function normalizeEnvValue(value: unknown) {
  if (typeof value !== 'string') {
    return ''
  }

  return value.trim()
}

function resolveAbsoluteOrigin(value: string) {
  if (!value) {
    return ''
  }

  try {
    return new URL(value).origin
  } catch {
    return ''
  }
}

function resolveApiProxyTarget(env: Record<string, string | undefined>) {
  const useCloudApi = normalizeEnvValue(env.VITE_USE_CLOUD_API) === 'true'
  if (useCloudApi) {
    const cloudBaseUrl = normalizeEnvValue(env.VITE_CLOUD_API_BASE_URL)
    const legacyBaseUrl = normalizeEnvValue(env.VITE_API_BASE_URL)
    return resolveAbsoluteOrigin(cloudBaseUrl) || resolveAbsoluteOrigin(legacyBaseUrl) || CLOUD_PMS_ORIGIN
  }

  const localBaseUrl = normalizeEnvValue(env.VITE_LOCAL_API_BASE_URL)
  const legacyBaseUrl = normalizeEnvValue(env.VITE_API_BASE_URL)
  return resolveAbsoluteOrigin(localBaseUrl) || resolveAbsoluteOrigin(legacyBaseUrl) || LOCAL_PMS_ORIGIN
}

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const loadedEnv = loadEnv(mode, process.cwd(), '')
  const env = {
    ...loadedEnv,
    ...process.env,
  }
  const apiProxyTarget = resolveApiProxyTarget(env)

  return {
    plugins: [
      vue(),
      legacy()
    ],
    server: {
      proxy: {
        '/api': {
          target: apiProxyTarget,
          changeOrigin: true,
        },
      },
    },
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
      },
    },
    test: {
      include: ['tests/unit/**/*.{spec,test}.{ts,tsx}'],
      globals: true,
      environment: 'jsdom',
      setupFiles: ['tests/unit/setup.ts'],
    }
  }
})
