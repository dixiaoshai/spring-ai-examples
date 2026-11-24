# Spring AI 示例集成测试指南

本指南介绍如何使用基于 JBang 的测试框架，为 Spring AI 示例创建、运行与维护集成测试。

## 快速开始

### 前置条件
- Java 17+
- JBang（`curl -Ls https://sh.jbang.dev | bash -s - app setup`）
- 必需的 API 密钥（例如 `OPENAI_API_KEY`、`ANTHROPIC_API_KEY`）
- Claude Code CLI（用于 AI 校验）：`npm install -g @anthropic-ai/claude-code`

### 运行测试

```bash
# Run all integration tests
./integration-testing/scripts/run-integration-tests.sh

# Run specific test by name filter
./integration-testing/scripts/run-integration-tests.sh "kotlin-hello-world"
./integration-testing/scripts/run-integration-tests.sh "weather"
./integration-testing/scripts/run-integration-tests.sh "chain-workflow"

# Clean old logs before running
./integration-testing/scripts/run-integration-tests.sh --clean-logs

# Run a single test directly with JBang
cd kotlin/kotlin-hello-world
jbang integration-tests/RunKotlinHelloWorld.java
```

## 测试架构

### 概览

集成测试框架使用：
- **Bash 运行脚本** - 发现并执行测试
- **JBang 测试脚本** - 用于启动测试的轻量级 Java 脚本
- **集中式工具类** - 用于常见测试模式的共享 Java 库
- **AI 校验** - 由 Claude 驱动的智能输出分析

### 目录结构

```
spring-ai-examples/
├── integration-testing/
│   ├── scripts/
│   │   └── run-integration-tests.sh    # Main test runner
│   ├── jbang-lib/
│   │   ├── IntegrationTestUtils.java   # Core test utilities
│   │   ├── WebServerTestUtils.java     # Web server test helpers
│   │   └── McpTestUtils.java           # MCP protocol test helpers
│   ├── docs/
│   │   ├── README.md                   # This file
│   │   ├── AI_VALIDATION.md            # AI validation details
│   │   └── TROUBLESHOOTING.md          # Common issues
│   └── logs/
│       └── background-runs/            # Test execution logs
│
└── <module>/
    └── integration-tests/
        ├── ExampleInfo.json            # Test configuration
        └── Run<Module>.java            # JBang test launcher
```

## 创建集成测试

### 第 1 步：创建测试目录

```bash
mkdir -p your-module/integration-tests
```

### 第 2 步：创建 ExampleInfo.json

```json
{
  "complexity": "simple",
  "timeoutSec": 120,
  "successRegex": [
    "Started.*Application",
    "Your expected output pattern"
  ],
  "requiredEnv": ["OPENAI_API_KEY"],
  "aiValidation": {
    "enabled": true,
    "validationMode": "hybrid",
    "expectedBehavior": "Application should demonstrate X functionality"
  }
}
```

### 第 3 步：创建 JBang 启动器

对于**简单应用**（输出控制台内容后退出）：

```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.zeroturnaround:zt-exec:1.12
//DEPS com.fasterxml.jackson.core:jackson-databind:2.17.1
//JAVA 17
//FILES ExampleInfo.json
//SOURCES ../../../integration-testing/jbang-lib/IntegrationTestUtils.java

/*
 * Integration test for your-module
 */
public class RunYourModule {
    public static void main(String... args) throws Exception {
        IntegrationTestUtils.runIntegrationTest("your-module");
    }
}
```

对于 **Web 服务器应用**（启动服务器、测试端点、关闭）：

