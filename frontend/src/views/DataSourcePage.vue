<template>
  <div class="ds-page">
    <div class="ds-header">
      <h3>数据源管理</h3>
      <button class="ds-add-btn" @click="openAdd">新建数据源</button>
    </div>
    <table class="ds-table">
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
          <td>{{ ds.host }}:{{ ds.port }}</td>
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
    <div v-if="showForm" class="ds-dialog-mask">
      <div class="ds-dialog">
        <h4>{{ formMode==='add' ? '新建' : (formMode==='edit' ? '编辑' : '数据源详情') }}</h4>
        <form @submit.prevent="submitForm">
          <div class="form-row">
            <label>类型</label>
            <select v-model="form.type" :disabled="formMode==='detail'">
              <option value="MySQL">MySQL</option>
              <option value="PostgreSQL">PostgreSQL</option>
              <option value="人大金仓">人大金仓</option>
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
            <input v-model="form.port" :readonly="formMode==='detail'" required />
          </div>
          <div class="form-row">
            <label>用户名</label>
            <input v-model="form.username" :readonly="formMode==='detail'" required />
          </div>
          <div class="form-row">
            <label>密码</label>
            <input v-model="form.password" :readonly="formMode==='detail'" :type="formMode==='detail' ? 'text' : 'password'" required />
          </div>
          <div class="form-row" v-if="formMode!=='detail'">
            <button class="ds-save-btn" type="submit">保存</button>
            <button class="ds-cancel-btn" type="button" @click="closeForm">取消</button>
          </div>
          <div class="form-row" v-else>
            <button class="ds-cancel-btn" type="button" @click="closeForm">关闭</button>
          </div>
        </form>
      </div>
    </div>
    <!-- 删除确认弹窗 -->
    <div v-if="showDelete" class="ds-dialog-mask">
      <div class="ds-dialog">
        <h4>确认删除？</h4>
        <p>确定要删除数据源 <b>{{ delTarget?.name }}</b> 吗？</p>
        <div style="text-align:right;margin-top:18px;">
          <button class="ds-cancel-btn" @click="showDelete=false">取消</button>
          <button class="ds-del-btn" @click="deleteDs">删除</button>
        </div>
      </div>
    </div>
    <!-- 测试连接弹窗 -->
    <div v-if="showTest" class="ds-dialog-mask">
      <div class="ds-dialog">
        <h4>连接测试</h4>
        <p>{{ testMsg }}</p>
        <div style="text-align:right;margin-top:18px;">
          <button class="ds-cancel-btn" @click="showTest=false">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref } from 'vue';
const dataSources = ref([
  { id: 1, name: '本地MySQL', type: 'MySQL', host: '127.0.0.1', port: '3306', username: 'root', password: '******' },
  { id: 2, name: 'PostgreSQL测试', type: 'PostgreSQL', host: '192.168.1.10', port: '5432', username: 'pguser', password: '******' },
]);
const showForm = ref(false);
const formMode = ref<'add'|'edit'|'detail'>('add');
const form = ref<any>({});
const showDelete = ref(false);
const delTarget = ref<any>(null);
const showTest = ref(false);
const testMsg = ref('');
function openAdd() {
  formMode.value = 'add';
  form.value = { type: 'MySQL', name: '', host: '', port: '', username: '', password: '' };
  showForm.value = true;
}
function editDs(ds:any) {
  formMode.value = 'edit';
  form.value = { ...ds };
  showForm.value = true;
}
function viewDetail(ds:any) {
  formMode.value = 'detail';
  form.value = { ...ds };
  showForm.value = true;
}
function closeForm() {
  showForm.value = false;
}
function submitForm() {
  if (formMode.value === 'add') {
    dataSources.value.push({ ...form.value, id: Date.now() });
  } else if (formMode.value === 'edit') {
    const idx = dataSources.value.findIndex((d:any) => d.id === form.value.id);
    if (idx > -1) dataSources.value[idx] = { ...form.value };
  }
  showForm.value = false;
}
function confirmDelete(ds:any) {
  delTarget.value = ds;
  showDelete.value = true;
}
function deleteDs() {
  dataSources.value = dataSources.value.filter((d:any) => d.id !== delTarget.value.id);
  showDelete.value = false;
}
function testConn(ds:any) {
  showTest.value = true;
  testMsg.value = '正在测试...';
  setTimeout(() => {
    testMsg.value = Math.random() > 0.2 ? '连接成功！' : '连接失败，请检查配置';
  }, 800);
}
</script>
<style scoped>
.ds-page {
  width: 100%;
  min-height: 0;  /* 允许flex容器收缩，关键 */
  flex: 1;        /* 自动填满父容器，关键 */
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}
/* 其余样式同Dashboard数据源管理区 */
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
  left: 0; top: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.18);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
}
.ds-dialog {
  background: #fff;
  border-radius: 10px;
  min-width: 320px;
  max-width: 96vw;
  padding: 28px 24px 18px 24px;
  box-shadow: 0 4px 24px 0 rgba(0,0,0,0.10);
}
.ds-dialog h4 {
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
.ds-save-btn {
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
.ds-save-btn:hover {
  background: #337ecc;
}
.ds-cancel-btn {
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
.ds-cancel-btn:hover {
  background: #e0e3e8;
}
.ds-del-btn {
  background: #e74c3c;
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 7px 18px;
  font-size: 1rem;
  cursor: pointer;
  transition: background 0.2s;
}
.ds-del-btn:hover {
  background: #c0392b;
}
</style> 