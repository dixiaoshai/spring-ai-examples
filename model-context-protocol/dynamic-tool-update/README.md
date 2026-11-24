# 动态工具更新示例

本示例演示模型上下文协议（MCP）服务器如何在运行时动态更新其可用工具，以及客户端如何检测并使用更新后的工具。

## 概述

MCP 协议允许 AI 模型通过标准化接口访问外部工具与资源。本示例展示了 MCP 的一个关键特性：能够在服务器端动态更新可用工具，并由客户端检测和利用这些变化。

## 关键组件

### 服务器

服务器应用由以下部分组成：

1. **WeatherService**：初始提供一个天气预报工具，用于获取指定地点的温度数据。
2. **MathTools**：包含数学运算（求和、乘法、除法），会被动态添加到服务器中。
3. **ServerApplication**：管理服务器生命周期，并处理动态工具更新流程。

### 客户端

客户端应用会：

1. 连接到 MCP 服务器
2. 获取初始可用工具列表
3. 触发服务器更新其工具
4. 检测工具变化
5. 获取更新后的工具列表

## 工作原理

1. **初始设置**：服务器启动时仅暴露天气预报工具。

2. **动态更新过程**：
   - 客户端向服务器的 `/updateTools` 端点发送请求
   - 服务器收到该信号后，将数学工具加入其可用工具
   - 服务器向已连接的客户端通知工具变化
   - 客户端收到通知，此后即可使用新工具

3. **工具发现**：客户端可随时查询可用工具，以验证工具列表已更新。

## 实现细节

### 服务器端实现

服务器使用 Spring AI 的 MCP 服务器实现：

```java
@Bean
public ToolCallbackProvider weatherTools(WeatherService weatherService) {
    return MethodToolCallbackProvider.builder().toolObjects(weatherService).build();
}

@Bean
public CommandLineRunner predefinedQuestions(McpSyncServer mcpSyncServer) {
    return args -> {
        logger.info("Server: " + mcpSyncServer.getServerInfo());

        latch.await(); // Wait for update signal

        // Add math tools dynamically
        List<SyncToolSpecification> newTools = McpToolUtils
                .toSyncToolSpecifications(ToolCallbacks.from(new MathTools()));

        mcpSyncServer.addTool(newTools.iterator().next());

        logger.info("Tools updated: ");
    };
}
```

### 客户端实现

客户端连接服务器并注册回调，以便在工具变化时收到通知：

```java
@Bean
McpSyncClientCustomizer customizeMcpClient() {
    return (name, mcpClientSpec) -> {
        mcpClientSpec.toolsChangeConsumer(tv -> {
            logger.info("\nMCP TOOLS CHANGE: " + tv);
            latch.countDown();
        });
    };
}
```

客户端在更新前后分别获取可用工具列表：

```java
// Get initial tools
List<ToolDescription> toolDescriptions = chatClientBuilder.build()
        .prompt("What tools are available?")
        .tools(tools)
        .call()
        .entity(new ParameterizedTypeReference<List<ToolDescription>>() {});

// Signal the server to update tools
String signal = RestClient.builder().build().get()
        .uri("http://localhost:8080/updateTools").retrieve().body(String.class);

// Wait for tool change notification
latch.await();

// Get updated tools
toolDescriptions = chatClientBuilder.build()
        .prompt("What tools are available?")
        .tools(tools)
        .call()
        .entity(new ParameterizedTypeReference<List<ToolDescription>>() {});
```

## 关键洞见

本示例展示了 Spring AI 中 MCP 实现的一个关键方面：

> 客户端实现依赖于这样一个事实：MCP 的 `ToolCallbackProvider#getToolCallbacks` 实现始终会从服务器获取当前的 MCP 工具列表。

这意味着客户端每次请求可用工具时，都会从服务器获得最新列表，无需重启或重新初始化客户端。

## 运行示例

1. 启动服务器应用：
   ```
   cd server
   ./mvnw spring-boot:run
   ```

2. 在另一个终端中启动客户端应用：
   ```
   cd client
   ./mvnw spring-boot:run
   ```

3. 观察控制台输出，可以看到：
   - 初始工具列表（仅天气预报）
   - 工具更新通知
   - 更新后的工具列表（天气预报 + 数学运算）

## MCP 协议规范

有关 MCP 协议的更多信息，请参阅官方规范：
[https://modelcontextprotocol.io/specification/2024-11-05/server/tools](https://modelcontextprotocol.io/specification/2024-11-05/server/tools)