```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.zeroturnaround:zt-exec:1.12
//DEPS com.fasterxml.jackson.core:jackson-databind:2.17.1
//JAVA 17
//FILES ExampleInfo.json
//SOURCES ../../../integration-testing/jbang-lib/IntegrationTestUtils.java
//SOURCES ../../../integration-testing/jbang-lib/WebServerTestUtils.java

/*
 * Web server integration test for your-module
 */
public class RunYourModuleWebServer {
    public static void main(String... args) throws Exception {
        WebServerTestUtils.runSimpleWebServerTest(
            WebServerTestUtils.WebServerTestConfig.withWait(
                "your-module",
                15,  // startup wait seconds
                () -> {
                    // Your validation logic here
                    var response = WebServerTestUtils.httpGet("http://localhost:8080/api/test");
                    return response.statusCode() == 200;
                }
            )
        );
    }
}
```

对于 **MCP 天气服务器**（一行式）：

```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.zeroturnaround:zt-exec:1.12
//DEPS com.fasterxml.jackson.core:jackson-databind:2.17.1
//JAVA 17
//FILES ExampleInfo.json
//SOURCES ../../../../integration-testing/jbang-lib/IntegrationTestUtils.java
//SOURCES ../../../../integration-testing/jbang-lib/WebServerTestUtils.java
//SOURCES ../../../../integration-testing/jbang-lib/McpTestUtils.java

/*
 * MCP weather server integration test
 */
public class RunWeatherServer {
    public static void main(String... args) throws Exception {
        McpTestUtils.runMcpWeatherServerTest("weather-server", 15, "http://localhost:8080/mcp");
    }
}
```

### 路径解析

根据模块深度调整 `//SOURCES` 路径：

| 模块深度 | 示例 | SOURCES 路径前缀 |
|--------------|---------|---------------------|
| 2 层 | `kotlin/kotlin-hello-world` | `../../../` |
| 3 层 | `models/chat/helloworld` | `../../../../` |
| 4 层 | `model-context-protocol/weather/starter-webmvc-server` | `../../../../` |

## 配置参考

### ExampleInfo.json 字段

| 字段 | 类型 | 是否必需 | 描述 |
|-------|------|----------|-------------|
| `complexity` | string | 否 | `"simple"`、`"complex"`、`"web-server"`、`"mcp"` |
| `timeoutSec` | number | 否 | 最长执行时间（默认：120） |
| `successRegex` | string[] | 否 | 必须出现在输出中的模式 |
| `requiredEnv` | string[] | 否 | 必需的环境变量 |
| `aiValidation` | object | 否 | AI 校验配置 |
| `setupCommands` | string[] | 否 | 测试前运行的命令 |
| `cleanupCommands` | string[] | 否 | 测试后运行的命令 |

### AI 校验配置

```json
{
  "aiValidation": {
    "enabled": true,
    "validationMode": "hybrid",
    "expectedBehavior": "Description of expected application behavior",
    "promptTemplate": "chat_example_validation"
  }
}
```

**校验模式：**
- `primary` - 仅 AI 校验（用于不可预测的 AI 输出）
- `hybrid` - 正则与 AI 都必须通过（推荐）
- `fallback` - 仅在正则失败时使用 AI 校验

详细文档见 [AI_VALIDATION.md](AI_VALIDATION.md)。

## 集中式工具类

### IntegrationTestUtils

所有测试的核心工具类：
- `runIntegrationTest(moduleName)` - 运行完整的集成测试
- `loadConfig()` - 加载 ExampleInfo.json 配置
- `verifyEnvironment(config)` - 检查必需的环境变量
- `buildApplication(moduleName)` - 使用 Maven 构建
- `performAIValidation(output, logFile, config, moduleName)` - 运行 AI 校验

### WebServerTestUtils

Web 服务器测试的工具类：
- `runSimpleWebServerTest(config)` - 构建、启动、校验、关闭
- `waitForHealthCheck(url, timeout)` - 等待服务器就绪
- `httpGet(url)` - 简单的 HTTP GET 请求
- `readServerLog()` - 读取服务器日志文件
- `shutdownServer(handle)` - 优雅停止服务器

### McpTestUtils

MCP 协议测试的工具类：
- `runMcpWeatherServerTest(name, timeout, endpoint)` - 天气服务器的一行式工具
- `testMcpSseServer(url, toolTests)` - 测试 MCP SSE 服务器
- `createWeatherTests()` - 标准的天气工具测试用例
- `createSqliteTests()` - 标准的 SQLite 工具测试用例

