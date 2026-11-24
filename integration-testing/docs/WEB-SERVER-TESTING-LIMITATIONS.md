# Web 服务器测试的局限

**日期**：2024 年 12 月  
**作者**：集成测试框架团队

## 问题陈述

部分 Spring AI 示例是暴露 HTTP 端点的 Web 服务器，而不是批处理应用。当前的集成测试框架期望应用：
1. 启动
2. 执行一些工作
3. 以状态码退出

Web 服务器违背了这一假设 - 它们启动后会持续运行，等待 HTTP 请求。

## 受影响的测试（当前排除 2 个）

1. **`misc/openai-streaming-response`**
   - 暴露 `/ai/generateStream` 端点
   - 通过 Server-Sent Events 流式返回 AI 响应
   - 需要 HTTP 客户端来测试其功能

2. **`model-context-protocol/weather/starter-webmvc-server`**
   - 暴露天气工具的 MCP 服务器
   - 需要 MCP 客户端连接并调用工具
   - 属于客户端—服务器架构的一部分

## 当前变通方案

这些测试被排除在 CI 之外，原因标注为“超时 - Web 服务器”。

## 建议的解决方案：第 5 阶段实现

第 5 阶段的“集成式客户端—服务器测试”增强将通过以下方式解决该问题：

### 对于简单的 Web 服务器（如 openai-streaming-response）：
```json
{
  "testMode": "web-server",
  "server": {
    "startupTime": 10,
    "healthCheck": "http://localhost:8080/actuator/health",
    "testEndpoints": [
      {
        "url": "http://localhost:8080/ai/generateStream?message=test",
        "method": "GET",
        "expectedResponse": "data.*chatcmpl"
      }
    ]
  }
}
```

### 对于 MCP 客户端—服务器配对：
```json
{
  "testMode": "client-server",
  "components": {
    "server": {
      "path": "./server",
      "startupTime": 10,
      "healthCheck": "http://localhost:8080/health"
    },
    "client": {
      "path": "./client",
      "startupDelay": 5,
      "serverUrl": "http://localhost:8080"
    }
  }
}
```

## 实现方案

### 选项 1：后台进程 + HTTP 客户端
1. 在后台启动 Spring Boot 应用
2. 等待健康检查端点就绪
3. 使用 curl 或 Java HTTP 客户端发起 HTTP 请求
4. 校验响应
5. 发送关闭信号（SIGTERM）

### 选项 2：测试应用模式
1. 创建一个测试客户端应用
2. 客户端启动服务器、发送请求并校验
3. 客户端以成功/失败状态码退出

### 选项 3：Docker Compose 编排
1. 使用 docker-compose 进行多组件测试
2. 服务器容器 + 客户端容器
3. 客户端执行测试并报告结果

## 临时变通方案

在第 5 阶段实现之前，可以考虑：

1. **单独的人工测试**：记录人工测试步骤
2. **模拟模式**：添加一种执行自检后即退出的测试模式
3. **超时即成功**：如果发现启动模式，则将超时视为成功

## 影响

- **当前排除 2 个测试**（占测试总数的 8%）
- 修复后可将 CI 覆盖率从 75% 提升到 83%
- 为测试分布式系统奠定基础

## 后续步骤

1. 实现第 5 阶段的编排基础设施
2. 从 `openai-streaming-response` 这个简单场景入手
3. 扩展到 MCP 客户端—服务器配对
4. 考虑为复杂场景使用基于 Docker 的编排