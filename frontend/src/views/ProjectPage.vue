<template>
  <div class="project-page">
    <div class="project-header">
      <h3>项目管理</h3>
      <button class="project-add-btn" @click="openAdd">新建项目</button>
    </div>
    
    <!-- 加载状态 -->
    <div v-if="loading" class="loading">加载中...</div>
    
    <table v-else class="project-table">
      <thead>
        <tr>
          <th>项目名称</th>
          <th>描述</th>
          <th>创建时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="proj in projects" :key="proj.id">
          <td>{{ proj.name }}</td>
          <td>{{ proj.description }}</td>
          <td>{{ formatDate(proj.createTime) }}</td>
          <td>
            <button class="project-op" @click="viewDetail(proj)">详情</button>
            <button class="project-op" @click="editProject(proj)">编辑</button>
            <button class="project-op" @click="confirmDelete(proj)">删除</button>
          </td>
        </tr>
        <tr v-if="!projects.length">
          <td colspan="4" style="text-align:center;color:#aaa;">暂无项目</td>
        </tr>
      </tbody>
    </table>
    
    <!-- 新建/编辑弹窗 -->
    <div v-if="showForm" class="project-dialog-mask">
      <div class="project-dialog">
        <h4>{{ formMode==='add' ? '新建项目' : '编辑项目' }}</h4>
        <form @submit.prevent="submitForm">
          <div class="form-row">
            <label>项目名称</label>
            <input v-model="form.name" required />
          </div>
          <div class="form-row">
            <label>描述</label>
            <input v-model="form.description" />
          </div>
          <div class="form-row">
            <label>数据源</label>
            <select v-model="form.datasourceId">
              <option value="">请选择数据源（可选）</option>
              <option v-for="ds in datasources" :key="ds.id" :value="ds.id">
                {{ ds.name }}
              </option>
            </select>
          </div>
          <div class="form-row">
            <button class="project-save-btn" type="submit" :disabled="submitting">
              {{ submitting ? '保存中...' : '保存' }}
            </button>
            <button class="project-cancel-btn" type="button" @click="closeForm">取消</button>
          </div>
        </form>
      </div>
    </div>
    
    <!-- 删除项目弹窗 -->
    <div v-if="showDelete" class="project-dialog-mask">
      <div class="project-dialog">
        <h4>确认删除？</h4>
        <p>确定要删除项目 <b>{{ delTarget?.name }}</b> 吗？</p>
        <div style="text-align:right;margin-top:18px;">
          <button class="project-cancel-btn" @click="showDelete=false">取消</button>
          <button class="project-del-btn" @click="deleteProject" :disabled="deleting">
            {{ deleting ? '删除中...' : '删除' }}
          </button>
        </div>
      </div>
    </div>
    
    <!-- 错误提示 -->
    <div v-if="error" class="error-toast">
      {{ error }}
      <button @click="error=''" class="close-btn">×</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import request from '../utils/request';

const router = useRouter();
const projects = ref<any[]>([]);
const datasources = ref<any[]>([]);
const showForm = ref(false);
const formMode = ref<'add'|'edit'>('add');
const form = ref<any>({});
const showDelete = ref(false);
const delTarget = ref<any>(null);
const loading = ref(false);
const submitting = ref(false);
const deleting = ref(false);
const error = ref('');

onMounted(() => {
  loadProjects();
  loadDatasources();
});

async function loadProjects() {
  try {
    loading.value = true;
    error.value = '';
    const response = await request.get('/api/project/list');
    projects.value = response.data.data || [];
  } catch (err: any) {
    error.value = err.message || '加载项目失败';
    console.error('加载项目失败:', err);
  } finally {
    loading.value = false;
  }
}

async function loadDatasources() {
  try {
    const response = await request.get('/api/datasource/list');
    datasources.value = response.data.data || [];
  } catch (err: any) {
    console.error('加载数据源失败:', err);
  }
}

function openAdd() {
  formMode.value = 'add';
  form.value = { name: '', description: '', datasourceId: null };
  showForm.value = true;
}

function editProject(proj: any) {
  formMode.value = 'edit';
  form.value = { ...proj };
  showForm.value = true;
}

