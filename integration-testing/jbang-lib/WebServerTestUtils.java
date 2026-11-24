/*
 * 用于测试 Web 服务器应用的工具类
 * 处理服务器启动、健康检查、HTTP 请求与关闭
 */

import com.fasterxml.jackson.databind.*;
import org.zeroturnaround.exec.*;
import java.nio.file.*;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.net.http.*;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.List;
import java.util.function.Supplier;
import java.io.File;
import java.io.FileOutputStream;
import static java.lang.System.*;

public class WebServerTestUtils {

    private static final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    // 简化版 Web 服务器测试运行器的配置
    public record WebServerTestConfig(
        String moduleName,         // 用于日志的名称
        int startupWaitSeconds,    // 等待服务器启动的时间
        String healthCheckUrl,     // 可选检查的 URL（null = 仅等待）
        Supplier<Boolean> validator // 校验逻辑，成功时返回 true
    ) {
        // 便于使用的 builder 风格工厂方法
        public static WebServerTestConfig withWait(String name, int seconds, Supplier<Boolean> validator) {
            return new WebServerTestConfig(name, seconds, null, validator);
        }

        public static WebServerTestConfig withHealthCheck(String name, String healthUrl, int timeout, Supplier<Boolean> validator) {
            return new WebServerTestConfig(name, timeout, healthUrl, validator);
        }
    }

    // Web 服务器测试的扩展配置（旧版）
    public record WebServerConfig(
        String healthCheck,        // 检查服务器是否就绪的 URL
        int startupTime,          // 等待启动的秒数
        List<EndpointTest> testEndpoints  // 待测试的端点
    ) {}

    public record EndpointTest(
        String url,
        String method,
        String expectedPattern,
        Map<String, String> headers
    ) {}

    /**
     * 简化的 Web 服务器集成测试运行器。
     * 处理所有样板工作：构建、启动、等待、校验、关闭。
     *
     * @param config 包含模块名、时序与校验器的测试配置
     * @throws Exception 测试失败时抛出
     */
    public static void runSimpleWebServerTest(WebServerTestConfig config) throws Exception {
        ProcessHandle serverHandle = null;
        File logFile = null;

        try {
            File moduleDir = new File(".").getAbsoluteFile();

            // 构建应用
            out.println("🏗️  Building " + config.moduleName() + "...");
            new ProcessExecutor()
                .command("./mvnw", "clean", "package", "-q", "-DskipTests")
                .directory(moduleDir)
                .timeout(300, TimeUnit.SECONDS)
                .redirectOutput(out)
                .redirectError(err)
                .execute();

            // 创建日志目录
            File logDir = new File(moduleDir, "target/integration-test-logs");
            logDir.mkdirs();
            logFile = new File(logDir, "web-server-" + System.currentTimeMillis() + ".log");

            // 在后台启动服务器
            out.println("🚀 Starting web server in background...");
            Process process = new ProcessExecutor()
                .command("./mvnw", "spring-boot:run", "-q")
                .directory(moduleDir)
                .redirectOutput(new FileOutputStream(logFile))
                .redirectError(err)
                .start()
                .getProcess();

            serverHandle = process.toHandle();

            // 等待服务器就绪
            if (config.healthCheckUrl() != null) {
                boolean ready = waitForHealthCheck(config.healthCheckUrl(), config.startupWaitSeconds());
                if (!ready) {
                    throw new RuntimeException("Server failed to start - health check failed");
                }
            } else {
                out.println("⏳ Waiting " + config.startupWaitSeconds() + " seconds for server startup...");
                Thread.sleep(config.startupWaitSeconds() * 1000L);
                out.println("✅ Server should be ready");
            }

            // 运行校验
            out.println("🧪 Running validation...");
            boolean success = config.validator().get();

            if (success) {
                out.println("🎉 " + config.moduleName() + " test passed!");
            } else {
                err.println("❌ " + config.moduleName() + " test failed!");
                exit(1);
            }

        } finally {
            if (serverHandle != null) {
                shutdownServer(serverHandle);
            }
        }
    }

    /**
     * 读取服务器日志文件，用于校验。
     * 如果校验器需要检查日志内容，请调用此方法。
     */
    public static String readServerLog() throws Exception {
        File logDir = new File("target/integration-test-logs");
        File[] logs = logDir.listFiles((dir, name) -> name.startsWith("web-server-"));
        if (logs != null && logs.length > 0) {
            // 获取最新的日志
            File latestLog = logs[0];
            for (File log : logs) {
                if (log.lastModified() > latestLog.lastModified()) {
                    latestLog = log;
                }
            }
            return Files.readString(latestLog.toPath());
        }
        return "";
    }

    /**
     * 用于校验的简单 HTTP GET 请求辅助方法。
     */
    public static HttpResponse<String> httpGet(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
    
    // 在后台启动服务器并返回进程句柄
    public static ProcessHandle startServerInBackground() throws Exception {
        out.println("🚀 Starting web server in background...");
        
        // 如日志目录不存在则创建
        Path logDir = Paths.get("target/integration-test-logs");
        Files.createDirectories(logDir);
        
        ProcessBuilder pb = new ProcessBuilder("./mvnw", "spring-boot:run", "-q");
        pb.redirectOutput(ProcessBuilder.Redirect.to(
            logDir.resolve("web-server-" + System.currentTimeMillis() + ".log").toFile()));
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        
        Process process = pb.start();
        return process.toHandle();
    }
    
    // 等待健康检查端点响应
    public static boolean waitForHealthCheck(String healthCheckUrl, int timeoutSeconds) {
        out.println("⏳ Waiting for server to be ready...");
        long startTime = System.currentTimeMillis();
        long timeout = timeoutSeconds * 1000L;
        
        while (System.currentTimeMillis() - startTime < timeout) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(healthCheckUrl))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
                    
                HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
                    
                if (response.statusCode() == 200) {
                    out.println("✅ Server is ready!");
                    return true;
                }
            } catch (Exception e) {
                // 服务器尚未就绪，继续尝试
            }
            
