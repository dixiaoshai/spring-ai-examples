# Spring AI MCP 天气服务器 — WebFlux Starter

使用 Spring AI WebFlux starter 构建的 MCP 服务器。通过 [Streamable HTTP](https://modelcontextprotocol.io/specification/2025-03-26/basic/transports#streamable-http)（默认）和 STDIO 传输暴露天气工具。

参考文档：[MCP Server Boot Starter](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html)

## 依赖

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webflux</artifactId>
</dependency>
```

## 构建

```bash
./mvnw clean install -DskipTests
```

## 运行

### Streamable HTTP（默认，8080 端口）

```bash
java -jar target/mcp-weather-starter-webflux-server-0.0.1-SNAPSHOT.jar
```

### STDIO

```bash
java -Dspring.ai.mcp.server.stdio=true \
     -Dspring.main.web-application-type=none \
     -Dspring.main.banner-mode=off \
     -Dlogging.pattern.console= \
     -jar target/mcp-weather-starter-webflux-server-0.0.1-SNAPSHOT.jar
```

## 配置

```properties
spring.ai.mcp.server.name=my-weather-server
spring.ai.mcp.server.version=0.0.1
spring.ai.mcp.server.protocol=STREAMABLE

# Required when using STDIO transport
# spring.main.banner-mode=off
# logging.pattern.console=
```

## 工具

| 工具 | 描述 |
|------|-------------|
| `getWeatherForecastByLocation` | 按纬度/经度获取预报（weather.gov） |
| `getAlerts` | 按美国两位州代码（例如 `NY`）获取当前预警 |
| `toUpperCase` | 将输入文本转换为大写 |

天气工具通过 [WeatherService](src/main/java/org/springframework/ai/mcp/sample/server/WeatherService.java) 上的 `@McpTool` 注册。`toUpperCase` 在 [McpServerApplication](src/main/java/org/springframework/ai/mcp/sample/server/McpServerApplication.java) 中以编程方式注册。

## 示例客户端

在 [src/main/java/.../client/](src/main/java/org/springframework/ai/mcp/sample/client/) 下提供了三个独立客户端：

| 客户端 | 传输 |
|--------|-----------|
| [StreamableHttpClient.java](src/main/java/org/springframework/ai/mcp/sample/client/StreamableHttpClient.java) | Streamable HTTP（`WebClientStreamableHttpTransport`） |
| [ClientSse.java](src/main/java/org/springframework/ai/mcp/sample/client/ClientSse.java) | SSE（`WebFluxSseClientTransport`） |
| [ClientStdio.java](src/main/java/org/springframework/ai/mcp/sample/client/ClientStdio.java) | STDIO（`StdioClientTransport`）—— 自动启动服务器 |

### Streamable HTTP 传输

```java
var transport = WebClientStreamableHttpTransport.builder(
        WebClient.builder().baseUrl("http://localhost:8080"))
    .build();
var client = McpClient.sync(transport).build();
client.initialize();
```

### SSE 传输

```java
var transport = new WebFluxSseClientTransport(
        WebClient.builder().baseUrl("http://localhost:8080"),
        McpJsonDefaults.getMapper());
var client = McpClient.sync(transport).build();
client.initialize();
```

### STDIO 传输

```java
var params = ServerParameters.builder("java")
    .args("-Dspring.ai.mcp.server.stdio=true",
          "-Dspring.main.web-application-type=none",
          "-Dspring.main.banner-mode=off",
          "-Dlogging.pattern.console=",
          "-jar", "target/mcp-weather-starter-webflux-server-0.0.1-SNAPSHOT.jar")
    .build();
var transport = new StdioClientTransport(params, McpJsonDefaults.getMapper());
var client = McpClient.sync(transport).build();
client.initialize();
```

## Boot Starter 客户端

如需更高级的客户端体验，请参见 [starter-default-client](../../client-starter/starter-default-client) 和 [starter-webflux-client](../../client-starter/starter-webflux-client) 项目。它们通过 `mcp-servers-config.json` 自动配置 STDIO 和/或 SSE 连接。

### 通过 starter 客户端使用 STDIO

`mcp-servers-config.json`：
```json
{
  "mcpServers": {
    "weather": {
      "command": "java",
      "args": [
        "-Dspring.ai.mcp.server.stdio=true",
        "-Dspring.main.web-application-type=none",
        "-Dlogging.pattern.console=",
        "-jar",
        "/absolute/path/to/mcp-weather-starter-webflux-server-0.0.1-SNAPSHOT.jar"
      ]
    }
  }
}
```

```bash
java -Dspring.ai.mcp.client.stdio.servers-configuration=file:mcp-servers-config.json \
     -Dai.user.input='What is the weather in NY?' \
     -Dlogging.pattern.console= \
     -jar mcp-starter-webflux-client-0.0.1-SNAPSHOT.jar
```

### 通过 starter 客户端使用 SSE

```bash
# Terminal 1 — start the server
java -jar mcp-weather-starter-webflux-server-0.0.1-SNAPSHOT.jar

# Terminal 2 — run the client
java -Dspring.ai.mcp.client.sse.connections.weather-server.url=http://localhost:8080 \
     -Dai.user.input='What is the weather in NY?' \
     -Dlogging.pattern.console= \
     -jar mcp-starter-webflux-client-0.0.1-SNAPSHOT.jar
```
