# Spring AI MCP 天气 STDIO 服务器

一个 Spring Boot 应用，演示使用 STDIO 传输的 MCP 服务器，通过 [National Weather Service API](https://api.weather.gov) 暴露天气工具。使用 `spring-ai-starter-mcp-server` 自动配置。

更多信息请参见 [MCP Server Boot Starter](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html) 参考文档。

## 前置条件

- Java 17+
- Maven 3.6+

## 工作原理

在 `@Service` Bean 的方法上添加 `@McpTool` 注解 —— starter 会自动将它们注册为 MCP 工具：

```java
@Service
public class WeatherService {

    @McpTool(description = "Get weather forecast for a specific latitude/longitude")
    public String getWeatherForecastByLocation(double latitude, double longitude) { ... }

    @McpTool(description = "Get weather alerts for a US state. Input is Two-letter US state code (e.g. CA, NY)")
    public String getAlerts(@McpToolParam(description = "Two-letter US state code (e.g. CA, NY") String state) { ... }
}
```

无需手动装配 Bean —— starter 会自动发现所有带 `@McpTool` 注解的方法。

## 可用的工具

| 工具 | 描述 |
|------|-------------|
| `getWeatherForecastByLocation` | 按纬度/经度坐标获取天气预报 |
| `getAlerts` | 按美国两位州代码（例如 `NY`、`CA`）获取当前天气预警 |

## 构建

```bash
./mvnw clean install -DskipTests
```

## 运行测试客户端

`ClientStdio` 以子进程方式启动服务器，并演练两个工具：

```bash
./mvnw exec:java -Dexec.mainClass="org.springframework.ai.mcp.sample.client.ClientStdio"
```

或在构建后从仓库根目录运行：

```bash
java -jar model-context-protocol/weather/starter-stdio-server/target/mcp-weather-stdio-server-0.0.1-SNAPSHOT.jar
```

## Claude Desktop 集成

构建 jar，然后将其添加到 Claude Desktop 的 `claude_desktop_config.json` 中：

```json
{
  "mcpServers": {
    "spring-ai-mcp-weather": {
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/mcp-weather-stdio-server-0.0.1-SNAPSHOT.jar"
      ]
    }
  }
}
```

## 配置

`application.properties` 中 STDIO 的关键要求：

```properties
# Disable web server and suppress console output so STDIO transport works
spring.main.web-application-type=none
spring.main.banner-mode=off
logging.pattern.console=

spring.ai.mcp.server.name=my-weather-server
spring.ai.mcp.server.version=0.0.1

# Optional: redirect logs to a file for debugging
logging.file.name=./target/mcp-weather-stdio-server.log
```

## 相关示例

- [starter-default-client](../../client-starter/starter-default-client) — 连接到此服务器的 MCP 客户端
- [starter-webflux-client](../../client-starter/starter-webflux-client) — 响应式 MCP 客户端
