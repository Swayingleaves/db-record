<template>
  <div class="project-page">
    <div class="project-header">
      <h3>项目管理</h3>
      <button class="project-add-btn" @click="openAdd">新建项目</button>
    </div>
    <table class="project-table">
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
          <td>{{ proj.desc }}</td>
          <td>{{ proj.createdAt }}</td>
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
        <h4>{{ formMode==='add' ? '新建项目' : (formMode==='edit' ? '编辑项目' : '项目详情') }}</h4>
        <form @submit.prevent="submitForm">
          <div class="form-row">
            <label>项目名称</label>
            <input v-model="form.name" :readonly="formMode==='detail'" required />
          </div>
          <div class="form-row">
            <label>描述</label>
            <input v-model="form.desc" :readonly="formMode==='detail'" />
          </div>
          <div class="form-row" v-if="formMode!=='detail'">
            <button class="project-save-btn" type="submit">保存</button>
            <button class="project-cancel-btn" type="button" @click="closeForm">取消</button>
          </div>
          <div class="form-row" v-else>
            <button class="project-cancel-btn" type="button" @click="closeForm">关闭</button>
          </div>
        </form>
        <!-- 版本管理tab，仅在详情模式下显示 -->
        <div v-if="formMode==='detail'" class="version-section">
          <div class="version-header">
            <span>版本管理</span>
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
                <td>{{ ver.version }}</td>
                <td>{{ ver.desc }}</td>
                <td>{{ ver.createdAt }}</td>
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
    </div>
    <!-- 删除项目弹窗 -->
    <div v-if="showDelete" class="project-dialog-mask">
      <div class="project-dialog">
        <h4>确认删除？</h4>
        <p>确定要删除项目 <b>{{ delTarget?.name }}</b> 吗？</p>
        <div style="text-align:right;margin-top:18px;">
          <button class="project-cancel-btn" @click="showDelete=false">取消</button>
          <button class="project-del-btn" @click="deleteProject">删除</button>
        </div>
      </div>
    </div>
    <!-- 新建/编辑/详情版本弹窗 -->
    <div v-if="showVersionForm" class="project-dialog-mask">
      <div class="project-dialog">
        <h4>{{ versionFormMode==='add' ? '新建版本' : (versionFormMode==='edit' ? '编辑版本' : '版本详情') }}</h4>
        <form @submit.prevent="submitVersionForm">
          <div class="form-row">
            <label>版本号</label>
            <input v-model="versionForm.version" :readonly="versionFormMode==='detail'" required />
          </div>
          <div class="form-row">
            <label>描述</label>
            <input v-model="versionForm.desc" :readonly="versionFormMode==='detail'" />
          </div>
          <div class="form-row" v-if="versionFormMode!=='detail'">
            <button class="version-save-btn" type="submit">保存</button>
            <button class="version-cancel-btn" type="button" @click="closeVersionForm">取消</button>
          </div>
          <div class="form-row" v-else>
            <button class="version-cancel-btn" type="button" @click="closeVersionForm">关闭</button>
          </div>
        </form>
      </div>
    </div>
    <!-- 版本对比弹窗 -->
    <div v-if="showCompare" class="project-dialog-mask">
      <div class="project-dialog">
        <h4>版本对比</h4>
        <div class="form-row">
          <label>选择对比版本</label>
          <select v-model="compareTargetId">
            <option v-for="ver in versions" :key="ver.id" :value="ver.id">{{ ver.version }}</option>
          </select>
        </div>
        <div class="compare-result">
          <p>结构变化（mock）：</p>
          <pre style="background:#f8f8f8;padding:10px;border-radius:6px;">{{ compareResult }}</pre>
        </div>
        <div style="text-align:right;margin-top:18px;">
          <button class="version-cancel-btn" @click="showCompare=false">关闭</button>
          <button class="version-save-btn" @click="exportCompareSql">导出SQL</button>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref } from 'vue';
