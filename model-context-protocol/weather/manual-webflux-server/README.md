# MCP 天气服务器 — 手动 WebFlux

一个手动构建（不使用 MCP starter）的 Spring AI MCP 服务器，使用 Spring WebFlux 实现。同时支持 **stdio** 和 **SSE** 传输，并通过 [National Weather Service API](https://www.weather.gov/documentation/services-web-api) 暴露天气预报工具。

## 构建

```bash
./mvnw clean install -DskipTests
```

## 运行服务器

由 `transport.mode` 属性控制：

**SSE 模式**（8080 端口上的 HTTP 服务器）：
```bash
java -Dtransport.mode=sse -jar target/mcp-weather-server-0.0.1-SNAPSHOT.jar
```

**Stdio 模式**（由 stdio 客户端自动启动）：
```bash
java -Dtransport.mode=stdio -Dspring.main.web-application-type=none -jar target/mcp-weather-server-0.0.1-SNAPSHOT.jar
```

## 示例客户端

构建后从仓库根目录运行：

**SSE 客户端** — 连接到正在运行的 SSE 服务器：
```bash
# Start server first (SSE mode), then:
java -cp target/mcp-weather-server-0.0.1-SNAPSHOT.jar \
  org.springframework.ai.mcp.sample.client.ClientSse
```

**Stdio 客户端** — 自动以子进程方式启动服务器：
```bash
java -cp target/mcp-weather-server-0.0.1-SNAPSHOT.jar \
  org.springframework.ai.mcp.sample.client.ClientStdio
```

## 可用的工具

| 工具 | 描述 | 参数 |
|------|-------------|------------|
| `getWeatherForecastByLocation` | 按坐标获取天气预报 | `latitude`、`longitude` |
| `getAlerts` | 按美国州获取当前天气预警 | `state`（例如 `NY`） |

## 服务器配置

服务器在 `McpServerConfig` 中手动装配：

```java
McpSyncServer server = McpServer.sync(transportProvider)
    .serverInfo("MCP Demo Weather Server", "1.0.0")
    .capabilities(McpSchema.ServerCapabilities.builder().tools(true).logging().build())
    .tools(McpToolUtils.toSyncToolSpecifications(ToolCallbacks.from(weatherApiClient)))
    .build();
```

传输提供者通过 `transport.mode` 条件启用：
- `stdio` → `StdioServerTransportProvider`
- `sse` → `WebFluxSseServerTransportProvider`（端点：`/mcp/message`，SSE：`/sse`）
