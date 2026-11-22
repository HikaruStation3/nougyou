import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  // 避免预构建 hash 变化时出现双 Vue 实例，导致 Element Plus 组件解析失败
  resolve: {
    dedupe: ["vue"],
  },
  server: {
    port: 3100,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
