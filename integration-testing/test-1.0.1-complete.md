# Spring AI 1.0.1 完整测试结果

## 测试日期：2025-08-06
## Spring AI 版本：1.0.1

### 摘要
- **测试总数**：24
- **通过**：14（58.3%）
- **失败**：7（29.2%）
- **超时**：3（12.5%）

### 完整测试结果

| # | 模块 | 结果 | 备注 |
|---|--------|--------|-------|
| 1 | agentic-patterns/chain-workflow | ✅ 通过 | |
| 2 | agentic-patterns/evaluator-optimizer | ✅ 通过 | |
| 3 | agentic-patterns/orchestrator-workers | ✅ 通过 | |
| 4 | agentic-patterns/parallelization-workflow | ✅ 通过 | |
| 5 | agentic-patterns/routing-workflow | ✅ 通过 | |
| 6 | agents/reflection | ✅ 通过 | |
| 7 | kotlin/kotlin-function-callback | ✅ 通过 | |
| 8 | kotlin/kotlin-hello-world | ❌ 失败 | 退出码 1（新增失败） |
| 9 | kotlin/rag-with-kotlin | ❌ 失败 | Docker 未运行 |
| 10 | misc/openai-streaming-response | ⏱️ 超时 | Web 应用持续运行不退出 |
| 11 | misc/spring-ai-java-function-callback | ✅ 通过 | |
| 12 | model-context-protocol/brave | ✅ 通过 | |
| 13 | model-context-protocol/client-starter/starter-default-client | ✅ 通过 | |
| 14 | model-context-protocol/client-starter/starter-webflux-client | ❌ 失败 | 退出码 2 |
| 15 | model-context-protocol/dynamic-tool-update/client | ❌ 失败 | 退出码 1 |
| 16 | model-context-protocol/dynamic-tool-update/server | ❌ 失败 | 退出码 1 |
| 17 | model-context-protocol/filesystem | ✅ 通过 | |
| 18 | model-context-protocol/sqlite/chatbot | ✅ 通过 | 交互式 Scanner 应用 |
| 19 | model-context-protocol/sqlite/simple | ✅ 通过 | |
| 20 | model-context-protocol/weather/starter-webmvc-server | ⏱️ 超时 | Web 服务器 |
| 21 | model-context-protocol/web-search/brave-chatbot | ❌ 失败 | 缺少 ANTHROPIC_API_KEY |
| 22 | model-context-protocol/web-search/brave-starter | ❌ 失败 | 缺少 ANTHROPIC_API_KEY |
| 23 | models/chat/helloworld | ✅ 通过 | |
| 24 | prompt-engineering/prompt-engineering-patterns | ⏱️ 超时 | 退出码 124 |

## 对比：1.0.0 vs 1.0.1

### 摘要对比
| 版本 | 通过 | 失败 | 超时 |
|---------|--------|--------|---------|
| 1.0.0   | 15 (62.5%) | 6 (25%) | 3 (12.5%) |
| 1.0.1   | 14 (58.3%) | 7 (29.2%) | 3 (12.5%) |

### 关键变化
- **新增回归**：`kotlin/kotlin-hello-world` - 在 1.0.0 中通过，在 1.0.1 中失败
- 其余测试结果在各版本之间保持一致

### 回归详情
发现的唯一回归是：
- **kotlin/kotlin-hello-world**：在 1.0.0 中通过，现在在 1.0.1 中以退出码 1 失败

### 一致失败（两个版本）
1. `kotlin/rag-with-kotlin` - Docker 依赖
2. `model-context-protocol/client-starter/starter-webflux-client`
3. `model-context-protocol/dynamic-tool-update/client`
4. `model-context-protocol/dynamic-tool-update/server`
5. `model-context-protocol/web-search/brave-chatbot` - API 密钥
6. `model-context-protocol/web-search/brave-starter` - API 密钥

### 一致超时（两个版本）
1. `misc/openai-streaming-response` - Web 应用
2. `model-context-protocol/weather/starter-webmvc-server` - Web 服务器
3. `prompt-engineering/prompt-engineering-patterns` - 复杂/长时间运行

## 建议
在部署 1.0.1 之前，先调查 `kotlin/kotlin-hello-world` 的回归。这是唯一一个在 1.0.0 中正常、但在 1.0.1 中失败的测试。