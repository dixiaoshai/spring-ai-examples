# 第 5 阶段：Web 服务器测试基础设施

**日期**：2024 年 12 月  
**状态**：✅ 已完成  
**影响**：实现了对流式 Web 服务器与 SSE 端点的测试支持

## 概览

成功实现了用于测试不会自然退出的 Spring Boot Web 服务器的基础设施，涵盖流式响应服务器以及使用 SSE 传输的 MCP 服务器。

## 解决的问题

部分 Spring AI 示例是暴露 HTTP 端点的 Web 服务器，而不是批处理应用。这些服务器违背了原有的测试假设，即应用会：
1. 启动
2. 执行一些工作  
3. 以状态码退出

Web 服务器启动后会持续运行，等待 HTTP 请求，这在原框架中导致了超时。

## 已实现的解决方案

### 1. WebServerTestUtils.java

创建了用于 Web 服务器测试的集中式工具类，包含：
- 后台进程管理
- 用于端点测试的 HTTP 客户端
- SSE（Server-Sent Events）支持
- 优雅关闭处理
- 本地日志目录管理

```java
public static void runWebServerTest(String moduleName, WebServerConfig config) {
    // Start server in background
    ProcessHandle serverHandle = startServerInBackground();
    
    // Wait for server readiness
    waitForHealthCheck(config.healthCheck, config.startupTime);
    
    // Test configured endpoints
    for (EndpointTest test : config.testEndpoints()) {
        testEndpoint(test);
    }
    
    // Graceful shutdown
    shutdownServer(serverHandle);
}
```

### 2. 测试配置结构

扩展 ExampleInfo.json，增加 Web 服务器配置：

```json
{
  "testMode": "web-server",
  "webServerConfig": {
    "startupTime": 15,
    "testEndpoints": [
      {
        "url": "http://localhost:8080/endpoint",
        "method": "GET",
        "expectedPattern": "response pattern",
        "headers": {
          "Accept": "text/event-stream"
        }
      }
    ]
  }
}
```

## 已成功测试的示例

### 1. openai-streaming-response
- **类型**：流式 AI 响应服务器
- **端点**：`/ai/generateStream`
- **挑战**：SSE 流不会自然关闭
- **方案**：带 SSE 请求头与模式匹配的 HTTP 客户端

### 2. weather/starter-webmvc-server
- **类型**：使用 SSE 传输的 MCP 服务器
- **端点**：`/mcp/message`
- **挑战**：基于 SSE 的 MCP 协议
- **方案**：正确处理 SSE 请求

## CI 集成

为 Web 服务器测试创建了独立工作流：
- `.github/workflows/web-server-tests.yml`
- 手动触发，可选择 Spring AI 版本
- 对 Web 服务器进行隔离测试
- CI 环境中的后台进程管理

## 关键成果

1. **后台进程管理**：服务器在后台运行，并具备完善的生命周期管理
2. **HTTP 测试**：完整的 HTTP 客户端实现，用于端点校验
3. **SSE 支持**：正确处理 Server-Sent Events 流
4. **日志捕获**：服务器日志捕获到本地目录以便调试
5. **优雅关闭**：干净终止，并提供 SIGTERM/SIGKILL 兜底
6. **CI 就绪**：工作流可接入 GitHub Actions

## 修复的问题

### prompt-engineering-patterns 超时
- **问题**：复杂示例会发起多次 AI API 调用
- **方案**：将超时时间从 120 秒增加到 300 秒
- **结果**：测试现在可以成功通过

## 指标

- **新增测试**：2 个 Web 服务器测试
- **覆盖率提升**：从 71% 提升到 75%（24 个测试中 18 个通过）
- **代码复用**：WebServerTestUtils 在所有 Web 服务器测试中共享
- **代码行数**：完整基础设施约 200 行

## 未来增强

1. **健康检查端点**：为服务器添加 actuator 端点，以便更好地检测就绪状态
2. **多端口支持**：支持运行在不同端口上的服务器
3. **WebSocket 支持**：扩展到基于 WebSocket 的服务器
4. **负载测试**：增加性能测试能力
5. **客户端—服务器编排**：同时测试 MCP 客户端—服务器配对

## 技术细节

### 进程管理
```java
// Start server with log redirection
ProcessBuilder pb = new ProcessBuilder("./mvnw", "spring-boot:run", "-q");
pb.redirectOutput(logFile);
Process process = pb.start();

// Graceful shutdown
handle.destroy(); // SIGTERM
if (!terminated) {
    handle.destroyForcibly(); // SIGKILL
}
```

### HTTP 客户端配置
```java
HttpClient httpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(5))
    .build();

HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create(url))
    .header("Accept", "text/event-stream")
    .GET()
    .build();
```

## 经验教训

1. **SSE 的复杂性**：SSE 流不会自然关闭，需要特殊处理
2. **进程生命周期**：在 CI 环境中，完善的进程管理至关重要
3. **日志管理**：本地日志目录优于复杂的路径解析
4. **超时调优**：不同示例需要不同的超时值
5. **健康检查**：可选的健康检查提供了灵活性

## 创建/修改的文件

### 新增文件
- `integration-testing/jbang-lib/WebServerTestUtils.java`
- `misc/openai-streaming-response/integration-tests/RunOpenaiStreamingResponseWebServer.java`
- `model-context-protocol/weather/starter-webmvc-server/integration-tests/RunWeatherStarterWebmvcServerWebServer.java`
- `.github/workflows/web-server-tests.yml`

### 修改的文件
- `misc/openai-streaming-response/integration-tests/ExampleInfo.json`
- `model-context-protocol/weather/starter-webmvc-server/integration-tests/ExampleInfo.json`
- `prompt-engineering/prompt-engineering-patterns/integration-tests/ExampleInfo.json`

## 结论

第 5 阶段成功扩展了集成测试框架，使其能够处理 Web 服务器、流式响应与 SSE 端点。该基础设施为将来测试更复杂的分布式系统与客户端—服务器架构奠定了基础。