<template>
  <div class="version-compare-page">
    <!-- 导航路径 -->
    <div class="breadcrumb">
      <router-link to="/dashboard/project" class="breadcrumb-link">项目管理</router-link>
      <span class="breadcrumb-separator">></span>
      <router-link :to="`/dashboard/project/${projectId}`" class="breadcrumb-link">{{ projectName || '项目详情' }}</router-link>
      <span class="breadcrumb-separator">></span>
      <span class="breadcrumb-current">版本对比</span>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading">
      <p>加载中...</p>
    </div>

    <!-- 错误提示 -->
    <div v-if="error" class="error-message">
      <p>{{ error }}</p>
      <button @click="loadVersions">重试</button>
    </div>

    <!-- 版本选择区域 -->
    <div v-if="!loading && !error" class="version-selector">
      <div class="selector-card">
        <h3>版本对比</h3>
        <div class="version-select-row">
          <div class="version-select-item">
            <label>基础版本：</label>
            <select v-model="baseVersionId" @change="onVersionChange">
              <option value="">请选择版本</option>
              <option v-for="ver in versions" :key="ver.id" :value="ver.id">{{ ver.versionName }}</option>
            </select>
          </div>
          <div class="switch-btn-container">
            <button class="switch-btn" @click="switchVersions" :disabled="!baseVersionId || !targetVersionId">
              ⇄ 切换
            </button>
          </div>
          <div class="version-select-item">
            <label>目标版本：</label>
            <select v-model="targetVersionId" @change="onVersionChange">
              <option value="">请选择版本</option>
              <option v-for="ver in versions.filter(v => v.id !== baseVersionId)" :key="ver.id" :value="ver.id">{{ ver.versionName }}</option>
            </select>
          </div>
        </div>
        <div class="action-buttons">
          <button class="compare-btn" @click="performCompare" :disabled="!baseVersionId || !targetVersionId || compareLoading">
            {{ compareLoading ? '对比中...' : '开始对比' }}
          </button>
          <button class="export-btn" @click="exportDiffSql" :disabled="!compareResult || exportLoading">
            {{ exportLoading ? '导出中...' : '导出差异SQL' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 对比结果区域 -->
    <div v-if="compareResult && !loading" class="compare-result">
      <div class="result-header">
        <h3>对比结果</h3>
        <div class="version-info">
          <span class="version-tag base">{{ getVersionName(baseVersionId) }}</span>
          <span class="arrow">→</span>
          <span class="version-tag target">{{ getVersionName(targetVersionId) }}</span>
        </div>
      </div>
      
      <!-- 统计信息 -->
      <div class="compare-stats">
        <div class="stat-item added">
          <span class="stat-label">新增表：</span>
          <span class="stat-value">{{ compareResult.addedTables?.length || 0 }}</span>
        </div>
        <div class="stat-item removed">
          <span class="stat-label">删除表：</span>
          <span class="stat-value">{{ compareResult.removedTables?.length || 0 }}</span>
        </div>
        <div class="stat-item modified">
          <span class="stat-label">修改表：</span>
          <span class="stat-value">{{ compareResult.modifiedTables?.length || 0 }}</span>
        </div>
      </div>

      <!-- 详细差异 -->
      <div class="diff-details">
        <!-- 新增的表 -->
        <div v-if="compareResult.addedTables?.length" class="diff-section">
          <h4 class="section-title added">新增的表 ({{ compareResult.addedTables.length }})</h4>
          <div class="table-list">
            <div v-for="table in compareResult.addedTables" :key="table.tableName" class="table-item added">
              <div class="table-header">
                <span class="table-name">{{ table.tableName }}</span>
                <span class="table-comment">{{ table.tableComment || '无注释' }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 删除的表 -->
        <div v-if="compareResult.removedTables?.length" class="diff-section">
          <h4 class="section-title removed">删除的表 ({{ compareResult.removedTables.length }})</h4>
          <div class="table-list">
            <div v-for="table in compareResult.removedTables" :key="table.tableName" class="table-item removed">
              <div class="table-header">
                <span class="table-name">{{ table.tableName }}</span>
                <span class="table-comment">{{ table.tableComment || '无注释' }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 修改的表 -->
        <div v-if="compareResult.modifiedTables?.length" class="diff-section">
          <h4 class="section-title modified">修改的表 ({{ compareResult.modifiedTables.length }})</h4>
          <div class="table-list">
            <div v-for="table in compareResult.modifiedTables" :key="table.tableName" class="table-item modified">
              <div class="table-header">
                <span class="table-name">{{ table.tableName }}</span>
                <span class="table-comment">{{ table.tableComment || '无注释' }}</span>
              </div>
              <div class="table-changes">
                <div v-if="table.addedColumns?.length" class="change-item">
                  <span class="change-label added">新增字段：</span>
                  <span class="change-value">{{ table.addedColumns.map((c: any) => c.columnName).join(', ') }}</span>
                </div>
                <div v-if="table.removedColumns?.length" class="change-item">
                  <span class="change-label removed">删除字段：</span>
                  <span class="change-value">{{ table.removedColumns.map((c: any) => c.columnName).join(', ') }}</span>
                </div>
                <div v-if="table.modifiedColumns?.length" class="change-item">
                  <span class="change-label modified">修改字段：</span>
                  <span class="change-value">{{ table.modifiedColumns.map((c: any) => c.columnName).join(', ') }}</span>
                </div>
                <div v-if="table.addedIndexes?.length" class="change-item">
                  <span class="change-label added">新增索引：</span>
                  <span class="change-value">{{ table.addedIndexes.map((i: any) => i.indexName).join(', ') }}</span>
                </div>
                <div v-if="table.removedIndexes?.length" class="change-item">
                  <span class="change-label removed">删除索引：</span>
                  <span class="change-value">{{ table.removedIndexes.map((i: any) => i.indexName).join(', ') }}</span>
                </div>
                <div v-if="table.modifiedIndexes?.length" class="change-item">
                  <span class="change-label modified">修改索引：</span>
                  <span class="change-value">{{ table.modifiedIndexes.map((i: any) => i.indexName).join(', ') }}</span>
                </div>
              </div>
            </div>
          </div>
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
import type { ProjectVersion } from '../types/api';

const route = useRoute();
const router = useRouter();

// 路由参数
const projectId = ref<number>(parseInt(route.params.projectId as string));
const initialVersionId = ref<number>(parseInt(route.params.versionId as string));

// 基础数据
const projectName = ref<string>('');
const versions = ref<ProjectVersion[]>([]);
const baseVersionId = ref<number | null>(null);
const targetVersionId = ref<number | null>(null);
const compareResult = ref<any>(null);

// 加载状态
const loading = ref(false);
const error = ref('');
const compareLoading = ref(false);
const exportLoading = ref(false);

// Toast消息
const toastMessage = ref('');
const toastType = ref<'success' | 'error'>('success');

// 页面加载
onMounted(() => {
  loadVersions();
});

// 加载版本列表
async function loadVersions() {
  try {
    loading.value = true;
    error.value = '';
    
    // 获取项目信息
    const projectResponse = await request.get(`/api/project/detail/${projectId.value}`);
    projectName.value = projectResponse.data.data.name;
    
    // 获取版本列表
    const versionsResponse = await request.get(`/api/project-version/list/${projectId.value}`);
    versions.value = versionsResponse.data.data;
    
    // 设置初始版本
    baseVersionId.value = initialVersionId.value;
    
  } catch (err: any) {
    error.value = err.message || '加载版本列表失败';
  } finally {
    loading.value = false;
  }
}

// 版本选择变化
function onVersionChange() {
  compareResult.value = null;
}

// 切换版本
function switchVersions() {
  const temp = baseVersionId.value;
  baseVersionId.value = targetVersionId.value;
  targetVersionId.value = temp;
  onVersionChange();
}

// 获取版本名称
function getVersionName(versionId: number | null): string {
  if (!versionId) return '';
  const version = versions.value.find(v => v.id === versionId);
  return version?.versionName || '';
}

// 执行对比
async function performCompare() {
  if (!baseVersionId.value || !targetVersionId.value) return;
  
  try {
    compareLoading.value = true;
    const response = await request.get(`/api/project-version/compare/${baseVersionId.value}/${targetVersionId.value}`);
    compareResult.value = response.data.data;
    showToast('对比完成');
  } catch (err: any) {
    showToast(err.message || '对比失败', 'error');
  } finally {
    compareLoading.value = false;
  }
}

// 导出差异SQL
async function exportDiffSql() {
  if (!baseVersionId.value || !targetVersionId.value || !compareResult.value) return;
  
  try {
    exportLoading.value = true;
    const response = await request.get(`/api/project-version/export-diff-sql/${baseVersionId.value}/${targetVersionId.value}`);
    
    // 创建下载链接
    const blob = new Blob([response.data.data.sql], { type: 'text/plain' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `diff_${getVersionName(baseVersionId.value)}_to_${getVersionName(targetVersionId.value)}.sql`;
    a.click();
    window.URL.revokeObjectURL(url);
    
    showToast('差异SQL导出成功');
  } catch (err: any) {
    showToast(err.message || '导出失败', 'error');
  } finally {
    exportLoading.value = false;
  }
}

// 显示Toast消息
function showToast(message: string, type: 'success' | 'error' = 'success') {
  toastMessage.value = message;
  toastType.value = type;
  setTimeout(() => {
    toastMessage.value = '';
  }, 3000);
}
</script>

<style scoped>
.version-compare-page {
  width: 100%;
  min-height: 0;
  flex: 1;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  padding: 20px;
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

/* 版本选择区域 */
.version-selector {
  margin-bottom: 24px;
}

.selector-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px 0 rgba(0,0,0,0.03);
}

.selector-card h3 {
  margin: 0 0 20px 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.version-select-row {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
}

.version-select-item {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
}

.version-select-item label {
  font-weight: 500;
  color: #666;
  white-space: nowrap;
}

.version-select-item select {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  font-size: 14px;
  background: #fff;
  transition: border-color 0.2s;
}

.version-select-item select:focus {
  outline: none;
  border-color: #409eff;
}

.switch-btn-container {
  display: flex;
  justify-content: center;
}

.switch-btn {
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 8px 16px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
  white-space: nowrap;
}

.switch-btn:hover:not(:disabled) {
  background: #337ecc;
}

.switch-btn:disabled {
  background: #c0c4cc;
  cursor: not-allowed;
}

.action-buttons {
  display: flex;
  gap: 12px;
}

.compare-btn, .export-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.compare-btn {
  background: #67c23a;
  color: #fff;
}

.compare-btn:hover:not(:disabled) {
  background: #5daf34;
}

.compare-btn:disabled {
  background: #c0c4cc;
  cursor: not-allowed;
}

.export-btn {
  background: #e6a23c;
  color: #fff;
}

.export-btn:hover:not(:disabled) {
  background: #cf9236;
}

.export-btn:disabled {
  background: #c0c4cc;
  cursor: not-allowed;
}

/* 对比结果区域 */
.compare-result {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px 0 rgba(0,0,0,0.03);
}

.result-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.result-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.version-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.version-tag {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.version-tag.base {
  background: #e1f3d8;
  color: #67c23a;
}

.version-tag.target {
  background: #fdf6ec;
  color: #e6a23c;
}

.arrow {
  color: #909399;
  font-weight: bold;
}

/* 统计信息 */
.compare-stats {
  display: flex;
  gap: 24px;
  margin-bottom: 24px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 6px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.stat-value {
  font-size: 16px;
  font-weight: 600;
}

.stat-item.added .stat-value {
  color: #67c23a;
}

.stat-item.removed .stat-value {
  color: #f56c6c;
}

.stat-item.modified .stat-value {
  color: #e6a23c;
}

/* 详细差异 */
.diff-details {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.diff-section {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  overflow: hidden;
}

.section-title {
  margin: 0;
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 600;
  border-bottom: 1px solid #ebeef5;
}

.section-title.added {
  background: #f0f9ff;
  color: #67c23a;
}

.section-title.removed {
  background: #fef0f0;
  color: #f56c6c;
}

.section-title.modified {
  background: #fdf6ec;
  color: #e6a23c;
}

.table-list {
  display: flex;
  flex-direction: column;
}

.table-item {
  padding: 16px;
  border-bottom: 1px solid #f5f7fa;
}

.table-item:last-child {
  border-bottom: none;
}

.table-item.added {
  background: #f0f9ff;
}

.table-item.removed {
  background: #fef0f0;
}

.table-item.modified {
  background: #fdf6ec;
}

.table-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.table-name {
  font-weight: 600;
  color: #333;
}

.table-comment {
  color: #666;
  font-size: 12px;
}

.table-changes {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-left: 16px;
}

.change-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.change-label {
  font-weight: 500;
  padding: 2px 6px;
  border-radius: 3px;
}

.change-label.added {
  background: #e1f3d8;
  color: #67c23a;
}

.change-label.removed {
  background: #fde2e2;
  color: #f56c6c;
}

.change-label.modified {
  background: #faecd8;
  color: #e6a23c;
}

.change-value {
  color: #666;
}

/* Toast消息 */
.toast {
  position: fixed;
  top: 20px;
  right: 20px;
  padding: 12px 20px;
  border-radius: 6px;
  color: #fff;
  font-size: 14px;
  z-index: 1000;
  animation: slideIn 0.3s ease;
}

.toast.success {
  background: #67c23a;
}

.toast.error {
  background: #f56c6c;
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}
</style>