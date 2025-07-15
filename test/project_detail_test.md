# 项目详情功能测试指南

## 测试环境准备

### 1. 启动后端服务
```bash
cd backend
mvn spring-boot:run
```

### 2. 启动前端服务
```bash
cd frontend
npm run dev
```

### 3. 数据库修复（如果需要）
```sql
-- 执行数据库修复脚本
mysql -u root -p < backend/db/fix_project_datasource.sql
```

## 测试步骤

### 问题1：新建项目时选择数据源但详情页未显示

#### 测试步骤：
1. 访问 `http://localhost:5173`
2. 登录系统
3. 进入项目管理页面
4. 点击"新建项目"
5. 填写项目信息并**选择数据源**
6. 保存项目
7. 点击项目的"详情"按钮
8. 检查数据源管理区域是否显示绑定的数据源

#### 预期结果：
- 数据源管理区域应该显示已绑定的数据源信息
- 显示数据源的名称、类型、地址、数据库名、用户名
- 提供"测试连接"和"解绑"按钮

#### 修复内容：
- ✅ 修改数据库表结构：`datasource_id` 字段允许为 NULL
- ✅ 修改前端表单：数据源选择改为可选
- ✅ 修改前端提交逻辑：空字符串转换为 null
- ✅ 后端接口已支持获取项目关联的数据源信息

### 问题2：详情页版本管理接口前后端联调

#### 测试步骤：
1. 进入项目详情页面
2. 在版本管理区域点击"新建版本"
3. 填写版本信息并保存
4. 测试版本的各项操作：
   - 查看版本详情
   - 编辑版本信息
   - 删除版本（带确认）
   - 版本对比功能
   - 导出版本SQL

#### 预期结果：
- 所有版本操作都应该成功调用后端API
- 操作成功后显示Toast提示
- 版本列表实时更新
- 错误情况有适当的错误提示

#### 已实现功能：
- ✅ 版本CRUD操作的前后端联调
- ✅ 版本对比功能（基础实现）
- ✅ 版本SQL导出功能（基础实现）
- ✅ 统一的错误处理和用户提示
- ✅ 加载状态管理

## API接口测试

### 项目相关接口：
- `GET /api/project/detail-with-datasource/{id}` - 获取项目详情（含数据源）
- `POST /api/project/bind-datasource` - 绑定数据源
- `POST /api/project/unbind-datasource/{projectId}` - 解绑数据源

### 版本相关接口：
- `GET /api/project-version/list/{projectId}` - 获取版本列表
- `POST /api/project-version/create` - 创建版本
- `PUT /api/project-version/update` - 更新版本
- `DELETE /api/project-version/delete/{id}` - 删除版本
- `GET /api/project-version/detail/{id}` - 获取版本详情
- `GET /api/project-version/compare/{fromVersionId}/{toVersionId}` - 版本对比
- `GET /api/project-version/export-sql/{id}` - 导出版本SQL

## 测试验证清单

### 数据源管理：
- [ ] 新建项目时选择数据源，详情页正确显示
- [ ] 新建项目时不选择数据源，详情页显示"暂未绑定数据源"
- [ ] 在详情页绑定数据源功能正常
- [ ] 在详情页解绑数据源功能正常
- [ ] 测试数据源连接功能正常

### 版本管理：
- [ ] 版本列表正确显示
- [ ] 新建版本功能正常
- [ ] 编辑版本功能正常
- [ ] 删除版本功能正常（带确认）
- [ ] 版本详情查看功能正常
- [ ] 版本对比功能正常
- [ ] 版本SQL导出功能正常

### 用户体验：
- [ ] 所有操作都有加载状态提示
- [ ] 操作成功有Toast提示
- [ ] 操作失败有错误提示
- [ ] 表单验证正常
- [ ] 防重复提交保护正常

## 注意事项

1. 确保数据库表结构已经更新（datasource_id允许为NULL）
2. 测试前请确保有可用的数据源
3. 版本对比和SQL导出功能目前为基础实现，可根据需要进一步完善
4. 所有API调用都经过统一的错误处理和Result封装 