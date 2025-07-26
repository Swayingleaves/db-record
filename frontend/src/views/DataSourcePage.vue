<template>
  <div class="ds-page">
    <div class="ds-header">
      <h3>数据源管理</h3>
      <button class="ds-add-btn" @click="openAdd">新建数据源</button>
    </div>
    
    <!-- 加载状态 -->
    <div v-if="loading" class="loading">加载中...</div>
    
    <table v-else class="ds-table">
      <thead>
        <tr>
          <th>名称</th>
          <th>类型</th>
          <th>地址</th>
          <th>用户名</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="ds in dataSources" :key="ds.id">
          <td>{{ ds.name }}</td>
          <td>{{ ds.type }}</td>
          <td>{{ ds.host }}:{{ ds.port }}/{{ ds.databaseName }}</td>
          <td>{{ ds.username }}</td>
          <td>
            <button class="ds-op" @click="viewDetail(ds)">详情</button>
            <button class="ds-op" @click="editDs(ds)">编辑</button>
            <button class="ds-op" @click="confirmDelete(ds)">删除</button>
            <button class="ds-op" @click="testConn(ds)">测试</button>
          </td>
        </tr>
        <tr v-if="!dataSources.length">
          <td colspan="5" style="text-align:center;color:#aaa;">暂无数据源</td>
        </tr>
      </tbody>
    </table>
    
    <!-- 新建/编辑弹窗 -->
    <div v-if="showForm" class="ds-dialog-mask" @click="closeForm">
      <div class="ds-dialog" @click.stop>
        <h4>{{ formMode==='add' ? '新建' : (formMode==='edit' ? '编辑' : '数据源详情') }}</h4>
        <form @submit.prevent="submitForm">
          <div class="form-row">
            <label>类型</label>
            <select v-model="form.type" :disabled="formMode==='detail'" @change="onTypeChange">
              <option value="mysql">MySQL</option>
              <option value="postgresql">PostgreSQL</option>
              <option value="kingbase">人大金仓</option>
            </select>
          </div>
          <div class="form-row">
            <label>名称</label>
            <input v-model="form.name" :readonly="formMode==='detail'" required />
          </div>
          <div class="form-row">
            <label>地址</label>
            <input v-model="form.host" :readonly="formMode==='detail'" required />
          </div>
          <div class="form-row">
            <label>端口</label>
            <input v-model="form.port" :readonly="formMode==='detail'" type="number" required />
          </div>
          <div class="form-row">
            <label>数据库名</label>
            <input v-model="form.databaseName" :readonly="formMode==='detail'" required />
          </div>
          <div class="form-row">
            <label>用户名</label>
            <input v-model="form.username" :readonly="formMode==='detail'" required />
          </div>
          <div class="form-row">
            <label>密码</label>
            <input v-model="form.password" :readonly="formMode==='detail'" :type="formMode==='detail' ? 'text' : 'password'" required />
          </div>
          <div class="form-row">
            <label>描述</label>
            <input v-model="form.description" :readonly="formMode==='detail'" />
          </div>
          <div class="form-row" v-if="formMode!=='detail'">
            <button class="ds-save-btn" type="submit" :disabled="submitting">
              {{ submitting ? '保存中...' : '保存' }}
            </button>
            <button class="ds-cancel-btn" type="button" @click="closeForm">取消</button>
          </div>
          <div class="form-row" v-else>
            <button class="ds-cancel-btn" type="button" @click="closeForm">关闭</button>
          </div>
        </form>
      </div>
    </div>
    
    <!-- 删除确认弹窗 -->
    <div v-if="showDelete" class="ds-dialog-mask" @click="showDelete=false">
      <div class="ds-dialog" @click.stop>
        <h4>确认删除？</h4>
        <p>确定要删除数据源 <b>{{ delTarget?.name }}</b> 吗？</p>
        <div style="text-align:right;margin-top:18px;">
          <button class="ds-cancel-btn" @click="showDelete=false">取消</button>
          <button class="ds-del-btn" @click="deleteDs" :disabled="deleting">
            {{ deleting ? '删除中...' : '删除' }}
          </button>
        </div>
      </div>
    </div>
    
    <!-- 测试连接弹窗 -->
    <div v-if="showTest" class="ds-dialog-mask" @click="showTest=false">
      <div class="ds-dialog" @click.stop>
        <h4>连接测试</h4>
        <p>{{ testMsg }}</p>
        <div style="text-align:right;margin-top:18px;">
          <button class="ds-cancel-btn" @click="showTest=false">关闭</button>
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
import request from '../utils/request';

