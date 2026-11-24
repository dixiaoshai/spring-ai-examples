#!/bin/bash

# 更新所有 pom.xml 文件中的 Spring AI 版本
# 用法：./scripts/update-spring-ai-version.sh [VERSION]
# 示例：./scripts/update-spring-ai-version.sh 1.0.1
# 示例：./scripts/update-spring-ai-version.sh 1.1.0-SNAPSHOT
# 示例：./scripts/update-spring-ai-version.sh 2.0.0-RC1

set -e

# 获取版本参数，或使用默认值
VERSION="${1:-2.0.0-RC1}"

# 校验版本格式
if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-(SNAPSHOT|RC[0-9]+|M[0-9]+))?$ ]]; then
    echo "Error: Invalid version format. Expected: X.Y.Z, X.Y.Z-SNAPSHOT, X.Y.Z-RC1, or X.Y.Z-M1"
    echo "Example: 2.0.0 or 2.0.0-SNAPSHOT or 2.0.0-RC1 or 2.0.0-M1"
    exit 1
fi

echo "Updating Spring AI version to: $VERSION"

# 查找脚本目录和项目根目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# 切换到项目根目录
cd "${PROJECT_ROOT}"

# 创建带时间戳的备份目录
BACKUP_DIR="${PROJECT_ROOT}/.version-backups/$(date +%Y%m%d_%H%M%S)"
mkdir -p "${BACKUP_DIR}"

# 已更新文件的计数器
UPDATED_COUNT=0
TOTAL_COUNT=0

# 查找并处理所有 pom.xml 文件
echo "Searching for pom.xml files..."
while IFS= read -r pom_file; do
    # 跳过备份目录
    if [[ "$pom_file" == *".version-backups"* ]]; then
        continue
    fi
    
    TOTAL_COUNT=$((TOTAL_COUNT + 1))
    updated_file=false
    
    # 检查该 pom 文件是否包含 spring-ai.version 属性
    if grep -q "<spring-ai.version>" "$pom_file"; then
        # 创建备份
        relative_path="${pom_file#./}"
        backup_file="${BACKUP_DIR}/${relative_path}"
        mkdir -p "$(dirname "$backup_file")"
        cp "$pom_file" "$backup_file"
        
        # 更新版本属性
        sed -i '' "s|<spring-ai.version>.*</spring-ai.version>|<spring-ai.version>${VERSION}</spring-ai.version>|g" "$pom_file"
        updated_file=true
    fi
    
    # 同时检查 dependencyManagement 中直接指定的 spring-ai-bom 版本
    if grep -q "spring-ai-bom" "$pom_file"; then
        if ! $updated_file; then
            # 如果尚未备份，则创建备份
            relative_path="${pom_file#./}"
            backup_file="${BACKUP_DIR}/${relative_path}"
            mkdir -p "$(dirname "$backup_file")"
            cp "$pom_file" "$backup_file"
        fi
        
        # 使用 perl 更新 BOM 版本（跨平台多行替换）
        perl -i -0pe 's|(<artifactId>spring-ai-bom</artifactId>\s*<version>)[^<]*(</version>)|${1}'"${VERSION}"'${2}|g' "$pom_file"
        
        updated_file=true
    fi
    
    if $updated_file; then
        echo "  ✓ Updated: ${pom_file#./}"
        UPDATED_COUNT=$((UPDATED_COUNT + 1))
    fi
done < <(find . -name "pom.xml" -type f)

echo ""
echo "Summary:"
echo "  Total pom.xml files found: $TOTAL_COUNT"
echo "  Files updated: $UPDATED_COUNT"
echo "  Backup location: $BACKUP_DIR"
echo ""

# 校验更新结果
echo "Verification - checking a sample file:"
sample_file="./misc/openai-streaming-response/pom.xml"
if [ -f "$sample_file" ]; then
    grep "<spring-ai.version>" "$sample_file" || echo "Sample file not found or doesn't contain spring-ai.version"
fi

echo ""
echo "✅ Spring AI version update complete!"
echo ""
echo "To restore from backup, run:"
echo "  ./scripts/restore-spring-ai-version.sh $BACKUP_DIR"