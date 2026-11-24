# Spring AI MCP Sampling

演示 MCP Sampling —— 该能力允许 MCP 服务器将 LLM 请求委托回客户端，从而在单次交互中实现多模型工作流。

## 项目

- **[mcp-sampling-server](./mcp-sampling-server)**：MCP 天气服务器，通过 sampling 将诗歌生成委托给客户端
- **[mcp-sampling-client](./mcp-sampling-client)**：MCP 客户端，根据模型提示将 sampling 请求路由到 OpenAI 或 Anthropic

## 工作原理

1. 客户端连接到 MCP Weather Server 并注册 sampling 处理器
2. 用户发送天气查询；服务器从 Open-Meteo 获取天气数据
3. 服务器向客户端发回两个 sampling 请求 —— 一个提示 `openai`，一个提示 `anthropic`
4. 客户端将每个请求路由到匹配的 `ChatClient`，并返回生成的诗歌
5. 服务器将两首诗与原始天气数据结合后返回结果

![MCP Sampling Sequence Diagram](./mvc-sampling-sq.svg)

## 服务器实现

```java
McpToolUtils.getMcpExchange(toolContext).ifPresent(exchange -> {

    exchange.loggingNotification(LoggingMessageNotification.builder(LoggingLevel.INFO, "Start sampling").build());

    if (exchange.getClientCapabilities().sampling() != null) {
        var messageRequestBuilder = McpSchema.CreateMessageRequest
            .builder(List.of(new McpSchema.SamplingMessage(McpSchema.Role.USER,
                TextContent.builder("Please write a poem about this weather forecast (temperature is in Celsius). Use markdown format :\n "
                    + JacksonUtils.getDefaultJsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(weatherResponse))
                    .build())), 2000)
            .systemPrompt("You are a poet!");

        CreateMessageResult openAiResponse = exchange.createMessage(
            messageRequestBuilder.modelPreferences(ModelPreferences.builder().addHint("openai").build()).build());

        CreateMessageResult anthropicResponse = exchange.createMessage(
            messageRequestBuilder.modelPreferences(ModelPreferences.builder().addHint("anthropic").build()).build());
    }

    exchange.loggingNotification(LoggingMessageNotification.builder(LoggingLevel.INFO, "Finish Sampling").build());
});
```

## 客户端实现

```java
@Bean
McpClientCustomizer<McpClient.SyncSpec> samplingCustomizer(Map<String, ChatClient> chatClients) {
    return (name, mcpClientSpec) -> {
        mcpClientSpec = mcpClientSpec.loggingConsumer(msg ->
            System.out.println("MCP LOGGING: [" + msg.level() + "] " + msg.data()));

        mcpClientSpec.sampling(llmRequest -> {
            var userPrompt = ((TextContent) llmRequest.messages().get(0).content()).text();
            String modelHint = llmRequest.modelPreferences().hints().get(0).name();

            ChatClient chatClient = chatClients.entrySet().stream()
                .filter(e -> e.getKey().contains(modelHint))
                .findFirst().orElseThrow().getValue();

            String response = chatClient.prompt()
                .system(llmRequest.systemPrompt())
                .user(userPrompt)
                .call().content();

            return CreateMessageResult.builder(Role.ASSISTANT, TextContent.builder(response).build(), modelHint).build();
        });
    };
}
```

> **注意**：请禁用 MCP 工具回调自动配置，以避免循环依赖：
> `spring.ai.mcp.client.toolcallback.enabled=false`

## 运行

**前置条件**：Java 17+、Maven 3.6+、`OPENAI_API_KEY`、`ANTHROPIC_API_KEY`

```bash
# Terminal 1 — start the server
cd mcp-sampling-server
./mvnw clean package -DskipTests
java -jar target/mcp-sampling-server-0.0.1-SNAPSHOT.jar

# Terminal 2 — run the client
export OPENAI_API_KEY=your-openai-key
export ANTHROPIC_API_KEY=your-anthropic-key
cd mcp-sampling-client
./mvnw clean package
java -Dai.user.input='What is the weather in Amsterdam right now?' -jar target/mcp-sampling-client-0.0.1-SNAPSHOT.jar
```

## 资源

* [Spring AI MCP 客户端 Starter](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html)
* [Spring AI MCP 服务器 Starter](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html)
* [模型上下文协议规范](https://modelcontextprotocol.github.io/specification/)
