// 后端统一返回格式
export interface ApiResult<T = any> {
  status: boolean;
  code: number;
  msg: string;
  data: T;
}

// 用户相关
export interface User {
  id: number;
  username: string;
  role: string;
  status: number;
  createTime?: string;
  updateTime?: string;
}

export interface LoginData {
  token: string;
  username: string;
  role: string;
}

// 数据源相关
export interface Datasource {
  id?: number;
  name: string;
  type: 'mysql' | 'postgresql' | 'kingbase';
  host: string;
  port: number;
  databaseName: string;
  username: string;
  password: string;
  description?: string;
  status?: number;
  userId?: number;
  createTime?: string;
  updateTime?: string;
}

// 项目相关
export interface Project {
  id?: number;
  name: string;
  description?: string;
  datasourceId?: number;
  status?: number;
  userId?: number;
  createTime?: string;
  updateTime?: string;
}

// 项目版本相关
export interface ProjectVersion {
  id?: number;
  projectId: number;
  versionName: string;
  description?: string;
  schemaSnapshot?: string;
  status?: number;
  userId?: number;
  createTime?: string;
  updateTime?: string;
}

// SQL记录相关
export interface SqlRecord {
  id?: number;
  projectId: number;
  sqlContent: string;
  executeResult?: string;
  executeStatus?: number; // 0-失败, 1-成功
  executeTime?: number;
  errorMessage?: string;
  userId?: number;
  createTime?: string;
  updateTime?: string;
}

// 分页相关
export interface PageParams {
  page?: number;
  size?: number;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

// 版本对比相关
interface TableInfo {
  tableName: string;
  tableComment?: string;
}

interface ColumnChange {
  columnName: string;
  // ... 其他可能的字段
}

interface IndexChange {
  indexName: string;
  // ... 其他可能的字段
}

interface ModifiedTable extends TableInfo {
  addedColumns?: ColumnChange[];
  removedColumns?: ColumnChange[];
  modifiedColumns?: ColumnChange[];
  addedIndexes?: IndexChange[];
  removedIndexes?: IndexChange[];
  modifiedIndexes?: IndexChange[];
}

interface SchemaChanges {
  schemaName: string;
  addedTables?: TableInfo[];
  removedTables?: TableInfo[];
  modifiedTables?: ModifiedTable[];
}

interface SchemaInfo {
  schemaName: string;
  tables: TableInfo[];
}

export interface CompareResult {
  addedSchemas?: SchemaInfo[];
  removedSchemas?: SchemaInfo[];
  modifiedSchemas?: SchemaChanges[];
  fromVersion?: string;
  toVersion?: string;
  error?: string;
}
 