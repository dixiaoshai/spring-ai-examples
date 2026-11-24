# Spring AI MCP 注解示例

展示使用 Spring AI 进行注解驱动的 MCP 开发。涵盖服务器端工具、资源、提示词与补全，以及客户端通知处理器 —— 全部基于声明式注解。

**版本：** Spring Boot `4.1.1` · Spring AI `2.1.0-SNAPSHOT`

## 模块

### 服务器：[mcp-annotations-server](mcp-annotations-server/)

MCP 服务器，包含四个 provider Bean：

| Provider | 注解风格 | 能力 |
|----------|-----------------|-------------|
| `SpringAiToolProvider` | `@Tool` | 天气预报与警报（weather.gov） |
| `McpToolProvider` | `@McpTool` | 带进度 token 的温度查询（open-meteo.com） |
| `DocumentProvider` | `@McpTool` / `@McpResource` / `@McpPrompt` | 读取/编辑本地 `docs/` 目录中的文档；列出并获取文档资源；Markdown `format` 提示词 |
| `ToolProvider2` | `@McpTool` | 综合工具，演示日志、进度、sampling 与 elicitation |
| `UserProfileResourceProvider` | `@McpResource` | 用户档案、属性、状态、连接、通知、位置、头像 |
| `PromptProvider` | `@McpPrompt` | `greeting`、`personalized-message`、`conversation-starter`、`map-arguments`、`single-message`、`string-list` |
| `AutocompleteProvider` | `@McpComplete` | 用户名、姓名与国家补全 |

依赖：`spring-ai-starter-mcp-server-webmvc`

### 客户端：[mcp-annotations-client](mcp-annotations-client/)

MCP 客户端，连接到服务器并演示客户端处理器：

| 注解 | 处理器 |
|------------|---------|
| `@McpProgress` | 记录进度通知 |
| `@McpLogging` | 记录服务器日志消息 |
| `@McpSampling` | 将 LLM 请求转交给模型处理 |
| `@McpElicitation` | 处理结构化 elicitation 请求（`StructuredElicitResult<Person>`） |

`CommandLineRunner` 使用 builder API 演练工具调用、补全、提示词获取与资源读取：

```java
CallToolRequest.builder("tool1").arguments(...).progressToken(666).build();
CompleteRequest.builder(new PromptReference("personalized-message"), new CompleteArgument("name", "J")).build();
GetPromptRequest.builder("personalized-message").arguments(Map.of("name", nameValue)).build();
ReadResourceRequest.builder("user-status://alice").build();
```

依赖：`spring-ai-starter-mcp-client`

## 快速开始

**1. 构建并启动服务器**

```bash
cd mcp-annotations-server
./mvnw clean package -DskipTests
java -Dspring.ai.mcp.server.protocol=STREAMABLE -jar target/mcp-annotations-server-0.0.1-SNAPSHOT.jar
```

**2. 运行客户端**

```bash
cd mcp-annotations-client
./mvnw clean package -DskipTests
java -Dspring.ai.mcp.client.streamable-http.connections.server1.url=http://localhost:8080 \
     -jar target/mcp-annotations-client-0.0.1-SNAPSHOT.jar
```

> 使用 SSE 时，将 `STREAMABLE` / `streamable-http` 替换为 `SSE` / `sse`。  
> 使用 STDIO 时，请参阅服务器 README。

## 更多资源

- [MCP 服务器 Boot Starter](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html)
- [MCP 客户端 Boot Starter](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html)
- [MCP Annotations 项目](https://github.com/spring-ai-community/mcp-annotations)
- [模型上下文协议规范](https://modelcontextprotocol.github.io/specification/)
