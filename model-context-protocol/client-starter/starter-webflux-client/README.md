# Spring AI - MCP Starter WebFlux 客户端

一个命令行 Spring Boot 应用，演示在 WebFlux（响应式）传输下使用 [MCP 客户端 Boot Starter](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html)。

**技术栈：** Spring Boot 4.1.1 · Spring AI 2.1.0-SNAPSHOT · Java 17

## 工作原理

1. 通过 STDIO 或 SSE 传输连接到一个或多个 MCP 服务器
2. 将所有 MCP 工具注册到 `ChatClient`
3. 向 Claude 发送问题（通过 `ai.user.input` 配置）
4. 打印响应后退出

## 前置条件

- Java 17+
- Node.js / `npx`（用于 Brave Search MCP 服务器）
- `ANTHROPIC_API_KEY`
- `BRAVE_API_KEY`

## 配置

应用使用 `spring.ai.mcp.client.stdio.connections.<name>.*` 属性定义 STDIO 服务器：

```properties
spring.ai.mcp.client.stdio.connections.brave-search.command=npx
spring.ai.mcp.client.stdio.connections.brave-search.args=-y,@modelcontextprotocol/server-brave-search

spring.ai.mcp.client.toolcallback.enabled=true
ai.user.input=What tools are available?
```

也可以指向 Claude Desktop 风格的 JSON 文件：

```properties
spring.ai.mcp.client.stdio.servers-configuration=classpath:/mcp-servers-config.json
```

对于 SSE 服务器（WebFlux 传输）：

```properties
spring.ai.mcp.client.sse.connections.my-server.url=http://localhost:8080
```

## 运行

```bash
export ANTHROPIC_API_KEY=your-key
export BRAVE_API_KEY=your-key

./mvnw spring-boot:run

# Custom question
./mvnw spring-boot:run -Dai.user.input="Does Spring AI support MCP?"
```

或通过打包后的 jar 运行：

```bash
./mvnw clean package
java -Dai.user.input="What tools are available?" -jar target/mcp-starter-webflux-client-0.0.1-SNAPSHOT.jar
```
