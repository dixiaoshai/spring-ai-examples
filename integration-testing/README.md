# Spring AI Examples - 集成测试框架

本目录包含 Spring AI Examples 的完整集成测试框架。该框架提供工具、文档与基础设施，用于跨不同复杂度层级可靠地测试所有 Spring AI 示例。

## 目录结构

```
integration-testing/
├── scripts/                    # Integration testing scripts (2 active tools)
│   ├── run-integration-tests.sh  # ⭐ PRIMARY: Direct test runner (recommended)
│   └── scaffold_integration_test.py  # ⭐ PRIMARY: Creates integration tests for new examples
├── jbang-lib/                 # Centralized JBang utilities
│   └── IntegrationTestUtils.java  # Common testing functionality
├── docs/                      # Integration testing documentation
│   └── README.md             # Detailed integration testing guide
└── logs/                     # Centralized log storage
    ├── background-runs/      # Main test run logs from rit-direct.sh
    └── integration-tests/    # Individual JBang test logs
```

## 快速开始

### 运行全部集成测试
```bash
# Recommended: Direct execution with reliable port cleanup
./integration-testing/scripts/run-integration-tests.sh

# Alternative: Use the legacy Python orchestrator (may have hanging issues - use run-integration-tests.sh instead)
# python3 integration-testing/scripts/run_integration_tests.py  # REMOVED - obsolete
```

### 创建新的集成测试
**⭐ 将框架扩展到新示例的必备工具**
```bash
# Creates both ExampleInfo.json and Run*.java files with proper templates
python3 integration-testing/scripts/scaffold_integration_test.py <module-path> [--complexity simple|complex|mcp]

# Examples:
python3 integration-testing/scripts/scaffold_integration_test.py kotlin/kotlin-hello-world --complexity simple
python3 integration-testing/scripts/scaffold_integration_test.py agentic-patterns/new-workflow --complexity complex
python3 integration-testing/scripts/scaffold_integration_test.py model-context-protocol/new-server --complexity mcp
```

### 测试单个示例
```bash
cd <module-directory>
jbang integration-tests/Run*.java
```

## 关键特性

### ✅ **100% 可靠的测试执行**
- **端口清理**：系统性清理 8080 端口，防止级联失败
- **串行执行**：避免 Spring Boot 应用之间的资源冲突
- **完善日志**：持久化日志保存在 `integration-testing/logs/` 中以便调试

### ✅ **开发者体验**
- **独立日志文件**：每个测试都会生成带时间戳的日志，便于调试
- **进度提示**：为长时间运行的测试提供实时反馈
- **人工验证**：展示捕获的应用输出，供“肉眼”校验

### ✅ **测试校验质量**
- **功能校验**：测试检查实际功能，而不仅是启动消息
- **错误检测**：完善的日志能揭示被通过的正则所掩盖的功能性失败
- **模式模板**：为不同示例类型提供经过验证的成功模式

## 按示例类型划分的测试方式

| 示例类型 | 测试位置 | 执行时间 | 校验策略 |
|--------------|---------------|----------------|-------------------|
| **简单** | `src/test/java/` | ~30s | 单元测试 + 基础冒烟测试 |
| **复杂** | `integration-tests/` | ~25-50s | 端到端测试 + 全面的模式匹配 |
| **MCP** | `integration-tests/` | ~50s+ | 借助外部服务的多阶段校验 |

## 框架架构

### **第 1 阶段**：基础设施（✅ 完成）
- Python 编排 + JBang 执行 + JSON 配置
- 跨平台兼容，并具备完善的错误处理
- 基于模板的脚手架，适配不同复杂度层级

### **第 2 阶段**：模式校验（✅ 完成）
- 覆盖所有复杂度层级的试点转换
- 基于内容的成功模式（而不仅是构建产物）
- 使用 `-DskipTests` 优化构建命令以提升性能

### **第 3a 阶段**：关键基础设施（✅ 完成）
- **第 3a.1 阶段**：通过系统性端口清理实现 100% 测试通过率
- **第 3a.2 阶段**：完善日志基础设施的实现 ✅
- **第 3a.3 阶段**：集中式目录结构重组 ✅
- **第 3a.4 阶段**：系统性日志修复 + 功能校验 ✅
- **第 3a.5 阶段**：增强日志与脚本改进 ✅
- **第 3a.6 阶段**：JBang 工具类集中化 ✅

