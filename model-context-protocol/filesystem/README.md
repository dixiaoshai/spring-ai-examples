# Spring AI 模型上下文协议 - 文件系统演示

一个跨平台演示应用，展示 Spring AI 与模型上下文协议（MCP）文件系统服务器的集成。该应用支持通过自然语言与本地文件系统交互。

连接到 [Filesystem MCP Server](https://github.com/modelcontextprotocol/servers/tree/main/src/filesystem)，并可访问 `target` 目录。

## 平台支持

✅ **Windows** - 为 npx 自动加上 `cmd.exe` 包装器
✅ **Linux** - 直接执行 npx
✅ **macOS** - 直接执行 npx

该应用会**自动检测**你的操作系统，并相应地配置 MCP 客户端。

## 功能特性

- ✨ **跨平台** - 在 Windows、Linux 与 macOS 上无需修改即可运行
- 🤖 以自然语言查询和更新本地文件系统
- 📝 预定义问题模式，用于自动化分析
- 🔄 两种配置方式：编程式（默认）或基于 JSON
- 🚀 基于 Spring AI 与模型上下文协议构建

## 前置条件

- Java 17 或更高版本
- Maven 3.6+
- Node.js 与 npx（npm 随 Node.js 一起安装）
- OpenAI API 密钥

### 安装 Node.js/npx

**Windows：**
```cmd
# Download and install from https://nodejs.org
# npx is included with npm
npx --version
```

**Linux（Ubuntu/Debian）：**
```bash
curl -fsSL https://deb.nodesource.com/setup_lts.x | sudo -E bash -
sudo apt-get install -y nodejs
npx --version
```

**macOS：**
```bash
brew install node
npx --version
```

## 快速开始

1. **克隆仓库：**
   ```bash
   git clone https://github.com/spring-projects/spring-ai-examples.git
   cd spring-ai-examples/model-context-protocol/filesystem
   ```

2. **设置 OpenAI API 密钥：**

   **Linux/macOS：**
   ```bash
   export OPENAI_API_KEY='your-api-key-here'
   ```

   **Windows（命令提示符）：**
   ```cmd
   set OPENAI_API_KEY=your-api-key-here
   ```

   **Windows（PowerShell）：**
   ```powershell
   $env:OPENAI_API_KEY="your-api-key-here"
   ```

3. **创建示例测试文件：**

   **Linux/macOS：**
   ```bash
   ./create-text-file.sh
   ```

   **Windows：**
   ```cmd
   mkdir target
   echo Sample content > target\spring-ai-mcp-overview.txt
   ```

4. **运行应用：**

   **Linux/macOS：**
   ```bash
   ./mvnw spring-boot:run
   ```

   **Windows：**
   ```cmd
   .\mvnw.cmd spring-boot:run
   ```

## 配置方式

该应用支持两种配置 MCP 客户端的方式：

### 方式一：编程式配置（默认 - 推荐）

默认方式在 `Application.java` 中使用自动操作系统检测：

```java
@Bean(destroyMethod = "close")
@ConditionalOnMissingBean(McpSyncClient.class)
public McpSyncClient mcpClient() {
    ServerParameters stdioParams;

    if (isWindows()) {
        // Windows: cmd.exe /c npx approach
        var winArgs = new ArrayList<>(Arrays.asList(
            "/c", "npx", "-y", "@modelcontextprotocol/server-filesystem", "target"));
        stdioParams = ServerParameters.builder("cmd.exe")
                .args(winArgs)
                .build();
    } else {
        // Linux/Mac: direct npx approach
        stdioParams = ServerParameters.builder("npx")
                .args("-y", "@modelcontextprotocol/server-filesystem", "target")
                .build();
    }

    // Create and initialize client...
}
```

**优点：**
- ✅ 在所有平台上开箱即用
- ✅ 无需配置文件
- ✅ 自动检测操作系统

**缺点：**
- ❌ 配置硬编码在 Java 中
- ❌ 对不同服务器配置的灵活性较差

### 方式二：JSON 配置（可选）

如需更高灵活性，可以使用基于 JSON 的配置。编辑 `src/main/resources/application.properties`：

**Windows：**
```properties
spring.ai.mcp.client.stdio.servers-configuration=classpath:/mcp-servers-config-windows.json
```

**Linux/macOS：**
```properties
spring.ai.mcp.client.stdio.servers-configuration=classpath:/mcp-servers-config-linux.json
```

**优点：**
- ✅ 配置外置
- ✅ 无需重新编译即可轻松修改
- ✅ 可配置多个 MCP 服务器

**缺点：**
- ❌ 必须选择与操作系统匹配的 JSON 文件
- ❌ 需要手动配置

**⚠️ 重要：** 启用 JSON 配置后，编程式的 `@Bean` 会通过 `@ConditionalOnMissingBean` 自动跳过，以避免冲突。

## 为什么 Windows 需要特殊处理

在 Windows 上，`npx` 以**批处理文件**（.cmd）形式实现，而不是原生可执行文件。Java 的 `ProcessBuilder`（由 `StdioClientTransport` 内部使用）无法直接执行批处理文件。

**解决方案：** 用 `cmd.exe /c` 包装命令：

```java
// Windows
ServerParameters.builder("cmd.exe")
    .args("/c", "npx", "-y", "@modelcontextprotocol/server-filesystem", "target")

// vs. Linux/macOS
ServerParameters.builder("npx")
    .args("-y", "@modelcontextprotocol/server-filesystem", "target")
```

该模式同样适用于其他 Windows 批处理文件：`npm.cmd`、`node.cmd` 等。

## 架构概览

### 跨平台 MCP 客户端的创建

该应用使用 Spring 的 `@ConditionalOnMissingBean` 来同时支持两种配置方式：

1. **编程式 Bean** - 在未启用 JSON 配置时创建
2. **自动配置** - 在启用 JSON 配置时创建

```java
@Bean(destroyMethod = "close")
@ConditionalOnMissingBean(McpSyncClient.class)
public McpSyncClient mcpClient() {
    // Only created if auto-config doesn't provide a client
}
```

### 同时支持两种方式的 CommandLineRunner

`CommandLineRunner` 可接受两种方式提供的客户端：

```java
@Bean
public CommandLineRunner predefinedQuestions(
        @Autowired(required = false) List<McpSyncClient> mcpSyncClients,  // From JSON config
        @Autowired(required = false) McpSyncClient mcpClient,              // From programmatic
        ...) {

    // Use whichever is available
    List<McpSyncClient> clients = (mcpSyncClients != null && !mcpSyncClients.isEmpty())
            ? mcpSyncClients
            : (mcpClient != null ? List.of(mcpClient) : List.of());
}
```

### 工具集成

MCP 工具会被自动发现并集成到 Spring AI 中：

```java
var chatClient = chatClientBuilder
    .defaultTools(new SyncMcpToolCallbackProvider(clients))
    .build();
```

随后 AI 模型可以通过自然语言调用 MCP 文件系统工具（read_file、write_file 等）。

## JSON 配置文件

### Windows 配置：mcp-servers-config-windows.json

```json
{
  "mcpServers": {
    "filesystem": {
      "command": "cmd.exe",
      "args": ["/c", "npx", "-y", "@modelcontextprotocol/server-filesystem", "target"],
      "env": {}
    }
  }
}
```

### Linux 配置：mcp-servers-config-linux.json

```json
{
  "mcpServers": {
    "filesystem": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-filesystem", "target"],
      "env": {}
    }
  }
}
```

## 路径处理

为兼容跨平台，示例使用**相对路径**（`"target"`）而非绝对路径：

```java
// ✅ Recommended: Relative path
.args("-y", "@modelcontextprotocol/server-filesystem", "target")

// ❌ Avoid: Absolute path with OS-specific separators
.args("-y", "@modelcontextprotocol/server-filesystem", "/home/user/project/target")
```

MCP 服务器会基于当前工作目录解析相对路径。

## 故障排查

### Windows：无法运行程序“npx”

**原因：** npx 不在 PATH 中，或者是 ProcessBuilder 无法直接执行的批处理文件。

**解决方案：** 确认应用使用的是 `cmd.exe` 包装器（默认的编程式方式应会自动使用）。

### Bean 冲突：“Sinks.many().unicast() sinks only allow a single Subscriber”

**原因：** 编程式配置与 JSON 配置同时创建了 MCP 客户端。

**解决方案：** 只选择一种方式：
- 在 `application.properties` 中注释掉 JSON 配置（使用编程式）
- 或者启用 JSON 配置（编程式会自动跳过）

### Linux/macOS：找不到 npx 命令

**原因：** 未安装 Node.js/npm，或不在 PATH 中。

**解决方案：** 安装 Node.js：
```bash
# Linux
curl -fsSL https://deb.nodesource.com/setup_lts.x | sudo -E bash -
sudo apt-get install -y nodejs

# macOS
brew install node
```

## 示例输出

```
Detected Unix-like OS - using npx directly
MCP Initialized: InitializeResult[protocolVersion=2024-11-05, ...]

Running predefined questions with AI model responses:

QUESTION: Can you explain the content of the target/spring-ai-mcp-overview.txt file?
ASSISTANT: The file contains an overview of the Model Context Protocol (MCP) Java SDK...

QUESTION: Please summarize the content... and store it in target/summary.md
ASSISTANT: I've created a summary and saved it to target/summary.md...
```

## 进一步学习

- [模型上下文协议规范](https://modelcontextprotocol.io)
- [MCP Filesystem Server](https://github.com/modelcontextprotocol/servers/tree/main/src/filesystem)
- [Spring AI 文档](https://docs.spring.io/spring-ai/reference/)
- [Spring AI MCP 客户端文档](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html)
