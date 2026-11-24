# Spring AI MCP 注解服务器

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

演示如何使用 Spring AI 的注解驱动模型构建 MCP 服务器。以声明式、基于注解的方式涵盖工具、资源、提示词与补全。

**依赖：** Spring Boot `4.1.1`，Spring AI `2.1.0-SNAPSHOT`

参考：[MCP 服务器 Boot Starter](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html)

## 能力

### 工具

| Provider | 注解 | 工具 |
|----------|-----------|-------|
| `SpringAiToolProvider` | `@Tool` | `getWeatherForecastByLocation`、`getAlerts` |
| `McpToolProvider` | `@McpTool` | `getTemperature`（带进度 token） |
| `DocumentProvider` | `@McpTool` | `read_doc_contents`、`edit_document` |
| `ToolProvider2` | `@McpTool` | `toolLggingSamplingElicitationProgress`（日志、sampling、elicitation、进度） |

### 资源

| URI | 说明 |
|-----|-------------|
| `user-profile://{username}` | 用户档案信息 |
| `user-attribute://{username}/{attribute}` | 指定的档案属性 |
| `user-profile-exchange://{username}` | 带服务器交换上下文的档案 |
| `user-connections://{username}` | 用户连接列表 |
| `user-notifications://{username}` | 用户通知 |
| `user-status://{username}` | 用户状态 |
| `user-location://{username}` | 用户位置 |
| `user-avatar://{username}` | Base64 编码的头像（image/png） |
| `docs://documents` | 列出所有文档（application/json） |
| `docs://documents/{docId}` | 获取文档内容 |
| `static://hello` | 静态 "Hello World!" 资源 |

### 提示词

`greeting`, `personalized-message`, `conversation-starter`, `map-arguments`, `single-message`, `string-list`, `format`（通过 `DocumentProvider` 重排 Markdown）

### 补全

`@McpComplete` 作用于 `user-status://{username}`（用户名建议），`personalized-message` 提示词（姓名建议），`travel-planner` 提示词（国家名称）。

## 构建

```bash
./mvnw clean install -DskipTests
```

## 运行

### WebMVC 传输（SSE / Streamable-HTTP / Stateless）

```bash
# Streamable-HTTP (default in application.properties)
java -Dspring.ai.mcp.server.protocol=STREAMABLE -jar target/mcp-annotations-server-0.0.1-SNAPSHOT.jar

# SSE
java -Dspring.ai.mcp.server.protocol=SSE -jar target/mcp-annotations-server-0.0.1-SNAPSHOT.jar
```

### STDIO

```bash
java -Dspring.ai.mcp.server.stdio=true \
     -Dspring.main.web-application-type=none \
     -jar target/mcp-annotations-server-0.0.1-SNAPSHOT.jar
```

## 配置

```properties
spring.ai.mcp.server.name=my-weather-server
spring.ai.mcp.server.version=0.0.1
spring.ai.mcp.server.protocol=STREAMABLE   # SSE | STREAMABLE | STATELESS

# STDIO transport (uncomment to enable)
# spring.ai.mcp.server.stdio=true
# spring.main.web-application-type=none
# logging.pattern.console=
```

## 连接客户端

请参阅配套的 `mcp-annotations-client` 模块。在 `application.properties` 中配置传输方式：

```properties
# Streamable-HTTP
spring.ai.mcp.client.streamable-http.connections.server1.url=http://localhost:8080

# SSE (alternative)
# spring.ai.mcp.client.sse.connections.server1.url=http://localhost:8080
```

使用 STDIO 时，请使用 `mcp-servers-config.json`：

```json
{
  "mcpServers": {
    "annotations-server": {
      "command": "java",
      "args": [
        "-Dspring.ai.mcp.server.stdio=true",
        "-Dspring.main.web-application-type=none",
        "-Dlogging.pattern.console=",
        "-jar", "/absolute/path/to/mcp-annotations-server-0.0.1-SNAPSHOT.jar"
      ]
    }
  }
}
```

## 更多资源

- [MCP 服务器 Boot Starter](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html)
- [MCP 客户端 Boot Starter](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html)
- [MCP Annotations 项目](https://github.com/spring-ai-community/mcp-annotations)
- [模型上下文协议规范](https://modelcontextprotocol.github.io/specification/)
