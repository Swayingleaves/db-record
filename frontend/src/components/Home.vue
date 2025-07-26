<template>
  <div class="home">
    <!-- 语言切换器 -->
    <div class="language-switcher-container">
      <LanguageSwitcher />
    </div>
    
    <h1>{{ $t('auth.welcome') }}</h1>
    <p>{{ $t('auth.description') }}</p>
    <div class="actions">
      <router-link to="/login" class="btn btn-primary">{{ $t('auth.login') }}</router-link>
      <router-link to="/register" class="btn btn-secondary">{{ $t('auth.register') }}</router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import LanguageSwitcher from './LanguageSwitcher.vue';
</script>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  text-align: center;
  position: relative;
}

.language-switcher-container {
  position: absolute;
  top: 20px;
  right: 20px;
}

.actions {
  margin-top: 2rem;
  display: flex;
  gap: 1rem;
}

.btn {
  padding: 0.5rem 1rem;
  text-decoration: none;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.btn-primary {
  background-color: #007bff;
  color: white;
}

.btn-secondary {
  background-color: #6c757d;
  color: white;
}

.btn:hover {
  opacity: 0.8;
}
</style>