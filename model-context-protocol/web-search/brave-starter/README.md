# Spring AI - MCP Brave 搜索（Starter）

使用 Spring Boot 自动配置，演示 Spring AI 的 MCP 客户端与 [Brave Search MCP Server](https://github.com/modelcontextprotocol/servers/tree/main/src/brave-search) 的集成。应用启动时提出一个预定义问题，打印答案后退出。

与手动方式不同，MCP 客户端完全通过 `application.properties` 与 `mcp-servers-config.json` 自动配置 —— 无需显式定义 Bean。

<img src="spring-ai-mcp-brave.jpg" width="600"/>

## 前置条件

- Java 17+
- Maven 3.6+
- npx（通过 [npm](https://docs.npmjs.com/downloading-and-installing-node-js-and-npm)）
- Anthropic API 密钥
- Brave Search API 密钥 —— [在此获取](https://brave.com/search/api/)

## 安装配置

```bash
npm install -g npx

git clone https://github.com/spring-projects/spring-ai-examples.git
cd spring-ai-examples/model-context-protocol/web-search/brave-starter

export ANTHROPIC_API_KEY='your-anthropic-api-key'
export BRAVE_API_KEY='your-brave-api-key'

./mvnw clean install
```

## 运行

```bash
./mvnw spring-boot:run
```

## 工作原理

**依赖**（`pom.xml`）：
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

**MCP 配置** —— 选择一种方式：

方式 A —— 直接写在 `application.properties` 中：
```properties
spring.ai.mcp.client.stdio.connections.brave-search.command=npx
spring.ai.mcp.client.stdio.connections.brave-search.args=-y,@modelcontextprotocol/server-brave-search
```

方式 B —— 使用外部 `mcp-servers-config.json`：
```properties
spring.ai.mcp.client.stdio.servers-configuration=classpath:/mcp-servers-config.json
```
```json
{
  "mcpServers": {
    "brave-search": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-brave-search"],
      "env": { "BRAVE_API_KEY": "${BRAVE_API_KEY}" }
    }
  }
}
```

**应用代码**：
```java
@Bean
public CommandLineRunner predefinedQuestions(ChatClient.Builder chatClientBuilder,
        List<McpSyncClient> mcpSyncClients, ConfigurableApplicationContext context) {
    return args -> {
        var chatClient = chatClientBuilder
            .defaultTools(SyncMcpToolCallbackProvider.builder().mcpClients(mcpSyncClients).build())
            .build();

        String question = "Does Spring AI support the Model Context Protocol? Please provide some references.";
        System.out.println("QUESTION: " + question);
        System.out.println("ASSISTANT: " + chatClient.prompt(question).call().content());

        context.close();
    };
}
```

## 版本

| 依赖 | 版本 |
|---|---|
| Spring Boot | 4.1.1 |
| Spring AI | 2.1.0-SNAPSHOT |
