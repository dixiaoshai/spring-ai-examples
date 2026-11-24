# Spring Boot OpenAI 流式集成

这个 Spring Boot 应用演示如何通过 Spring AI 与 OpenAI 的 API 进行实时流式集成。它提供一个简单的端点，使用 Spring WebFlux 以流式方式返回 AI 响应。

## 前置条件

- Java 17 或更高版本
- Maven
- OpenAI API 密钥

所有必要的依赖（包括用于 web、webflux 和 OpenAI 的 Spring Boot starter）都已在 Maven 的 pom.xml 中声明。

## 配置

1. 创建 `application.properties` 或 `application.yml` 文件，放在 `src/main/resources` 中
2. 添加你的 OpenAI API 密钥：

```properties
spring.ai.openai.api-key=your-api-key-here
```

## 运行应用

1. 克隆本仓库
2. 按上述说明配置你的 OpenAI API 密钥
3. 运行应用：
```bash
./mvnw spring-boot:run
```

应用默认会在 8080 端口启动。

## 用法

应用公开一个流式端点，以响应式流的形式返回 OpenAI 响应。

### 端点

```
GET /ai/generateStream
```

#### 参数
- `message`（可选）：发送给 OpenAI 的输入消息
    - 默认值："Tell me a joke"

#### 使用 curl 测试

你可以使用 curl 测试该流式端点。以下命令会流式获取响应，并使用 `jq` 进行格式化：

```bash
curl localhost:8080/ai/generateStream | sed 's/data://' | jq .
```

## 实现细节

应用使用了：
- Spring WebFlux 实现响应式流式传输
- Spring AI 的 OpenAI 集成实现 AI 模型交互

主要组件包括：
1. `OpenAiStreamingApplication`：Spring Boot 应用入口
2. `ChatController`：处理流式端点的 REST 控制器

### 代码细节

流式端点的实现如下：

```java
@GetMapping(value = "/generateStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ChatResponse> generateStream(
       @RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
    return chatModel.stream(new Prompt(new UserMessage(message)));
}
```

要点：
- 端点使用 `MediaType.TEXT_EVENT_STREAM_VALUE` 表示响应将以文本事件流的形式流式传输。
- 返回 `Flux<ChatResponse>`，这是 Spring WebFlux 用于处理多元素流的响应式类型。
- 接收可选的 `message` 参数，默认值为 "Tell me a joke"。
- 使用 Spring AI 的 `chatModel.stream()` 方法，它返回来自 OpenAI 的响应式响应流。
- `Prompt` 和 `UserMessage` 类由 Spring AI 提供，用于构造发送给 OpenAI 的请求。

调用时，该端点会：
1. 接收用户的输入消息（或使用默认值）
2. 将其包装到 Spring AI 的 `Prompt` 对象中
3. 实时将 AI 的响应流式返回给客户端
4. 每个响应分块都会自动序列化为 JSON
