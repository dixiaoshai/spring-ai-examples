# Spring AI LLM-as-a-Judge 演示

演示 Spring AI 2.0 中的 **LLM-as-a-Judge** 模式，使用自定义的 `SelfRefineEvaluationAdvisor` —— 一个递归顾问，用于评估 AI 响应，并在未达标时带上反馈重试，直到满足质量阈值。

## 工作原理

`SelfRefineEvaluationAdvisor` 实现了 `CallAdvisor`，并通过 `callAdvisorChain.copy(this).nextCall()` 进行递归循环：

1. 使用主模型（Anthropic Claude）生成响应
2. 使用专用的评审模型（Ollama）按 1–4 分制对其进行评估
3. 如果评分 < `successRating`，则用反馈增强提示词并重试
4. 当评分通过或 `maxRepeatAttempts` 用尽时返回

工具调用响应会被自动跳过 —— 只有最终文本答案会被评估。

```java
ChatClient chatClient = ChatClient.builder(anthropicChatModel)
    .defaultTools(new MyTools())
    .defaultAdvisors(
        SelfRefineEvaluationAdvisor.builder()
            .chatClientBuilder(ChatClient.builder(ollamaChatModel)) // separate judge
            .maxRepeatAttempts(15)
            .successRating(4)
            .order(0)
            .build(),
        new MyLoggingAdvisor(2))
    .build();
```

天气工具会返回随机温度（包括像 -255°C 这样物理上不可能的值），以刻意触发评估失败与重试。

## 示例输出

```
>>> Tool Call responseTemp: -255
Evaluation failed on attempt 1: temperature of -255°C is physically impossible.

>>> Tool Call responseTemp: 15
Evaluation passed on attempt 2: Excellent response with realistic weather data.

The current weather in Paris is sunny with a temperature of 15°C.
```

## 前置条件

- Java 17+、Maven 3.6+
- Anthropic API 密钥
- 本地运行的、带评审模型的 [Ollama](https://ollama.com)

## 配置

```bash
# Pull a judge model
ollama pull avcodes/flowaicom-flow-judge:q4

export ANTHROPIC_API_KEY=your-key
./mvnw spring-boot:run
```

`application.properties`（已配置）：
```properties
spring.ai.ollama.chat.model=avcodes/flowaicom-flow-judge:q4
spring.ai.ollama.chat.temperature=0
spring.ai.chat.client.enabled=false
```

## 参考

- [Recursive Advisor Demo](../recursive-advisor-demo) —— 基础递归模式
- [Spring AI Advisors](https://docs.spring.io/spring-ai/reference/api/advisors.html)
- [Judge Arena Leaderboard](https://huggingface.co/spaces/AtlaAI/judge-arena)
