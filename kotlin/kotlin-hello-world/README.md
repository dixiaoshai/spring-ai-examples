# Kotlin Spring AI Hello World 应用

一个用 Kotlin 编写的简单 "Hello World" Spring Boot 应用，通过从 OpenAI 获取笑话来演示 Spring AI 集成。

## 概述

这个 hello world 应用展示了如何在 Kotlin Spring Boot 项目中开始使用 Spring AI 与 OpenAI。运行时，它会连接 OpenAI 的 API 并请求一个笑话，然后显示铺垫和笑点——这是对传统 "Hello World" 示例的有趣改编。应用通过使用 `org.springframework.ai.chat.client.entity` 将聊天响应自动转换为 `Joke` 数据对象，展示了 Kotlin 扩展函数的能力，说明 Kotlin 的类型安全构建器如何让 AI 响应处理更优雅、更类型安全。

## 前置条件

- JDK 17 或更高版本
- OpenAI API 密钥

## 依赖

应用使用以下主要依赖：
- Spring Boot
- Spring AI OpenAI starter
- Kotlin 标准库与反射
- Spring Web starter

## 配置

将你的 OpenAI API 密钥添加到 `application.properties` 或 `application.yml`：
```yaml
spring.ai.openai.api-key=your-api-key-here
```

## 工作原理

应用使用 Spring AI 的 OpenAI 集成调用 OpenAI 服务。主要组件包括：

1. 一个 Spring Boot 应用类（`KotlinHelloWorldApplication`）
2. 一个 `CommandLineRunner` Bean，它：
- 创建聊天客户端
- 发送请求笑话的提示词
- 将响应解析为 `Joke` 对象
- 打印铺垫和笑点

## 运行应用

```bash
./mvnw spring-boot:run
```

应用将启动，从 OpenAI 获取一个笑话并显示在控制台中。

## 示例输出

```
Joke:
Setup: Why don't programmers like nature?
Punchline: It has too many bugs!
```

## 说明

- 请务必妥善保管你的 OpenAI API 密钥，切勿将其提交到版本控制中
- 应用需要有效的互联网连接才能与 OpenAI 的 API 通信
- 根据你的 OpenAI 账户设置，可能会受到速率限制并产生 API 使用费用
