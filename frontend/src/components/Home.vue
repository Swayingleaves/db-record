<template>
  <div class="home-container">
    <h2>欢迎，{{ username || '用户' }}</h2>
    <p>请通过左侧菜单进入Dashboard页面。</p>
    <button @click="logout">退出登录</button>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';

const username = ref('');
const router = useRouter();

onMounted(async () => {
  // 可选：从token解析用户名，或请求后端获取
  const token = localStorage.getItem('token');
  if (!token) {
    router.push('/login');
    return;
  }
  // 简单解析token（不安全，仅演示）
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    username.value = payload.sub;
  } catch {
    username.value = '';
  }
});

const logout = () => {
  localStorage.removeItem('token');
  router.push('/login');
};
</script>

<style scoped>
.home-container {
  max-width: 400px;
  margin: 60px auto;
  padding: 32px;
  border: 1px solid #eee;
  border-radius: 8px;
  background: #fff;
  text-align: center;
}
.home-container button {
  margin-top: 24px;
  padding: 8px 24px;
}
</style> 