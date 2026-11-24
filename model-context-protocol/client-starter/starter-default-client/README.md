# MCP Starter 默认客户端

基于 Spring Boot 4 / Spring AI 2.0 的命令行应用，展示使用 [MCP 客户端 Boot Starter](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html) 的最简 MCP 客户端配置。

该应用通过 STDIO 连接到 Brave Search MCP 服务器，将所有已发现的工具注入 `ChatClient`，发送一个问题，打印答案后退出。

## 前置条件

- Java 17+
- Node.js / `npx`（用于 Brave Search MCP 服务器）
- `ANTHROPIC_API_KEY`
- `BRAVE_API_KEY`

## 关键依赖

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-client</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-anthropic</artifactId>
</dependency>
```

## 配置

`application.properties` 通过 STDIO 配置 Brave Search 服务器并启用工具回调：

```properties
spring.ai.mcp.client.stdio.connections.brave-search.command=npx
spring.ai.mcp.client.stdio.connections.brave-search.args=-y,@modelcontextprotocol/server-brave-search

spring.ai.mcp.client.toolcallback.enabled=true

ai.user.input=What tools are available?
```

也可以指向 Claude Desktop 风格的 JSON 配置文件：

```properties
spring.ai.mcp.client.stdio.servers-configuration=classpath:/mcp-servers-config.json
```

同样支持 SSE（HTTP）连接：

```properties
spring.ai.mcp.client.sse.connections.server1.url=http://localhost:8080
```

## 运行

```bash
export ANTHROPIC_API_KEY=<your-key>
export BRAVE_API_KEY=<your-key>

./mvnw spring-boot:run

# Or with a custom question
./mvnw spring-boot:run -Dai.user.input="Does Spring AI support MCP?"
```

## 工作原理

`ToolCallbackProvider` 由 starter 自动配置，将所有 MCP 工具暴露为 Spring AI 工具回调。应用会直接将其注入 `ChatClient`：

```java
var chatClient = chatClientBuilder.defaultTools(tools).build();
System.out.println(chatClient.prompt(userInput).call().content());
```

## 资源

- [MCP 客户端 Boot Starter 文档](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html)
- [Spring AI 参考文档](https://docs.spring.io/spring-ai/reference/)
- [MCP 规范](https://modelcontextprotocol.io/specification)