            try {
                Thread.sleep(1000); // 重试前等待 1 秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        err.println("❌ Server failed to start within " + timeoutSeconds + " seconds");
        return false;
    }
    
    // 测试端点并校验响应
    public static boolean testEndpoint(EndpointTest test) throws Exception {
        out.println("🔍 Testing endpoint: " + test.url());
        
        // 检查这是否是 SSE 端点
        boolean isSSE = test.headers() != null && 
            test.headers().getOrDefault("Accept", "").contains("event-stream");
        
        if (isSSE) {
            return testSSEEndpoint(test);
        }
        
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(test.url()))
            .timeout(Duration.ofSeconds(30));
        
        // 如提供了请求头则添加
        if (test.headers() != null) {
            test.headers().forEach(requestBuilder::header);
        }
        
        // 设置 HTTP 方法
        switch (test.method().toUpperCase()) {
            case "GET":
                requestBuilder.GET();
                break;
            case "POST":
                requestBuilder.POST(HttpRequest.BodyPublishers.noBody());
                break;
            default:
                throw new IllegalArgumentException("Unsupported method: " + test.method());
        }
        
        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = httpClient.send(request, 
            HttpResponse.BodyHandlers.ofString());
        
        String body = response.body();
        out.println("📡 Response status: " + response.statusCode());
        out.println("📝 Response body (first 500 chars): " + 
            body.substring(0, Math.min(500, body.length())));
        
        // 检查响应是否匹配预期模式
        if (body.matches("(?s).*" + test.expectedPattern() + ".*")) {
            out.println("✅ Response matches expected pattern");
            return true;
        } else {
            err.println("❌ Response doesn't match pattern: " + test.expectedPattern());
            return false;
        }
    }
    
    // 通过捕获几秒钟的数据流来测试 SSE 端点
    private static boolean testSSEEndpoint(EndpointTest test) throws Exception {
        out.println("🌊 Testing SSE stream endpoint...");
        
        ProcessBuilder pb = new ProcessBuilder(
            "timeout", "10", "curl", "-s", "-N",
            "-H", "Accept: text/event-stream",
            test.url()
        );
        
        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        
        try (var reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        process.waitFor();
        String sseData = output.toString();
        
        out.println("📡 SSE stream sample (first 1000 chars): " + 
            sseData.substring(0, Math.min(1000, sseData.length())));
        
        // 检查 SSE 数据是否匹配预期模式
        if (test.expectedPattern() == null || test.expectedPattern().isEmpty()) {
            // 空模式表示仅检查 SSE 端点是否有响应
            out.println("✅ SSE endpoint responded (no pattern check)");
            return true;
        } else if (sseData.matches("(?s).*" + test.expectedPattern() + ".*")) {
            out.println("✅ SSE stream contains expected pattern");
            return true;
        } else {
            err.println("❌ SSE stream doesn't match pattern: " + test.expectedPattern());
            err.println("📝 Full SSE output: " + sseData);
            return false;
        }
    }
    
    // 优雅关闭服务器
    public static void shutdownServer(ProcessHandle handle) {
        out.println("🛑 Shutting down server...");
        
        if (handle != null && handle.isAlive()) {
            handle.destroy(); // 发送 SIGTERM
            
            try {
                // 最多等待 10 秒以优雅关闭
                boolean terminated = handle.onExit()
                    .get(10, TimeUnit.SECONDS) != null;
                    
                if (!terminated && handle.isAlive()) {
                    out.println("⚠️ Forcing server shutdown...");
                    handle.destroyForcibly();
                }
                
                out.println("✅ Server shut down");
            } catch (Exception e) {
                err.println("⚠️ Error shutting down server: " + e.getMessage());
                if (handle.isAlive()) {
                    handle.destroyForcibly();
                }
            }
        }
    }
    
    // Web 服务器的主测试执行流程
    public static void runWebServerTest(String moduleName, WebServerConfig config) throws Exception {
        ProcessHandle serverHandle = null;
        
        try {
            // 先构建应用
            out.println("🏗️  Building " + moduleName + "...");
            new ProcessExecutor()
                .command("./mvnw", "clean", "package", "-q", "-DskipTests")
                .timeout(300, TimeUnit.SECONDS)
                .redirectOutput(System.out)
                .redirectError(System.err)
                .execute();
            
            // 在后台启动服务器
            serverHandle = startServerInBackground();
            
            // 等待服务器就绪
            if (config.healthCheck() != null) {
                boolean ready = waitForHealthCheck(
                    config.healthCheck(),
                    config.startupTime() > 0 ? config.startupTime() : 30
                );
                
                if (!ready) {
                    throw new RuntimeException("Server failed to start");
                }
            } else {
                // 无健康检查，仅等待启动时间
                out.println("⏳ Waiting " + config.startupTime() + " seconds for server startup...");
                Thread.sleep(config.startupTime() * 1000L);
                out.println("✅ Assuming server is ready after wait time");
            }
            
            // 测试所有已配置的端点
            boolean allTestsPassed = true;
            if (config.testEndpoints() != null) {
                for (EndpointTest test : config.testEndpoints()) {
                    if (!testEndpoint(test)) {
                        allTestsPassed = false;
                    }
                }
            }
            
            if (allTestsPassed) {
                out.println("🎉 All web server tests passed!");
            } else {
                err.println("❌ Some tests failed");
                exit(1);
            }
            
        } finally {
            // 总是尝试关闭服务器
            if (serverHandle != null) {
                shutdownServer(serverHandle);
            }
        }
    }
}