// mock项目数据
const projects = ref([
  { id: 1, name: '用户中心', desc: '用户与权限管理', createdAt: '2024-07-01' },
  { id: 2, name: '订单系统', desc: '订单与支付', createdAt: '2024-07-02' },
]);
const showForm = ref(false);
const formMode = ref<'add'|'edit'|'detail'>('add');
const form = ref<any>({});
const showDelete = ref(false);
const delTarget = ref<any>(null);
// 版本管理相关
const versions = ref<any[]>([]);
const showVersionForm = ref(false);
const versionFormMode = ref<'add'|'edit'|'detail'>('add');
const versionForm = ref<any>({});
const showCompare = ref(false);
const compareTargetId = ref<number|null>(null);
const compareResult = ref('');

function openAdd() {
  formMode.value = 'add';
  form.value = { name: '', desc: '' };
  showForm.value = true;
  versions.value = [];
}
function editProject(proj:any) {
  formMode.value = 'edit';
  form.value = { ...proj };
  showForm.value = true;
  versions.value = mockVersions(proj.id);
}
function viewDetail(proj:any) {
  formMode.value = 'detail';
  form.value = { ...proj };
  showForm.value = true;
  versions.value = mockVersions(proj.id);
}
function closeForm() {
  showForm.value = false;
}
function submitForm() {
  if (formMode.value === 'add') {
    projects.value.push({ ...form.value, id: Date.now(), createdAt: new Date().toISOString().slice(0,10) });
  } else if (formMode.value === 'edit') {
    const idx = projects.value.findIndex((d:any) => d.id === form.value.id);
    if (idx > -1) projects.value[idx] = { ...form.value };
  }
  showForm.value = false;
}
function confirmDelete(proj:any) {
  delTarget.value = proj;
  showDelete.value = true;
}
function deleteProject() {
  projects.value = projects.value.filter((d:any) => d.id !== delTarget.value.id);
  showDelete.value = false;
}
// 版本管理相关
function mockVersions(projectId:number) {
  if (projectId === 1) {
    return [
      { id: 101, version: 'v1.0.0', desc: '初始版本', createdAt: '2024-07-01' },
      { id: 102, version: 'v1.1.0', desc: '优化表结构', createdAt: '2024-07-05' },
    ];
  } else if (projectId === 2) {
    return [
      { id: 201, version: 'v1.0.0', desc: '订单初版', createdAt: '2024-07-02' },
    ];
  }
  return [];
}
function openAddVersion() {
  versionFormMode.value = 'add';
  versionForm.value = { version: '', desc: '' };
  showVersionForm.value = true;
}
function editVersion(ver:any) {
  versionFormMode.value = 'edit';
  versionForm.value = { ...ver };
  showVersionForm.value = true;
}
function viewVersion(ver:any) {
  versionFormMode.value = 'detail';
  versionForm.value = { ...ver };
  showVersionForm.value = true;
}
function closeVersionForm() {
  showVersionForm.value = false;
}
function submitVersionForm() {
  if (versionFormMode.value === 'add') {
    versions.value.push({ ...versionForm.value, id: Date.now(), createdAt: new Date().toISOString().slice(0,10) });
  } else if (versionFormMode.value === 'edit') {
    const idx = versions.value.findIndex((d:any) => d.id === versionForm.value.id);
    if (idx > -1) versions.value[idx] = { ...versionForm.value };
  }
  showVersionForm.value = false;
}
function confirmDeleteVersion(ver:any) {
  delTarget.value = ver;
  showDelete.value = true;
}
function openCompare(ver:any) {
  showCompare.value = true;
  compareTargetId.value = ver.id;
  compareResult.value = '字段A 新增\n字段B 删除\n字段C 类型变更';
}
function exportSql(ver:any) {
  alert('导出SQL（mock）：' + ver.version);
}
function exportCompareSql() {
  alert('导出对比SQL（mock）');
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
.version-section {
  margin-top: 28px;
}
.version-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.version-add-btn {
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 6px 14px;
  font-size: 0.98rem;
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
  box-shadow: 0 2px 8px 0 rgba(0,0,0,0.03);
}
.version-table th, .version-table td {
  padding: 10px 8px;
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
.compare-result {
  margin: 12px 0 0 0;
  font-size: 0.98rem;
  color: #333;
}
</style> 