## 集中式 JBang 架构

### **第 3a.6 阶段成果**：零代码重复
全部 18 个 JBang 集成测试脚本现在都使用集中式工具类：

- **之前**：每个脚本包含约 110-130 行重复代码
- **之后**：每个脚本缩减到约 18 行，使用集中式工具类
- **代码缩减**：代码行数减少 84%
- **重复**：0% - 所有逻辑集中于 IntegrationTestUtils.java
- **维护**：缺陷修复与增强只需在一处进行

### **架构组件**

1. **IntegrationTestUtils.java**（`jbang-lib/`）
   - 配置加载（解析 ExampleInfo.json）
   - 环境变量校验
   - 针对不同模块深度的动态路径解析
   - 带超时处理的命令执行
   - 标准化日志文件的创建与管理
   - Spring Boot 构建与运行的编排
   - 输出模式校验
   - 安装/清理命令的执行

2. **精简后的 JBang 脚本**
   ```java
   ///usr/bin/env jbang "$0" "$@" ; exit $?
   //DEPS org.zeroturnaround:zt-exec:1.12
   //DEPS com.fasterxml.jackson.core:jackson-databind:2.17.1
   //JAVA 17
   //FILES ExampleInfo.json
   //SOURCES ../../../integration-testing/jbang-lib/IntegrationTestUtils.java

   public class RunExample {
       public static void main(String... args) throws Exception {
           IntegrationTestUtils.runIntegrationTest("example-name");
       }
   }
   ```

## 日志架构

### **集中式日志**：`integration-testing/logs/`
```
logs/
├── background-runs/           # Main test execution logs
│   └── run-integration-tests_YYYYMMDD_HHMMSS.log
└── integration-tests/         # Individual Spring Boot application logs
    └── MODULE-spring-boot-TIMESTAMP.log
```

### **持久化日志模板**（JBang 脚本）
```java
// Create persistent log file for debugging
Path logDir = Paths.get("../../integration-testing/logs/integration-tests");
Files.createDirectories(logDir);
Path logFile = logDir.resolve("MODULE-spring-boot-" + System.currentTimeMillis() + ".log");

// Keep log file for debugging - DO NOT DELETE
out.println("📁 Spring Boot log preserved: " + logFile.toAbsolutePath());
```

## 最佳实践

### ✅ **有效模式**
1. **系统性端口清理**：在每个测试前后清理 8080 端口
2. **持久化日志文件**：在可预测的位置使用带时间戳的日志
3. **逐个测试校验**：单独运行测试以隔离问题
4. **完整输出展示**：展示捕获的应用输出以供人工验证
5. **串行执行**：以可靠的执行顺序防止资源冲突

### ❌ **应避免的反模式**
1. **临时文件日志**：绝不要使用 `Files.createTempFile()` 并删除日志
2. **仅靠正则校验**：不要只依赖成功模式而不检查 ERROR
3. **无约束的并行执行**：避免在没有资源管理的情况下并行测试
4. **重模式匹配而轻功能**：应校验应用的实际功能
5. **缺少独立日志文件**：始终保留详细日志以便调试

## 故障排查

### **常见问题**

**端口冲突**：
```bash
# Clean up hanging processes
lsof -ti:8080 | xargs kill -9
```

**缺少环境变量**：
```bash
export OPENAI_API_KEY="your-key-here"
export BRAVE_API_KEY="your-brave-key"  # For Brave examples
```

**集成测试超时**：
- 检查 `ExampleInfo.json` 的超时设置
- 确认外部服务可访问
- 查看 `integration-testing/logs/` 中的完整日志

## 文档

详细集成测试指南见：`integration-testing/docs/README.md`

## 框架状态

**✅ 生产就绪**：24 个模块带集成测试  
**✅ 测试覆盖**：全部 24 个模块以 100% 的可靠性通过（正则校验）  
**✅ 完善日志**：已实现完整的调试能力  
**✅ 开发者体验**：实时进度与持久化日志  
**✅ 零重复**：所有 JBang 脚本都使用集中式工具类  
**✅ 版本管理**：支持在多个 Spring AI 版本下测试

## 测试统计
- **带集成测试的模块总数**：24
- **使用 spring-ai.version 属性的模块**：17  
- **直接使用 BOM 版本的模块**：7
- **管理的 pom.xml 文件总数**：32