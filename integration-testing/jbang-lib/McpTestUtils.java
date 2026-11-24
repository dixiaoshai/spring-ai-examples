/*
 * MCP 协议测试工具类
 * 为 Model Context Protocol 服务器提供全面的测试支持
 * 涵盖初始化、工具发现与调用
 * 直接使用 HTTP 客户端以支持新的 STREAMABLE 协议
 */

import java.net.http.*;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import static java.lang.System.*;

public class McpTestUtils {
    
    // 针对特定工具的测试配置
    public record ToolTest(
        String toolName,
        Map<String, Object> parameters,
        String expectedContent,
        boolean optional  // 某些工具可能并非所有服务器都提供
    ) {
        // 构造方法，optional 默认为 false
        public ToolTest(String toolName, Map<String, Object> parameters, String expectedContent) {
            this(toolName, parameters, expectedContent, false);
        }
    }
    
    // MCP 服务器测试的结果
    public record McpTestResult(
        boolean success,
        String serverInfo,
        List<String> availableTools,
        Map<String, String> toolResults,
        String errorMessage
    ) {}
    
    private static final ObjectMapper mapper = new ObjectMapper();
    private static int requestId = 1;
    private static String sessionId = null;

    /**
     * 通过 HTTP STREAMABLE 传输测试 MCP 服务器
     * @param baseUrl MCP 服务器的基础 URL（例如 "http://localhost:8080/mcp"）
     * @param toolTests 待测试的工具列表及其参数
     * @return 包含服务器与工具调用详情的测试结果
     */
    public static McpTestResult testMcpSseServer(String baseUrl, List<ToolTest> toolTests) {
        out.println("🔌 Testing MCP server via HTTP STREAMABLE transport at: " + baseUrl);

        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

        sessionId = null;  // 每次测试都重置会话

        try {
            // 初始化 MCP 连接
            out.println("🤝 Initializing MCP connection...");
            String initRequest = createJsonRpcRequest("initialize", Map.of(
                "protocolVersion", "1.0",
                "capabilities", Map.of(),
                "clientInfo", Map.of("name", "integration-test", "version", "1.0")
            ));

            JsonNode initResponse = sendMcpRequest(client, baseUrl, initRequest, true);
            JsonNode result = initResponse.get("result");
            JsonNode serverInfoNode = result.get("serverInfo");
            String serverInfo = String.format("Server: %s %s",
                serverInfoNode.get("name").asText(),
                serverInfoNode.get("version").asText());
            out.println("✅ Connected to: " + serverInfo);
            out.println("📋 Session ID: " + sessionId);

            // 列出可用工具
            out.println("🔧 Discovering available tools...");
            String listToolsRequest = createJsonRpcRequest("tools/list", Map.of());
            JsonNode toolsResponse = sendMcpRequest(client, baseUrl, listToolsRequest, false);

            List<String> availableTools = new ArrayList<>();
            JsonNode toolsResult = toolsResponse.get("result");
            if (toolsResult != null && toolsResult.has("tools")) {
                for (JsonNode tool : toolsResult.get("tools")) {
                    String toolName = tool.get("name").asText();
                    String description = tool.has("description") ? tool.get("description").asText() : "No description";
                    availableTools.add(toolName);
                    out.println("  - " + toolName + ": " + description);
                }
            }
            out.println("✅ Found " + availableTools.size() + " tools");

            // 测试每个请求的工具
            Map<String, String> toolResults = new HashMap<>();
            boolean allTestsPassed = true;

            for (ToolTest test : toolTests) {
                out.println("\n🧪 Testing tool: " + test.toolName());

                // 检查工具是否可用
                if (!availableTools.contains(test.toolName())) {
                    if (test.optional()) {
                        out.println("⚠️ Tool not available (optional): " + test.toolName());
                        continue;
                    } else {
                        err.println("❌ Required tool not found: " + test.toolName());
                        allTestsPassed = false;
                        continue;
                    }
                }

                try {
                    // 调用工具
                    out.println("  Parameters: " + test.parameters());
                    String callRequest = createJsonRpcRequest("tools/call", Map.of(
                        "name", test.toolName(),
                        "arguments", test.parameters()
                    ));
                    JsonNode callResponse = sendMcpRequest(client, baseUrl, callRequest, false);

                    // 提取结果内容
                    String resultContent = "";
                    JsonNode callResult = callResponse.get("result");
                    if (callResult != null && callResult.has("content")) {
                        JsonNode content = callResult.get("content");
                        if (content.isArray() && content.size() > 0) {
                            JsonNode firstContent = content.get(0);
                            if (firstContent.has("text")) {
                                resultContent = firstContent.get("text").asText();
                            } else {
                                resultContent = firstContent.toString();
                            }
                        }
                    }

                    // 保存结果
                    toolResults.put(test.toolName(), resultContent);
                    out.println("  Response length: " + resultContent.length() + " chars");

                    // 如提供了预期内容则进行校验
                    if (test.expectedContent() != null && !test.expectedContent().isEmpty()) {
                        if (resultContent.toLowerCase().contains(test.expectedContent().toLowerCase())) {
                            out.println("✅ Tool response contains expected content: " + test.expectedContent());
                        } else {
                            err.println("❌ Tool response missing expected content: " + test.expectedContent());
                            out.println("  Actual response (first 200 chars): " +
                                resultContent.substring(0, Math.min(200, resultContent.length())));
                            allTestsPassed = false;
                        }
                    } else {
                        out.println("✅ Tool invoked successfully");
                    }

                } catch (Exception e) {
                    err.println("❌ Error calling tool " + test.toolName() + ": " + e.getMessage());
                    toolResults.put(test.toolName(), "ERROR: " + e.getMessage());
                    allTestsPassed = false;
                }
            }

            out.println("\n✅ MCP protocol test completed");

            return new McpTestResult(
                allTestsPassed,
                serverInfo,
                availableTools,
                toolResults,
                allTestsPassed ? "All tests passed" : "Some tests failed"
            );

        } catch (Exception e) {
            err.println("❌ MCP test failed: " + e.getMessage());
            e.printStackTrace();
            return new McpTestResult(
                false,
                "Unknown",
                List.of(),
                Map.of(),
                "Connection error: " + e.getMessage()
            );
        }
    }

