#!/bin/bash

# KingbaseES 完整功能测试脚本
# 测试项目创建、数据库版本创建、对比和 SQL 导出功能

BASE_URL="http://localhost:8081"
USERNAME="admin"
PASSWORD="zd@123"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 获取 token
get_token() {
    log_info "正在登录获取 token..."
    local response=$(curl -s -X POST "$BASE_URL/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}")
    
    local token=$(echo $response | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    
    if [ -n "$token" ]; then
        log_success "登录成功，获取到 token"
        echo $token
    else
        log_error "登录失败: $response"
        exit 1
    fi
}

# 创建 KingbaseES 数据源
create_datasource() {
    local token=$1
    log_info "正在创建 KingbaseES 数据源..."
    
    local response=$(curl -s -X POST "$BASE_URL/api/datasource/create" \
        -H "Authorization: Bearer $token" \
        -H "Content-Type: application/json" \
        -d '{
            "name": "KingbaseES测试数据源",
            "type": "kingbase",
            "host": "172.22.37.139",
            "port": 54321,
            "databaseName": "experiment_user",
            "username": "kingbase",
            "password": "Kingbase@2506_16",
            "description": "KingbaseES数据源测试"
        }')
    
    local datasource_id=$(echo $response | grep -o '"id":[0-9]*' | cut -d':' -f2)
    
    if [ -n "$datasource_id" ]; then
        log_success "数据源创建成功，ID: $datasource_id"
        echo $datasource_id
    else
        log_error "数据源创建失败: $response"
        exit 1
    fi
}

# 测试数据源连接
test_datasource_connection() {
    local token=$1
    local datasource_id=$2
    log_info "正在测试数据源连接..."
    
    local response=$(curl -s -X POST "$BASE_URL/api/datasource/test" \
        -H "Authorization: Bearer $token" \
        -H "Content-Type: application/json" \
        -d "{
            \"id\": $datasource_id,
            \"name\": \"KingbaseES测试数据源\",
            \"type\": \"kingbase\",
            \"host\": \"172.22.37.139\",
            \"port\": 54321,
            \"databaseName\": \"experiment_user\",
            \"username\": \"kingbase\",
            \"password\": \"Kingbase@2506_16\"
        }")
    
    local status=$(echo $response | grep -o '"status":[^,]*' | cut -d':' -f2)
    
    if [ "$status" = "true" ]; then
        log_success "数据源连接测试成功"
    else
        log_error "数据源连接测试失败: $response"
        exit 1
    fi
}

# 创建项目
create_project() {
    local token=$1
    local datasource_id=$2
    log_info "正在创建项目..."
    
    local response=$(curl -s -X POST "$BASE_URL/api/project/create" \
        -H "Authorization: Bearer $token" \
        -H "Content-Type: application/json" \
        -d "{
            \"name\": \"KingbaseES测试项目\",
            \"description\": \"用于测试KingbaseES数据源的完整功能\",
            \"datasourceId\": $datasource_id
        }")
    
    local project_id=$(echo $response | grep -o '"id":[0-9]*' | cut -d':' -f2)
    
    if [ -n "$project_id" ]; then
        log_success "项目创建成功，ID: $project_id"
        echo $project_id
    else
        log_error "项目创建失败: $response"
        exit 1
    fi
}

# 创建数据库版本
create_version() {
    local token=$1
    local project_id=$2
    local version_name=$3
    log_info "正在创建数据库版本: $version_name"
    
    local response=$(curl -s -X POST "$BASE_URL/api/project-version/create" \
        -H "Authorization: Bearer $token" \
        -H "Content-Type: application/json" \
        -d "{
            \"projectId\": $project_id,
            \"versionName\": \"$version_name\",
            \"description\": \"KingbaseES数据库版本 - $version_name\"
        }")
    
    local version_id=$(echo $response | grep -o '"id":[0-9]*' | cut -d':' -f2)
    
    if [ -n "$version_id" ]; then
        log_success "版本创建成功，ID: $version_id"
        echo $version_id
    else
        log_error "版本创建失败: $response"
        exit 1
    fi
}

# 注意：数据库结构现在在创建版本时自动捕获，无需单独调用
# 如果需要重新捕获，可以删除版本重新创建

# 获取版本详情
get_version_detail() {
    local token=$1
    local version_id=$2
    log_info "正在获取版本详情..."
    
    local response=$(curl -s -X GET "$BASE_URL/api/project-version/detail/$version_id" \
        -H "Authorization: Bearer $token")
    
    local status=$(echo $response | grep -o '"status":[^,]*' | cut -d':' -f2)
    
    if [ "$status" = "true" ]; then
        log_success "版本详情获取成功"
        echo $response | jq '.' 2>/dev/null || echo $response
    else
        log_error "版本详情获取失败: $response"
    fi
}

# 获取版本结构
get_version_structure() {
    local token=$1
    local version_id=$2
    log_info "正在获取版本结构..."
    
    local response=$(curl -s -X GET "$BASE_URL/api/project-version/structure/$version_id" \
        -H "Authorization: Bearer $token")
    
    local status=$(echo $response | grep -o '"status":[^,]*' | cut -d':' -f2)
    
    if [ "$status" = "true" ]; then
        log_success "版本结构获取成功"
        # 提取表数量信息
        local table_count=$(echo $response | grep -o '"tableName"' | wc -l)
        log_info "发现 $table_count 个表"
    else
        log_error "版本结构获取失败: $response"
    fi
}

# 导出 SQL
export_sql() {
    local token=$1
    local version_id=$2
    log_info "正在导出 SQL..."
    
    local response=$(curl -s -X GET "$BASE_URL/api/project-version/export-sql/$version_id" \
        -H "Authorization: Bearer $token")
    
    local status=$(echo $response | grep -o '"status":[^,]*' | cut -d':' -f2)
    
    if [ "$status" = "true" ]; then
        log_success "SQL 导出成功"
        
        # 保存 SQL 到文件
        local sql_content=$(echo $response | grep -o '"sql":"[^"]*"' | cut -d'"' -f4 | sed 's/\\n/\n/g' | sed 's/\\"/"/g')
        echo "$sql_content" > "kingbase_export_$(date +%Y%m%d_%H%M%S).sql"
        log_success "SQL 已保存到文件"
        
        # 显示 SQL 预览
        echo -e "\n${BLUE}=== SQL 预览 ===${NC}"
        echo "$sql_content" | head -20
        echo "..."
        echo -e "${BLUE}=== SQL 预览结束 ===${NC}\n"
    else
        log_error "SQL 导出失败: $response"
    fi
}

# 版本对比（如果有两个版本）
compare_versions() {
    local token=$1
    local version1_id=$2
    local version2_id=$3
    log_info "正在对比版本 $version1_id 和 $version2_id..."
    
    local response=$(curl -s -X GET "$BASE_URL/api/project-version/compare/$version1_id/$version2_id" \
        -H "Authorization: Bearer $token")
    
    local status=$(echo $response | grep -o '"status":[^,]*' | cut -d':' -f2)
    
    if [ "$status" = "true" ]; then
        log_success "版本对比成功"
        echo $response | jq '.' 2>/dev/null || echo $response
    else
        log_warning "版本对比失败或无差异: $response"
    fi
}

# 主测试流程
main() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}  KingbaseES 完整功能测试开始${NC}"
    echo -e "${BLUE}========================================${NC}\n"
    
    # 1. 登录获取 token
    TOKEN=$(get_token)
    
    # 2. 创建数据源
    DATASOURCE_ID=$(create_datasource $TOKEN)
    
    # 3. 测试数据源连接
    test_datasource_connection $TOKEN $DATASOURCE_ID
    
    # 4. 创建项目
    PROJECT_ID=$(create_project $TOKEN $DATASOURCE_ID)
    
    # 5. 创建第一个版本（数据库结构会自动捕获）
    VERSION1_ID=$(create_version $TOKEN $PROJECT_ID "v1.0-initial")
    
    # 7. 获取版本详情
    get_version_detail $TOKEN $VERSION1_ID
    
    # 8. 获取版本结构
    get_version_structure $TOKEN $VERSION1_ID
    
    # 9. 导出 SQL
    export_sql $TOKEN $VERSION1_ID
    
    # 10. 创建第二个版本（用于对比测试）
    log_info "等待 5 秒后创建第二个版本..."
    sleep 5
    VERSION2_ID=$(create_version $TOKEN $PROJECT_ID "v1.1-update")
    
    # 11. 版本对比
    compare_versions $TOKEN $VERSION1_ID $VERSION2_ID
    
    echo -e "\n${GREEN}========================================${NC}"
    echo -e "${GREEN}  KingbaseES 完整功能测试完成${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}项目 ID: $PROJECT_ID${NC}"
    echo -e "${GREEN}数据源 ID: $DATASOURCE_ID${NC}"
    echo -e "${GREEN}版本1 ID: $VERSION1_ID${NC}"
    echo -e "${GREEN}版本2 ID: $VERSION2_ID${NC}"
}

# 执行主流程
main
