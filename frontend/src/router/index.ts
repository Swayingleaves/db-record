import { createRouter, createWebHistory } from 'vue-router';
import Login from '../components/Login.vue';
import Register from '../components/Register.vue';
import Home from '../components/Home.vue';
import Dashboard from '../components/Dashboard.vue';
import DataSourcePage from '../views/DataSourcePage.vue';
import ProjectPage from '../views/ProjectPage.vue';
import ProjectDetailPage from '../views/ProjectDetailPage.vue';
import SqlConsolePage from '../views/SqlConsolePage.vue';

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
      { path: 'project/:id', name: 'ProjectDetail', component: ProjectDetailPage },
      { path: 'sql', name: 'Sql', component: SqlConsolePage },
      { path: 'datasource', name: 'DataSource', component: DataSourcePage },
    ]
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;