<template>
  <div class="dashboard">
    <div class="dashboard-header">
      <h1>DB-Record</h1>
      <div class="header-right">
        <LanguageSwitcher />
        <div class="user-info">
          <span class="username">{{ username }}</span>
          <div class="dropdown" @click="toggleDropdown">
            ▼
            <div v-if="showDropdown" class="dropdown-menu">
              <a @click="logout">{{ $t('nav.logout') }}</a>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <div class="dashboard-body">
      <div class="sidebar">
        <nav class="nav-menu">
          <router-link to="/dashboard/project" class="nav-item" active-class="active">
            {{ $t('nav.project') }}
          </router-link>
          <router-link to="/dashboard/datasource" class="nav-item" active-class="active">
            {{ $t('nav.datasource') }}
          </router-link>
        </nav>
        </div>
      
      <div class="main-content">
          <router-view />
        </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import LanguageSwitcher from './LanguageSwitcher.vue';

const router = useRouter();
const showDropdown = ref(false);
const username = ref('');

onMounted(() => {
  username.value = localStorage.getItem('username') || 'User';
});

function toggleDropdown() {
  showDropdown.value = !showDropdown.value;
}

function logout() {
  localStorage.removeItem('token');
  localStorage.removeItem('username');
  router.push('/login');
}
</script>

<style scoped>
.dashboard {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.dashboard-header {
  background: #409eff;
  color: #fff;
  padding: 0 20px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.dashboard-header h1 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: bold;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  position: relative;
}

.username {
  font-size: 1rem;
}

.dropdown {
  cursor: pointer;
  position: relative;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
  min-width: 100px;
  z-index: 1000;
}

.dropdown-menu a {
  display: block;
  padding: 8px 12px;
  color: #333;
  text-decoration: none;
  cursor: pointer;
}

.dropdown-menu a:hover {
  background: #f5f5f5;
}

.dashboard-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.sidebar {
  width: 200px;
  background: #f5f7fa;
  border-right: 1px solid #e4e7ed;
  padding: 20px 0;
}

.nav-menu {
  display: flex;
  flex-direction: column;
}

.nav-item {
  padding: 12px 20px;
  color: #606266;
  text-decoration: none;
  border-left: 3px solid transparent;
  transition: all 0.3s;
}

.nav-item:hover {
  background: #ecf5ff;
  color: #409eff;
}

.nav-item.active {
  background: #ecf5ff;
  color: #409eff;
  border-left-color: #409eff;
}

.main-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background: #f8f9fa;
}
</style>