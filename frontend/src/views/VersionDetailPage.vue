<template>
  <div class="version-detail-page">
    <!-- 导航路径 -->
    <div class="breadcrumb">
      <router-link to="/dashboard/project" class="breadcrumb-link">{{ t('project.title') }}</router-link>
      <span class="breadcrumb-separator">></span>
      <router-link :to="`/dashboard/project/${projectId}`" class="breadcrumb-link">{{ projectName }}</router-link>
      <span class="breadcrumb-separator">></span>
      <span class="breadcrumb-current">{{ t('version.detail') }} - {{ version?.versionName }}</span>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading">
      <p>{{ t('common.loading') }}</p>
    </div>

    <!-- 错误提示 -->
    <div v-if="error" class="error-message">
      <p>{{ error }}</p>
      <button @click="loadVersionDetail">{{ t('common.retry') }}</button>
    </div>

    <!-- 版本内容 -->
    <div v-if="!loading && !error">
      <!-- 版本基本信息 -->
      <div class="version-info-card">
        <div class="card-header">
          <h3>{{ t('version.info') }}</h3>
        </div>
        <div class="version-info">
          <div class="info-item">
            <label>{{ t('version.versionNumber') }}：</label>
            <span>{{ version?.versionName }}</span>
          </div>
          <div class="info-item">
            <label>{{ t('version.description') }}：</label>
            <span>{{ version?.description || t('version.noDescription') }}</span>
          </div>
          <div class="info-item">
            <label>{{ t('version.createTime') }}：</label>
            <span>{{ formatDate(version?.createTime) }}</span>
          </div>
        </div>
      </div>

      <!-- 数据库结构信息 -->
      <div class="database-structure-card">
        <div class="card-header">
          <h3>{{ t('database.structure') }}</h3>
        </div>
        
        <!-- 数据库基本信息 -->
        <div v-if="databaseSchema" class="database-info">
          <h4>{{ t('database.info') }}</h4>
          <div class="info-grid">
            <div class="info-item">
              <label>{{ t('database.name') }}：</label>
              <span>{{ databaseSchema.databaseName }}</span>
            </div>
            <div class="info-item">
              <label>{{ t('database.charset') }}：</label>
              <span>{{ databaseSchema.charset }}</span>
            </div>
            <div class="info-item">
              <label>{{ t('database.collation') }}：</label>
              <span>{{ databaseSchema.collation }}</span>
            </div>
            <div class="info-item">
              <label>{{ t('database.snapshotTime') }}：</label>
              <span>{{ formatDate(databaseSchema.snapshotTime) }}</span>
            </div>
          </div>
          
          <!-- PostgreSQL/KingbaseES Schema信息 -->
          <div v-if="schemasInfo && schemasInfo.length > 0" class="schemas-section">
            <h4>{{ t('database.schemaInfo') }} ({{ schemasInfo.length }}{{ t('database.schemaCount') }})</h4>
            <div class="schemas-container">
              <div v-for="schema in schemasInfo" :key="schema.schema_name" class="schema-card">
                <div class="schema-info">
                  <span class="schema-name">{{ schema.schema_name }}</span>
                  <span class="schema-owner" v-if="schema.schema_owner">{{ t('database.owner') }}: {{ schema.schema_owner }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 表结构信息 -->
        <div v-if="tables.length > 0" class="tables-section">
          <h4>{{ t('database.tableStructure') }} ({{ tables.length }}{{ t('database.tableCount') }})</h4>

          <!-- PostgreSQL/KingbaseES 分层显示 -->
          <div v-if="isSchemaBasedDatabase && schemaGroups" class="schemas-tables-container">
            <div v-for="(schemaTables, schemaName) in schemaGroups" :key="schemaName" class="schema-group">
              <div class="schema-header" @click="toggleSchema(String(schemaName))">
                <h5>
                  <span class="toggle-icon" :class="{ 'expanded': isSchemaExpanded(String(schemaName)) }">▼</span>
                  <span class="schema-icon">📁</span>
                  Schema: {{ schemaName }} ({{ schemaTables.length }}{{ t('database.tableCount') }})
                </h5>
              </div>

              <div v-if="isSchemaExpanded(String(schemaName))" class="schema-tables">
                <div v-for="table in schemaTables" :key="table.id" class="table-card">
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
                    <span class="label">{{ t('table.type') }}：</span>
                    <span>{{ table.tableType }}</span>
                  </div>
                  <div class="info-row">
                    <span class="label">{{ t('table.engine') }}：</span>
                    <span>{{ table.engine }}</span>
                  </div>
                  <div class="info-row">
                    <span class="label">{{ t('table.charset') }}：</span>
                    <span>{{ table.charset }}</span>
                  </div>
                  <div class="info-row">
                    <span class="label">{{ t('table.collation') }}：</span>
                    <span>{{ table.collation }}</span>
                  </div>
                </div>

                <!-- 字段信息 -->
                <div class="columns-section">
                  <h6>{{ t('table.columnInfo') }} ({{ table.columns?.length || 0 }} {{ t('table.columnCount') }})</h6>
                  <table class="columns-table">
                    <thead>
                      <tr>
                        <th>{{ t('table.columnName') }}</th>
                        <th>{{ t('table.dataType') }}</th>
                        <th>{{ t('table.length') }}</th>
                        <th>{{ t('table.nullable') }}</th>
                        <th>{{ t('table.keyType') }}</th>
                        <th>{{ t('table.defaultValue') }}</th>
                        <th>{{ t('table.extra') }}</th>
                        <th>{{ t('table.comment') }}</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="column in table.columns" :key="column.id">
                        <td class="column-name">{{ column.columnName }}</td>
                        <td>{{ column.columnType }}</td>
                        <td>{{ column.characterMaximumLength || '-' }}</td>
                        <td>
                          <span class="nullable" :class="column.isNullable === 'YES' ? 'yes' : 'no'">
                            {{ column.isNullable === 'YES' ? t('common.yes') : t('common.no') }}
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
                  <h6>{{ t('table.indexInfo') }} ({{ table.indexes?.length || 0 }} {{ t('table.indexCount') }})</h6>
                  <table class="indexes-table">
                    <thead>
                      <tr>
                        <th>{{ t('table.indexName') }}</th>
                        <th>{{ t('table.indexType') }}</th>
                        <th>{{ t('table.isUnique') }}</th>
                        <th>{{ t('table.isPrimary') }}</th>
                        <th>{{ t('table.relatedColumns') }}</th>
                        <th>{{ t('table.comment') }}</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="index in table.indexes" :key="index.id">
                        <td class="index-name">{{ index.indexName }}</td>
                        <td>{{ index.indexType }}</td>
                        <td>
                          <span class="unique" :class="index.isUnique ? 'yes' : 'no'">
                            {{ index.isUnique ? t('common.yes') : t('common.no') }}
                          </span>
                        </td>
                        <td>
                          <span class="primary" :class="index.isPrimary ? 'yes' : 'no'">
                            {{ index.isPrimary ? t('common.yes') : t('common.no') }}
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
          </div>

          <!-- 非Schema数据库 平铺显示 -->
          <div v-else class="tables-container">
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
                    <span class="label">{{ t('table.type') }}：</span>
                    <span>{{ table.tableType }}</span>
                  </div>
                  <div class="info-row">
                    <span class="label">{{ t('table.engine') }}：</span>
                    <span>{{ table.engine }}</span>
                  </div>
                  <div class="info-row">
                    <span class="label">{{ t('table.charset') }}：</span>
                    <span>{{ table.charset }}</span>
                  </div>
                  <div class="info-row">
                    <span class="label">{{ t('table.collation') }}：</span>
                    <span>{{ table.collation }}</span>
                  </div>
                </div>

                <!-- 字段信息 -->
                <div class="columns-section">
                  <h6>{{ t('table.columnInfo') }} ({{ table.columns?.length || 0 }} {{ t('table.columnCount') }})</h6>
                  <table class="columns-table">
                    <thead>
                      <tr>
                        <th>{{ t('table.columnName') }}</th>
                        <th>{{ t('table.dataType') }}</th>
                        <th>{{ t('table.length') }}</th>
                        <th>{{ t('table.nullable') }}</th>
                        <th>{{ t('table.keyType') }}</th>
                        <th>{{ t('table.defaultValue') }}</th>
                        <th>{{ t('table.extra') }}</th>
                        <th>{{ t('table.comment') }}</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="column in table.columns" :key="column.id">
                        <td class="column-name">{{ column.columnName }}</td>
                        <td>{{ column.columnType }}</td>
                        <td>{{ column.characterMaximumLength || '-' }}</td>
                        <td>
                          <span class="nullable" :class="column.isNullable === 'YES' ? 'yes' : 'no'">
                            {{ column.isNullable === 'YES' ? t('common.yes') : t('common.no') }}
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
                <div class="indexes-section">
                  <h6>{{ t('table.indexInfo') }} ({{ table.indexes?.length || 0 }} {{ t('table.indexCount') }})</h6>
                  <table class="indexes-table">
                    <thead>
                      <tr>
                        <th>{{ t('table.indexName') }}</th>
                        <th>{{ t('table.indexType') }}</th>
                        <th>{{ t('table.uniqueness') }}</th>
                        <th>{{ t('table.primaryKey') }}</th>
                        <th>{{ t('table.fields') }}</th>
                        <th>{{ t('table.comment') }}</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="index in table.indexes" :key="index.id">
                        <td class="index-name">{{ index.indexName }}</td>
                        <td>{{ index.indexType }}</td>
                        <td>
                          <span class="unique" :class="index.isUnique ? 'yes' : 'no'">
                            {{ index.isUnique ? t('common.yes') : t('common.no') }}
                          </span>
                        </td>
                        <td>
                          <span class="primary" :class="index.isPrimary ? 'yes' : 'no'">
                            {{ index.isPrimary ? t('common.yes') : t('common.no') }}
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
          <p>{{ t('version.noStructureInfo') }}</p>
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
import { ref, onMounted, computed } from 'vue';
import { useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import request from '../utils/request';
import type { ProjectVersion } from '../types/api';

const { t } = useI18n();

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
const datasourceType = ref<string | null>(null);
const expandedSchemas = ref<string[]>([]);

// 加载状态
const loading = ref(false);
const error = ref('');

// Toast消息
const toastMessage = ref('');
const toastType = ref<'success' | 'error'>('success');

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
    datasourceType.value = structureData.datasourceType || null;

    // 解析基于Schema数据库的schema信息（PostgreSQL/KingbaseES）
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

    // 如果是基于Schema的数据库（PostgreSQL/KingbaseES），初始化展开的schema（默认展开所有schema）
    if (datasourceType.value === 'postgresql' || datasourceType.value === 'kingbase') {
      const schemaNames = new Set<string>();
      tables.value.forEach(table => {
        if (table.schemaName) {
          schemaNames.add(table.schemaName);
        }
      });
      expandedSchemas.value = Array.from(schemaNames);
    }
    
  } catch (err: any) {
    error.value = err.message || '加载版本详情失败';
  } finally {
    loading.value = false;
  }
}

