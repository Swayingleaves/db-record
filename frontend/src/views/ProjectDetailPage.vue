<template>
  <div class="project-detail-page">
    <!-- 导航路径 -->
    <div class="breadcrumb">
      <router-link to="/dashboard/project" class="breadcrumb-link">项目管理</router-link>
      <span class="breadcrumb-separator">></span>
      <span class="breadcrumb-current">{{ project?.name || '项目详情' }}</span>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading">
      <p>加载中...</p>
    </div>

    <!-- 错误提示 -->
    <div v-if="error" class="error-message">
      <p>{{ error }}</p>
      <button @click="loadProjectDetail">重试</button>
    </div>

    <!-- 项目内容 -->
    <div v-if="!loading && !error">
    <!-- 项目基本信息 -->
    <div class="project-info-card">
      <div class="card-header">
        <h3>项目信息</h3>
        <button class="edit-btn" @click="editProject">编辑项目</button>
      </div>
      <div class="project-info">
        <div class="info-item">
          <label>项目名称：</label>
          <span>{{ project?.name }}</span>
        </div>
        <div class="info-item">
          <label>项目描述：</label>
            <span>{{ project?.description || '暂无描述' }}</span>
        </div>
        <div class="info-item">
          <label>创建时间：</label>
            <span>{{ formatDate(project?.createTime) }}</span>
        </div>
      </div>
    </div>

    <!-- 数据源管理 -->
    <div class="datasource-section">
      <div class="datasource-header">
        <h3>数据源管理</h3>
          <button class="datasource-add-btn" @click="openBindDataSource">绑定数据源</button>
      </div>
        <div class="datasource-content">
          <div v-if="currentDatasource" class="datasource-info">
            <div class="info-item">
              <label>名称：</label>
              <span>{{ currentDatasource.name }}</span>
            </div>
            <div class="info-item">
              <label>类型：</label>
              <span>{{ currentDatasource.type }}</span>
            </div>
            <div class="info-item">
              <label>地址：</label>
              <span>{{ currentDatasource.host }}:{{ currentDatasource.port }}</span>
            </div>
            <div class="info-item">
              <label>数据库：</label>
              <span>{{ currentDatasource.databaseName }}</span>
            </div>
            <div class="info-item">
              <label>用户名：</label>
              <span>{{ currentDatasource.username }}</span>
            </div>
            <div class="datasource-actions">
              <button class="datasource-op" @click="testDataSourceConnection">测试连接</button>
              <button class="datasource-op" @click="unbindDataSource">解绑</button>
            </div>
          </div>
          <div v-else class="no-datasource">
            <p>暂未绑定数据源</p>
          </div>
        </div>
    </div>

    <!-- 版本管理 -->
    <div class="version-section">
      <div class="version-header">
        <h3>版本管理</h3>
        <button class="version-add-btn" @click="openAddVersion">新建版本</button>
      </div>
      <table class="version-table">
        <thead>
          <tr>
            <th>版本号</th>
            <th>描述</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="ver in versions" :key="ver.id">
              <td>{{ ver.versionName }}</td>
              <td>{{ ver.description || '暂无描述' }}</td>
              <td>{{ formatDate(ver.createTime) }}</td>
            <td>
              <button class="version-op" @click="viewVersion(ver)">详情</button>
              <button class="version-op" @click="editVersion(ver)">编辑</button>
              <button class="version-op" @click="confirmDeleteVersion(ver)">删除</button>
              <button class="version-op" @click="openCompare(ver)">对比</button>
              <button class="version-op" @click="exportSql(ver)">导出SQL</button>
            </td>
          </tr>
          <tr v-if="!versions.length">
            <td colspan="4" style="text-align:center;color:#aaa;">暂无版本</td>
          </tr>
        </tbody>
      </table>
      </div>
    </div>

    <!-- 编辑项目弹窗 -->
    <div v-if="showEditForm" class="project-dialog-mask">
      <div class="project-dialog">
        <h4>编辑项目</h4>
        <form @submit.prevent="submitEditForm">
          <div class="form-row">
            <label>项目名称</label>
            <input v-model="editForm.name" required />
          </div>
          <div class="form-row">
            <label>描述</label>
            <input v-model="editForm.description" />
          </div>
          <div class="form-row">
            <button class="project-save-btn" type="submit" :disabled="editFormLoading">
              {{ editFormLoading ? '保存中...' : '保存' }}
            </button>
            <button class="project-cancel-btn" type="button" @click="closeEditForm">取消</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 新建/编辑/详情版本弹窗 -->
    <div v-if="showVersionForm" class="project-dialog-mask">
      <div class="project-dialog">
        <h4>{{ versionFormMode==='add' ? '新建版本' : (versionFormMode==='edit' ? '编辑版本' : '版本详情') }}</h4>
        <form @submit.prevent="submitVersionForm">
          <div class="form-row">
            <label>版本号</label>
            <input v-model="versionForm.versionName" :readonly="versionFormMode==='detail'" required />
          </div>
          <div class="form-row">
            <label>描述</label>
            <input v-model="versionForm.description" :readonly="versionFormMode==='detail'" />
          </div>
          <div class="form-row" v-if="versionFormMode!=='detail'">
            <button class="version-save-btn" type="submit" :disabled="versionFormLoading">
              {{ versionFormLoading ? '保存中...' : '保存' }}
            </button>
            <button class="version-cancel-btn" type="button" @click="closeVersionForm">取消</button>
          </div>
          <div class="form-row" v-else>
            <button class="version-cancel-btn" type="button" @click="closeVersionForm">关闭</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 删除版本确认弹窗 -->
    <div v-if="showDelete" class="project-dialog-mask">
      <div class="project-dialog">
        <h4>确认删除？</h4>
        <p>确定要删除版本 <b>{{ delTarget?.versionName }}</b> 吗？</p>
        <div style="text-align:right;margin-top:18px;">
          <button class="project-cancel-btn" @click="showDelete=false">取消</button>
          <button class="project-del-btn" @click="deleteVersion" :disabled="deleteLoading">
            {{ deleteLoading ? '删除中...' : '删除' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 版本对比弹窗 -->
    <div v-if="showCompare" class="project-dialog-mask">
      <div class="project-dialog">
        <h4>版本对比</h4>
        <div class="form-row">
          <label>选择对比版本</label>
          <select v-model="compareTargetId">
            <option value="">请选择版本</option>
            <option v-for="ver in versions.filter(v => v.id !== compareBaseVersion?.id)" :key="ver.id" :value="ver.id">{{ ver.versionName }}</option>
          </select>
        </div>
        <div class="compare-result" v-if="compareResult">
          <p>对比结果：</p>
          <pre style="background:#f8f8f8;padding:10px;border-radius:6px;">{{ compareResult }}</pre>
        </div>
        <div style="text-align:right;margin-top:18px;">
          <button class="version-cancel-btn" @click="showCompare=false">关闭</button>
          <button class="version-save-btn" @click="performCompare" :disabled="!compareTargetId || compareLoading">
            {{ compareLoading ? '对比中...' : '开始对比' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 绑定数据源弹窗 -->
    <div v-if="showBindDataSource" class="project-dialog-mask">
      <div class="project-dialog">
        <h4>绑定数据源</h4>
          <div class="form-row">
            <label>选择数据源</label>
            <select v-model="selectedDataSourceId">
              <option value="">请选择数据源</option>
              <option v-for="ds in availableDataSources" :key="ds.id" :value="ds.id">{{ ds.name }} ({{ ds.type }})</option>
            </select>
          </div>
          <div style="text-align:right;margin-top:18px;">
          <button class="project-cancel-btn" @click="closeBindDataSource">取消</button>
          <button class="project-save-btn" @click="bindDataSource" :disabled="!selectedDataSourceId || bindLoading">
            {{ bindLoading ? '绑定中...' : '绑定' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 测试连接结果弹窗 -->
    <div v-if="showTestResult" class="project-dialog-mask">
      <div class="project-dialog">
        <h4>连接测试结果</h4>
        <p>{{ testResultMessage }}</p>
        <div style="text-align:right;margin-top:18px;">
          <button class="project-cancel-btn" @click="showTestResult=false">关闭</button>
        </div>
      </div>
    </div>

    <!-- Toast消息 -->
    <div v-if="toastMessage" class="toast" :class="toastType">
      {{ toastMessage }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import request from '../utils/request';
import type { Project, ProjectVersion, Datasource } from '../types/api';

const route = useRoute();
const router = useRouter();

// 基础数据
const project = ref<Project | null>(null);
const currentDatasource = ref<Datasource | null>(null);
const versions = ref<ProjectVersion[]>([]);
const availableDataSources = ref<Datasource[]>([]);

// 加载状态
const loading = ref(false);
const error = ref('');
const editFormLoading = ref(false);
const versionFormLoading = ref(false);
const deleteLoading = ref(false);
const compareLoading = ref(false);
const bindLoading = ref(false);

// 项目编辑相关
const showEditForm = ref(false);
const editForm = ref<Partial<Project>>({});

// 版本管理相关
const showVersionForm = ref(false);
const versionFormMode = ref<'add'|'edit'|'detail'>('add');
const versionForm = ref<Partial<ProjectVersion>>({});
const showDelete = ref(false);
const delTarget = ref<ProjectVersion | null>(null);

// 版本对比相关
const showCompare = ref(false);
const compareBaseVersion = ref<ProjectVersion | null>(null);
const compareTargetId = ref<number | null>(null);
const compareResult = ref('');

// 数据源相关
const showBindDataSource = ref(false);
const selectedDataSourceId = ref<number | null>(null);
const showTestResult = ref(false);
const testResultMessage = ref('');

// Toast消息
const toastMessage = ref('');
const toastType = ref<'success' | 'error'>('success');

// 页面加载
onMounted(() => {
  loadProjectDetail();
});

// 加载项目详情
async function loadProjectDetail() {
  try {
    loading.value = true;
    error.value = '';
    
    const projectId = parseInt(route.params.id as string);
    
    // 获取项目详情（包含数据源信息）
    const projectResponse = await request.get(`/api/project/detail-with-datasource/${projectId}`);
    
    // 从Result格式中获取数据
    const projectData = projectResponse.data.data;
    project.value = projectData.project;
    currentDatasource.value = projectData.datasource || null;
    
    // 获取版本列表
    const versionsResponse = await request.get(`/api/project-version/list/${projectId}`);
    versions.value = versionsResponse.data.data;
    
    // 获取可用数据源列表
    const datasourcesResponse = await request.get('/api/datasource/list');
    availableDataSources.value = datasourcesResponse.data.data;
    
  } catch (err: any) {
    error.value = err.message || '加载项目详情失败';
  } finally {
    loading.value = false;
  }
}

// 格式化日期
function formatDate(dateString?: string): string {
  if (!dateString) return '';
  return new Date(dateString).toLocaleString('zh-CN');
}

// 显示Toast消息
function showToast(message: string, type: 'success' | 'error' = 'success') {
  toastMessage.value = message;
  toastType.value = type;
  setTimeout(() => {
    toastMessage.value = '';
  }, 3000);
}

// 项目编辑相关
function editProject() {
  editForm.value = { ...project.value };
  showEditForm.value = true;
}

function closeEditForm() {
  showEditForm.value = false;
  editForm.value = {};
}

async function submitEditForm() {
  try {
    editFormLoading.value = true;
    await request.put('/api/project/update', editForm.value);
    if (project.value) {
      project.value = { ...project.value, ...editForm.value } as Project;
    }
  showEditForm.value = false;
    showToast('项目更新成功');
  } catch (err: any) {
    showToast(err.message || '项目更新失败', 'error');
  } finally {
    editFormLoading.value = false;
  }
}

// 版本管理相关
function openAddVersion() {
  versionFormMode.value = 'add';
  versionForm.value = { 
    projectId: project.value?.id,
    versionName: '', 
    description: '' 
  };
  showVersionForm.value = true;
}

function editVersion(ver: ProjectVersion) {
  versionFormMode.value = 'edit';
  versionForm.value = { ...ver };
  showVersionForm.value = true;
}

function viewVersion(ver: ProjectVersion) {
  // 跳转到版本详情页面
  router.push(`/dashboard/project/${project.value?.id}/version/${ver.id}`);
}

function closeVersionForm() {
  showVersionForm.value = false;
  versionForm.value = {};
}

async function submitVersionForm() {
  try {
    versionFormLoading.value = true;
    
  if (versionFormMode.value === 'add') {
      const response = await request.post('/api/project-version/create', versionForm.value);
      versions.value.unshift(response.data.data);
      showToast('版本创建成功');
  } else if (versionFormMode.value === 'edit') {
      await request.put('/api/project-version/update', versionForm.value);
      const index = versions.value.findIndex(v => v.id === versionForm.value.id);
      if (index > -1) {
        versions.value[index] = { ...versions.value[index], ...versionForm.value };
  }
      showToast('版本更新成功');
    }
    
  showVersionForm.value = false;
  } catch (err: any) {
    showToast(err.message || '操作失败', 'error');
  } finally {
    versionFormLoading.value = false;
  }
}

function confirmDeleteVersion(ver: ProjectVersion) {
  delTarget.value = ver;
  showDelete.value = true;
}

async function deleteVersion() {
  if (!delTarget.value) return;
  
  try {
    deleteLoading.value = true;
    await request.delete(`/api/project-version/delete/${delTarget.value.id}`);
    versions.value = versions.value.filter(v => v.id !== delTarget.value?.id);
  showDelete.value = false;
    showToast('版本删除成功');
  } catch (err: any) {
    showToast(err.message || '删除失败', 'error');
  } finally {
    deleteLoading.value = false;
  }
}

// 版本对比相关
function openCompare(ver: ProjectVersion) {
  compareBaseVersion.value = ver;
  compareTargetId.value = null;
  compareResult.value = '';
  showCompare.value = true;
}

async function performCompare() {
  if (!compareBaseVersion.value || !compareTargetId.value) return;
  
  try {
    compareLoading.value = true;
    const response = await request.get(`/api/project-version/compare/${compareBaseVersion.value.id}/${compareTargetId.value}`);
    compareResult.value = JSON.stringify(response.data.data, null, 2);
  } catch (err: any) {
    showToast(err.message || '对比失败', 'error');
  } finally {
    compareLoading.value = false;
  }
}

async function exportSql(ver: ProjectVersion) {
  try {
    const response = await request.get(`/api/project-version/export-sql/${ver.id}`);
    // 创建下载链接
    const blob = new Blob([response.data.data.sql], { type: 'text/plain' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${ver.versionName}.sql`;
    a.click();
    window.URL.revokeObjectURL(url);
    showToast('SQL导出成功');
  } catch (err: any) {
    showToast(err.message || '导出失败', 'error');
  }
}

// 数据源相关
function openBindDataSource() {
  selectedDataSourceId.value = null;
  showBindDataSource.value = true;
}

function closeBindDataSource() {
  showBindDataSource.value = false;
  selectedDataSourceId.value = null;
}

async function bindDataSource() {
  if (!selectedDataSourceId.value || !project.value) return;
  
  try {
    bindLoading.value = true;
    await request.post('/api/project/bind-datasource', {
      projectId: project.value.id,
      datasourceId: selectedDataSourceId.value
    });
    
    // 重新加载项目详情
    await loadProjectDetail();
    showBindDataSource.value = false;
    showToast('数据源绑定成功');
  } catch (err: any) {
    showToast(err.message || '绑定失败', 'error');
  } finally {
    bindLoading.value = false;
}
}

async function unbindDataSource() {
  if (!project.value) return;
  
  try {
    await request.post(`/api/project/unbind-datasource/${project.value.id}`);
    currentDatasource.value = null;
    project.value.datasourceId = undefined;
    showToast('数据源解绑成功');
  } catch (err: any) {
    showToast(err.message || '解绑失败', 'error');
}
}

async function testDataSourceConnection() {
  if (!currentDatasource.value) return;
  
  try {
    testResultMessage.value = '正在测试连接...';
    showTestResult.value = true;
    
    const response = await request.post('/api/datasource/test-connection', currentDatasource.value);
    testResultMessage.value = response.data.data ? '连接成功！' : '连接失败，请检查配置';
  } catch (err: any) {
    testResultMessage.value = '连接失败：' + (err.message || '未知错误');
  }
}
</script>

<style scoped>
.project-detail-page {
  width: 100%;
  min-height: 0;
  flex: 1;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

/* 导航路径 */
.breadcrumb {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  font-size: 14px;
  color: #666;
}

.breadcrumb-link {
  color: #409eff;
  text-decoration: none;
  transition: color 0.2s;
}

.breadcrumb-link:hover {
  color: #337ecc;
}

.breadcrumb-separator {
  margin: 0 8px;
  color: #ccc;
}

.breadcrumb-current {
  color: #333;
  font-weight: 500;
}

/* 加载状态 */
.loading {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 24px;
}

.loading p {
  color: #909399;
  font-size: 16px;
}

/* 错误提示 */
.error-message {
  background: #fde2e2;
  color: #f56c6c;
  padding: 15px 20px;
  border-radius: 8px;
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 1px solid #faeced;
}

.error-message p {
  margin: 0;
  font-size: 14px;
}

.error-message button {
  background: #f56c6c;
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 6px 12px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.error-message button:hover {
  background: #f78989;
}

/* 项目信息卡片 */
.project-info-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px 0 rgba(0,0,0,0.03);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.edit-btn {
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 8px 16px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.edit-btn:hover {
  background: #337ecc;
}

.project-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-item {
  display: flex;
  align-items: center;
}

.info-item label {
  width: 100px;
  color: #666;
  font-weight: 500;
}

.info-item span {
  color: #333;
}

/* 数据源管理 */
.datasource-section {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px 0 rgba(0,0,0,0.03);
}

.datasource-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.datasource-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.datasource-add-btn {
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 8px 16px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.datasource-add-btn:hover {
  background: #337ecc;
}

.datasource-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.datasource-info {
  flex: 1;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 6px;
  border: 1px solid #ebeef5;
}

.datasource-info .info-item {
  margin-bottom: 8px;
}

.datasource-info .info-item:last-child {
  margin-bottom: 0;
}

.datasource-info .info-item label {
  color: #555;
  font-size: 0.9rem;
}

.datasource-info .info-item span {
  color: #333;
  font-size: 1rem;
}

.datasource-actions {
  margin-top: 15px;
  display: flex;
  gap: 10px;
}

.datasource-actions .datasource-op {
  background: none;
  border: 1px solid #dcdfe6;
  color: #606266;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.datasource-actions .datasource-op:hover {
  color: #409eff;
  border-color: #409eff;
}

.no-datasource {
  text-align: center;
  color: #aaa;
  padding: 20px;
}

.no-datasource p {
  margin: 0;
  font-size: 14px;
}

/* 版本管理 */
.version-section {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px 0 rgba(0,0,0,0.03);
}

.version-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.version-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.version-add-btn {
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 8px 16px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.version-add-btn:hover {
  background: #337ecc;
}

.version-table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.version-table th, .version-table td {
  padding: 12px 10px;
  border-bottom: 1px solid #f0f0f0;
  text-align: left;
}

.version-table th {
  background: #f5f7fa;
  color: #666;
  font-weight: 500;
}

.version-op {
  background: none;
  border: none;
  color: #409eff;
  cursor: pointer;
  margin-right: 8px;
  font-size: 0.95rem;
  padding: 0 4px;
  transition: color 0.2s;
}

.version-op:hover {
  color: #337ecc;
}

/* 弹窗样式 */
.project-dialog-mask {
  position: fixed;
  left: 0; top: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.18);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.project-dialog {
  background: #fff;
  border-radius: 10px;
  min-width: 320px;
  max-width: 96vw;
  padding: 28px 24px 18px 24px;
  box-shadow: 0 4px 24px 0 rgba(0,0,0,0.10);
}

.project-dialog h4 {
  margin: 0 0 18px 0;
  font-size: 1.18rem;
  font-weight: 600;
}

.form-row {
  display: flex;
  align-items: center;
  margin-bottom: 14px;
}

.form-row label {
  width: 70px;
  color: #555;
  font-size: 1rem;
}

.form-row input, .form-row select {
  flex: 1;
  padding: 7px 10px;
  border: 1px solid #e0e3e8;
  border-radius: 5px;
  font-size: 1rem;
  outline: none;
  background: #f8fafc;
  transition: border 0.2s;
}

.form-row input:focus, .form-row select:focus {
  border: 1.5px solid #409eff;
  background: #fff;
}

.project-save-btn, .version-save-btn {
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 7px 18px;
  font-size: 1rem;
  cursor: pointer;
  margin-right: 12px;
  transition: background 0.2s;
}

.project-save-btn:hover, .version-save-btn:hover {
  background: #337ecc;
}

.project-cancel-btn, .version-cancel-btn {
  background: #eee;
  color: #555;
  border: none;
  border-radius: 6px;
  padding: 7px 18px;
  font-size: 1rem;
  cursor: pointer;
  margin-right: 8px;
  transition: background 0.2s;
}

.project-cancel-btn:hover, .version-cancel-btn:hover {
  background: #e0e3e8;
}

.project-del-btn {
  background: #e74c3c;
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 7px 18px;
  font-size: 1rem;
  cursor: pointer;
  transition: background 0.2s;
}

.project-del-btn:hover {
  background: #c0392b;
}

.compare-result {
  margin: 12px 0 0 0;
  font-size: 0.98rem;
  color: #333;
}

/* Toast消息样式 */
.toast {
  position: fixed;
  top: 20px;
  right: 20px;
  background-color: #409eff;
  color: #fff;
  padding: 10px 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 10000;
  opacity: 0.9;
  transition: opacity 0.5s ease-in-out;
}

.toast.success {
  background-color: #67c23a;
}

.toast.error {
  background-color: #f56c6c;
}
</style>