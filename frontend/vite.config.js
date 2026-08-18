import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path' // 引入 path 模块处理路径

export default defineConfig({
  plugins: [vue()],
  build: {
    // 核心修改：将输出目录指向 nginx/html/dist
    // '../nginx/html/dist' 表示向上一级目录走，进入 nginx/html/dist
    outDir: path.resolve(__dirname, '../nginx/html/dist'),
    emptyOutDir: true, // 每次构建前清空该目录，防止旧文件残留
  },
})
