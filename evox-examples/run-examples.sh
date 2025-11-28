#!/bin/bash

# EvoX 示例应用快速运行脚本

echo "========================================="
echo "  EvoX 示例应用运行脚本"
echo "========================================="
echo ""

# 检查 Maven
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven 未安装，请先安装 Maven"
    exit 1
fi

echo "✅ Maven 已安装"
echo ""

# 进入示例目录
cd "$(dirname "$0")"

# 显示菜单
echo "请选择要运行的示例:"
echo ""
echo "1. 优化器示例 (SimpleOptimizerExample)"
echo "   - 展示 TextGrad, MIPRO, AFlow 三种优化器"
echo ""
echo "2. HITL 审批示例 (EmailSendingWithApprovalExample)"
echo "   - 展示邮件发送前的人工审批流程"
echo ""
echo "3. 编译所有模块"
echo ""
echo "4. 运行所有测试"
echo ""
echo "0. 退出"
echo ""

read -p "请输入选项 (0-4): " choice

case $choice in
    1)
        echo ""
        echo "========================================="
        echo "  运行优化器示例"
        echo "========================================="
        echo ""
        echo "提示: 这是简化示例，使用模拟数据"
        echo ""
        mvn clean compile exec:java \
            -Dexec.mainClass="io.leavesfly.evox.examples.optimizer.SimpleOptimizerExample"
        ;;
    2)
        echo ""
        echo "========================================="
        echo "  运行 HITL 审批示例"
        echo "========================================="
        echo ""
        echo "提示: 运行过程中会提示您批准或拒绝操作"
        echo "      请输入 'a' 批准 或 'r' 拒绝"
        echo ""
        mvn clean compile exec:java \
            -Dexec.mainClass="io.leavesfly.evox.examples.hitl.EmailSendingWithApprovalExample"
        ;;
    3)
        echo ""
        echo "========================================="
        echo "  编译所有模块"
        echo "========================================="
        echo ""
        cd ..
        mvn clean compile -DskipTests
        ;;
    4)
        echo ""
        echo "========================================="
        echo "  运行所有测试"
        echo "========================================="
        echo ""
        cd ..
        mvn clean test
        ;;
    0)
        echo "退出"
        exit 0
        ;;
    *)
        echo "❌ 无效选项"
        exit 1
        ;;
esac

echo ""
echo "========================================="
echo "  完成"
echo "========================================="