## CI/CD 集成

### GitHub Actions 工作流

测试在 GitHub Actions 中按分组执行：

| 分组 | 测试 | 需要的 API 密钥 |
|-------|-------|-------------------|
| `mcp-servers` | 天气服务器（webmvc、webflux、oauth2、stdio） | Anthropic（AI 校验） |
| `agentic` | 链式、评估器、编排器、并行化、路由、反思 | OpenAI、Anthropic |
| `openai-2` | Kotlin 示例、misc 示例、chat/helloworld | OpenAI、Anthropic |
| `openai-3` | MCP 客户端、filesystem、sqlite、dynamic-tool-update | OpenAI、Anthropic、Brave |
| `anthropic-multi` | 递归顾问、document-forge、brave、sampling | OpenAI、Anthropic、Brave |
| `docker-tests` | rag-with-kotlin、evaluation-recursive-advisor-demo | OpenAI、Anthropic + Docker |

### 触发 CI

```bash
# Run all tests
gh workflow run integration-tests.yml

# Run specific group
gh workflow run integration-tests.yml -f test_group=mcp-servers

# Run specific test
gh workflow run integration-tests.yml -f test_filter=kotlin-hello-world
```

### 必需的密钥

在 GitHub 仓库设置中配置以下内容：
- `OPENAI_API_KEY` - OpenAI API 访问
- `ANTHROPIC_API_KEY` - Anthropic API（用于 AI 校验与 Anthropic 示例）
- `BRAVE_API_KEY` - Brave Search API（用于 Web 搜索示例）

## 故障排查

### 常见问题

#### “未找到 JBang”
```bash
curl -Ls https://sh.jbang.dev | bash -s - app setup
export PATH="$HOME/.jbang/bin:$PATH"
```

#### “缺少环境变量”
```bash
export OPENAI_API_KEY="your-key"
export ANTHROPIC_API_KEY="your-key"
```

#### “测试超时”
- 增大 ExampleInfo.json 中的 `timeoutSec`
- 检查外部服务是否可访问
- 查看 `integration-testing/logs/background-runs/` 中的日志

#### “模式校验失败”
- 检查日志文件中的实际输出
- 更新 `successRegex` 模式
- 考虑对不可预测的输出使用 AI 校验

### 查看日志

```bash
# List recent test logs
ls -lt integration-testing/logs/background-runs/*.log | head -10

# View specific test log
cat integration-testing/logs/background-runs/test_kotlin_kotlin-hello-world_*.log

# Search for errors
grep -r "ERROR\|Exception\|FAILED" integration-testing/logs/background-runs/
```

## 最佳实践

### 测试脚本
- 保持 JBang 脚本精简（17-20 行）
- 所有逻辑都使用集中式工具类
- 核实 SOURCES 路径与模块深度匹配

### 配置
- 从 `hybrid` 校验模式开始
- 使用具体的成功模式而不是通用模式
- 设置合理的超时值

### 维护
- 在集中式工具类中修复缺陷，而不是在单个脚本中
- 让成功模式与应用改动保持同步
- 监控 CI 中的不稳定测试

## 按模式分类的示例

### 简单控制台应用
- `kotlin/kotlin-hello-world` - 18 行
- `models/chat/helloworld` - 17 行
- `agentic-patterns/chain-workflow` - 18 行

### Web 服务器应用
- `weather/starter-webmvc-server` - 17 行（使用 McpTestUtils）
- `kotlin/rag-with-kotlin` - 83 行（自定义校验）

### 复合应用（服务器 + 客户端）
- `model-context-protocol/sampling` - 173 行
- `model-context-protocol/dynamic-tool-update` - 122 行

## 获取帮助

- 查看现有测试以了解可用模式
- 参阅 [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
- 遇到 AI 校验问题请参阅 [AI_VALIDATION.md](AI_VALIDATION.md)
- 查看测试日志以获取详细错误信息
