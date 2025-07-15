# 前端API适配说明

## 概述

前端已完成对后端新的 `Result` 统一返回格式的适配。所有API调用现在都遵循统一的错误处理和数据解析模式。

## 后端返回格式

```typescript
interface ApiResult<T = any> {
  status: boolean;  // 业务状态：true-成功，false-失败
  code: number;     // 状态码：200-成功，其他-失败
  msg: string;      // 提示信息
  data: T;          // 返回数据
}
```

## 前端适配内容

### 1. 请求工具类更新 (`frontend/src/utils/request.ts`)

**主要改进：**
- 统一处理后端 `Result` 格式
- 自动处理业务错误（`status: false`）
- 完善的错误处理和用户友好的错误信息
- 自动处理认证失败，清除token并跳转登录

**使用示例：**
```typescript
try {
  const response = await request.get('/api/datasource/list');
  const data = response.data.data; // 直接获取业务数据
} catch (error) {
  // 统一的错误处理
  console.error(error.message);
}
```

### 2. 组件更新

**已更新的组件：**
- ✅ `Login.vue` - 登录组件
- ✅ `Register.vue` - 注册组件  
- ✅ `DataSourcePage.vue` - 数据源管理页面
- ✅ `ProjectPage.vue` - 项目管理页面

**主要改进：**
- 移除了 `response.data.success` 判断
- 直接使用 `response.data.data` 获取业务数据
- 统一的错误处理和用户提示
- 添加加载状态和按钮防重复点击
- 优化的错误提示UI（toast形式）

### 3. 类型定义 (`frontend/src/types/api.ts`)

定义了完整的API数据结构，包括：
- `ApiResult<T>` - 统一返回格式
- `User`, `LoginData` - 用户相关
- `Datasource` - 数据源相关
- `Project` - 项目相关
- `ProjectVersion` - 项目版本相关
- `SqlRecord` - SQL记录相关
- `PageParams`, `PageResult<T>` - 分页相关

## 错误处理机制

### 1. HTTP错误处理
- 401: 自动清除token，跳转登录
- 403: 权限不足提示
- 500: 服务器错误提示
- 超时: 请求超时提示

### 2. 业务错误处理
- `status: false` 时自动抛出错误
- 错误信息来自后端的 `msg` 字段
- 组件层面统一catch处理

### 3. 用户体验优化
- 加载状态显示
- 错误toast提示
- 按钮防重复点击
- 友好的错误信息

## 使用规范

### 1. API调用标准模式

```typescript
async function loadData() {
  try {
    loading.value = true;
    error.value = '';
    
    const response = await request.get('/api/xxx');
    data.value = response.data.data;
  } catch (err: any) {
    error.value = err.message || '操作失败';
  } finally {
    loading.value = false;
  }
}
```

### 2. 表单提交标准模式

```typescript
async function submitForm() {
  if (submitting.value) return;
  
  try {
    submitting.value = true;
    error.value = '';
    
    await request.post('/api/xxx', formData);
    // 成功处理
  } catch (err: any) {
    error.value = err.message || '提交失败';
  } finally {
    submitting.value = false;
  }
}
```

### 3. 错误提示UI

```vue
<template>
  <!-- 错误提示 -->
  <div v-if="error" class="error-toast">
    {{ error }}
    <button @click="error=''" class="close-btn">×</button>
  </div>
</template>
```

## 待完成的适配

以下组件还需要继续适配：
- `ProjectDetailPage.vue` - 项目详情页面
- `Dashboard.vue` - 仪表板组件

## 测试建议

1. **成功场景测试**：验证正常的API调用和数据显示
2. **错误场景测试**：
   - 网络错误
   - 服务器错误（500）
   - 业务错误（后端返回 `status: false`）
   - 认证失败（401）
   - 权限不足（403）
3. **用户体验测试**：
   - 加载状态显示
   - 错误提示显示和关闭
   - 按钮防重复点击

## 注意事项

1. 所有API调用都应该使用统一的 `request` 工具
2. 不要直接判断 `response.data.success`，让工具类处理
3. 统一使用 `response.data.data` 获取业务数据
4. 错误处理应该用户友好，避免技术性错误信息
5. 重要操作要有加载状态和防重复点击保护 