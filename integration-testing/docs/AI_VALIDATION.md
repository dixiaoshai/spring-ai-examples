# Spring AI 示例的 AI 校验指南

本综合指南介绍如何在 Spring AI 集成测试框架中使用 AI 驱动的校验。AI 校验使用 Claude 智能分析应用日志，判断示例是否成功展示了预期功能。

## 概览

### 什么是 AI 校验？

AI 校验超越了简单的模式匹配，能够理解：
- **上下文与意图** - 阅读 README 文档以理解示例应完成什么
- **不可预测的输出** - 校验 AI 生成的内容，如笑话、对话与创意回复
- **复杂工作流** - 分析多步流程、智能体模式与分布式系统
- **质量评估** - 判断输出是否连贯、恰当，并展示了预期功能

### 主要优势

1. **智能评估** - 超越正则模式，理解示例是否达成了目的
2. **处理 AI 输出** - 校验不可预测的 AI 生成内容（笑话、对话等）
3. **上下文感知** - 使用 README 文档理解预期行为
4. **多组件支持** - 对分布式示例（客户端/服务器）进行整体校验
5. **清晰的推理** - 为通过/失败判定提供详细说明与置信度评分
6. **成本高效** - 每次校验约 400 tokens，缓存利用率高

## 何时使用 AI 校验

### ✅ 强烈推荐用于

| 示例类型 | AI 校验的优势 |
|--------------|--------------------------|
| **聊天示例** | 校验笑话质量、对话流畅度与回复的恰当性 |
| **智能体模式** | 理解多步推理、工作流完成情况与迭代周期 |
| **MCP 示例** | 校验协议握手、工具发现与客户端—服务器通信 |
| **函数调用** | 核实真实的工具使用、参数传递与响应集成 |
| **流式响应** | 评估实时内容的质量与完整性 |
| **创意内容** | 评估诗歌、故事与结构化的创意输出 |

### ❌ 无需用于

- 简单的构建/启动验证
- 纯粹确定性的输出（如文件 I/O 操作）
- 不含 AI 组件的基础设施测试
- 正则模式已足以捕捉成功的示例

## 配置指南

### ExampleInfo.json 结构

```json
{
  "timeoutSec": 300,
  "successRegex": ["BUILD SUCCESS", "Started.*Application"],
  "requiredEnv": ["OPENAI_API_KEY"],
  "aiValidation": {
    "enabled": true,
    "validationMode": "hybrid",
    "readmeFile": "../README.md",
    "expectedBehavior": "Detailed description of what the example should demonstrate",
    "promptTemplate": "chat_example_validation",
    "components": ["client", "server"],
    "successCriteria": {
      "requiresUserInteraction": false,
      "expectedOutputTypes": ["conversation", "tool_usage"],
      "expectedSteps": 3,
      "evaluationRequired": true
    }
  }
}
```

### 配置字段

| 字段 | 是否必需 | 描述 | 示例 |
|-------|----------|-------------|---------|
| `enabled` | 是 | 启用/禁用 AI 校验 | `true` |
| `validationMode` | 是 | AI 校验与正则的配合方式 | `"hybrid"` |
| `readmeFile` | 否 | 用于提供上下文的 README 路径 | `"../README.md"` |
| `expectedBehavior` | 是 | 示例应展示的内容 | `"Chat with coherent responses"` |
| `promptTemplate` | 是 | 使用的校验模板 | `"chat_example_validation"` |
| `components` | 否 | 需要校验的系统组件 | `["client", "server"]` |
| `successCriteria` | 否 | 额外的校验标准 | 见上方 schema |

## 校验模式

### Primary 模式（`"primary"`）
- **行为**：仅使用 AI 校验，忽略正则模式
- **使用场景**：AI 输出完全不可预测的示例
- **最适合**：创意内容、对话式 AI、高度多变的响应
- **示例**：每个笑话/对话都独一无二的聊天应用

```json
{
  "aiValidation": {
    "enabled": true,
    "validationMode": "primary",
    "expectedBehavior": "Generate creative jokes with setup and punchline",
    "promptTemplate": "chat_example_validation"
  }
}
```

### Hybrid 模式（`"hybrid"`）
- **行为**：正则模式与 AI 校验都必须通过
- **使用场景**：同时具有确定性组件与 AI 组件的示例
- **最适合**：复杂工作流、MCP 示例、多组件系统
- **示例**：既有结构化输出又有 AI 推理的智能体模式

```json
{
  "aiValidation": {
    "enabled": true,
    "validationMode": "hybrid",
    "expectedBehavior": "Execute 4-step workflow with AI evaluation",
    "promptTemplate": "workflow_validation"
  }
}
```

### Fallback 模式（`"fallback"`）
- **行为**：仅在正则模式失败时使用 AI 校验
- **使用场景**：从正则逐步迁移到 AI 校验
- **最适合**：正则通常有效、但需要 AI 兜底的示例
- **示例**：将现有测试迁移到 AI 校验

```json
{
  "aiValidation": {
    "enabled": true,
    "validationMode": "fallback",
    "expectedBehavior": "Standard functionality with AI backup validation",
    "promptTemplate": "example_validation"
  }
}
```

