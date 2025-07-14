<template>
  <div class="sql-console-page">
    <!-- 控制台头部 -->
    <div class="console-header">
      <h3>SQL控制台</h3>
      <div class="console-controls">
        <div class="control-group">
          <label>项目：</label>
          <select v-model="selectedProject" @change="onProjectChange">
            <option value="">请选择项目</option>
            <option v-for="project in projects" :key="project.id" :value="project.id">
              {{ project.name }}
            </option>
          </select>
        </div>
        <div class="control-group">
          <label>版本：</label>
          <select v-model="selectedVersion" :disabled="!selectedProject">
            <option value="">请选择版本</option>
            <option v-for="version in versions" :key="version.id" :value="version.id">
              {{ version.version }}
            </option>
          </select>
        </div>
      </div>
    </div>

    <!-- SQL编辑器区域 -->
    <div class="sql-editor-section">
      <div class="editor-header">
        <span>SQL编辑器</span>
        <div class="editor-actions">
          <button class="action-btn" @click="formatSql" :disabled="!sqlContent.trim()">格式化</button>
          <button class="action-btn" @click="clearSql">清空</button>
          <button class="execute-btn" @click="executeSql" :disabled="!canExecute" :class="{executing: isExecuting}">
            {{ isExecuting ? '执行中...' : '执行 (Ctrl+Enter)' }}
          </button>
        </div>
      </div>
      <div class="sql-editor">
        <textarea 
          v-model="sqlContent" 
          placeholder="请输入SQL语句..."
          @keydown="handleKeydown"
          ref="sqlTextarea"
        ></textarea>
      </div>
    </div>

    <!-- 执行结果区域 -->
    <div class="result-section">
      <div class="result-header">
        <span>执行结果</span>
        <div class="result-info" v-if="lastExecution">
          <span class="status" :class="lastExecution.status">{{ getStatusText(lastExecution.status) }}</span>
          <span class="time">执行时间: {{ lastExecution.duration }}ms</span>
          <span class="timestamp">{{ formatTime(lastExecution.timestamp) }}</span>
        </div>
      </div>
      <div class="result-content">
        <div v-if="!lastExecution" class="no-result">
          暂无执行结果
        </div>
        <div v-else-if="lastExecution.status === 'error'" class="error-result">
          <pre>{{ lastExecution.error }}</pre>
        </div>
        <div v-else-if="lastExecution.status === 'success'" class="success-result">
          <div class="result-meta">
            <span>影响行数: {{ lastExecution.affectedRows }}</span>
            <span>返回行数: {{ lastExecution.data?.length || 0 }}</span>
          </div>
          <div class="result-table" v-if="lastExecution.data && lastExecution.data.length > 0">
            <table>
              <thead>
                <tr>
                  <th v-for="column in lastExecution.columns" :key="column">{{ column }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, index) in lastExecution.data" :key="index">
                  <td v-for="column in lastExecution.columns" :key="column">
                    {{ row[column] }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="no-data">
            SQL执行成功，无返回数据
          </div>
        </div>
      </div>
    </div>

    <!-- 历史记录区域 -->
    <div class="history-section">
      <div class="history-header">
        <span>执行历史</span>
        <div class="history-actions">
          <button class="action-btn" @click="clearHistory" :disabled="!executionHistory.length">清空历史</button>
          <button class="action-btn" @click="exportHistory" :disabled="!executionHistory.length">导出历史</button>
        </div>
      </div>
      <div class="history-list">
        <div v-if="!executionHistory.length" class="no-history">
          暂无执行历史
        </div>
        <div v-else>
          <div 
            v-for="(record, index) in executionHistory" 
            :key="index" 
            class="history-item"
            :class="record.status"
            @click="loadHistoryRecord(record)"
          >
            <div class="history-sql">
              <pre>{{ record.sql }}</pre>
            </div>
            <div class="history-meta">
              <span class="status">{{ getStatusText(record.status) }}</span>
              <span class="duration">{{ record.duration }}ms</span>
              <span class="time">{{ formatTime(record.timestamp) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';

interface Project {
  id: number;
  name: string;
  desc: string;
}

interface DataSource {
  id: number;
  name: string;
  type: string;
  host: string;
  port: string;
}

interface Version {
  id: number;
  version: string;
  desc: string;
}

interface ExecutionResult {
  status: string;
  sql: string;
  duration: number;
  timestamp: Date;
  affectedRows?: number;
  columns?: string[];
  data?: any[];
  error?: string;
}

// 数据定义
const projects = ref<Project[]>([
  { id: 1, name: '用户中心', desc: '用户与权限管理' },
  { id: 2, name: '订单系统', desc: '订单与支付' },
]);

const dataSources = ref<DataSource[]>([
  { id: 1, name: '本地MySQL', type: 'MySQL', host: '127.0.0.1', port: '3306' },
  { id: 2, name: 'PostgreSQL测试', type: 'PostgreSQL', host: '192.168.1.10', port: '5432' },
]);

const versions = ref<Version[]>([]);
const selectedProject = ref<string>('');
const selectedVersion = ref<string>('');
const selectedDataSource = ref<string>('');
const sqlContent = ref<string>('');
const isExecuting = ref<boolean>(false);
const lastExecution = ref<ExecutionResult | null>(null);
const executionHistory = ref<ExecutionResult[]>([]);
const sqlTextarea = ref<HTMLTextAreaElement | null>(null);

// 计算属性
const canExecute = computed(() => {
  return sqlContent.value.trim() && 
         selectedProject.value && 
         selectedVersion.value && 
         selectedDataSource.value && 
         !isExecuting.value;
});

// 方法定义
function onProjectChange() {
  selectedVersion.value = '';
  versions.value = mockVersions(parseInt(selectedProject.value));
}

function mockVersions(projectId: number): Version[] {
  if (projectId === 1) {
    return [
      { id: 101, version: 'v1.0.0', desc: '初始版本' },
      { id: 102, version: 'v1.1.0', desc: '优化表结构' },
    ];
  } else if (projectId === 2) {
    return [
      { id: 201, version: 'v1.0.0', desc: '订单初版' },
    ];
  }
  return [];
}

function formatSql() {
  // 简单的SQL格式化
  let formatted = sqlContent.value
    .replace(/\s+/g, ' ')
    .replace(/\s*,\s*/g, ',\n  ')
    .replace(/\s*(SELECT|FROM|WHERE|JOIN|LEFT JOIN|RIGHT JOIN|INNER JOIN|ORDER BY|GROUP BY|HAVING|UNION)\s+/gi, '\n$1 ')
    .replace(/\s*(AND|OR)\s+/gi, '\n  $1 ')
    .trim();
  sqlContent.value = formatted;
}

function clearSql() {
  sqlContent.value = '';
}

function handleKeydown(event: KeyboardEvent) {
  if (event.ctrlKey && event.key === 'Enter') {
    event.preventDefault();
    if (canExecute.value) {
      executeSql();
    }
  }
}

async function executeSql() {
  if (!canExecute.value) return;
  
  isExecuting.value = true;
  const startTime = Date.now();
  const sql = sqlContent.value.trim();
  
  try {
    // 模拟SQL执行
    await new Promise(resolve => setTimeout(resolve, Math.random() * 1000 + 500));
    
    const duration = Date.now() - startTime;
    const isSelectQuery = sql.toLowerCase().startsWith('select');
    
    let result;
    if (Math.random() > 0.1) { // 90%成功率
      if (isSelectQuery) {
        // 模拟查询结果
        result = {
          status: 'success',
          sql,
          duration,
          timestamp: new Date(),
          affectedRows: 0,
          columns: ['id', 'name', 'email', 'created_at'],
          data: [
            { id: 1, name: '张三', email: 'zhangsan@example.com', created_at: '2024-01-01 10:00:00' },
            { id: 2, name: '李四', email: 'lisi@example.com', created_at: '2024-01-02 11:00:00' },
            { id: 3, name: '王五', email: 'wangwu@example.com', created_at: '2024-01-03 12:00:00' },
          ]
        };
      } else {
        // 模拟增删改结果
        result = {
          status: 'success',
          sql,
          duration,
          timestamp: new Date(),
          affectedRows: Math.floor(Math.random() * 5) + 1,
          columns: [],
          data: []
        };
      }
    } else {
      // 模拟错误
      result = {
        status: 'error',
        sql,
        duration,
        timestamp: new Date(),
        error: 'Table \'users\' doesn\'t exist (Error Code: 1146)'
      };
    }
    
    lastExecution.value = result;
    executionHistory.value.unshift(result);
    
    // 限制历史记录数量
    if (executionHistory.value.length > 50) {
      executionHistory.value = executionHistory.value.slice(0, 50);
    }
    
  } catch (error) {
    const duration = Date.now() - startTime;
    const result: ExecutionResult = {
      status: 'error',
      sql,
      duration,
      timestamp: new Date(),
      error: error instanceof Error ? error.message : '执行失败'
    };
    
    lastExecution.value = result;
    executionHistory.value.unshift(result);
  } finally {
    isExecuting.value = false;
  }
}

function getStatusText(status: string): string {
  switch (status) {
    case 'success': return '成功';
    case 'error': return '失败';
    default: return '未知';
  }
}

function formatTime(timestamp: Date): string {
  return new Date(timestamp).toLocaleString('zh-CN');
}

function loadHistoryRecord(record: ExecutionResult) {
  sqlContent.value = record.sql;
  lastExecution.value = record;
}

function clearHistory() {
  if (confirm('确定要清空所有执行历史吗？')) {
    executionHistory.value = [];
  }
}

function exportHistory() {
  const data = executionHistory.value.map(record => ({
    sql: record.sql,
    status: getStatusText(record.status),
    duration: record.duration + 'ms',
    timestamp: formatTime(record.timestamp),
    error: record.error || ''
  }));
  
  const csv = [
    ['SQL语句', '状态', '执行时间', '执行时间戳', '错误信息'],
    ...data.map(row => [row.sql, row.status, row.duration, row.timestamp, row.error])
  ].map(row => row.map(cell => `"${cell}"`).join(',')).join('\n');
  
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = `sql_history_${new Date().toISOString().slice(0, 10)}.csv`;
  link.click();
}

onMounted(() => {
  // 初始化时可以加载一些示例数据
});
</script>

<style scoped>
.sql-console-page {
  width: 100%;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
  box-sizing: border-box;
  overflow-y: auto;
}

/* 控制台头部 */
.console-header {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px 0 rgba(0,0,0,0.03);
}

.console-header h3 {
  margin: 0 0 16px 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.console-controls {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.control-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.control-group label {
  font-weight: 500;
  color: #666;
  white-space: nowrap;
}

.control-group select {
  min-width: 150px;
  padding: 6px 10px;
  border: 1px solid #e0e3e8;
  border-radius: 4px;
  background: #fff;
  font-size: 14px;
}

.control-group select:focus {
  border-color: #409eff;
  outline: none;
}

.control-group select:disabled {
  background: #f5f7fa;
  color: #999;
}

/* SQL编辑器 */
.sql-editor-section {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px 0 rgba(0,0,0,0.03);
  flex: 2;
  min-height: 300px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.editor-header span {
  font-weight: 600;
  color: #333;
}

.editor-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  padding: 6px 12px;
  border: 1px solid #e0e3e8;
  border-radius: 4px;
  background: #fff;
  color: #666;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn:hover:not(:disabled) {
  border-color: #409eff;
  color: #409eff;
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.execute-btn {
  padding: 6px 16px;
  border: none;
  border-radius: 4px;
  background: #409eff;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.execute-btn:hover:not(:disabled) {
  background: #337ecc;
}

.execute-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.execute-btn.executing {
  background: #67c23a;
}

.sql-editor {
  flex: 1;
  padding: 20px;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.sql-editor textarea {
  flex: 1;
  width: 100%;
  border: none;
  outline: none;
  resize: vertical;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 14px;
  line-height: 1.5;
  background: #fafbfc;
  padding: 16px;
  border-radius: 4px;
  border: 1px solid #e0e3e8;
  box-sizing: border-box;
  min-height: 150px;
  overflow-y: auto;
}

.sql-editor textarea:focus {
  border-color: #409eff;
}

/* 执行结果 */
.result-section {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px 0 rgba(0,0,0,0.03);
  flex: 1;
  min-height: 200px;
  max-height: 300px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.result-header span {
  font-weight: 600;
  color: #333;
}

.result-info {
  display: flex;
  gap: 16px;
  font-size: 14px;
}

.status {
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}

.status.success {
  background: #f0f9ff;
  color: #67c23a;
}

.status.error {
  background: #fef2f2;
  color: #f56565;
}

.time, .timestamp {
  color: #666;
}

.result-content {
  flex: 1;
  overflow: auto;
  padding: 20px;
}

.no-result {
  text-align: center;
  color: #999;
  padding: 40px;
}

.error-result pre {
  background: #fef2f2;
  color: #f56565;
  padding: 16px;
  border-radius: 4px;
  margin: 0;
  white-space: pre-wrap;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
}

.success-result {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-meta {
  display: flex;
  gap: 16px;
  font-size: 14px;
  color: #666;
}

.result-table {
  overflow: auto;
  border: 1px solid #e0e3e8;
  border-radius: 4px;
}

.result-table table {
  width: 100%;
  border-collapse: collapse;
}

.result-table th,
.result-table td {
  padding: 8px 12px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
}

.result-table th {
  background: #f5f7fa;
  font-weight: 500;
  color: #666;
}

.no-data {
  text-align: center;
  color: #999;
  padding: 20px;
  background: #f9f9f9;
  border-radius: 4px;
}

/* 历史记录 */
.history-section {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px 0 rgba(0,0,0,0.03);
  flex: 1;
  min-height: 200px;
  max-height: 300px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.history-header span {
  font-weight: 600;
  color: #333;
}

.history-actions {
  display: flex;
  gap: 8px;
}

.history-list {
  flex: 1;
  overflow: auto;
  padding: 16px 20px;
}

.no-history {
  text-align: center;
  color: #999;
  padding: 40px;
}

.history-item {
  border: 1px solid #e0e3e8;
  border-radius: 4px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.history-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px 0 rgba(64, 158, 255, 0.1);
}

.history-item.success {
  border-left: 4px solid #67c23a;
}

.history-item.error {
  border-left: 4px solid #f56565;
}

.history-sql {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.history-sql pre {
  margin: 0;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  line-height: 1.4;
  white-space: pre-wrap;
  color: #333;
}

.history-meta {
  padding: 8px 16px;
  display: flex;
  gap: 16px;
  font-size: 12px;
  background: #fafbfc;
}

.history-meta .status {
  font-weight: 500;
}

.history-meta .duration,
.history-meta .time {
  color: #666;
}
</style>