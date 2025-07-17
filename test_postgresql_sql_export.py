#!/usr/bin/env python3
"""
测试 PostgreSQL SQL 导出功能
"""

import requests
import json
import sys

# 配置
BASE_URL = "http://localhost:8081"
USERNAME = "admin"
PASSWORD = "123456"

def login():
    """登录获取token"""
    login_data = {
        "username": USERNAME,
        "password": PASSWORD
    }
    
    response = requests.post(f"{BASE_URL}/login", json=login_data)
    if response.status_code == 200:
        result = response.json()
        if result.get("code") == 200:
            return result["data"]["token"]
    
    print("登录失败")
    return None

def get_headers(token):
    """获取请求头"""
    return {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }

def test_mysql_sql_export(token):
    """测试MySQL SQL导出"""
    print("=== 测试 MySQL SQL 导出 ===")
    
    headers = get_headers(token)
    
    # 获取版本列表
    response = requests.get(f"{BASE_URL}/api/project-version/list/2", headers=headers)
    if response.status_code == 200:
        versions = response.json()["data"]
        if versions:
            version_id = versions[0]["id"]
            print(f"使用版本ID: {version_id}")
            
            # 导出SQL
            response = requests.get(f"{BASE_URL}/api/project-version/export-sql/{version_id}", headers=headers)
            if response.status_code == 200:
                sql_content = response.text
                print("MySQL SQL导出成功:")
                print("=" * 50)
                print(sql_content[:1000] + "..." if len(sql_content) > 1000 else sql_content)
                print("=" * 50)
                
                # 保存到文件
                with open("mysql_export.sql", "w", encoding="utf-8") as f:
                    f.write(sql_content)
                print("SQL已保存到 mysql_export.sql")
            else:
                print(f"SQL导出失败: {response.status_code}")
        else:
            print("没有找到版本")
    else:
        print("获取版本列表失败")

def create_mock_postgresql_data(token):
    """创建模拟的PostgreSQL数据"""
    print("=== 创建模拟 PostgreSQL 数据 ===")
    
    headers = get_headers(token)
    
    # 模拟PostgreSQL表结构数据（包含schema信息）
    mock_tables = [
        {
            "tableName": "users",
            "schemaName": "public",
            "tableComment": "用户表",
            "tableType": "BASE TABLE",
            "engine": None,
            "charset": None,
            "collation": None,
            "columns": [
                {
                    "columnName": "id",
                    "columnType": "bigserial",
                    "isNullable": "NO",
                    "columnKey": "PRI",
                    "columnDefault": None,
                    "extra": "auto_increment",
                    "columnComment": "主键ID"
                },
                {
                    "columnName": "username",
                    "columnType": "character varying(50)",
                    "isNullable": "NO",
                    "columnKey": "UNI",
                    "columnDefault": None,
                    "extra": "",
                    "columnComment": "用户名"
                },
                {
                    "columnName": "email",
                    "columnType": "character varying(100)",
                    "isNullable": "YES",
                    "columnKey": "",
                    "columnDefault": None,
                    "extra": "",
                    "columnComment": "邮箱"
                },
                {
                    "columnName": "created_at",
                    "columnType": "timestamp without time zone",
                    "isNullable": "NO",
                    "columnKey": "",
                    "columnDefault": "CURRENT_TIMESTAMP",
                    "extra": "",
                    "columnComment": "创建时间"
                }
            ],
            "indexes": [
                {
                    "indexName": "users_pkey",
                    "indexType": "BTREE",
                    "isUnique": True,
                    "isPrimary": True,
                    "columnNames": "id",
                    "indexComment": "主键索引"
                },
                {
                    "indexName": "users_username_key",
                    "indexType": "BTREE",
                    "isUnique": True,
                    "isPrimary": False,
                    "columnNames": "username",
                    "indexComment": "用户名唯一索引"
                }
            ]
        },
        {
            "tableName": "orders",
            "schemaName": "sales",
            "tableComment": "订单表",
            "tableType": "BASE TABLE",
            "engine": None,
            "charset": None,
            "collation": None,
            "columns": [
                {
                    "columnName": "id",
                    "columnType": "bigserial",
                    "isNullable": "NO",
                    "columnKey": "PRI",
                    "columnDefault": None,
                    "extra": "auto_increment",
                    "columnComment": "订单ID"
                },
                {
                    "columnName": "user_id",
                    "columnType": "bigint",
                    "isNullable": "NO",
                    "columnKey": "MUL",
                    "columnDefault": None,
                    "extra": "",
                    "columnComment": "用户ID"
                },
                {
                    "columnName": "total_amount",
                    "columnType": "numeric(10,2)",
                    "isNullable": "NO",
                    "columnKey": "",
                    "columnDefault": "0.00",
                    "extra": "",
                    "columnComment": "总金额"
                },
                {
                    "columnName": "status",
                    "columnType": "character varying(20)",
                    "isNullable": "NO",
                    "columnKey": "",
                    "columnDefault": "'pending'",
                    "extra": "",
                    "columnComment": "订单状态"
                }
            ],
            "indexes": [
                {
                    "indexName": "orders_pkey",
                    "indexType": "BTREE",
                    "isUnique": True,
                    "isPrimary": True,
                    "columnNames": "id",
                    "indexComment": "主键索引"
                },
                {
                    "indexName": "idx_orders_user_id",
                    "indexType": "BTREE",
                    "isUnique": False,
                    "isPrimary": False,
                    "columnNames": "user_id",
                    "indexComment": "用户ID索引"
                }
            ]
        },
        {
            "tableName": "products",
            "schemaName": "inventory",
            "tableComment": "产品表",
            "tableType": "BASE TABLE",
            "engine": None,
            "charset": None,
            "collation": None,
            "columns": [
                {
                    "columnName": "id",
                    "columnType": "bigserial",
                    "isNullable": "NO",
                    "columnKey": "PRI",
                    "columnDefault": None,
                    "extra": "auto_increment",
                    "columnComment": "产品ID"
                },
                {
                    "columnName": "name",
                    "columnType": "character varying(100)",
                    "isNullable": "NO",
                    "columnKey": "",
                    "columnDefault": None,
                    "extra": "",
                    "columnComment": "产品名称"
                },
                {
                    "columnName": "price",
                    "columnType": "numeric(8,2)",
                    "isNullable": "NO",
                    "columnKey": "",
                    "columnDefault": None,
                    "extra": "",
                    "columnComment": "价格"
                }
            ],
            "indexes": [
                {
                    "indexName": "products_pkey",
                    "indexType": "BTREE",
                    "isUnique": True,
                    "isPrimary": True,
                    "columnNames": "id",
                    "indexComment": "主键索引"
                }
            ]
        }
    ]
    
    # 构造完整的结构数据
    mock_structure = {
        "database": {
            "databaseName": "test_postgresql",
            "charset": "UTF8",
            "collation": "en_US.UTF-8",
            "snapshotTime": "2025-07-17T23:00:00",
            "schemasInfo": json.dumps([
                {"schemaName": "public", "schemaComment": "默认schema"},
                {"schemaName": "sales", "schemaComment": "销售相关schema"},
                {"schemaName": "inventory", "schemaComment": "库存相关schema"}
            ])
        },
        "tables": mock_tables,
        "datasourceType": "postgresql"
    }
    
    return mock_structure