## 提示词模板

### `example_validation`
**用途**：基础功能校验
**最适合**：简单的 Spring Boot 应用、基础 AI 集成
**校验内容**：应用启动、基础 AI 响应、核心功能

### `chat_example_validation`
**用途**：对话质量评估
**最适合**：聊天应用、笑话生成器、创意内容
**校验内容**：响应连贯性、对话流畅度、内容恰当性

### `workflow_validation`
**用途**：多步流程校验
**最适合**：智能体模式、复杂工作流、迭代流程
**校验内容**：步骤完成情况、数据流、流程逻辑、评估循环

### `client_server_validation`
**用途**：分布式系统校验
**最适合**：MCP 示例、微服务、客户端—服务器通信
**校验内容**：连接建立、协议符合性、工具发现

## AI 校验结果

### 成功响应
```json
{
  "success": true,
  "confidence": 0.95,
  "reasoning": "The Spring AI MCP client successfully demonstrated core functionality. The application started correctly, connected to MCP servers, discovered tools (Brave Web Search, Brave Local Search), and provided intelligent responses about available tools.",
  "components_validated": [
    "client",
    "communication_protocol",
    "tools/services"
  ],
  "functionality_demonstrated": [
    "Spring Boot application started successfully",
    "MCP client connected to servers using STDIO transport",
    "Tools were discovered and registered",
    "AI assistant provided appropriate responses"
  ],
  "issues_found": [],
  "recommendations": [],
  "cost_info": {
    "input_tokens": 11,
    "output_tokens": 540,
    "total_tokens": 551,
    "cache_creation_input_tokens": 5885,
    "cache_read_input_tokens": 25308,
    "duration_seconds": 17.22
  }
}
```

### 失败响应
```json
{
  "success": false,
  "confidence": 0.80,
  "reasoning": "While the application started successfully, it failed to demonstrate the expected chat functionality. No AI responses were found in the output.",
  "components_validated": [
    "spring_boot_startup"
  ],
  "functionality_demonstrated": [
    "Application started without errors"
  ],
  "issues_found": [
    "No chat interaction found in logs",
    "Expected AI responses not present",
    "User input processing unclear"
  ],
  "recommendations": [
    "Verify AI model configuration",
    "Check user input injection",
    "Review application.properties settings"
  ],
  "cost_info": { /* ... */ }
}
```

## 成本分析

### 典型性能指标

| 指标 | 范围 | 备注 |
|--------|-------|-------|
| **输入 Tokens** | 10-15 | 因模板高效而极少 |
| **输出 Tokens** | 380-540 | 全面分析 |
| **总 Tokens** | 390-550 | 每次校验非常合理 |
| **耗时** | 12-18 秒 | 对集成测试可以接受 |
| **缓存读取** | 25-28K | 缓存利用率高 = 成本高效 |
| **缓存创建** | 2-5K | 因模板复用而创建成本低 |
| **置信度** | 0.85-1.00 | 校验准确度高 |

### 成本优化

**缓存效率**：模板复用带来 10-25 倍的缓存读取比，大幅降低成本
**模板设计**：优化后的提示词在最小化输入 tokens 的同时最大化校验质量
**可预测的成本**：不同示例类型的 token 用量稳定，便于做预算规划

## 集成示例

### 简单聊天示例
```json
{
  "timeoutSec": 120,
  "successRegex": ["Spring AI Hello World!"],
  "requiredEnv": ["OPENAI_API_KEY"],
  "aiValidation": {
    "enabled": true,
    "validationMode": "primary",
    "expectedBehavior": "Application should accept user input 'tell me a joke' and return a coherent joke response with setup and punchline",
    "promptTemplate": "chat_example_validation"
  }
}
```

### 复杂工作流示例
```json
{
  "timeoutSec": 300,
  "successRegex": ["EVALUATION: PASS", "Chain completed"],
  "requiredEnv": ["OPENAI_API_KEY"],
  "aiValidation": {
    "enabled": true,
    "validationMode": "hybrid",
    "readmeFile": "../README.md",
    "expectedBehavior": "Execute 4-step data transformation pipeline: extract values, standardize format, sort data, create markdown table",
    "promptTemplate": "workflow_validation",
    "successCriteria": {
      "expectedSteps": 4,
      "outputFormat": "markdown_table",
      "evaluationRequired": true
    }
  }
}
```

### MCP 客户端—服务器示例
```json
{
  "timeoutSec": 300,
  "successRegex": ["Connected", "Tools discovered"],
  "requiredEnv": ["OPENAI_API_KEY", "BRAVE_API_KEY"],
  "aiValidation": {
    "enabled": true,
    "validationMode": "hybrid",
    "readmeFile": "../README.md",
    "expectedBehavior": "MCP client should connect to servers, discover available tools (like Brave Search), handle user questions, and demonstrate tool usage through AI responses",
    "promptTemplate": "client_server_validation",
    "components": ["client", "mcp-servers"],
    "successCriteria": {
      "requiresUserInteraction": false,
      "expectedOutputTypes": ["tool_discovery", "ai_response", "tool_usage"]
    }
  }
}
```