// 计算属性：按schema分组的表数据
const schemaGroups = computed(() => {
  if (datasourceType.value !== 'postgresql' && datasourceType.value !== 'kingbase') {
    return null; // 非基于Schema的数据源不使用分组
  }

  const groups: { [schemaName: string]: TableStructure[] } = {};

  tables.value.forEach(table => {
    const schemaName = table.schemaName || 'public';
    if (!groups[schemaName]) {
      groups[schemaName] = [];
    }
    groups[schemaName].push(table);
  });

  return groups;
});

// 计算属性：是否为基于Schema的数据库
const isSchemaBasedDatabase = computed(() => {
  return datasourceType.value === 'postgresql' || datasourceType.value === 'kingbase';
});

// 切换schema展开状态
function toggleSchema(schemaName: string) {
  const index = expandedSchemas.value.indexOf(schemaName);
  if (index > -1) {
    expandedSchemas.value.splice(index, 1);
  } else {
    expandedSchemas.value.push(schemaName);
  }
}

// 判断schema是否展开
function isSchemaExpanded(schemaName: string): boolean {
  return expandedSchemas.value.includes(schemaName);
}

// 格式化日期
function formatDate(dateString?: string): string {
  if (!dateString) return '';
  return new Date(dateString).toLocaleString('zh-CN');
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
    case 'PRI': return t('table.primaryKeyShort');
    case 'UNI': return t('table.uniqueShort');
    case 'MUL': return t('table.indexShort');
    default: return keyType;
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

/* Schema分组样式 */
.schemas-tables-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.schema-group {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.schema-header {
  background: #f0f2f5;
  padding: 16px 20px;
  cursor: pointer;
  border-bottom: 1px solid #e4e7ed;
  transition: background 0.2s;
}

.schema-header:hover {
  background: #e6e8eb;
}

.schema-header h5 {
  margin: 0;
  font-size: 16px;
  color: #333;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.schema-header .toggle-icon {
  color: #606266;
  font-size: 14px;
}

.schema-icon {
  font-size: 16px;
  margin-right: 4px;
}

.schema-tables {
  padding: 16px;
  background: #fafbfc;
}

.schema-tables .table-card {
  margin-bottom: 16px;
}

.schema-tables .table-card:last-child {
  margin-bottom: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .schema-header h5 {
    font-size: 14px;
  }

  .schema-tables {
    padding: 12px;
  }

  .schemas-tables-container {
    gap: 16px;
  }
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