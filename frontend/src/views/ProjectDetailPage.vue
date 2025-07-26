<template>
  <div class="project-detail-page">
    <!-- 导航路径 -->
    <div class="breadcrumb">
      <router-link to="/dashboard/project" class="breadcrumb-link">{{ $t('project.title') }}</router-link>
      <span class="breadcrumb-separator">></span>
      <span class="breadcrumb-current">{{ project?.name || $t('project.detail') }}</span>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading">
      <p>{{ $t('common.loading') }}</p>
    </div>

    <!-- 错误提示 -->
    <div v-if="error" class="error-message">
      <p>{{ error }}</p>
      <button @click="loadProjectDetail">{{ $t('common.retry') }}</button>
    </div>

    <!-- 项目内容 -->
    <div v-if="!loading && !error">
    <!-- 项目基本信息 -->
    <div class="project-info-card">
      <div class="card-header">
        <h3>{{ $t('project.info') }}</h3>
        <button class="edit-btn" @click="editProject">{{ $t('project.edit') }}</button>
      </div>
      <div class="project-info">
        <div class="info-item">
          <label>{{ $t('project.name') }}：</label>
          <span>{{ project?.name }}</span>
        </div>
        <div class="info-item">
          <label>{{ $t('project.description') }}：</label>
            <span>{{ project?.description || $t('project.noDescription') }}</span>
        </div>
        <div class="info-item">
          <label>{{ $t('project.createTime') }}：</label>
            <span>{{ formatDate(project?.createTime) }}</span>
        </div>
      </div>
    </div>

    <!-- 数据源管理 -->
    <div class="datasource-section">
      <div class="datasource-header">
        <h3>{{ $t('datasource.title') }}</h3>
          <button class="datasource-add-btn" @click="openBindDataSource">{{ $t('datasource.bind') }}</button>
      </div>
        <div class="datasource-content">
          <div v-if="currentDatasource" class="datasource-info">
            <div class="info-item">
              <label>{{ $t('datasource.name') }}：</label>
              <span>{{ currentDatasource.name }}</span>
            </div>
            <div class="info-item">
              <label>{{ $t('datasource.type') }}：</label>
              <span>{{ currentDatasource.type }}</span>
            </div>
            <div class="info-item">
              <label>{{ $t('datasource.host') }}：</label>
              <span>{{ currentDatasource.host }}:{{ currentDatasource.port }}</span>
            </div>
            <div class="info-item">
              <label>{{ $t('datasource.database') }}：</label>
              <span>{{ currentDatasource.databaseName }}</span>
            </div>
            <div class="info-item">
              <label>{{ $t('datasource.username') }}：</label>
              <span>{{ currentDatasource.username }}</span>
            </div>
            <div class="datasource-actions">
              <button class="datasource-op" @click="testDataSourceConnection">{{ $t('datasource.test') }}</button>
              <button class="datasource-op" @click="unbindDataSource">{{ $t('datasource.unbind') }}</button>
            </div>
          </div>
          <div v-else class="no-datasource">
            <p>{{ $t('datasource.notBound') }}</p>
          </div>
        </div>
    </div>

    <!-- 版本管理 -->
    <div class="version-section">
      <div class="version-header">
        <h3>{{ $t('version.title') }}</h3>
        <button class="version-add-btn" @click="openAddVersion">{{ $t('version.create') }}</button>
      </div>
      <table class="version-table">
        <thead>
          <tr>
            <th>{{ $t('version.name') }}</th>
            <th>{{ $t('version.description') }}</th>
            <th>{{ $t('version.createTime') }}</th>
            <th>{{ $t('common.actions') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="ver in versions" :key="ver.id">
              <td>{{ ver.versionName }}</td>
              <td>{{ ver.description || $t('version.noDescription') }}</td>
              <td>{{ formatDate(ver.createTime) }}</td>
            <td>
              <button class="version-op" @click="viewVersion(ver)">{{ $t('version.detail') }}</button>
              <button class="version-op" @click="editVersion(ver)">{{ $t('version.edit') }}</button>
              <button class="version-op" @click="confirmDeleteVersion(ver)">{{ $t('version.delete') }}</button>
              <button class="version-op" @click="openCompare(ver)">{{ $t('version.compare') }}</button>
              <button class="version-op" @click="exportSql(ver)">{{ $t('version.export') }}</button>
            </td>
          </tr>
          <tr v-if="!versions.length">
            <td colspan="4" style="text-align:center;color:#aaa;">{{ $t('version.noData') }}</td>
          </tr>
        </tbody>
      </table>
      </div>
    </div>

    <!-- 编辑项目弹窗 -->
    <div v-if="showEditForm" class="project-dialog-mask">
      <div class="project-dialog">
        <h4>{{ $t('project.edit') }}</h4>
        <form @submit.prevent="submitEditForm">
          <div class="form-row">
            <label>{{ $t('project.name') }}</label>
            <input v-model="editForm.name" required />
          </div>
          <div class="form-row">
            <label>{{ $t('project.description') }}</label>
            <input v-model="editForm.description" />
          </div>
          <div class="form-row">
            <button class="project-save-btn" type="submit" :disabled="editFormLoading">
              {{ editFormLoading ? $t('common.saving') : $t('common.save') }}
            </button>
            <button class="project-cancel-btn" type="button" @click="closeEditForm">{{ $t('common.cancel') }}</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 新建/编辑/详情版本弹窗 -->
    <div v-if="showVersionForm" class="project-dialog-mask">
      <div class="project-dialog">
        <h4>{{ versionFormMode==='add' ? $t('version.create') : (versionFormMode==='edit' ? $t('version.edit') : $t('version.detail')) }}</h4>
        <form @submit.prevent="submitVersionForm">
          <div class="form-row">
            <label>{{ $t('version.name') }}</label>
            <input v-model="versionForm.versionName" :readonly="versionFormMode==='detail'" required />
          </div>
          <div class="form-row">
            <label>{{ $t('version.description') }}</label>
            <input v-model="versionForm.description" :readonly="versionFormMode==='detail'" />
          </div>
          <div class="form-row" v-if="versionFormMode!=='detail'">
            <button class="version-save-btn" type="submit" :disabled="versionFormLoading">
              {{ versionFormLoading ? $t('common.saving') : $t('common.save') }}
            </button>
            <button class="version-cancel-btn" type="button" @click="closeVersionForm">{{ $t('common.cancel') }}</button>
          </div>
          <div class="form-row" v-else>
            <button class="version-cancel-btn" type="button" @click="closeVersionForm">{{ $t('common.close') }}</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 删除版本确认弹窗 -->
    <div v-if="showDelete" class="project-dialog-mask">
      <div class="project-dialog">
        <h4>{{ $t('version.confirmDelete') }}</h4>
        <p>{{ $t('version.confirmDeleteMessage', { name: delTarget?.versionName }) }}</p>
        <div style="text-align:right;margin-top:18px;">
          <button class="project-cancel-btn" @click="showDelete=false">{{ $t('common.cancel') }}</button>
          <button class="project-del-btn" @click="deleteVersion" :disabled="deleteLoading">
            {{ deleteLoading ? $t('common.deleting') : $t('common.delete') }}
          </button>
        </div>
      </div>
    </div>



    <!-- 绑定数据源弹窗 -->
    <div v-if="showBindDataSource" class="project-dialog-mask">
      <div class="project-dialog">
        <h4>{{ $t('datasource.bind') }}</h4>
          <div class="form-row">
            <label>{{ $t('datasource.select') }}</label>
            <select v-model="selectedDataSourceId">
              <option value="">{{ $t('datasource.pleaseSelect') }}</option>
              <option v-for="ds in availableDataSources" :key="ds.id" :value="ds.id">{{ ds.name }} ({{ ds.type }})</option>
            </select>
          </div>
          <div style="text-align:right;margin-top:18px;">
          <button class="project-cancel-btn" @click="closeBindDataSource">{{ $t('common.cancel') }}</button>
          <button class="project-save-btn" @click="bindDataSource" :disabled="!selectedDataSourceId || bindLoading">
            {{ bindLoading ? $t('datasource.binding') : $t('datasource.bind') }}
          </button>
        </div>
      </div>
    </div>

    <!-- 测试连接结果弹窗 -->
    <div v-if="showTestResult" class="project-dialog-mask">
      <div class="project-dialog">
        <h4>{{ $t('datasource.testResult') }}</h4>
        <p>{{ testResultMessage }}</p>
        <div style="text-align:right;margin-top:18px;">
          <button class="project-cancel-btn" @click="showTestResult=false">{{ $t('common.close') }}</button>
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
import { useI18n } from 'vue-i18n';
import request from '../utils/request';
import type { Project, ProjectVersion, Datasource } from '../types/api';

const route = useRoute();
const router = useRouter();
const { t } = useI18n();

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
    error.value = err.message || t('project.loadFailed');
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
    showToast(t('project.updateSuccess'));
  } catch (err: any) {
    showToast(err.message || t('project.updateFailed'), 'error');
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
      showToast(t('version.createSuccess'));
  } else if (versionFormMode.value === 'edit') {
      await request.put('/api/project-version/update', versionForm.value);
      const index = versions.value.findIndex(v => v.id === versionForm.value.id);
      if (index > -1) {
        versions.value[index] = { ...versions.value[index], ...versionForm.value };
  }
      showToast(t('version.updateSuccess'));
    }
    
  showVersionForm.value = false;
  } catch (err: any) {
    showToast(err.message || t('common.operationFailed'), 'error');
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
    showToast(t('version.deleteSuccess'));
  } catch (err: any) {
    showToast(err.message || t('version.deleteFailed'), 'error');
  } finally {
    deleteLoading.value = false;
  }
}

// 版本对比相关
function openCompare(ver: ProjectVersion) {
  // 跳转到版本对比页面，传递基础版本ID和项目ID
  router.push(`/dashboard/project/${project.value?.id}/version/${ver.id}/compare`);
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
    showToast(t('version.exportSuccess'));
  } catch (err: any) {
    showToast(err.message || t('version.exportFailed'), 'error');
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
    showToast(t('datasource.bindSuccess'));
  } catch (err: any) {
    showToast(err.message || t('datasource.bindFailed'), 'error');
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
    showToast(t('datasource.unbindSuccess'));
  } catch (err: any) {
    showToast(err.message || t('datasource.unbindFailed'), 'error');
}
}

async function testDataSourceConnection() {
  if (!currentDatasource.value) return;
  
  try {
    testResultMessage.value = t('datasource.testing');
    showTestResult.value = true;
    
    const response = await request.post('/api/datasource/test-connection', currentDatasource.value);
    testResultMessage.value = response.data.data ? t('datasource.testSuccess') : t('datasource.testFailed');
  } catch (err: any) {
    testResultMessage.value = t('datasource.testError') + ': ' + (err.message || t('common.unknownError'));
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