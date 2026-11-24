# 工具参数增强器演示

演示 Spring AI 的 [工具参数增强](https://docs.spring.io/spring-ai/reference/api/tools.html#tool-argument-augmentation) —— 一种在不修改底层工具实现的情况下，向工具调用注入额外参数（例如 LLM 推理过程、置信度、记忆笔记）的方法。

## 工作原理

```
User: "What is the weather in Paris?"
        │
        ▼
Tool schema augmented transparently:
  Original:  { location: string }
  Augmented: { location: string, innerThought: string,
               confidence: string, memoryNotes: string[] }
        │
        ▼
LLM fills all arguments including reasoning fields
        │
        ├─► argumentConsumer processes extra args (logging, observability)
        │
        └─► Original tool receives only: { location: "Paris" }
```

## 关键组件

**`AgentThinking`** —— 定义添加到每个工具 schema 的额外参数：

```java
public record AgentThinking(
    @ToolParam(description = "Step-by-step reasoning for calling this tool", required = true)
    String innerThought,

    @ToolParam(description = "Confidence level (low, medium, high)", required = false)
    String confidence,

    @ToolParam(description = "Key insights to remember for future interactions", required = true)
    List<String> memoryNotes
) {}
```

**`AugmentedToolCallbackProvider`** —— 包装工具并处理额外参数：

```java
AugmentedToolCallbackProvider<AgentThinking> provider = AugmentedToolCallbackProvider
    .<AgentThinking>builder()
    .toolObject(new MyTools())
    .argumentType(AgentThinking.class)
    .argumentConsumer(event -> {
        AgentThinking thinking = event.arguments();
        logger.info("LLM Reasoning: {}", thinking.innerThought());
        logger.info("Confidence: {}", thinking.confidence());
        logger.info("Memory Notes: {}", thinking.memoryNotes());
        logger.info("Tool: {}", event.toolDefinition().name());
    })
    .removeExtraArgumentsAfterProcessing(true) // strip before calling actual tool
    .build();
```

**接入 `ChatClient`，并配合会话记忆与日志顾问：**

```java
SessionService sessionService = DefaultSessionService.builder()
    .sessionRepository(InMemorySessionRepository.builder().build())
    .build();

ChatClient chatClient = chatClientBuilder
    .defaultTools(provider)
    .defaultAdvisors(SessionAdvisors.builder(sessionService)
        .memory(memory -> memory.eventFilter(EventFilter.lastN(100)))
        .build())
    .defaultAdvisors(new MyLogAdvisor())
    .build();

chatClient.prompt("What is current weather in Paris?")
    .advisors(SessionAdvisors.session("default-conversation"))
    .call()
    .entity(MyResponse.class);
```

## 前置条件

- Java 17+
- Spring Boot 4.1.1 / Spring AI 2.1.0-SNAPSHOT
- OpenAI API 密钥

## 运行

```bash
export OPENAI_API_KEY=your-api-key

cd advisors/tool-argument-augmenter-demo
./mvnw spring-boot:run
```

## 示例输出

```
LLM Reasoning: The user wants current weather in Paris. I should call the weather tool with location=Paris.
Confidence: high
Memory Notes: [User interested in Paris weather]
Tool: weather

MyResponse[result=The current weather in Paris is sunny with a temperature of 25°C., thinking=...]
```

## 资源

- [工具参数增强文档](https://docs.spring.io/spring-ai/reference/api/tools.html#tool-argument-augmentation)
- [可解释 AI 智能体博客文章](https://spring.io/blog/2025/12/21/explainable-ai-agents-capture-llm-tool-call-reasoning-with-spring-ai)
- [Spring AI 顾问指南](https://docs.spring.io/spring-ai/reference/api/advisors.html)
