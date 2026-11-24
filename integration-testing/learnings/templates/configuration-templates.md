# 集成测试配置模板

## 概览

本文档提供针对不同类型 Spring AI 示例的标准化 `ExampleInfo.json` 模板。请使用这些模板，确保新集成测试的一致性并实现最优配置。

## 模板类别

### 1. 交互式应用模板
**适用场景**：使用 Scanner.nextLine()、聊天界面、交互式提示词的应用

```json
{
  "timeoutSec": 20,
  "successRegex": [
    "Started.*Application",
    "Interactive prompt pattern"
  ],
  "requiredEnv": [
    "OPENAI_API_KEY"
  ],
  "aiValidation": {
    "enabled": true,
    "validationMode": "fallback",
    "readmeFile": "../README.md",
    "expectedBehavior": "Interactive [TYPE] should start successfully and display the interaction prompt, indicating the system is ready for user input. The application will wait for interactive input to demonstrate [FUNCTIONALITY].",
    "promptTemplate": "example_validation",
    "successCriteria": {
      "requiresUserInteraction": true,
      "expectedComponents": ["spring_boot_startup", "[APP_TYPE]_ready", "interactive_prompt"],
      "validationNote": "Tests readiness for interactive [FUNCTIONALITY] workflow"
    }
  }
}
```

**示例**：agents/reflection、sqlite/chatbot、brave-chatbot

### 2. 智能体工作流模板
**适用场景**：多步 AI 处理、复杂推理工作流、评估系统

```json
{
  "timeoutSec": 300,
  "successRegex": [
    "Started.*Application",
    "EVALUATION:\\\\s+PASS"
  ],
  "requiredEnv": [
    "OPENAI_API_KEY"
  ],
  "aiValidation": {
    "enabled": true,
    "validationMode": "primary",
    "readmeFile": "../README.md",
    "expectedBehavior": "[WORKFLOW_TYPE] should execute successfully with multi-step AI processing and produce evaluation results showing the workflow completed correctly.",
    "promptTemplate": "workflow_validation",
    "successCriteria": {
      "expectedSteps": 3,
      "evaluationRequired": true,
      "expectedComponents": ["spring_boot_startup", "workflow_execution", "evaluation_results"]
    }
  }
}
```

