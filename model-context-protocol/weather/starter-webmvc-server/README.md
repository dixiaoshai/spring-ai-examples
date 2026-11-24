# Spring AI MCP 天气服务器示例（WebMVC Starter）

演示使用 Spring AI MCP Server Boot Starter 与 WebMVC 传输的 MCP 服务器，通过 National Weather Service API 暴露天气工具。

更多信息请参见 [MCP Server Boot Starter](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html) 参考文档。

## 概述

- 与 `spring-ai-mcp-server-webmvc-spring-boot-starter` 集成
- 支持 SSE、Streamable HTTP 和 STDIO 传输
- 通过 `@McpTool` 注解自动注册工具（无需手动装配 Bean）
- 两个天气工具：按经纬度获取预报、按美国州获取预警

## 依赖

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-mcp-server-webmvc-spring-boot-starter</artifactId>
</dependency>
```

## 构建

```bash
./mvnw clean install -DskipTests
```

## 运行服务器

### WebMVC SSE / Streamable HTTP（默认）
```bash
java -jar target/mcp-weather-starter-webmvc-server-0.0.1-SNAPSHOT.jar
```
服务器在 8080 端口启动。

### STDIO 模式
```bash
java -Dspring.ai.mcp.server.stdio=true -Dspring.main.web-application-type=none \
  -jar target/mcp-weather-starter-webmvc-server-0.0.1-SNAPSHOT.jar
```

## 配置

```properties
spring.ai.mcp.server.name=my-weather-server
spring.ai.mcp.server.version=0.0.1
spring.ai.mcp.server.type=SYNC
spring.ai.mcp.server.stdio=false
spring.ai.mcp.server.sse-message-endpoint=/mcp/message

# Required for STDIO transport
spring.main.banner-mode=off
logging.file.name=./target/starter-webmvc-server.log
```

## 工具注册

工具通过 `@McpTool` 自动注册 —— 无需 `ToolCallbackProvider` Bean：

```java
@Service
public class WeatherService {

    @McpTool(description = "Get weather forecast for a specific latitude/longitude")
    public String getWeatherForecastByLocation(double latitude, double longitude) { ... }

    @McpTool(description = "Get weather alerts for a US state. Input is Two-letter US state code (e.g. CA, NY)")
    public String getAlerts(String state) { ... }
}
```

## 可用的工具

| 工具 | 参数 | 描述 |
|------|------------|-------------|
| `getWeatherForecastByLocation` | `latitude: double`、`longitude: double` | 按位置获取天气预报 |
| `getAlerts` | `state: String` | 按美国州（例如 `NY`）获取当前天气预警 |

## MCP 客户端

示例包含三个客户端实现：

- [SampleClient.java](src/main/java/org/springframework/ai/mcp/sample/client/SampleClient.java) — SSE 传输
- [ClientStdio.java](src/main/java/org/springframework/ai/mcp/sample/client/ClientStdio.java) — STDIO 传输
- [StreamableHttpClient.java](src/main/java/org/springframework/ai/mcp/sample/client/StreamableHttpClient.java) — Streamable HTTP 传输

### SSE 客户端
```java
var transport = HttpClientSseClientTransport.builder("http://localhost:8080").build();
var client = McpClient.sync(transport).build();
```

### Streamable HTTP 客户端
```java
var transport = HttpClientStreamableHttpTransport.builder("http://localhost:8080").build();
var client = McpClient.sync(transport).build();
```

### STDIO 客户端
```java
var stdioParams = ServerParameters.builder("java")
    .args("-Dspring.ai.mcp.server.stdio=true",
          "-Dspring.main.web-application-type=none",
          "-Dlogging.pattern.console=",
          "-jar", "target/mcp-weather-starter-webmvc-server-0.0.1-SNAPSHOT.jar")
    .build();
var client = McpClient.sync(new StdioClientTransport(stdioParams)).build();
```

## 使用 Boot Starter 客户端

构建 [starter-default-client](../../client-starter/starter-default-client)，并通过 STDIO 或 SSE 连接。

### STDIO 传输

`mcp-servers-config.json`：
```json
{
  "mcpServers": {
    "weather-starter-webmvc-server": {
      "command": "java",
      "args": [
        "-Dspring.ai.mcp.server.stdio=true",
        "-Dspring.main.web-application-type=none",
        "-Dlogging.pattern.console=",
        "-jar", "/absolute/path/to/mcp-weather-starter-webmvc-server-0.0.1-SNAPSHOT.jar"
      ]
    }
  }
}
```

```bash
java -Dspring.ai.mcp.client.stdio.servers-configuration=file:mcp-servers-config.json \
     -Dai.user.input='What is the weather in NY?' \
     -Dlogging.pattern.console= \
     -jar mcp-starter-default-client-0.0.1-SNAPSHOT.jar
```

### SSE 传输

```bash
java -Dspring.ai.mcp.client.sse.connections.weather-server.url=http://localhost:8080 \
     -Dai.user.input='What is the weather in NY?' \
     -Dlogging.pattern.console= \
     -jar mcp-starter-default-client-0.0.1-SNAPSHOT.jar
```

## 附加资源

- [Spring AI 文档](https://docs.spring.io/spring-ai/reference/)
- [MCP Server Boot Starter](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html)
- [MCP Client Boot Starter](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html)
- [Model Context Protocol 规范](https://modelcontextprotocol.github.io/specification/)
