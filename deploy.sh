#!/bin/bash

# Docker部署脚本
# 使用方法: ./deploy.sh [start|stop|restart|logs|status]

set -e

COMPOSE_FILE="docker-compose.yml"

function check_docker() {
    if ! command -v docker &> /dev/null; then
        echo "错误: Docker未安装，请先安装Docker"
        exit 1
    fi
    
    if ! docker info &> /dev/null; then
        echo "错误: Docker守护进程未运行，请启动Docker"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        echo "错误: docker-compose未安装，请先安装docker-compose"
        exit 1
    fi
}

function start_services() {
    echo "启动所有服务..."
    docker-compose up -d
    echo "服务启动完成！"
    echo ""
    echo "访问地址："
    echo "  前端: http://localhost"
    echo "  后端API: http://localhost:8081"
    echo "  数据库: localhost:3306"
    echo ""
    echo "使用 './deploy.sh logs' 查看日志"
    echo "使用 './deploy.sh status' 查看服务状态"
}

function stop_services() {
    echo "停止所有服务..."
    docker-compose down
    echo "服务已停止"
}

function restart_services() {
    echo "重启所有服务..."
    docker-compose down
    docker-compose up -d
    echo "服务重启完成！"
}

function show_logs() {
    docker-compose logs -f
}

function show_status() {
    echo "服务状态："
    docker-compose ps
    echo ""
    echo "网络状态："
    docker network ls | grep db-record || echo "未找到db-record网络"
    echo ""
    echo "卷状态："
    docker volume ls | grep db-record || echo "未找到db-record卷"
}

function build_images() {
    echo "构建Docker镜像..."
    docker-compose build
    echo "镜像构建完成！"
}

function main() {
    check_docker
    
    case "${1:-start}" in
        start)
            start_services
            ;;
        stop)
            stop_services
            ;;
        restart)
            restart_services
            ;;
        logs)
            show_logs
            ;;
        status)
            show_status
            ;;
        build)
            build_images
            ;;
        help|--help|-h)
            echo "用法: $0 [start|stop|restart|logs|status|build|help]"
            echo ""
            echo "命令说明："
            echo "  start   - 启动所有服务（默认）"
            echo "  stop    - 停止所有服务"
            echo "  restart - 重启所有服务"
            echo "  logs    - 查看服务日志"
            echo "  status  - 查看服务状态"
            echo "  build   - 构建Docker镜像"
            echo "  help    - 显示此帮助信息"
            ;;
        *)
            echo "错误: 未知命令 '$1'"
            echo "使用 '$0 help' 查看帮助信息"
            exit 1
            ;;
    esac
}

main "$@"