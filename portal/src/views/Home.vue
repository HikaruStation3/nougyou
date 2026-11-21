<template>
  <div class="home">
    <section class="hero">
      <div class="hero-copy">
        <el-tag type="success" effect="plain" round>Nougyou 用户门户</el-tag>
        <h1>让农业生产<br />更简单、更智能</h1>
        <p>
          一个基于 Vue3 与 Spring Boot 的用户门户示例，内置注册登录、JWT 鉴权、
          AI 助手与文件云存储能力。
        </p>
        <div class="hero-actions">
          <router-link :to="store.state.user ? '/profile' : '/register'">
            <el-button type="primary" size="large">
              {{ store.state.user ? "进入个人中心" : "免费注册" }}
            </el-button>
          </router-link>
          <router-link v-if="!store.state.user" to="/login">
            <el-button size="large">已有账号，去登录</el-button>
          </router-link>
        </div>
      </div>
      <div class="hero-visual">
        <div class="field-card">
          <div class="field-row">
            <span>今日土壤湿度</span>
            <strong>62%</strong>
          </div>
          <el-progress :percentage="62" :stroke-width="10" color="#2e9e5b" />
          <div class="field-row">
            <span>AI 农事建议</span>
            <strong>2 条</strong>
          </div>
          <div class="field-tip">「未来三天有降雨，建议推迟灌溉。」</div>
        </div>
      </div>
    </section>

    <section class="features">
      <h2>平台能力</h2>
      <div class="feature-grid">
        <el-card v-for="item in features" :key="item.title" shadow="hover" class="feature-card">
          <el-icon :size="28" color="#2e9e5b"><component :is="item.icon" /></el-icon>
          <h3>{{ item.title }}</h3>
          <p>{{ item.desc }}</p>
        </el-card>
      </div>
    </section>
  </div>
</template>

<script setup>
import { MagicStick, FolderOpened, UserFilled } from "@element-plus/icons-vue";
import { useUserStore } from "../store/user";

const store = useUserStore();

const features = [
  {
    title: "账号体系",
    desc: "注册、登录、JWT 鉴权与个人中心，开箱即用。",
    icon: UserFilled,
  },
  {
    title: "AI 助手",
    desc: "基于 Spring AI 的智能问答，可接入 OpenAI 兼容模型。",
    icon: MagicStick,
  },
  {
    title: "文件云存储",
    desc: "MinIO 对象存储，支持上传、下载与访问链接生成。",
    icon: FolderOpened,
  },
];
</script>
