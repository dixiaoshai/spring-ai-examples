# 工单客户端，Streamable HTTP

Spring AI MCP 客户端（`spring-ai-starter-mcp-client-webflux`），为 `http://localhost:8080` 配置。可用于 [stateless-server](../stateless-server/) 和 [streamable-server](../streamable-server/)。

`ClientHandlers` 应答 streamable 服务器发起的服务器端请求：`@McpElicitation` 接受确认，`@McpSampling` 返回预置的摘要，`@McpLogging` 和 `@McpProgress` 打印收到的内容，`@McpToolListChanged` 打印运行时添加后的新工具列表。运行器仅在服务器列出交互式工具时才调用它们。

属于 [modern-migration](../README.md)。用 `./mvnw spring-boot:run` 运行；服务器监听 `http://localhost:8080`。
