<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-side">
        <span class="brand-logo large">N</span>
        <h2>加入 Nougyou</h2>
        <p>注册后即可使用 AI 助手、文件云存储等平台能力。</p>
      </div>

      <div class="auth-form">
        <h3>创建账号</h3>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" size="large" placeholder="4-20 位，登录使用" clearable />
          </el-form-item>
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="form.nickname" size="large" placeholder="选填，默认与用户名一致" clearable />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              size="large"
              type="password"
              placeholder="6-32 位"
              show-password
            />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              size="large"
              type="password"
              placeholder="请再次输入密码"
              show-password
              @keyup.enter="handleSubmit"
            />
          </el-form-item>

          <el-button
            type="primary"
            size="large"
            class="submit-button"
            :loading="loading"
            @click="handleSubmit"
          >
            注册
          </el-button>
        </el-form>

        <p class="auth-switch">
          已有账号？
          <router-link to="/login">去登录</router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { register } from "../api/auth";

const router = useRouter();
const formRef = ref();
const loading = ref(false);

const form = reactive({
  username: "",
  nickname: "",
  password: "",
  confirmPassword: "",
});

const validateConfirm = (rule, value, callback) => {
  if (!value) {
    callback(new Error("请再次输入密码"));
  } else if (value !== form.password) {
    callback(new Error("两次输入的密码不一致"));
  } else {
    callback();
  }
};

const rules = {
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    { min: 4, max: 20, message: "用户名长度需为 4-20 位", trigger: "blur" },
  ],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, max: 32, message: "密码长度需为 6-32 位", trigger: "blur" },
  ],
  confirmPassword: [{ validator: validateConfirm, trigger: "blur" }],
};

async function handleSubmit() {
  await formRef.value.validate();
  loading.value = true;
  try {
    await register({
      username: form.username.trim(),
      nickname: form.nickname.trim(),
      password: form.password,
    });
    ElMessage.success("注册成功，请登录");
    router.push({ name: "login", query: { username: form.username.trim() } });
  } finally {
    loading.value = false;
  }
}
</script>
