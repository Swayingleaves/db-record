<template>
  <div class="dashboard-root">
    <aside class="sidebar">
      <div class="logo">DB-Record</div>
      <nav>
        <ul>
          <li :class="{active: activeMenu==='project'}" @click="activeMenu='project'">项目管理</li>
          <li :class="{active: activeMenu==='sql'}" @click="activeMenu='sql'">SQL控制台</li>
          <li :class="{active: activeMenu==='datasource'}" @click="activeMenu='datasource'">数据源管理</li>
        </ul>
      </nav>
    </aside>
    <div class="main">
      <header class="header">
        <div class="user-info" @click="toggleDropdown">
          <img class="avatar" src="https://unpkg.com/@tabler/icons@2.30.0/icons/user.svg" alt="avatar" />
          <span class="username">{{ username }}</span>
          <svg class="arrow" width="16" height="16" viewBox="0 0 24 24"><path d="M7 10l5 5 5-5" stroke="#888" stroke-width="2" fill="none"/></svg>
        </div>
        <div v-if="dropdown" class="dropdown">
          <div class="dropdown-item" @click="logout">退出登录</div>
        </div>
      </header>
      <section class="content">
        <div class="content-inner">
          <div v-if="activeMenu==='project'">项目管理内容区（待实现）</div>
          <div v-else-if="activeMenu==='sql'">SQL控制台内容区（待实现）</div>
          <div v-else>数据源管理内容区（待实现）</div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();
const activeMenu = ref('project');
const dropdown = ref(false);
const username = ref('');

onMounted(() => {
  const token = localStorage.getItem('token');
  if (!token) {
    router.push('/login');
    return;
  }
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    username.value = payload.sub;
  } catch {
    username.value = '';
  }
});

const toggleDropdown = () => {
  dropdown.value = !dropdown.value;
};

const logout = () => {
  localStorage.removeItem('token');
  router.push('/login');
};
</script>

<style scoped>
.dashboard-root {
  display: flex;
  height: 100vh;
  width: 100vw;
  background: #f5f7fa;
}
.sidebar {
  width: 200px;
  background: #222e3c;
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  padding-top: 18px;
}
.logo {
  font-size: 1.3rem;
  font-weight: bold;
  text-align: center;
  margin-bottom: 32px;
  letter-spacing: 2px;
}
nav ul {
  list-style: none;
  padding: 0;
  margin: 0;
}
nav li {
  padding: 14px 32px;
  cursor: pointer;
  color: #cfd8dc;
  font-size: 1.08rem;
  transition: background 0.2s, color 0.2s;
}
nav li.active, nav li:hover {
  background: #409eff;
  color: #fff;
}
.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.header {
  height: 56px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 32px;
  position: relative;
  box-shadow: 0 2px 8px 0 rgba(0,0,0,0.03);
}
.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  position: relative;
}
.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #eee;
  margin-right: 10px;
}
.username {
  font-weight: 500;
  color: #333;
  margin-right: 6px;
}
.arrow {
  transition: transform 0.2s;
}
.dropdown {
  position: absolute;
  right: 32px;
  top: 56px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 16px 0 rgba(0,0,0,0.10);
  min-width: 120px;
  z-index: 10;
}
.dropdown-item {
  padding: 12px 20px;
  cursor: pointer;
  color: #333;
  font-size: 1rem;
  transition: background 0.2s;
}
.dropdown-item:hover {
  background: #f5f7fa;
}
.content {
  flex: 1;
  min-width: 0;
  padding: 32px 24px 24px 24px;
  background: #f5f7fa;
  overflow: auto;
  text-align: left;
  display: flex;
  flex-direction: column;
}
.content-inner {
  max-width: 900px;
  width: 100%;
  margin-left: 0;
  margin-right: auto;
  flex: 1;
  display: flex;
  flex-direction: column;
}
@media (max-width: 700px) {
  .sidebar {
    width: 56px;
    padding-top: 8px;
  }
  .logo {
    font-size: 1rem;
    margin-bottom: 16px;
  }
  nav li {
    padding: 12px 8px;
    font-size: 0.98rem;
  }
  .main {
    padding-left: 0;
  }
  .header {
    padding: 0 8px;
  }
  .content {
    padding: 12px;
  }
}
</style> 