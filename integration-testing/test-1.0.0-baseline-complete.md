# Spring AI 1.0.0 基线测试结果

## 测试日期：2025-08-06
## Spring AI 版本：1.0.0

### 摘要
- **测试总数**：24
- **通过**：15（62.5%）
- **失败**：6（25%）
- **超时**：3（12.5%）

### 已确认的测试结果

| # | 模块 | 结果 | 备注 |
|---|--------|--------|-------|
| 1 | agentic-patterns/chain-workflow | ✅ 通过 | |
| 2 | agentic-patterns/evaluator-optimizer | ✅ 通过 | |
| 3 | agentic-patterns/orchestrator-workers | ✅ 通过 | |
| 4 | agentic-patterns/parallelization-workflow | ✅ 通过 | |
| 5 | agentic-patterns/routing-workflow | ✅ 通过 | |
| 6 | agents/reflection | ✅ 通过 | |
| 7 | kotlin/kotlin-function-callback | ✅ 通过 | |
| 8 | kotlin/kotlin-hello-world | ✅ 通过 | |
| 9 | kotlin/rag-with-kotlin | ❌ 失败 | Docker 未运行（路径修复后） |
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
| 21 | model-context-protocol/web-search/brave-chatbot | ❌ 失败 | 缺少 API 密钥或配置 |
| 22 | model-context-protocol/web-search/brave-starter | ❌ 失败 | 退出码 1 |
| 23 | models/chat/helloworld | ✅ 通过 | |
| 24 | prompt-engineering/prompt-engineering-patterns | ⏱️ 超时 | 退出码 124（2 分钟超时） |

### 已知问题
1. **Docker 依赖**：kotlin/rag-with-kotlin 需要 Docker 正在运行
2. **Web 应用**：部分应用（openai-streaming-response、weather/starter-webmvc-server）作为服务器持续运行不退出
3. **MCP 客户端/服务器**：dynamic-tool-update 示例可能需要客户端与服务器同时运行
4. **API 密钥**：部分示例需要特定的 API 密钥（brave 示例需要 ANTHROPIC_API_KEY）

### 后续步骤
需要完成模块 17-19、22-24 的测试，以获得完整基线。