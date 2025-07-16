<template>
  <div class="register-bg">
    <div class="register-card">
      <h2>注册账号</h2>
      <form @submit.prevent="handleRegister">
        <div class="form-item">
          <input v-model="username" required placeholder="用户名" autocomplete="username" />
        </div>
        <div class="form-item">
          <input type="password" v-model="password" required placeholder="密码" autocomplete="new-password" />
        </div>
        <button class="register-btn" type="submit" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>
        <p v-if="error" class="error">{{ error }}</p>
        <p v-if="success" class="success">注册成功，正在跳转...</p>
      </form>
      <div class="login-tip">
        已有账号？<router-link to="/login">登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import request from '../utils/request';

const username = ref('');
const password = ref('');
const error = ref('');
const success = ref(false);
const loading = ref(false);
const router = useRouter();

const handleRegister = async () => {
  if (loading.value) return;
  
  error.value = '';
  success.value = false;
  loading.value = true;
  
  try {
    await request.post('/api/register', { 
      username: username.value, 
      password: password.value 
    });
    
    // 请求成功，response.data 是 ApiResult 格式
      success.value = true;
    setTimeout(() => router.push('/login'), 1500);
  } catch (err: any) {
    error.value = err.message || '注册失败';
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.register-bg {
  min-height: 100vh;
  width: 100vw;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}
.register-card {
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
.register-card h2 {
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
.register-btn {
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
.register-btn:hover:not(:disabled) {
  background: #337ecc;
}
.register-btn:disabled {
  background: #a0cfff;
  cursor: not-allowed;
}
.error {
  color: #e74c3c;
  margin: 10px 0 0 0;
  font-size: 0.98rem;
  text-align: center;
}
.success {
  color: #2ecc71;
  margin: 10px 0 0 0;
  font-size: 0.98rem;
  text-align: center;
}
.login-tip {
  margin-top: 18px;
  font-size: 0.98rem;
  color: #888;
}
.login-tip a {
  color: #409eff;
  text-decoration: none;
  margin-left: 4px;
}
@media (max-width: 480px) {
  .register-card {
    padding: 28px 8px 24px 8px;
    max-width: 96vw;
  }
}
</style>