function viewDetail(proj: any) {
  router.push(`/dashboard/project/${proj.id}`);
}

function closeForm() {
  showForm.value = false;
  error.value = '';
}

async function submitForm() {
  if (submitting.value) return;
  
  try {
    submitting.value = true;
    error.value = '';
    
    // 处理数据源ID：空字符串转换为null
    const formData = { ...form.value };
    if (formData.datasourceId === '') {
      formData.datasourceId = null;
    }
    
    if (formMode.value === 'add') {
      await request.post('/api/project/create', formData);
      await loadProjects();
      showForm.value = false;
    } else if (formMode.value === 'edit') {
      await request.put('/api/project/update', formData);
      await loadProjects();
      showForm.value = false;
    }
  } catch (err: any) {
    error.value = err.message || '操作失败';
    console.error('提交失败:', err);
  } finally {
    submitting.value = false;
  }
}

function confirmDelete(proj: any) {
  delTarget.value = proj;
  showDelete.value = true;
}

async function deleteProject() {
  if (deleting.value) return;
  
  try {
    deleting.value = true;
    error.value = '';
    await request.delete(`/api/project/delete/${delTarget.value.id}`);
    await loadProjects();
    showDelete.value = false;
  } catch (err: any) {
    error.value = err.message || '删除失败';
    console.error('删除失败:', err);
  } finally {
    deleting.value = false;
  }
}

function formatDate(dateString: string) {
  if (!dateString) return '';
  return new Date(dateString).toLocaleDateString('zh-CN');
}
</script>

<style scoped>
.project-page {
  width: 100%;
  min-height: 0;
  flex: 1;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  position: relative;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #666;
}

.error-toast {
  position: fixed;
  top: 20px;
  right: 20px;
  background: #f56565;
  color: white;
  padding: 12px 16px;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
  z-index: 1000;
  display: flex;
  align-items: center;
  gap: 8px;
}

.close-btn {
  background: none;
  border: none;
  color: white;
  font-size: 18px;
  cursor: pointer;
  padding: 0;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.project-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.project-add-btn {
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 8px 18px;
  font-size: 1rem;
  cursor: pointer;
  transition: background 0.2s;
}

.project-add-btn:hover {
  background: #337ecc;
}

.project-table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px 0 rgba(0,0,0,0.03);
}

.project-table th, .project-table td {
  padding: 12px 10px;
  border-bottom: 1px solid #f0f0f0;
  text-align: left;
}

.project-table th {
  background: #f5f7fa;
  color: #666;
  font-weight: 500;
}

.project-op {
  background: none;
  border: none;
  color: #409eff;
  cursor: pointer;
  margin-right: 8px;
  font-size: 0.98rem;
  padding: 0 4px;
  transition: color 0.2s;
}

.project-op:hover {
  color: #337ecc;
}

.project-dialog-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.project-dialog {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  width: 90%;
  max-width: 500px;
  max-height: 80vh;
  overflow-y: auto;
}

.project-dialog h4 {
  margin-bottom: 20px;
  color: #333;
}

.form-row {
  margin-bottom: 16px;
}

.form-row label {
  display: block;
  margin-bottom: 4px;
  color: #666;
  font-weight: 500;
}

.form-row input, .form-row select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.form-row input:focus, .form-row select:focus {
  outline: none;
  border-color: #409eff;
}

.project-save-btn, .project-cancel-btn, .project-del-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  margin-right: 8px;
  font-size: 14px;
  transition: background 0.2s;
}

.project-save-btn {
  background: #409eff;
  color: #fff;
}

.project-save-btn:hover:not(:disabled) {
  background: #337ecc;
}

.project-save-btn:disabled {
  background: #a0cfff;
  cursor: not-allowed;
}

.project-cancel-btn {
  background: #f0f0f0;
  color: #666;
}

.project-cancel-btn:hover {
  background: #e0e0e0;
}

.project-del-btn {
  background: #f56565;
  color: #fff;
}

.project-del-btn:hover:not(:disabled) {
  background: #e53e3e;
}

.project-del-btn:disabled {
  background: #feb2b2;
  cursor: not-allowed;
}
</style>