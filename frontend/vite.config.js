import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  build: {
    // 构建产物输出到 nginx/html/dist 目录
    outDir: '../nginx/html/dist',
    // 每次构建前清空目标目录
    emptyOutDir: true,
  },
})