def test_postgresql_sql_generation(mock_structure):
    """测试PostgreSQL SQL生成"""
    print("=== 测试 PostgreSQL SQL 生成 ===")
    
    # 模拟调用generateCompleteStructureSql方法
    # 这里我们直接构造预期的SQL输出来验证逻辑
    
    tables = mock_structure["tables"]
    database_info = mock_structure["database"]
    
    # 按schema分组
    schema_groups = {}
    schemas = set()
    
    for table in tables:
        schema_name = table.get("schemaName", "public")
        schemas.add(schema_name)
        
        if schema_name not in schema_groups:
            schema_groups[schema_name] = []
        schema_groups[schema_name].append(table)
    
    print("发现的Schema:")
    for schema in schemas:
        tables_in_schema = len(schema_groups[schema])
        print(f"  - {schema}: {tables_in_schema}个表")
    
    print("\n预期的SQL结构:")
    print("1. CREATE SCHEMA语句")
    for schema in schemas:
        if schema != "public":
            print(f"   CREATE SCHEMA IF NOT EXISTS \"{schema}\";")
    
    print("\n2. 按Schema分组的表结构:")
    for schema_name, schema_tables in schema_groups.items():
        print(f"   Schema: {schema_name} ({len(schema_tables)}个表)")
        for table in schema_tables:
            full_table_name = f"{schema_name}.{table['tableName']}" if schema_name != "public" else table['tableName']
            print(f"     - {full_table_name}")

def main():
    """主函数"""
    print("PostgreSQL SQL 导出功能测试")
    print("=" * 50)
    
    # 登录
    token = login()
    if not token:
        sys.exit(1)
    
    print(f"登录成功，Token: {token[:20]}...")
    
    # 测试MySQL SQL导出
    test_mysql_sql_export(token)
    
    print("\n")
    
    # 创建模拟PostgreSQL数据并测试
    mock_structure = create_mock_postgresql_data(token)
    test_postgresql_sql_generation(mock_structure)
    
    print("\n测试完成！")

if __name__ == "__main__":
    main()
