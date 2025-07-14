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
        <h4>{{ formMode==='add' ? '新建项目' : '编辑项目' }}</h4>
        <form @submit.prevent="submitForm">
          <div class="form-row">
            <label>项目名称</label>
            <input v-model="form.name" required />
          </div>
          <div class="form-row">
            <label>描述</label>
            <input v-model="form.desc" />
          </div>
          <div class="form-row">
            <button class="project-save-btn" type="submit">保存</button>
            <button class="project-cancel-btn" type="button" @click="closeForm">取消</button>
          </div>
        </form>
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

  </div>
</template>
<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();
// mock项目数据
const projects = ref([
  { id: 1, name: '用户中心', desc: '用户与权限管理', createdAt: '2024-07-01' },
  { id: 2, name: '订单系统', desc: '订单与支付', createdAt: '2024-07-02' },
]);
const showForm = ref(false);
const formMode = ref<'add'|'edit'>('add');
const form = ref<any>({});
const showDelete = ref(false);
const delTarget = ref<any>(null);


function openAdd() {
  formMode.value = 'add';
  form.value = { name: '', desc: '' };
  showForm.value = true;
}
function editProject(proj:any) {
  formMode.value = 'edit';
  form.value = { ...proj };
  showForm.value = true;
}
function viewDetail(proj:any) {
  router.push(`/dashboard/project/${proj.id}`);
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
.project-save-btn {
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
.project-save-btn:hover {
  background: #337ecc;
}
.project-cancel-btn {
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
.project-cancel-btn:hover {
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

</style>