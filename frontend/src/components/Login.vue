<template>
  <div class="login-bg">
    <div class="login-card">
      <h2>欢迎登录</h2>
      <form @submit.prevent="handleLogin">
        <div class="form-item">
          <input v-model="username" required placeholder="用户名" autocomplete="username" />
        </div>
        <div class="form-item">
          <input type="password" v-model="password" required placeholder="密码" autocomplete="current-password" />
        </div>
        <button class="login-btn" type="submit">登录</button>
        <p v-if="error" class="error">{{ error }}</p>
      </form>
      <div class="register-tip">
        没有账号？<router-link to="/register">注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';

const username = ref('');
const password = ref('');
const error = ref('');
const router = useRouter();

const handleLogin = async () => {
  error.value = '';
  try {
    const res = await axios.post('/login', { username: username.value, password: password.value });
    if (res.data.success) {
      localStorage.setItem('token', res.data.token);
      router.push('/dashboard');
    } else {
      error.value = res.data.message || '登录失败';
    }
  } catch (e) {
    error.value = '请求失败';
  }
};
</script>

<style scoped>
.login-bg {
  min-height: 100vh;
  width: 100vw;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}
.login-card {
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 4px 24px 0 rgba(0,0,0,0.08);
  padding: 40px 32px 32px 32px;
  width: 100%;
  max-width: 340px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.login-card h2 {
  margin-bottom: 28px;
  font-weight: 600;
  color: #222;
  font-size: 1.6rem;
}
.form-item {
  width: 100%;
  margin-bottom: 18px;
}
.form-item input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #e0e3e8;
  border-radius: 6px;
  font-size: 1rem;
  outline: none;
  transition: border 0.2s;
  background: #f8fafc;
}
.form-item input:focus {
  border: 1.5px solid #409eff;
  background: #fff;
}
.login-btn {
  width: 100%;
  padding: 10px 0;
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 1.1rem;
  font-weight: 500;
  cursor: pointer;
  margin-top: 6px;
  transition: background 0.2s;
}
.login-btn:hover {
  background: #337ecc;
}
.error {
  color: #e74c3c;
  margin: 10px 0 0 0;
  font-size: 0.98rem;
  text-align: center;
}
.register-tip {
  margin-top: 18px;
  font-size: 0.98rem;
  color: #888;
}
.register-tip a {
  color: #409eff;
  text-decoration: none;
  margin-left: 4px;
}
@media (max-width: 480px) {
  .login-card {
    padding: 28px 8px 24px 8px;
    max-width: 96vw;
  }
}
</style> 