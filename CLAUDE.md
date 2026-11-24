# CLAUDE.md

本文件为 Claude Code（claude.ai/code）在本仓库中处理代码时提供指导。

## 仓库概览

本仓库是一组 Spring AI 示例，演示各种 AI 集成模式与用例。仓库以多模块 Maven 项目组织，示例覆盖：

- **智能体模式（Agentic Patterns）**：链式工作流、编排器—工作者、并行化、路由、评估器—优化器等模式
- **模型上下文协议（MCP）**：内容丰富的 MCP 客户端与服务器示例，包括 SQLite、文件系统、天气、Web 搜索与注解
- **Kotlin 示例**：Kotlin 版 Hello World、函数回调与 RAG 实现
- **jinfer（`jinfer/`）**：基于 Qxotic jinfer（聊天、工具搜索、转录）的 JVM 内本地模型；需要 Java 25，由根目录的 `java25` profile 构建
- **其他**：函数回调、流式响应与提示词工程模式

## 构建系统与命令

本项目使用 Maven 与 Maven Wrapper（mvnw）。每个模块都可以独立构建和运行。

### 常用开发命令

**构建整个项目：**
```bash
./mvnw clean package
```

**运行指定示例：**
```bash
./run-example.sh <project-directory-name>
# Example: ./run-example.sh agentic-patterns/chain-workflow
```

**构建并运行单个模块：**
```bash
cd <module-directory>
./mvnw clean package
./mvnw spring-boot:run
```

**从根目录构建（所有模块）：**
```bash
mvn clean package
```

### 版本管理

**更新所有模块的 Spring AI 版本：**
```bash
./scripts/update-spring-ai-version.sh 1.0.1  # or 1.1.0-SNAPSHOT
```

版本管理脚本会处理两种模式：
- **17 个模块**使用 `<spring-ai.version>` 属性
- **15 个模块**在 dependencyManagement 中直接指定 BOM 版本
- 全仓库共更新 **32 个 pom.xml 文件**

**查看当前版本：**
```bash
./scripts/check-spring-ai-version.sh
```

**从备份恢复：**
```bash
./scripts/restore-spring-ai-version.sh /path/to/backup
```

## 架构

### 模块结构
- 每个示例都是自包含的 Spring Boot 应用
- 模块遵循标准 Maven 目录结构：`src/main/java` 与 `src/test/java`
- 通用 Spring AI 版本：`2.1.0-SNAPSHOT`
- Java 版本：17（document-forge 需要 21）
- Spring Boot parent：`4.1.1`

### 主要模块分类

**智能体模式（`agentic-patterns/`）：**
- 演示 AI 智能体工作流模式
- 每个模式都是独立模块，拥有自己的 Application 类

**模型上下文协议（`model-context-protocol/`）：**
- MCP 客户端与服务器实现
- 覆盖多种协议：SQLite、文件系统访问、天气 API、Web 搜索
- 同时包含手动实现与基于 starter 的实现
- 服务器类型：WebMVC、WebFlux 与 STDIO

**Kotlin 示例（`kotlin/`）：**
- 基于 Kotlin 的 Spring AI 实现
- 包含 RAG（检索增强生成）示例

### 配置模式
- 应用配置通常位于 `src/main/resources/application.properties` 或 `application.yaml`
- MCP 配置常使用 `mcp-servers-config.json`
- 复杂环境使用 Docker Compose 文件（例如 `compose.yml`）

## 测试框架

本仓库使用一套完整的集成测试框架，并通过 **AI 驱动的校验**确保所有示例在不同版本下都能正常工作。该框架目前覆盖 **24 个带集成测试的模块**，并为交互式应用提供智能校验。

### 测试方式

| 示例类型 | 测试位置 | 命令 | 用途 |
|--------------|---------------|---------|---------|
| **简单** | `src/test/java/` | `./mvnw test` | 单元测试、基本功能 |
| **复杂** | `integration-tests/` | `jbang integration-tests/Run*.java` | 带 AI 校验的端到端集成 |

### 主要特性

- **覆盖 24 个模块**：为所有主要示例类别提供集成测试
- **AI 校验**：对非确定性输出进行应用日志的智能分析
- **交互式应用支持**：测试聊天机器人与基于 Scanner 的应用
- **集中式架构**：单一事实来源，代码量减少 84%
- **完整日志**：保留应用全部输出以便调试

### 主要集成测试工具

该框架提供 **两个核心工具** 用于集成测试：

**1. ⭐ 运行全部集成测试（主测试运行器）：**
```bash
./integration-testing/scripts/run-integration-tests.sh
```

**运行指定集成测试：**
```bash
./integration-testing/scripts/run-integration-tests.sh module-name
```

**使用干净日志运行：**
```bash
./integration-testing/scripts/run-integration-tests.sh --clean-logs
```

**2. ⭐ 为新示例创建集成测试（脚手架工具）：**
```bash
# Essential for extending the framework - creates ExampleInfo.json + Run*.java files
python3 integration-testing/scripts/scaffold_integration_test.py <module-path> [--complexity simple|complex|mcp]
```

