Spring AI MCP Apps 服务器
===
本示例展示如何利用 MCP 服务器工具与资源上的元数据来启用 [MCP Apps](https://modelcontextprotocol.io/extensions/apps/overview)。

构建并运行服务器
---
本示例使用 Gradle 构建。项目目录中提供了 Gradle wrapper，因此可以这样构建可执行 JAR 文件：

```
./gradlew build
```

然后这样运行：

```
java -jar build/libs/mcp-apps-server-0.0.1-SNAPSHOT.jar
```

也可以使用 Spring Boot Gradle 插件来构建并运行应用：

```
./gradlew bootRun
```

或者，如果愿意，也可以通过你喜欢的 IDE 直接运行应用。

应用启动后，MCP 服务器将监听 3001 端口。

使用 MCP App 服务器
---
要实际体验 MCP App，需要在支持 MCP Apps 的 MCP 客户端中配置该服务器。这类客户端包括 MCP Jam 与 Claude Desktop。Goose 也支持 MCP Apps，但（截至 1.27.2 版本）尚不支持应用更新上下文模型的能力，因此目前还不能使用 Goose。

配置 MCP 服务器时，请选择 Streamable HTTP 作为传输协议，并将 URL 设为 "http://localhost:3001/mcp"。

Claude Desktop（至少截至 1.1.5749 版本）不支持 Streamable HTTP。但你仍可以通过 mcp-remote（STDIO 传输）MCP 服务器代理到 Streamable HTTP 服务器，从而使用 Streamable HTTP MCP 服务器。在 Claude Desktop 配置 JSON 中这样配置：

```
"mcpServers": {
  "dice-tools": {
    "command": "npx",
    "args": [
      "-y",
      "mcp-remote",
      "http://localhost:3001/mcp"
    ]
  }
}
```

在 MCP 客户端中配置好该服务器后，在聊天中输入 "Roll the dice"。如果配置正确，稍等片刻就会看到一对掷出点数的骰子。然后询问 "What was the result?"，以验证应用能否更新模型上下文。接着点击 "Roll Dice" 按钮重新掷骰子，再次提问。
