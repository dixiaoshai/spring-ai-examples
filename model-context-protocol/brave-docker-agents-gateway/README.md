# Spring AI - 模型上下文协议（MCP）Brave 搜索示例

本示例演示如何创建与 [Brave Search MCP Server](https://github.com/modelcontextprotocol/servers/tree/main/src/brave-search) 通信的 Spring AI 模型上下文协议（MCP）客户端。该应用展示了如何构建 MCP 客户端，以实现与 Brave Search 的自然语言交互，让你通过对话界面执行互联网搜索。本示例使用 Spring Boot 自动配置，通过配置文件完成 MCP 客户端的设置。

运行时，应用会提出一个特定问题来演示 MCP 客户端的能力：“Does Spring AI support the Model Context Protocol? Please provide some references.” MCP 客户端使用 Brave Search 查找相关信息并返回全面的答案。给出响应后，应用随即退出。

<img src="spring-ai-mcp-brave.jpg" width="600"/>

## 前置条件

- Java 17 或更高版本
- Maven 3.6+
- Docker Desktop 4.41 或更高版本
- Git
- OpenAI API 密钥
- Brave Search API 密钥（在 https://brave.com/search/api/ 获取）

## 安装配置

1.  **启动容器化服务：**
    确保 Docker（例如 Docker Desktop 4.41 或更高版本，或带 Docker Compose CLI 的 Docker Engine）已安装并运行。然后在项目根目录（`compose.yml` 所在目录）启动所需服务：
    ```bash
    docker compose up
    ```

2. 克隆仓库：
   ```bash
   git clone https://github.com/spring-projects/spring-ai-examples.git
   cd model-context-protocol/brave-docker-agents-gateway
   ```

3. 设置 API 密钥：
   ```bash
   export OPENAI_API_KEY='your-openai-api-key-here'
   export BRAVE_API_KEY='your-brave-api-key-here'
   ```

4. 构建应用：
   ```bash
   ./mvnw clean install
   ```

## 运行应用

使用 Maven 运行应用：
```bash
./mvnw spring-boot:run
```

应用会执行一次查询，询问 Spring AI 对模型上下文协议的支持情况。它使用 Brave Search MCP 服务器在互联网上搜索相关信息，通过 MCP 客户端处理结果，并在退出前给出详细响应。

## 工作原理

应用通过 Spring Boot 自动配置将 Spring AI 与 Brave Search MCP 服务器集成：

### MCP 客户端配置

MCP 客户端通过配置文件进行配置：

1. `application.properties`：
```properties
spring.ai.mcp.client.see.gateway.url=http://localhost:8811
```

该配置会：
1. 通过 Docker MCP Gateway 使用 Brave Search MCP 服务器
2. Brave API 密钥从环境变量传入
3. 初始化到服务器的同步连接

### 聊天集成

ChatClient 在 Application 类中配置了 MCP 工具回调：

```java
var chatClient = chatClientBuilder
        .defaultTools(new SyncMcpToolCallbackProvider(mcpSyncClients))
        .build();
```

该配置让 AI 模型能够：
- 判断何时使用 Brave Search
- 合理构造查询
- 处理搜索结果并将其融入响应

## 依赖

本项目使用：
- Spring Boot 4.1.1
- Spring AI 2.1.0-SNAPSHOT
- spring-ai-starter-model-openai
- spring-ai-starter-mcp-client
