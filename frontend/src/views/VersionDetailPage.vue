<template>
  <div class="version-detail-page">
    <!-- 导航路径 -->
    <div class="breadcrumb">
      <router-link to="/dashboard/project" class="breadcrumb-link">项目管理</router-link>
      <span class="breadcrumb-separator">></span>
      <router-link :to="`/dashboard/project/${projectId}`" class="breadcrumb-link">{{ projectName }}</router-link>
      <span class="breadcrumb-separator">></span>
      <span class="breadcrumb-current">版本详情 - {{ version?.versionName }}</span>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading">
      <p>加载中...</p>
    </div>

    <!-- 错误提示 -->
    <div v-if="error" class="error-message">
      <p>{{ error }}</p>
      <button @click="loadVersionDetail">重试</button>
    </div>

    <!-- 版本内容 -->
    <div v-if="!loading && !error">
      <!-- 版本基本信息 -->
      <div class="version-info-card">
        <div class="card-header">
          <h3>版本信息</h3>
        </div>
        <div class="version-info">
          <div class="info-item">
            <label>版本号：</label>
            <span>{{ version?.versionName }}</span>
          </div>
          <div class="info-item">
            <label>描述：</label>
            <span>{{ version?.description || '暂无描述' }}</span>
          </div>
          <div class="info-item">
            <label>创建时间：</label>
            <span>{{ formatDate(version?.createTime) }}</span>
          </div>
        </div>
      </div>

      <!-- 数据库结构信息 -->
      <div class="database-structure-card">
        <div class="card-header">
          <h3>数据库结构</h3>
          <button class="capture-btn" @click="captureSchema" :disabled="captureLoading">
            {{ captureLoading ? '捕获中...' : '重新捕获结构' }}
          </button>
        </div>
        
        <!-- 数据库基本信息 -->
        <div v-if="databaseSchema" class="database-info">
          <h4>数据库信息</h4>
          <div class="info-grid">
            <div class="info-item">
              <label>数据库名：</label>
              <span>{{ databaseSchema.databaseName }}</span>
            </div>
            <div class="info-item">
              <label>字符集：</label>
              <span>{{ databaseSchema.charset }}</span>
            </div>
            <div class="info-item">
              <label>排序规则：</label>
              <span>{{ databaseSchema.collation }}</span>
            </div>
            <div class="info-item">
              <label>快照时间：</label>
              <span>{{ formatDate(databaseSchema.snapshotTime) }}</span>
            </div>
          </div>
          
          <!-- PostgreSQL Schema信息 -->
          <div v-if="schemasInfo && schemasInfo.length > 0" class="schemas-section">
            <h4>Schema信息 ({{ schemasInfo.length }}个Schema)</h4>
            <div class="schemas-container">
              <div v-for="schema in schemasInfo" :key="schema.schema_name" class="schema-card">
                <div class="schema-info">
                  <span class="schema-name">{{ schema.schema_name }}</span>
                  <span class="schema-owner" v-if="schema.schema_owner">所有者: {{ schema.schema_owner }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 表结构信息 -->
        <div v-if="tables.length > 0" class="tables-section">
          <h4>表结构 ({{ tables.length }}个表)</h4>
          <div class="tables-container">
            <div v-for="table in tables" :key="table.id" class="table-card">
              <div class="table-header" @click="toggleTable(table.id)">
                <h5>
                  <span v-if="table.schemaName && table.schemaName !== 'public'" class="schema-prefix">{{ table.schemaName }}.</span>{{ table.tableName }}
                </h5>
                <span class="table-comment" v-if="table.tableComment">{{ table.tableComment }}</span>
                <span class="toggle-icon" :class="{ 'expanded': expandedTables.includes(table.id) }">▼</span>
              </div>
              
              <div v-if="expandedTables.includes(table.id)" class="table-content">
                <!-- 表基本信息 -->
                <div class="table-info">
                  <div class="info-row" v-if="table.schemaName">
                    <span class="label">Schema：</span>
                    <span>{{ table.schemaName }}</span>
                  </div>
                  <div class="info-row">
                    <span class="label">表类型：</span>
                    <span>{{ table.tableType }}</span>
                  </div>
                  <div class="info-row">
                    <span class="label">存储引擎：</span>
                    <span>{{ table.engine }}</span>
                  </div>
                  <div class="info-row">
                    <span class="label">字符集：</span>
                    <span>{{ table.charset }}</span>
                  </div>
                  <div class="info-row">
                    <span class="label">排序规则：</span>
                    <span>{{ table.collation }}</span>
                  </div>
                </div>

                <!-- 字段信息 -->
                <div class="columns-section">
                  <h6>字段信息 ({{ table.columns?.length || 0 }} 个字段)</h6>
                  <table class="columns-table">
                    <thead>
                      <tr>
                        <th>字段名</th>
                        <th>数据类型</th>
                        <th>长度</th>
                        <th>允许空值</th>
                        <th>键类型</th>
                        <th>默认值</th>
                        <th>额外信息</th>
                        <th>注释</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="column in table.columns" :key="column.id">
                        <td class="column-name">{{ column.columnName }}</td>
                        <td>{{ column.columnType }}</td>
                        <td>{{ column.characterMaximumLength || '-' }}</td>
                        <td>
                          <span class="nullable" :class="column.isNullable === 'YES' ? 'yes' : 'no'">
                            {{ column.isNullable === 'YES' ? '是' : '否' }}
                          </span>
                        </td>
                        <td>
                          <span v-if="column.columnKey" class="key-type" :class="column.columnKey.toLowerCase()">
                            {{ getKeyTypeText(column.columnKey) }}
                          </span>
                        </td>
                        <td>{{ column.columnDefault || '-' }}</td>
                        <td>{{ column.extra || '-' }}</td>
                        <td>{{ column.columnComment || '-' }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>

                <!-- 索引信息 -->
                <div v-if="table.indexes && table.indexes.length > 0" class="indexes-section">
                  <h6>索引信息 ({{ table.indexes?.length || 0 }} 个索引)</h6>
                  <table class="indexes-table">
                    <thead>
                      <tr>
                        <th>索引名</th>
                        <th>索引类型</th>
                        <th>是否唯一</th>
                        <th>是否主键</th>
                        <th>关联列</th>
                        <th>注释</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="index in table.indexes" :key="index.id">
                        <td class="index-name">{{ index.indexName }}</td>
                        <td>{{ index.indexType }}</td>
                        <td>
                          <span class="unique" :class="index.isUnique ? 'yes' : 'no'">
                            {{ index.isUnique ? '是' : '否' }}
                          </span>
                        </td>
                        <td>
                          <span class="primary" :class="index.isPrimary ? 'yes' : 'no'">
                            {{ index.isPrimary ? '是' : '否' }}
                          </span>
                        </td>
                        <td>{{ index.columnNames }}</td>
                        <td>{{ index.indexComment || '-' }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 无数据提示 -->
        <div v-if="!databaseSchema && tables.length === 0" class="no-data">
          <p>该版本暂无数据库结构信息</p>
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
import { useRoute } from 'vue-router';
import request from '../utils/request';
import type { ProjectVersion } from '../types/api';

interface DatabaseSchema {
  id: number;
  projectVersionId: number;
  databaseName: string;
  charset: string;
  collation: string;
  snapshotTime: string;
  schemasInfo?: string;
}

interface TableColumn {
  id: number;
  versionTableId: number;
  columnName: string;
  ordinalPosition: number;
  columnDefault: string;
  isNullable: string;
  dataType: string;
  characterMaximumLength: number;
  columnType: string;
  columnKey: string;
  extra: string;
  columnComment: string;
}

interface TableIndex {
  id: number;
  versionTableId: number;
  indexName: string;
  indexType: string;
  isUnique: boolean;
  isPrimary: boolean;
  columnNames: string;
  indexComment: string;
}

interface TableStructure {
  id: number;
  projectVersionId: number;
  tableName: string;
  schemaName?: string;
  tableComment: string;
  tableType: string;
  engine: string;
  charset: string;
  collation: string;
  columns: TableColumn[];
  indexes: TableIndex[];
}

const route = useRoute();

// 基础数据
const version = ref<ProjectVersion | null>(null);
const databaseSchema = ref<DatabaseSchema | null>(null);
const tables = ref<TableStructure[]>([]);
const projectId = ref<number>(0);
const projectName = ref<string>('');
const expandedTables = ref<number[]>([]);
const schemasInfo = ref<any[]>([]);

// 加载状态
const loading = ref(false);
const error = ref('');
const captureLoading = ref(false);

// Toast消息
const toastMessage = ref('');
const toastType = ref<'success' | 'error'>('success');

// 格式化字节数
function formatBytes(bytes: number | null | undefined): string {
  if (!bytes || bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

// 页面加载
onMounted(() => {
  loadVersionDetail();
});

// 加载版本详情
async function loadVersionDetail() {
  try {
    loading.value = true;
    error.value = '';
    
    const versionId = parseInt(route.params.versionId as string);
    projectId.value = parseInt(route.params.projectId as string);
    
    // 获取版本基本信息
    const versionResponse = await request.get(`/api/project-version/detail/${versionId}`);
    version.value = versionResponse.data.data;
    
    // 获取项目名称
    const projectResponse = await request.get(`/api/project/detail/${projectId.value}`);
    projectName.value = projectResponse.data.data.name;
    
    // 获取版本完整结构信息
    const structureResponse = await request.get(`/api/project-version/structure/${versionId}`);
    const structureData = structureResponse.data.data;
    
    databaseSchema.value = structureData.database;
    tables.value = structureData.tables || [];
    
    // 解析PostgreSQL的schema信息
    if (structureData.database?.schemasInfo) {
      try {
        schemasInfo.value = JSON.parse(structureData.database.schemasInfo);
      } catch (e) {
        console.warn('解析schema信息失败:', e);
        schemasInfo.value = [];
      }
    } else {
      schemasInfo.value = [];
    }
    
  } catch (err: any) {
    error.value = err.message || '加载版本详情失败';
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

// 切换表展开状态
function toggleTable(tableId: number) {
  const index = expandedTables.value.indexOf(tableId);
  if (index > -1) {
    expandedTables.value.splice(index, 1);
  } else {
    expandedTables.value.push(tableId);
  }
}

// 获取键类型文本
function getKeyTypeText(keyType: string): string {
  switch (keyType) {
    case 'PRI': return '主键';
    case 'UNI': return '唯一';
    case 'MUL': return '索引';
    default: return keyType;
  }
}

// 手动捕获数据库结构
async function captureSchema() {
  try {
    captureLoading.value = true;
    const versionId = parseInt(route.params.versionId as string);
    
    await request.post(`/api/project-version/capture-schema/${versionId}`);
    
    showToast('数据库结构捕获成功', 'success');
    
    // 重新加载版本详情
    await loadVersionDetail();
    
  } catch (err: any) {
    showToast(err.response?.data?.msg || '捕获数据库结构失败', 'error');
  } finally {
    captureLoading.value = false;
  }
}
</script>

<style scoped>
.version-detail-page {
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

/* 版本信息卡片 */
.version-info-card {
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

.capture-btn {
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 8px 16px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.capture-btn:hover:not(:disabled) {
  background: #337ecc;
}

.capture-btn:disabled {
  background: #c0c4cc;
  cursor: not-allowed;
}

.version-info {
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

/* 数据库结构卡片 */
.database-structure-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px 0 rgba(0,0,0,0.03);
}

.database-info {
  margin-bottom: 30px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 6px;
}

.database-info h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
  color: #333;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 12px;
}

/* 表结构部分 */
.tables-section {
  margin-top: 30px;
}

.tables-section h4 {
  margin: 0 0 20px 0;
  font-size: 16px;
  color: #333;
}

.tables-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.table-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.table-header {
  background: #f5f7fa;
  padding: 16px 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 12px;
  transition: background 0.2s;
}

.table-header:hover {
  background: #ecf5ff;
}

.table-header h5 {
  margin: 0;
  font-size: 16px;
  color: #333;
  font-weight: 600;
}

.table-comment {
  color: #666;
  font-size: 14px;
  flex: 1;
}

.toggle-icon {
  color: #909399;
  font-size: 12px;
  transition: transform 0.2s;
}

.toggle-icon.expanded {
  transform: rotate(180deg);
}

.table-content {
  padding: 20px;
}

.table-info {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
  margin-bottom: 24px;
  padding: 16px;
  background: #fafbfc;
  border-radius: 6px;
}

.info-row {
  display: flex;
  align-items: center;
}

.info-row .label {
  width: 80px;
  color: #666;
  font-size: 14px;
}

.info-row span:last-child {
  color: #333;
  font-size: 14px;
}

/* 字段表格 */
.columns-section, .indexes-section {
  margin-top: 24px;
}

.columns-section h6, .indexes-section h6 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #333;
  font-weight: 600;
}

.columns-table, .indexes-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.columns-table th, .indexes-table th {
  background: #f5f7fa;
  padding: 10px 8px;
  text-align: left;
  border: 1px solid #e4e7ed;
  color: #666;
  font-weight: 500;
}

.columns-table td, .indexes-table td {
  padding: 8px;
  border: 1px solid #e4e7ed;
  color: #333;
}

.column-name, .index-name {
  font-weight: 500;
  color: #409eff;
}

.nullable, .unique, .primary {
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12px;
  font-weight: 500;
}

.nullable.yes, .unique.yes, .primary.yes {
  background: #f0f9ff;
  color: #409eff;
}

.nullable.no, .unique.no, .primary.no {
  background: #fef0f0;
  color: #f56c6c;
}

.key-type {
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12px;
  font-weight: 500;
}

.key-type.pri {
  background: #fff2e8;
  color: #e6a23c;
}

.key-type.uni {
  background: #f0f9ff;
  color: #409eff;
}

.key-type.mul {
  background: #f5f7fa;
  color: #909399;
}

/* Schema信息样式 */
.schemas-section {
  margin-top: 20px;
}

.schemas-section h4 {
  margin: 0 0 12px 0;
  font-size: 16px;
  color: #333;
  font-weight: 600;
}

.schemas-container {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.schema-card {
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  padding: 12px 16px;
  min-width: 200px;
}

.schema-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.schema-name {
  font-weight: 600;
  color: #409eff;
  font-size: 14px;
}

.schema-owner {
  font-size: 12px;
  color: #666;
}

.schema-prefix {
  color: #909399;
  font-weight: normal;
}

/* 无数据提示 */
.no-data {
  text-align: center;
  padding: 40px;
  color: #909399;
}

.no-data p {
  margin: 0;
  font-size: 16px;
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