**创建带 AI 校验的集成测试：**
```bash
# Simple example (hybrid AI validation)
python3 integration-testing/scripts/scaffold_integration_test.py kotlin/kotlin-hello-world --complexity simple

# Complex workflow (primary AI validation)
python3 integration-testing/scripts/scaffold_integration_test.py agentic-patterns/chain-workflow --complexity complex

# Interactive application (fallback AI validation)
python3 integration-testing/scripts/scaffold_integration_test.py agents/reflection --complexity complex

# MCP example (hybrid validation)
python3 integration-testing/scripts/scaffold_integration_test.py model-context-protocol/weather/server --complexity complex
```

**测试指定示例：**
```bash
cd <module-directory>
jbang integration-tests/Run*.java  # For complex examples
./mvnw test                         # For simple examples
```

### 集成测试结构

复杂示例包含带 AI 校验的 `integration-tests/` 目录：
```
module/
├── integration-tests/
│   ├── ExampleInfo.json      # Test configuration with AI validation support
│   └── RunModule.java        # JBang launcher (uses centralized utilities)
```

### JBang 脚本模式与 AI 校验

所有集成测试脚本都使用带 AI 校验支持的集中式工具类：
- **集中式架构**：每个脚本仅约 18 行（代码量减少 84%）
- **AI 集成**：使用 Claude 自动进行 AI 校验与智能分析
- **通用支持**：所有测试逻辑位于 `integration-testing/jbang-lib/IntegrationTestUtils.java`
- **交互式应用**：针对基于 Scanner 的应用做特殊处理

**校验模式：**
- **Primary**：仅使用 AI 校验，适用于不可预测的 AI 输出
- **Hybrid**：正则表达式 + AI 校验，兼顾可靠性
- **Fallback**：以正则为主、AI 兜底，适用于交互式应用

完整指南见 `integration-testing/docs/README.md`，故障排查见 `integration-testing/docs/TROUBLESHOOTING.md`。

### 🤖 AI 校验（新功能）

集成测试框架现在包含基于 Claude 的 AI 驱动校验，可智能分析测试输出。它超越了正则表达式匹配，能够理解上下文、校验不可预测的 AI 输出并评估复杂工作流。

**主要优势：**
- **智能评估**：判断示例是否达成了预期目的
- **处理 AI 输出**：可校验笑话、对话与创意内容
- **上下文感知**：利用 README 文档作为校验上下文
- **多组件支持**：对分布式系统进行整体校验
- **成本高效**：每次校验约 400 tokens，缓存利用率高

**AI 校验的优势场景：**
- 聊天示例（笑话、对话、创意回复）
- 智能体模式（多步推理、工作流完成）
- MCP 示例（协议校验、工具发现）
- 函数调用（真实工具使用验证）
- 输出不可预测的复杂工作流

**配置示例：**
```json
{
  "timeoutSec": 300,
  "successRegex": ["BUILD SUCCESS", "Started.*Application"],
  "requiredEnv": ["OPENAI_API_KEY"],
  "aiValidation": {
    "enabled": true,
    "validationMode": "hybrid",
    "expectedBehavior": "Application should demonstrate chat functionality with coherent AI responses",
    "promptTemplate": "chat_example_validation"
  }
}
```

**校验模式：**
- `primary` - 仅 AI 校验（最适合不可预测的 AI 输出）
- `hybrid` - 正则与 AI 都必须通过（大多数场景推荐）
- `fallback` - 正则失败时再使用 AI 校验（渐进式迁移）

完整细节见 `integration-testing/docs/AI_VALIDATION.md`。

## 开发说明

- 使用集成测试框架对示例进行全面测试与校验
- 每个模块都有自己的 Maven Wrapper，便于隔离构建
- 示例同时演示 OpenAI 及其他 AI 模型的集成
- MCP 示例通常包含数据库初始化脚本（例如 `create-database.sh`）
- 复杂示例应包含集成测试，用于 CI/CD 校验

## 提交信息规范

**重要**：向本仓库提交代码时，提交信息应看起来由人类编写：

- ❌ **不要包含**：机器人 emoji（🤖）、“Generated with Claude Code”、“Co-Authored-By: Claude”，或任何 AI 署名
- ✅ **应当使用**：遵循既有规范的专业提交信息

### 优秀 Git 提交信息的七条规则

基于 [Chris Beams 的经典指南](https://cbea.ms/git-commit/)，遵循以下规则：

1. **用空行分隔标题与正文**
2. **标题限制在 50 个字符以内**
3. **标题首字母大写**
4. **标题末尾不加句号**
5. **标题使用祈使语气**（“Add feature” 而不是 “Added feature”）
6. **正文每行不超过 72 个字符**
7. **正文解释“是什么、为什么”，而不是“怎么做”**

### 现代约定（推荐）

将经典规则与 Conventional Commits 格式结合使用：

- `feat(scope): add new feature` - 新增功能
- `fix(scope): resolve issue description` - 缺陷修复
- `docs(scope): update documentation` - 文档变更
- `refactor(scope): restructure without changing behavior` - 代码重构
- `test(scope): add or update tests` - 测试变更
- `chore(scope): maintenance tasks` - 构建、依赖等维护工作

### 示例

**好：**
```
feat(mcp): add weather server with OAuth2 support

Implements OAuth2 authentication for weather data access.
Resolves rate limiting issues and improves security.
```

**好（简单）：**
```
fix: resolve port conflicts in integration tests
```

**差：**
```
🤖 Generated with Claude Code: Fixed some stuff

Co-Authored-By: Claude <noreply@anthropic.com>
```

### 写作提示
你的提交信息应当能够补全这句话：“If applied, this commit will _[subject line]_”（若应用此提交，将……）