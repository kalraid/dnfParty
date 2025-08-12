import { fileURLToPath, URL } from 'node:url'
import { defineConfig, normalizePath, searchForWorkspaceRoot } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'node:path'

export default defineConfig({
  plugins: [vue()],
  root: normalizePath(path.resolve('C:/Users/INSoft/.git/game/df-party-frontend')),
  server: {
    fs: {
      allow: [
        normalizePath(searchForWorkspaceRoot(process.cwd())),
        normalizePath(path.resolve('C:/Users/INSoft/.git/game/df-party-frontend'))
      ],
      strict: false
    }
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  }
});