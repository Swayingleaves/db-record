# API数据访问修复说明

## 问题描述

项目详情页面调用了 `http://localhost:5173/api/project/detail-with-datasource/1` 接口，但是【数据源管理】区域没有显示数据源信息。

## 根本原因

前端在处理API响应数据时，没有正确访问后端统一返回格式 `Result<T>` 中的数据。

### 后端返回格式
```json
{
  "status": true,
  "code": 200,
  "msg": "操作成功",
  "data": {
    "project": { ... },
    "datasource": { ... }
  }
}
```

### 前端错误的访问方式
```javascript
// 错误：直接访问 response.data.project
project.value = projectResponse.data.project;
currentDatasource.value = projectResponse.data.datasource;
```

### 前端正确的访问方式
```javascript
// 正确：先获取 data 字段，再访问具体数据
const projectData = projectResponse.data.data;
project.value = projectData.project;
currentDatasource.value = projectData.datasource;
```

## 修复的文件和位置

### 1. 项目详情数据加载 (`loadProjectDetail`)
- **位置**: `frontend/src/views/ProjectDetailPage.vue`
- **修复**: 
  - `projectResponse.data.project` → `projectResponse.data.data.project`
  - `projectResponse.data.datasource` → `projectResponse.data.data.datasource`
  - `versionsResponse.data` → `versionsResponse.data.data`
  - `datasourcesResponse.data` → `datasourcesResponse.data.data`

### 2. 版本创建 (`submitVersionForm`)
- **位置**: `frontend/src/views/ProjectDetailPage.vue`
- **修复**: `response.data` → `response.data.data`

### 3. 版本对比 (`performCompare`)
- **位置**: `frontend/src/views/ProjectDetailPage.vue`
- **修复**: `response.data` → `response.data.data`

### 4. 版本SQL导出 (`exportSql`)
- **位置**: `frontend/src/views/ProjectDetailPage.vue`
- **修复**: `response.data.sql` → `response.data.data.sql`

### 5. 数据源连接测试 (`testDataSourceConnection`)
- **位置**: `frontend/src/views/ProjectDetailPage.vue`
- **修复**: `response.data` → `response.data.data`

## 统一的API数据访问模式

为了避免类似问题，建议在所有API调用中统一使用以下模式：

```javascript
// GET请求获取数据
const response = await request.get('/api/xxx');
const data = response.data.data; // 从Result格式中获取实际数据

// POST请求创建数据
const response = await request.post('/api/xxx', payload);
const createdData = response.data.data; // 获取创建的数据

// PUT请求更新数据
await request.put('/api/xxx', payload);
// 更新操作通常不需要返回数据，只需要成功状态

// DELETE请求删除数据
await request.delete('/api/xxx');
// 删除操作通常不需要返回数据，只需要成功状态
```

## 验证方法

1. 启动后端服务：`mvn spring-boot:run`
2. 启动前端服务：`npm run dev`
3. 创建一个项目并选择数据源
4. 进入项目详情页面
5. 检查【数据源管理】区域是否正确显示绑定的数据源信息

## 注意事项

- 所有API调用都经过统一的Result格式封装
- 前端的request拦截器会处理业务错误（status: false）
- 成功的响应需要通过 `response.data.data` 访问实际数据
- 错误处理由拦截器统一处理，会自动抛出异常 