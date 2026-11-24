# Spring AI - MCP Brave 搜索聊天机器人

交互式聊天机器人，将 Spring AI 的模型上下文协议（MCP）与 [Brave Search MCP Server](https://github.com/modelcontextprotocol/servers/tree/main/src/brave-search) 结合。它通过 [会话记忆](https://docs.spring.io/spring-ai/reference/api/session.html)（`SessionAdvisors`）并配合显式会话 ID 维护对话历史，同时借助 Brave Search 执行实时 Web 搜索。

<img src="spring-ai-mcp-brave.jpg" width="600"/>

## 前置条件

- Java 17+
- Maven 3.6+
- npx（通过 [npm](https://docs.npmjs.com/downloading-and-installing-node-js-and-npm)）
- Anthropic API 密钥 —— [在此获取](https://docs.anthropic.com/en/docs/initial-setup)
- Brave Search API 密钥 —— [在此获取](https://brave.com/search/api/)

## 安装配置

```bash
npm install -g npx

git clone https://github.com/spring-projects/spring-ai-examples.git
cd spring-ai-examples/model-context-protocol/web-search/brave-chatbot

export ANTHROPIC_API_KEY='your-anthropic-api-key'
export BRAVE_API_KEY='your-brave-api-key'

./mvnw clean install
```

## 运行

```bash
./mvnw spring-boot:run
```

启动一个交互式聊天会话。你可以询问任何内容 —— 机器人使用 Brave Search 获取实时 Web 数据，并在会话中记住之前的对话轮次。

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

**MCP 服务器配置**（`mcp-servers-config.json`）：
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

**聊天客户端设置**：
```java
var chatClient = chatClientBuilder
    .defaultSystem("You are a useful assistant that can perform web searches via Brave's search API.")
    .defaultTools(SyncMcpToolCallbackProvider.builder().mcpClients(mcpSyncClients).build())
    .defaultAdvisors(SessionAdvisors.builder(sessionService)
        .compaction(new TurnCountTrigger(20), WindowCompactionStrategy.builder().lastTurns(10).build())
        .build())
    .build();

var conversationId = UUID.randomUUID().toString();

chatClient.prompt(userInput)
    .advisors(SessionAdvisors.session(conversationId))
    .call()
    .content();
```

要点：
- 使用 `SyncMcpToolCallbackProvider.builder()`（Spring AI 2.0.0 API）
- 稳定的 `conversationId` 使记忆限定在当前会话内
- 会话超过 20 轮后会被压缩到最近 10 轮
- 运行直到按下 Ctrl-C

## 版本

| 依赖 | 版本 |
|---|---|
| Spring Boot | 4.1.1 |
| Spring AI | 2.1.0-SNAPSHOT |
