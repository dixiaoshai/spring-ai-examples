# Spring AI 递归顾问演示

演示 Spring AI 2.0 中的 **递归顾问（Recursive Advisors）** 模式，使用 `ToolCallingAdvisor` 在顾问链内部处理工具调用循环。

## 概述

`ToolCallingAdvisor` 是一个内置的递归顾问，会迭代遍历顾问链，直到所有工具调用完成。它默认**自动注册** —— 只有在需要自定义其配置或控制其在链中的位置时，才需要显式添加。

```java
ChatClient chatClient = chatClientBuilder
    .defaultTools(new MyTools())
    .defaultAdvisors(
        ToolCallingAdvisor.builder().build(), // optional: auto-registered if absent
        new MyLogAdvisor())                   // logs each iteration
    .build();
```

`MyLogAdvisor` 实现了 `BaseAdvisor`，在每次递归迭代时记录请求/响应，从而可观察工具调用循环的过程：

```
REQUEST: [user: "What is current weather in Paris?"]
RESPONSE: [assistant tool call → weather("Paris")]

REQUEST: [user + assistant tool call + tool result]
RESPONSE: [assistant: "The current weather in Paris is sunny with a temperature of 25°C."]
```

## 前置条件

- Java 17+
- Maven 3.6+
- Anthropic API 密钥（`ANTHROPIC_API_KEY`）

## 运行

```bash
export ANTHROPIC_API_KEY=your-key
./mvnw spring-boot:run
```

## 参考

- [Spring AI Advisors](https://docs.spring.io/spring-ai/reference/api/advisors.html)
- [Spring AI 工具调用](https://docs.spring.io/spring-ai/reference/api/tools.html)
