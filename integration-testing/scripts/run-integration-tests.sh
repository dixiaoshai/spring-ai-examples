#!/bin/bash

# Direct RIT - 直接通过 JBang 运行集成测试
# 完全绕过 Python 脚本，以避免挂起问题
#
# 用法：
#   ./run-integration-tests.sh                  # 运行全部测试
#   ./run-integration-tests.sh --clean-logs     # 清理所有日志并运行全部测试
#   ./run-integration-tests.sh helloworld       # 仅运行匹配 "helloworld" 的测试
#   ./run-integration-tests.sh --clean-logs helloworld  # 清理日志并运行指定测试

# 命令失败时不退出（让各测试自行失败）
set -uo pipefail

# 查找脚本目录与项目根目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

# 切换到项目根目录以确保行为一致
cd "${PROJECT_ROOT}"

# 解析参数
CLEAN_LOGS=false
TEST_FILTER=""

for arg in "$@"; do
    case $arg in
        --clean-logs)
            CLEAN_LOGS=true
            shift
            ;;
        *)
            TEST_FILTER="$arg"
            ;;
    esac
done

LOGS_BASE_DIR="integration-testing/logs"
LOGS_DIR="${LOGS_BASE_DIR}/background-runs"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="${PROJECT_ROOT}/${LOGS_DIR}/run-integration-tests_${TIMESTAMP}.log"

mkdir -p "${LOGS_DIR}"

# 如需要则清理所有日志
if [ "$CLEAN_LOGS" = true ]; then
    echo "🧹 Cleaning all logs..."
    rm -rf "${LOGS_BASE_DIR}"/*
    mkdir -p "${LOGS_DIR}"
    mkdir -p "${LOGS_BASE_DIR}/integration-tests"
fi

# 先创建日志文件
touch "${LOG_FILE}"

echo "🚀 Starting Spring AI Examples Integration Tests (Direct Mode)"
if [ -n "$TEST_FILTER" ]; then
    echo "🔍 Filter: $TEST_FILTER"
fi
echo "📝 Log file: ${LOG_FILE}"
echo "==============================================="

# 清理上次运行的旧日志（但不含当前日志文件）
echo "🧹 Cleaning up old run logs..." | tee -a "${LOG_FILE}"
find "${LOGS_DIR}" -name "*.log" -not -name "$(basename "${LOG_FILE}")" -delete 2>/dev/null || true

# 端口清理 - 结束占用 8080 端口的进程以防止冲突
echo "🔧 Cleaning up port 8080..." | tee -a "${LOG_FILE}"
if lsof -ti:8080 >/dev/null 2>&1; then
    echo "  Found processes using port 8080, terminating..." | tee -a "${LOG_FILE}"
    lsof -ti:8080 | xargs kill -9 2>/dev/null || true
    sleep 2  # 给进程留出清理时间
    echo "  Port 8080 cleanup completed" | tee -a "${LOG_FILE}"
else
    echo "  Port 8080 is free" | tee -a "${LOG_FILE}"
fi

# 查找所有 JBang 集成测试脚本
if [ -n "$TEST_FILTER" ]; then
    # 按提供的模式过滤脚本
    jbang_scripts=($(find . -name "Run*.java" -path "*/integration-tests/*" | grep -i "$TEST_FILTER" | sort))
else
    # 查找所有脚本
    jbang_scripts=($(find . -name "Run*.java" -path "*/integration-tests/*" | sort))
fi

echo "Found ${#jbang_scripts[@]} JBang integration test scripts" | tee -a "${LOG_FILE}"
echo "" | tee -a "${LOG_FILE}"

passed=0
failed=0
failed_tests=()

for script in "${jbang_scripts[@]}"; do
    test_dir=$(dirname "${script}")
    module_dir=$(dirname "${test_dir}")
    module_name=$(basename "${module_dir}")
    parent_name=$(basename $(dirname "${module_dir}"))
    full_name="${parent_name}/${module_name}"
    
    # 创建单个测试的日志文件
    individual_log="${LOGS_DIR}/test_${parent_name}_${module_name}_${TIMESTAMP}.log"
    
    echo "🔄 Testing ${full_name}..." | tee -a "${LOG_FILE}"
    echo "   Script: ${script}" | tee -a "${LOG_FILE}"
    echo "   Individual log: ${individual_log}" | tee -a "${LOG_FILE}"
    
    # 运行 JBang 脚本，同时写入主日志与单个日志
    if (cd "${module_dir}" && timeout 300s jbang "integration-tests/$(basename "${script}")") 2>&1 | tee "${individual_log}" >> "${LOG_FILE}"; then
        echo "✅ ${full_name} - PASSED" | tee -a "${LOG_FILE}"
        echo "   ✅ Individual log: ${individual_log}" | tee -a "${LOG_FILE}"
        ((passed++))
    else
        exit_code=$?
        if [ ${exit_code} -eq 124 ]; then
            echo "⏰ ${full_name} - TIMEOUT" | tee -a "${LOG_FILE}"
        else
            echo "❌ ${full_name} - FAILED (exit code: ${exit_code})" | tee -a "${LOG_FILE}"
        fi
        echo "   ❌ Individual log: ${individual_log}" | tee -a "${LOG_FILE}"
        ((failed++))
        failed_tests+=("${full_name}")
    fi
    
    # 每个测试后清理 8080 端口上挂起的进程
    if lsof -ti:8080 >/dev/null 2>&1; then
        echo "🧹 Cleaning up hanging processes on port 8080..." | tee -a "${LOG_FILE}"
        lsof -ti:8080 | xargs kill -9 2>/dev/null || true
        sleep 1
    fi
    
    echo "" | tee -a "${LOG_FILE}"
done

echo "===============================================" | tee -a "${LOG_FILE}"
echo "📊 Results:" | tee -a "${LOG_FILE}"
echo "  Total: $((passed + failed))" | tee -a "${LOG_FILE}"
echo "  Passed: ${passed}" | tee -a "${LOG_FILE}"
echo "  Failed: ${failed}" | tee -a "${LOG_FILE}"

if [ ${failed} -gt 0 ]; then
    echo "" | tee -a "${LOG_FILE}"
    echo "💥 Failed tests:" | tee -a "${LOG_FILE}"
    for test in "${failed_tests[@]}"; do
        echo "  - ${test}" | tee -a "${LOG_FILE}"
    done
fi

echo "" | tee -a "${LOG_FILE}"
echo "📝 Full log: ${LOG_FILE}"

# 以正确的退出码退出
if [ ${failed} -gt 0 ]; then
    echo "❌ Testing completed with failures!"
    exit 1
else
    echo "✅ Testing completed!"
    exit 0
fi