const dataSources = ref<any[]>([]);
const showForm = ref(false);
const formMode = ref<'add'|'edit'|'detail'>('add');
const form = ref<any>({});
const showDelete = ref(false);
const delTarget = ref<any>(null);
const showTest = ref(false);
const testMsg = ref('');
const loading = ref(false);
const submitting = ref(false);
const deleting = ref(false);
const error = ref('');

onMounted(() => {
  loadDataSources();
});

async function loadDataSources() {
  try {
    loading.value = true;
    error.value = '';
    const response = await request.get('/api/datasource/list');
    dataSources.value = response.data.data || [];
  } catch (err: any) {
    error.value = err.message || '加载数据源失败';
    console.error('加载数据源失败:', err);
  } finally {
    loading.value = false;
  }
}

function openAdd() {
  formMode.value = 'add';
  form.value = { 
    type: 'mysql', 
    name: '', 
    host: '', 
    port: 3306, 
    databaseName: '', 
    username: '', 
    password: '', 
    description: '' 
  };
  showForm.value = true;
}

// 监听数据库类型变化，自动设置默认端口
function onTypeChange() {
  if (form.value.type === 'mysql') {
    form.value.port = 3306;
  } else if (form.value.type === 'postgresql') {
    form.value.port = 5432;
  } else if (form.value.type === 'kingbase') {
    form.value.port = 54321;
  }
}

function editDs(ds: any) {
  formMode.value = 'edit';
  form.value = { ...ds };
  showForm.value = true;
}

function viewDetail(ds: any) {
  formMode.value = 'detail';
  form.value = { ...ds };
  showForm.value = true;
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
    
    if (formMode.value === 'add') {
      await request.post('/api/datasource/create', form.value);
        await loadDataSources();
        showForm.value = false;
    } else if (formMode.value === 'edit') {
      await request.put('/api/datasource/update', form.value);
        await loadDataSources();
        showForm.value = false;
    }
  } catch (err: any) {
    error.value = err.message || '操作失败';
    console.error('提交失败:', err);
  } finally {
    submitting.value = false;
  }
}

function confirmDelete(ds: any) {
  delTarget.value = ds;
  showDelete.value = true;
}

async function deleteDs() {
  if (deleting.value) return;
  
  try {
    deleting.value = true;
    error.value = '';
    await request.delete(`/api/datasource/delete/${delTarget.value.id}`);
      await loadDataSources();
      showDelete.value = false;
  } catch (err: any) {
    error.value = err.message || '删除失败';
    console.error('删除失败:', err);
  } finally {
    deleting.value = false;
  }
}

async function testConn(ds: any) {
  showTest.value = true;
  testMsg.value = '正在测试连接...';
  
  try {
    await request.post('/api/datasource/test', ds);
    testMsg.value = '连接成功！';
  } catch (err: any) {
    testMsg.value = err.message || '连接失败，请检查配置';
  }
}
</script>

<style scoped>
.ds-page {
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

.ds-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.ds-add-btn {
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 8px 18px;
  font-size: 1rem;
  cursor: pointer;
  transition: background 0.2s;
}

.ds-add-btn:hover {
  background: #337ecc;
}

.ds-table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px 0 rgba(0,0,0,0.03);
}

.ds-table th, .ds-table td {
  padding: 12px 10px;
  border-bottom: 1px solid #f0f0f0;
  text-align: left;
}

.ds-table th {
  background: #f5f7fa;
  color: #666;
  font-weight: 500;
}

.ds-op {
  background: none;
  border: none;
  color: #409eff;
  cursor: pointer;
  margin-right: 8px;
  font-size: 0.98rem;
  padding: 0 4px;
  transition: color 0.2s;
}

.ds-op:hover {
  color: #337ecc;
}

.ds-dialog-mask {
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

.ds-dialog {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  width: 90%;
  max-width: 500px;
  max-height: 80vh;
  overflow-y: auto;
}

.ds-dialog h4 {
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

.form-row input[readonly] {
  background: #f5f5f5;
  cursor: not-allowed;
}

.ds-save-btn, .ds-cancel-btn, .ds-del-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  margin-right: 8px;
  font-size: 14px;
  transition: background 0.2s;
}

.ds-save-btn {
  background: #409eff;
  color: #fff;
}

.ds-save-btn:hover:not(:disabled) {
  background: #337ecc;
}

.ds-save-btn:disabled {
  background: #a0cfff;
  cursor: not-allowed;
}

.ds-cancel-btn {
  background: #f0f0f0;
  color: #666;
}

.ds-cancel-btn:hover {
  background: #e0e0e0;
}

.ds-del-btn {
  background: #f56565;
  color: #fff;
}

.ds-del-btn:hover:not(:disabled) {
  background: #e53e3e;
}

.ds-del-btn:disabled {
  background: #feb2b2;
  cursor: not-allowed;
}
</style>