<template>
  <div class="project-detail-page">
    <!-- 导航路径 -->
    <div class="breadcrumb">
      <router-link to="/dashboard/project" class="breadcrumb-link">项目管理</router-link>
      <span class="breadcrumb-separator">></span>
      <span class="breadcrumb-current">{{ project?.name || '项目详情' }}</span>
    </div>

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
          <span>{{ project?.desc }}</span>
        </div>
        <div class="info-item">
          <label>创建时间：</label>
          <span>{{ project?.createdAt }}</span>
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
            <input v-model="editForm.desc" />
          </div>
          <div class="form-row">
            <button class="project-save-btn" type="submit">保存</button>
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

    <!-- 删除版本确认弹窗 -->
    <div v-if="showDelete" class="project-dialog-mask">
      <div class="project-dialog">
        <h4>确认删除？</h4>
        <p>确定要删除版本 <b>{{ delTarget?.version }}</b> 吗？</p>
        <div style="text-align:right;margin-top:18px;">
          <button class="project-cancel-btn" @click="showDelete=false">取消</button>
          <button class="project-del-btn" @click="deleteVersion">删除</button>
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
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter();

// 项目信息
const project = ref<any>(null);
const showEditForm = ref(false);
const editForm = ref<any>({});

// 版本管理相关
const versions = ref<any[]>([]);
const showVersionForm = ref(false);
const versionFormMode = ref<'add'|'edit'|'detail'>('add');
const versionForm = ref<any>({});
const showDelete = ref(false);
const delTarget = ref<any>(null);
const showCompare = ref(false);
const compareTargetId = ref<number|null>(null);
const compareResult = ref('');

// mock项目数据
const mockProjects = [
  { id: 1, name: '用户中心', desc: '用户与权限管理', createdAt: '2024-07-01' },
  { id: 2, name: '订单系统', desc: '订单与支付', createdAt: '2024-07-02' },
];

// mock版本数据
function mockVersions(projectId: number) {
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

onMounted(() => {
  const projectId = parseInt(route.params.id as string);
  project.value = mockProjects.find(p => p.id === projectId);
  if (project.value) {
    versions.value = mockVersions(project.value.id);
  }
});

// 项目编辑相关
function editProject() {
  editForm.value = { ...project.value };
  showEditForm.value = true;
}

function closeEditForm() {
  showEditForm.value = false;
}

function submitEditForm() {
  // 这里应该调用API更新项目信息
  project.value = { ...editForm.value };
  showEditForm.value = false;
}

// 版本管理相关
function openAddVersion() {
  versionFormMode.value = 'add';
  versionForm.value = { version: '', desc: '' };
  showVersionForm.value = true;
}

function editVersion(ver: any) {
  versionFormMode.value = 'edit';
  versionForm.value = { ...ver };
  showVersionForm.value = true;
}

function viewVersion(ver: any) {
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
    const idx = versions.value.findIndex((d: any) => d.id === versionForm.value.id);
    if (idx > -1) versions.value[idx] = { ...versionForm.value };
  }
  showVersionForm.value = false;
}

function confirmDeleteVersion(ver: any) {
  delTarget.value = ver;
  showDelete.value = true;
}

function deleteVersion() {
  versions.value = versions.value.filter((d: any) => d.id !== delTarget.value.id);
  showDelete.value = false;
}

function openCompare(ver: any) {
  showCompare.value = true;
  compareTargetId.value = ver.id;
  compareResult.value = '字段A 新增\n字段B 删除\n字段C 类型变更';
}

function exportSql(ver: any) {
  alert('导出SQL（mock）：' + ver.version);
}

function exportCompareSql() {
  alert('导出对比SQL（mock）');
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
</style>