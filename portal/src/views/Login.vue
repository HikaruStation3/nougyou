<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-side">
        <span class="brand-logo large">N</span>
        <h2>欢迎回来</h2>
        <p>登录 Nougyou 用户门户，管理你的账号与农业数据。</p>
      </div>

      <div class="auth-form">
        <h3>账号登录</h3>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" size="large" placeholder="请输入用户名" clearable />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              size="large"
              type="password"
              placeholder="请输入密码"
              show-password
              @keyup.enter="handleSubmit"
            />
          </el-form-item>
          <el-form-item label="验证码" prop="captchaCode">
            <div class="captcha-row">
              <el-input
                v-model="form.captchaCode"
                size="large"
                placeholder="请输入验证码"
                maxlength="4"
                @keyup.enter="handleSubmit"
              />
              <img
                v-if="captcha.image"
                :src="captcha.image"
                class="captcha-image"
                alt="验证码"
                title="点击刷新"
                @click="fetchCaptcha"
              />
              <span v-else class="captcha-placeholder" @click="fetchCaptcha">点击获取</span>
            </div>
          </el-form-item>

          <el-button
            type="primary"
            size="large"
            class="submit-button"
            :loading="loading"
            @click="handleSubmit"
          >
            登录
          </el-button>
        </el-form>

        <p class="auth-switch">
          还没有账号？
          <router-link to="/register">立即注册</router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { getCaptcha, login } from "../api/auth";
import { useUserStore } from "../store/user";

const route = useRoute();
const router = useRouter();
const store = useUserStore();

const formRef = ref();
const loading = ref(false);
const captcha = reactive({ id: "", image: "" });

const form = reactive({
  username: route.query.username || "",
  password: "",
  captchaCode: "",
});

const rules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }],
  captchaCode: [{ required: true, message: "请输入验证码", trigger: "blur" }],
};

async function fetchCaptcha() {
  form.captchaCode = "";
  try {
    const data = await getCaptcha();
    captcha.id = data.captchaId;
    captcha.image = data.captchaBase64;
  } catch {
    captcha.id = "";
    captcha.image = "";
  }
}

async function handleSubmit() {
  await formRef.value.validate();
  loading.value = true;
  try {
    const result = await login({
      username: form.username,
      password: form.password,
      captchaId: captcha.id,
      captchaCode: form.captchaCode,
    });
    store.saveLogin(result);
    await store.loadUser();
    ElMessage.success("登录成功");
    router.replace(route.query.redirect || "/profile");
  } catch {
    fetchCaptcha();
  } finally {
    loading.value = false;
  }
}

onMounted(fetchCaptcha);
</script>