**示例**：agentic-patterns/*、orchestrator-workers、routing-workflow

### 3. MCP 集成模板
**适用场景**：Model Context Protocol 示例、客户端—服务器通信

```json
{
  "timeoutSec": 120,
  "successRegex": [
    "Started.*Application",
    "MCP.*[Ii]nitialized",
    "Connection established|Tools available"
  ],
  "requiredEnv": [
    "OPENAI_API_KEY"
  ],
  "aiValidation": {
    "enabled": true,
    "validationMode": "hybrid",
    "readmeFile": "../README.md",
    "expectedBehavior": "MCP [CLIENT/SERVER] should start successfully, establish protocol connection, and demonstrate [FUNCTIONALITY] integration through Model Context Protocol.",
    "promptTemplate": "client_server_validation",
    "successCriteria": {
      "expectedComponents": ["spring_boot_startup", "mcp_initialization", "[PROTOCOL]_connection", "tool_availability"],
      "validationNote": "Tests MCP integration for [FUNCTIONALITY]"
    }
  }
}
```

**示例**：model-context-protocol/*、filesystem、brave、weather 服务器

### 4. 简单示例模板
**适用场景**：基础演示、hello world 示例、简单的函数回调

```json
{
  "timeoutSec": 120,
  "successRegex": [
    "Started.*Application",
    "[Specific output pattern]"
  ],
  "requiredEnv": [
    "OPENAI_API_KEY"
  ],
  "aiValidation": {
    "enabled": true,
    "validationMode": "hybrid",
    "readmeFile": "../README.md",
    "expectedBehavior": "Simple [EXAMPLE_TYPE] should start successfully and demonstrate [FUNCTIONALITY] with clear output showing the integration working correctly.",
    "promptTemplate": "example_validation",
    "successCriteria": {
      "expectedComponents": ["spring_boot_startup", "[FUNCTIONALITY]_demonstration"],
      "validationNote": "Tests basic [FUNCTIONALITY] integration"
    }
  }
}
```

**示例**：models/chat/helloworld、kotlin/kotlin-hello-world、misc/function-callback

### 5. Docker 依赖模板
**适用场景**：需要 Docker Compose 或外部服务的应用

```json
{
  "timeoutSec": 180,
  "setupCommands": [
    "docker-compose up -d",
    "sleep 10"
  ],
  "cleanupCommands": [
    "docker-compose down"
  ],
  "successRegex": [
    "Started.*Application",
    "Connected to.*database|Service ready"
  ],
  "requiredEnv": [
    "OPENAI_API_KEY"
  ],
  "expectedFailurePatterns": [
    "Connection refused",
    "Service unavailable"
  ],
  "aiValidation": {
    "enabled": true,
    "validationMode": "hybrid",
    "readmeFile": "../README.md",
    "expectedBehavior": "[APPLICATION_TYPE] should start successfully, connect to external services via Docker, and demonstrate [FUNCTIONALITY] integration.",
    "promptTemplate": "example_validation",
    "successCriteria": {
      "requiresExternalServices": true,
      "expectedComponents": ["spring_boot_startup", "service_connection", "[FUNCTIONALITY]_demonstration"],
      "validationNote": "Tests [FUNCTIONALITY] with external service dependencies"
    }
  }
}
```

**示例**：kotlin/rag-with-kotlin、brave-docker-agents-gateway

## 配置指南

### 超时标准
| 应用类型 | 超时 | 理由 |
|------------------|---------|-----------|
| 简单示例 | 120s | 基础 Spring Boot 启动 + 演示 |
| 交互式应用 | 15-25s | 快速启动校验，不等待完整工作流 |
| MCP 集成 | 120s | 协议握手 + 工具发现 |
| 智能体工作流 | 300-600s | 复杂的多步 AI 处理 |
| Docker 依赖 | 180s | 服务启动 + 应用初始化 |

### AI 校验模式
| 模式 | 使用场景 | 模式检查 | AI 检查 |
|------|----------|---------------|----------|
| **primary** | 不可预测的 AI 输出 | ❌ 跳过 | ✅ 仅使用 |
| **hybrid** | 可预测/可变混合 | ✅ 必需 | ✅ 额外使用 |
| **fallback** | 交互式应用 | ✅ 为主 | ✅ 正则失败时使用 |

### 成功模式指南
1. **始终包含**：用于 Spring Boot 校验的 `"Started.*Application"`
2. **应用特有**：表明功能正常工作的关键输出
3. **交互式应用**：包含就绪标志（`"Let's chat!"`、`"Starting interactive"`）
4. **MCP 应用**：包含协议初始化（`"MCP.*[Ii]nitialized"`）
5. **评估类应用**：包含完成标志（`"EVALUATION:\\\\s+PASS"`）

### 环境变量标准
- **主要**：`OPENAI_API_KEY`（最常见）
- **Anthropic**：`ANTHROPIC_API_KEY`（用于基于 Claude 的示例）
- **服务特有**：`BRAVE_API_KEY`、`DATABASE_URL` 等
- **多提供商**：使用数组：`["OPENAI_API_KEY", "ANTHROPIC_API_KEY"]`

### AI 校验最佳实践

#### expectedBehavior 指南
- **要具体**：描述应用实际应该做什么
- **包含上下文**：提及关键技术（MCP、Docker、特定 AI 功能）
- **设定预期**：明确什么算作成功
- **说明限制**：对交互式应用，明确测试范围

#### successCriteria 标准
```json
"successCriteria": {
  "requiresUserInteraction": true,        // For interactive apps
  "requiresExternalServices": true,       // For Docker/external deps
  "expectedSteps": 3,                     // For multi-step workflows
  "evaluationRequired": true,             // For evaluation frameworks
  "expectedComponents": [                 // Always include
    "spring_boot_startup",               // Standard component
    "app_specific_component",            // Application-specific
    "functionality_demonstration"        // What it demonstrates
  ],
  "validationNote": "Tests [specific purpose]"  // Brief test description
}
```

## 模板选择指南

1. **确定应用类型**：
   - 是否需要用户输入？→ 交互式模板
   - 是否为多步 AI 工作流？→ 智能体模板  
   - 是否使用 MCP？→ MCP 模板
   - 是否需要 Docker？→ Docker 依赖模板
   - 其他情况 → 简单模板

2. **定制模板**：
   - 将 `[PLACEHOLDERS]` 替换为具体值
   - 根据复杂度调整超时
   - 添加应用特有的成功模式
   - 配置合适的校验模式

3. **校验配置**：
   - 提交前先在本地测试
   - 核实所有必需的环境变量
   - 检查超时是否充足但不过长
   - 确保成功模式与实际输出匹配

## 维护说明

- 当多个示例中出现新模式时，**更新模板**
- 对 Schema 做破坏性变更时，**为模板设定版本**
- 当示例需要非标准配置时，**记录例外情况**
- **定期审查**现有配置与当前模板的匹配情况