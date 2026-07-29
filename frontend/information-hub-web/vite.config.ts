import { fileURLToPath, URL } from 'node:url'

import vue from '@vitejs/plugin-vue'
import { defineConfig, loadEnv, type UserConfig } from 'vite'

const DEFAULT_PROXY_TARGET = 'http://127.0.0.1:8080'

export function createViteConfig(proxyTarget?: string): UserConfig {
  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      proxy: {
        '/api': {
          target: proxyTarget?.trim() || DEFAULT_PROXY_TARGET,
          changeOrigin: true,
        },
      },
    },
  }
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), 'INFORMATION_HUB_')
  return createViteConfig(env.INFORMATION_HUB_PROXY_TARGET)
})
