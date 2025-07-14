import { createRouter, createWebHistory } from 'vue-router';
import Login from '../components/Login.vue';
import Register from '../components/Register.vue';
import Home from '../components/Home.vue';
import Dashboard from '../components/Dashboard.vue';
import DataSourcePage from '../views/DataSourcePage.vue';
import ProjectPage from '../views/ProjectPage.vue';

// 占位页面
const SqlPage = { template: '<div>SQL控制台内容区（待实现）</div>' };

const routes = [
  { path: '/', name: 'Home', component: Home },
  { path: '/login', name: 'Login', component: Login },
  { path: '/register', name: 'Register', component: Register },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: Dashboard,
    children: [
      { path: '', redirect: '/dashboard/project' },
      { path: 'project', name: 'Project', component: ProjectPage },
      { path: 'sql', name: 'Sql', component: SqlPage },
      { path: 'datasource', name: 'DataSource', component: DataSourcePage },
    ]
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router; 