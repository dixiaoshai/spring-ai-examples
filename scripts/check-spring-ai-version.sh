#!/bin/bash

# 检查所有 pom.xml 文件中的 Spring AI 版本
# 用法：./scripts/check-spring-ai-version.sh

set -e

echo "Checking Spring AI versions in all pom.xml files..."
echo ""

# 查找脚本目录和项目根目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# 切换到项目根目录
cd "${PROJECT_ROOT}"

# 统计发现的版本
declare -A VERSION_COUNT
TOTAL_WITH_VERSION=0
TOTAL_WITHOUT_VERSION=0

# 查找并检查所有 pom.xml 文件（排除备份）
echo "Files with spring-ai.version property:"
echo "----------------------------------------"
while IFS= read -r pom_file; do
    if grep -q "<spring-ai.version>" "$pom_file"; then
        version=$(grep "<spring-ai.version>" "$pom_file" | sed 's/.*<spring-ai.version>\(.*\)<\/spring-ai.version>.*/\1/')
        relative_path="${pom_file#./}"
        echo "  $version - $relative_path"
        
        # 统计该版本
        if [ -n "$version" ]; then
            VERSION_COUNT["$version"]=$((${VERSION_COUNT["$version"]:-0} + 1))
            TOTAL_WITH_VERSION=$((TOTAL_WITH_VERSION + 1))
        fi
    else
        TOTAL_WITHOUT_VERSION=$((TOTAL_WITHOUT_VERSION + 1))
    fi
done < <(find . -name "pom.xml" -type f -not -path "./.version-backups/*" | sort)

echo ""
echo "Version Summary:"
echo "----------------"
for version in "${!VERSION_COUNT[@]}"; do
    echo "  $version: ${VERSION_COUNT[$version]} files"
done

echo ""
echo "Statistics:"
echo "-----------"
echo "  Total pom.xml files: $((TOTAL_WITH_VERSION + TOTAL_WITHOUT_VERSION))"
echo "  Files with spring-ai.version: $TOTAL_WITH_VERSION"
echo "  Files without spring-ai.version: $TOTAL_WITHOUT_VERSION"

# 检查版本是否不一致
if [ ${#VERSION_COUNT[@]} -gt 1 ]; then
    echo ""
    echo "⚠️  WARNING: Multiple versions detected! Consider running:"
    echo "    ./scripts/update-spring-ai-version.sh <desired-version>"
else
    echo ""
    echo "✅ All files use the same Spring AI version"
fi