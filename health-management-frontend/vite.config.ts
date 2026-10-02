// vite.config.ts
import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';
import { resolve } from 'path';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const proxy = {
    '/api': {
      target: env.API_PROXY_TARGET || 'http://localhost:8081',
      // Keep the browser-facing Host so same-origin POSTs also work on LAN URLs.
      changeOrigin: false,
    },
  };
  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src'),
      },
    },
    server: { proxy },
    preview: { proxy },
  };
});
