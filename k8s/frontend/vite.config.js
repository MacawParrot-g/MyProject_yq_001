import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
    plugins: [vue()],
    server: {
        port: 5173,
        proxy: {
            '/api': { target: 'http://localhost:8080', changeOrigin: true },
            '/check': { target: 'http://localhost:8080', changeOrigin: true },
            '/execute': { target: 'http://localhost:8080', changeOrigin: true }
        }
    }
})
