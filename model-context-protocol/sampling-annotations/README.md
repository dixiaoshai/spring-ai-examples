# MCP Sampling — 基于注解

包含两个模块的示例，展示如何使用 Spring AI 注解实现 MCP Sampling。服务器暴露一个天气工具，它通过 MCP Sampling 向客户端的 LLM 索取诗歌；客户端则根据模型提示将这些 sampling 请求路由到 OpenAI 或 Anthropic。

```
mcp-sampling-server-annotations/   # MCP server — weather tool + sampling
mcp-sampling-client-annotations/   # MCP client — sampling handler + chat entry point
```

## 工作方式

1. 客户端发送提示词：*"What is the weather in Amsterdam right now?"*
2. 服务器的 `@McpTool` 从 Open-Meteo 获取实时天气，然后调用 `exchange.createMessage()` 两次 —— 一次提示 `openai`，一次提示 `anthropic`。
3. 客户端的 `@McpSampling` 处理器接收每个请求，选择匹配的 `ChatClient`，并返回一首诗。
4. 服务器将两首诗与原始天气数据结合，并把结果返回给客户端。

## 关键注解

| 注解 | 端 | 用途 |
|---|---|---|
| `@McpTool` | 服务器 | 将方法注册为 MCP 工具 |
| `@McpSampling` | 客户端 | 处理来自服务器的 sampling 请求 |
| `@McpLogging` | 客户端 | 接收服务器的日志通知 |
| `@McpProgress` | 客户端 | 接收服务器的进度通知 |

## 前置条件

- Java 17+
- OpenAI API 密钥
- Anthropic API 密钥

## 运行

**1. 启动服务器**

```bash
cd mcp-sampling-server-annotations
./mvnw spring-boot:run
```

使用 Streamable HTTP 传输，在 `http://localhost:8080` 上启动。

**2. 设置 API 密钥**

```bash
export OPENAI_API_KEY=your-openai-key
export ANTHROPIC_API_KEY=your-anthropic-key
```

**3. 运行客户端**

```bash
cd mcp-sampling-client-annotations
./mvnw spring-boot:run
```

## 配置

**服务器**（`application.properties`）：
```properties
spring.ai.mcp.server.name=mcp-sampling-server-annotations
spring.ai.mcp.server.version=0.0.1
spring.ai.mcp.server.protocol=STREAMABLE
spring.main.banner-mode=off
```

**客户端**（`application.properties`）：
```properties
spring.main.web-application-type=none
spring.ai.chat.client.enabled=false
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.anthropic.api-key=${ANTHROPIC_API_KEY}
spring.ai.mcp.client.streamable-http.connections.server1.url=http://localhost:8080
```

## 关键实现片段

**服务器 —— 带 sampling 的天气工具：**
```java
@Service
public class WeatherService {

    @McpTool(description = "Get the temperature (in celsius) for a specific location")
    public String getTemperature2(McpSyncServerExchange exchange,
            @McpToolParam(description = "The location latitude") double latitude,
            @McpToolParam(description = "The location longitude") double longitude) {

        // fetch weather, then request poems via MCP Sampling
        var request = McpSchema.CreateMessageRequest
            .builder(messages, 1000)
            .systemPrompt("You are a poet!")
            .modelPreferences(ModelPreferences.builder().addHint("openai").build())
            .build();
        CreateMessageResult result = exchange.createMessage(request);
        // ...
    }
}
```

**客户端 —— sampling 处理器：**
```java
@Service
public class McpClientHandlers {

    @McpSampling(clients = "server1")
    public CreateMessageResult samplingHandler(CreateMessageRequest llmRequest) {
        var userPrompt = ((TextContent) llmRequest.messages().get(0).content()).text();
        String modelHint = llmRequest.modelPreferences().hints().get(0).name();

        ChatClient hintedChatClient = chatClients.entrySet().stream()
            .filter(e -> e.getKey().contains(modelHint))
            .findFirst().orElseThrow().getValue();

        String response = hintedChatClient.prompt()
            .system(llmRequest.systemPrompt()).user(userPrompt).call().content();

        return CreateMessageResult.builder(Role.ASSISTANT, TextContent.builder(response).build(), "any").build();
    }
}
```

**客户端 —— 入口：**
```java
@Bean
public CommandLineRunner predefinedQuestions(OpenAiChatModel openAiChatModel,
        ToolCallbackProvider toolCallbackProvider) {
    return args -> {
        ChatClient chatClient = ChatClient.builder(openAiChatModel)
            .defaultTools(toolCallbackProvider).build();
        System.out.println(chatClient.prompt("""
            What is the weather in Amsterdam right now?
            Please incorporate all creative responses from all LLM providers.
            After the other providers add a poem that synthesizes the poems from all the other providers.
            """).call().content());
    };
}
```
