# 当前集成测试状态
**最近更新**：2024 年 12 月  
**速查说明**：本文件跟踪所有集成测试的精确状态

## 📊 总体状态
- **示例总数**：33
- **已创建测试**：24（73%）
- **实际可用**：18（54%）
- **已纳入 CI**：17（52%）

## ✅ CI 中通过（17 个测试）
```
✓ models/chat/helloworld
✓ kotlin/kotlin-hello-world
✓ kotlin/kotlin-function-callback (added to CI)
✓ misc/spring-ai-java-function-callback
✓ agents/reflection
✓ agents/tools-and-agent-tools
✓ agentic-patterns/chain-workflow
✓ agentic-patterns/evaluator-optimizer-pattern
✓ agentic-patterns/orchestrator-workers
✓ agentic-patterns/parallelization
✓ agentic-patterns/routing-agent
✓ model-context-protocol/brave
✓ model-context-protocol/client-starter/starter-default-client
✓ model-context-protocol/filesystem
✓ model-context-protocol/sqlite/chatbot
✓ model-context-protocol/sqlite/simple
✓ model-context-protocol/web-search/brave-chatbot
✓ model-context-protocol/web-search/brave-starter
```

## ⚠️ 已修复但未纳入 CI（1 个测试）
```
⚠️ prompt-engineering/prompt-engineering-patterns
   - Fixed: Timeout increased 120s → 300s
   - Status: Works locally
   - Action: Add to CI
```

## ❌ 失败/未完成（6 个测试）

### Web 服务器（2 个）- 校验偏弱
```
❌ misc/openai-streaming-response
   - Problem: Only validates SSE format, not actual content
   - Fix needed: Parse SSE chunks, validate joke response
   
❌ model-context-protocol/weather/starter-webmvc-server
   - Problem: Only checks content-type, no MCP protocol test
   - Fix needed: Send MCP messages, validate tool listing
```

### 客户端—服务器配对（2 个）- 需要编排
```
❌ model-context-protocol/dynamic-tool-update/client
   - Problem: Needs server running
   
❌ model-context-protocol/dynamic-tool-update/server
   - Problem: Needs client to connect
```

### 基础设施问题（2 个）
```
❌ kotlin/rag-with-kotlin
   - Problem: Requires Docker for vector database
   
❌ model-context-protocol/client-starter/starter-webflux-client
   - Problem: Connection refused (needs MCP servers)
```

## 🔧 尚未创建测试（9 个示例）
```
- agents/creative-writing
- agents/github-assistant
- agents/naive-rag
- model-context-protocol/annotations/client
- model-context-protocol/annotations/server
- model-context-protocol/web-search/client
- model-context-protocol/weather/client
- model-context-protocol/weather/server
- prompt-engineering/prompt-templating
```

---
**注意**：测试状态发生变化时，应及时更新本文件。