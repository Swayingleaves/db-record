<template>
  <div class="login-bg">
    <!-- 语言切换器 -->
    <div class="language-switcher-container">
      <LanguageSwitcher />
    </div>
    
    <div class="login-card">
      <h1 class="app-title">DB-RECORD</h1>
      <h2>{{ $t('auth.welcomeLogin') }}</h2>
      <form @submit.prevent="handleLogin">
        <div class="form-item">
          <input v-model="username" required :placeholder="$t('auth.username')" autocomplete="username" />
        </div>
        <div class="form-item">
          <input type="password" v-model="password" required :placeholder="$t('auth.password')" autocomplete="current-password" />
        </div>
        <button class="login-btn" type="submit" :disabled="loading">
          {{ loading ? $t('auth.loggingIn') : $t('auth.login') }}
        </button>
        <p v-if="error" class="error">{{ error }}</p>
      </form>
      <div class="register-tip">
        {{ $t('auth.noAccount') }}<router-link to="/register">{{ $t('auth.register') }}</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import request from '../utils/request';
import LanguageSwitcher from './LanguageSwitcher.vue';

const username = ref('');
const password = ref('');
const error = ref('');
const loading = ref(false);
const router = useRouter();

const handleLogin = async () => {
  if (loading.value) return;
  
  error.value = '';
  loading.value = true;
  
  try {
    const response = await request.post('/login', { 
      username: username.value, 
      password: password.value 
    });
    
    // 请求成功，response.data 是 ApiResult 格式
    const { data } = response.data;
    
    // 保存token和用户信息
    if (data.token) {
      localStorage.setItem('token', data.token);
      localStorage.setItem('username', data.username);
      localStorage.setItem('role', data.role);
      
      // 跳转到仪表板
      router.push('/dashboard');
    } else {
      error.value = '登录失败：未获取到token';
    }
  } catch (err: any) {
    error.value = err.message || '登录失败';
  } finally {
    loading.value = false;
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
  position: relative;
}

.language-switcher-container {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 10;
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
.app-title {
  font-size: 2.2rem;
  font-weight: bold;
  font-style: italic;
  color: #409eff;
  margin-bottom: 20px;
  text-align: center;
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
.login-btn:hover:not(:disabled) {
  background: #337ecc;
}
.login-btn:disabled {
  background: #a0cfff;
  cursor: not-allowed;
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