## 使用脚手架工具

### 生成带 AI 校验的测试（默认）
```bash
# Simple example with hybrid AI validation
python3 scripts/scaffold_integration_test.py kotlin/kotlin-hello-world

# Complex workflow with primary AI validation
python3 scripts/scaffold_integration_test.py agentic-patterns/chain-workflow --complexity complex --ai-mode primary

# MCP example with client-server validation
python3 scripts/scaffold_integration_test.py model-context-protocol/weather/server --complexity mcp
```

### 禁用 AI 校验
```bash
# Generate integration test without AI validation
python3 scripts/scaffold_integration_test.py simple-example --no-ai-validation
```

### 覆盖校验模式
```bash
# Force primary mode for creative content
python3 scripts/scaffold_integration_test.py creative-example --ai-mode primary

# Use fallback mode for gradual migration
python3 scripts/scaffold_integration_test.py existing-example --ai-mode fallback --force
```

## 故障排查

### 常见问题

#### “AI 校验脚本失败，退出码：2”
**原因**：环境变量未传递给 Python 子进程
**方案**：确保在 shell 中设置了必需的环境变量（如 `BRAVE_API_KEY`）

#### “AI 校验失败：没有那个文件或目录”
**原因**：缺少依赖，或 Python 路径不正确
**方案**：安装依赖：`pip3 install -r integration-testing/ai-validator/requirements.txt`

#### “置信度过低”或意外失败
**原因**：示例输出与预期行为描述不符
**方案**：检查 `expectedBehavior`，确保它准确描述了示例的实际行为

### 性能问题

#### AI 校验缓慢（超过 30 秒）
**原因**：网络问题或内容分析复杂
**方案**：检查网络连接；考虑简化 `expectedBehavior`

#### Token 用量过高
**原因**：应用日志过于冗长，或校验标准过于复杂
**方案**：检查日志过滤；考虑更聚焦的 `expectedBehavior`

## 最佳实践

### 配置
1. **要具体**：编写清晰、详细的 `expectedBehavior` 描述
2. **选择正确的模式**：AI 密集型示例使用 `primary`，混合内容使用 `hybrid`
3. **选对模板**：让提示词模板与示例类型匹配
4. **本地测试**：提交前先校验配置

### 开发工作流
1. **从简单开始**：先使用基础 AI 校验，再逐步增加复杂度
2. **迭代**：根据校验结果优化 `expectedBehavior`
3. **监控成本**：跟踪 token 用量以便预算规划
4. **记录变更**：更改校验方式时更新 README

### 维护
1. **定期审查**：定期检查校验准确度与成本
2. **更新模板**：让提示词模板与示例的演进保持同步
3. **性能监控**：跟踪校验耗时与成功率
4. **环境管理**：确保 API 密钥配置一致

## 高级配置

### 自定义成功标准
```json
{
  "successCriteria": {
    "requiresUserInteraction": false,
    "expectedOutputTypes": ["conversation", "tool_usage", "api_response"],
    "expectedSteps": 3,
    "evaluationRequired": true,
    "outputFormat": "markdown_table",
    "minimumResponseLength": 100
  }
}
```

### 多组件校验
```json
{
  "components": ["server", "client", "database"],
  "expectedBehavior": "Complete distributed workflow: server starts, client connects, database operations execute, results returned"
}
```

### 环境特定配置
```json
{
  "requiredEnv": ["OPENAI_API_KEY", "BRAVE_API_KEY", "DATABASE_URL"],
  "aiValidation": {
    "enabled": true,
    "validationMode": "hybrid",
    "environmentSpecific": {
      "development": { "promptTemplate": "detailed_validation" },
      "ci": { "promptTemplate": "quick_validation" }
    }
  }
}
```

## 迁移指南

### 从仅用正则迁移到 AI 校验

1. **从 Fallback 模式开始**
   ```json
   {
     "aiValidation": {
       "enabled": true,
       "validationMode": "fallback",
       "expectedBehavior": "Basic functionality description"
     }
   }
   ```

2. **测试并优化**
   - 多次运行测试以确保一致性
   - 根据结果优化 `expectedBehavior`
   - 监控置信度评分

3. **升级到 Hybrid 模式**
   ```json
   {
     "aiValidation": {
       "validationMode": "hybrid"
     }
   }
   ```

4. **对 AI 密集型示例考虑 Primary 模式**
   ```json
   {
     "aiValidation": {
       "validationMode": "primary"
     }
   }
   ```

## 未来增强

AI 校验系统仍在持续演进：

1. **模板扩展** - 为专门用例增加更多提示词模板
2. **批量校验** - 在单次请求中校验多个示例以提升成本效率
3. **自定义校验器** - 用于领域特定校验逻辑的插件系统
4. **性能优化** - 缓存策略与请求批处理
5. **增强报告** - 关于校验模式与成本的详细分析

---

*本指南涵盖 Spring AI 示例的完整 AI 校验系统。如需更多帮助或报告问题，请参阅主 [集成测试 README](README.md)。*