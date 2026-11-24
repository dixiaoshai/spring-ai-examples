# 剩余测试失败分析

**日期**：2024 年 12 月  
**状态**：仍有 7 个测试失败/被排除在 CI 之外

## 问题汇总

### 1. Web 服务器测试（部分解决）

#### ❌ `misc/openai-streaming-response`
- **状态**：测试已创建，但校验偏弱
- **当前校验**：仅检查 SSE 数据模式
- **缺失**：未校验实际的笑话内容或流式数据块
- **需要**：更好的 SSE 解析与内容校验
- **未纳入 CI**：需要加入主工作流或 Web 服务器工作流

#### ❌ `model-context-protocol/weather/starter-webmvc-server`  
- **状态**：测试已创建，但校验极少
- **当前校验**：仅检查 SSE 内容类型
- **缺失**：没有真正的 MCP 协议测试
- **需要**：发送 MCP initialize 请求，校验工具列表
- **未纳入 CI**：需要加入主工作流或 Web 服务器工作流

### 2. 基础设施依赖

#### ❌ `kotlin/rag-with-kotlin`
- **问题**：需要 Docker 提供向量数据库
- **方案**：为 CI 添加 Docker 支持，或模拟数据库
- **复杂度**：高 - 需要在 GitHub Actions 中使用 Docker

### 3. 连接问题

#### ❌ `model-context-protocol/client-starter/starter-webflux-client`
- **问题**：连接 MCP 服务器时被拒绝
- **错误**：可能是在尝试连接未运行的服务器
- **方案**：需要在运行客户端之前启动 MCP 服务器
- **复杂度**：中 - 需要编排

### 4. 客户端—服务器编排

#### ❌ `model-context-protocol/dynamic-tool-update/client`
- **问题**：需要服务器正在运行
- **方案**：先启动服务器，再运行客户端
- **复杂度**：高 - 需要完整实现第 5 阶段

#### ❌ `model-context-protocol/dynamic-tool-update/server`
- **问题**：需要客户端连接
- **方案**：先启动服务器，再运行客户端进行测试
- **复杂度**：高 - 需要完整实现第 5 阶段

### 5. 长时间运行的测试（已修复但未纳入 CI）

#### ⚠️ `prompt-engineering/prompt-engineering-patterns`
- **状态**：已修复超时（120 秒 → 300 秒）
- **问题**：尚未加入 CI
- **方案**：以更长的超时时间加入 CI
- **复杂度**：低 - 只需更新 CI

## 校验质量问题

### 当前 Web 服务器测试的局限

1. **openai-streaming-response**：
   - 发送：`message=Tell%20me%20a%20short%20joke`
   - 校验：基础 SSE 格式
   - **缺失**：实际笑话内容、流式数据块、完成状态

2. **weather/starter-webmvc-server**：
   - 发送：对 `/mcp/message` 的 GET 请求
   - 校验：仅内容类型
   - **缺失**：MCP 协议握手、工具发现、天气查询

## 建议的修复优先级

### 快速见效（低复杂度）
1. **将 `prompt-engineering-patterns` 加入 CI** - 只需更新工作流
2. **将 `kotlin-function-callback` 加入 CI** - 本地已通过

### 中等复杂度
3. **改进 Web 服务器校验** - 正确解析 SSE、校验内容
4. **将 Web 服务器测试加入 CI** - 更新工作流以包含它们

### 高复杂度  
5. **客户端—服务器编排** - 需要完整实现第 5 阶段
6. **Docker 支持** - 为 `kotlin/rag-with-kotlin` 在 CI 中添加 Docker
7. **修复 WebFlux 客户端** - 排查连接问题，可能需要准备服务器

## 实际测试覆盖情况

- **示例总数**：约 33
- **已创建测试**：24（73%）
- **实际通过**：18（含 prompt-engineering 的修复）
- **已纳入 CI**：17（缺少 prompt-engineering 与 kotlin-function-callback）
- **真实覆盖率**：54%（18/33）

## 后续步骤

1. **立即**：将 2 个已修复的测试加入 CI（prompt-engineering、kotlin-function-callback）
2. **短期**：改进 Web 服务器测试的校验
3. **中期**：实现合适的客户端—服务器编排
4. **长期**：为剩余测试添加 Docker 支持