    private static String createJsonRpcRequest(String method, Map<String, Object> params) throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("method", method);
        request.put("params", params);
        request.put("id", requestId++);
        return mapper.writeValueAsString(request);
    }

    private static JsonNode sendMcpRequest(HttpClient client, String baseUrl, String jsonBody, boolean isInit) throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl))
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream, application/json")
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

        // 为非初始化请求添加会话 ID 请求头
        if (!isInit && sessionId != null) {
            requestBuilder.header("mcp-session-id", sessionId);
        }

        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // 从初始化响应中获取会话 ID
        if (isInit) {
            response.headers().firstValue("mcp-session-id").ifPresent(id -> sessionId = id);
        }

        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        }

        String body = response.body();

        // 初始化请求返回纯 JSON，其他请求返回 SSE 格式
        if (isInit) {
            return mapper.readTree(body);
        } else {
            // 解析 SSE 格式：从 "data:" 行中提取 JSON
            return parseSseResponse(body);
        }
    }

    private static JsonNode parseSseResponse(String sseBody) throws Exception {
        // SSE 格式："id: X\ndata: {...}\n\n"
        StringBuilder jsonData = new StringBuilder();
        for (String line : sseBody.split("\n")) {
            if (line.startsWith("data:")) {
                String data = line.substring(5).trim();
                if (!data.isEmpty()) {
                    jsonData.append(data);
                }
            }
        }
        if (jsonData.length() == 0) {
            throw new RuntimeException("No data found in SSE response: " + sseBody);
        }
        return mapper.readTree(jsonData.toString());
    }
    
    /**
     * 创建常用天气服务测试的辅助方法
     */
    public static List<ToolTest> createWeatherTests() {
        return List.of(
            new ToolTest(
                "getWeatherForecastByLocation",
                Map.of("latitude", 47.6062, "longitude", -122.3321),
                "Temperature"  // 天气预报应包含温度信息
            ),
            new ToolTest(
                "getAlerts",
                Map.of("state", "NY"),
                "Event"  // 警报响应包含 "Event:"，其后是警报类型（Watch、Warning 等）
            )
        );
    }
    
    /**
     * 创建常用 SQLite 测试的辅助方法
     */
    public static List<ToolTest> createSqliteTests() {
        return List.of(
            new ToolTest(
                "executeSqlQuery",
                Map.of("query", "SELECT * FROM users LIMIT 1"),
                "user"  // 应返回用户数据
            ),
            new ToolTest(
                "listTables",
                Map.of(),
                "table"  // 应列出可用表
            )
        );
    }
    
    /**
     * 创建文件系统测试的辅助方法
     */
    public static List<ToolTest> createFilesystemTests() {
        return List.of(
            new ToolTest(
                "listDirectory",
                Map.of("path", "."),
                "file"  // 应列出文件
            ),
            new ToolTest(
                "readFile",
                Map.of("path", "README.md"),
                "Spring AI"  // README 应提及 Spring AI
            )
        );
    }

    /**
     * 运行完整的 MCP 天气服务器集成测试。
     * 这是用于测试标准 MCP 天气服务器的一行式工具。
     *
     * @param moduleName 用于日志的模块名
     * @param startupSeconds 等待服务器启动的秒数
     * @param mcpEndpoint MCP 端点 URL（例如 "http://localhost:8080/mcp"）
     */
    public static void runMcpWeatherServerTest(String moduleName, int startupSeconds, String mcpEndpoint) throws Exception {
        WebServerTestUtils.runSimpleWebServerTest(
            WebServerTestUtils.WebServerTestConfig.withWait(
                moduleName,
                startupSeconds,
                () -> {
                    try {
                        var toolTests = createWeatherTests();
                        var mcpResult = testMcpSseServer(mcpEndpoint, toolTests);

                        if (mcpResult.success()) {
                            out.println("  Server: " + mcpResult.serverInfo());
                            out.println("  Available tools: " + mcpResult.availableTools());
                            out.println("  Tool tests: " + mcpResult.toolResults().size() + " passed");
                            return true;
                        } else {
                            err.println("  Error: " + mcpResult.errorMessage());
                            return false;
                        }
                    } catch (Exception e) {
                        err.println("  Exception: " + e.getMessage());
                        return false;
                    }
                }
            )
        );
    }
}