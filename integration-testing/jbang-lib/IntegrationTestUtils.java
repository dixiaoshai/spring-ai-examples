/*
 * JBang 集成测试的集中式工具类
 * 提供通用功能，以消除测试脚本之间的代码重复
 */

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.zeroturnaround.exec.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import static java.lang.System.*;

public class IntegrationTestUtils {
    
    // AI 校验配置的记录类型
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AIValidationConfig(
        boolean enabled,
        String validationMode,    // "primary", "hybrid", "fallback"
        String readmeFile,
        String expectedBehavior,
        String[] components,
        String promptTemplate,
        Map<String, Object> successCriteria
    ) {}
    
    // 测试配置的记录类型
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExampleInfo(
        int timeoutSec, 
        String[] successRegex, 
        String[] requiredEnv,
        String[] setupCommands,
        String[] cleanupCommands,
        AIValidationConfig aiValidation
    ) {}
    
    // 从 ExampleInfo.json 加载配置
    public static ExampleInfo loadConfig() throws Exception {
        Path configPath = Path.of("integration-tests/ExampleInfo.json");
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper.readValue(configPath.toFile(), ExampleInfo.class);
    }
    
    // 校验必需的环境变量
    public static void verifyEnvironment(ExampleInfo cfg) {
        for (String envVar : cfg.requiredEnv()) {
            if (getenv(envVar) == null) {
                err.println("❌ Missing required environment variable: " + envVar);
                exit(1);
            }
        }
    }
    
    // 带超时地运行命令
    public static void runCommand(String[] cmd, int timeoutSec) throws Exception {
        ProcessResult result = new ProcessExecutor()
            .command(cmd)
            .timeout(timeoutSec, TimeUnit.SECONDS)
            .redirectOutput(System.out)
            .redirectError(System.err)
            .execute();
            
        int exit = result.getExitValue();
        if (exit != 0) {
            throw new RuntimeException("Command failed with exit code " + exit + ": " + String.join(" ", cmd));
        }
    }
    
    // 如有指定，则运行安装命令
    public static void runSetupCommands(ExampleInfo cfg) throws Exception {
        if (cfg.setupCommands() != null) {
            for (String setupCmd : cfg.setupCommands()) {
                out.println("🔧 Running setup: " + setupCmd);
                runCommand(setupCmd.split("\\s+"), 60); // 安装命令超时 1 分钟
            }
        }
    }
    
    // 如有指定，则运行清理命令
    public static void runCleanupCommands(ExampleInfo cfg) {
        if (cfg.cleanupCommands() != null) {
            for (String cleanupCmd : cfg.cleanupCommands()) {
                out.println("🧹 Running cleanup: " + cleanupCmd);
                try {
                    runCommand(cleanupCmd.split("\\s+"), 30);
                } catch (Exception e) {
                    err.println("⚠️  Cleanup command failed (non-fatal): " + e.getMessage());
                }
            }
        }
    }
    
    // 构建应用
    public static void buildApplication(String moduleName) throws Exception {
        out.println("🏗️  Building " + moduleName + "...");
        runCommand(new String[]{"./mvnw", "clean", "package", "-q", "-DskipTests"}, 300);
    }
    
    // 创建日志文件路径
    public static Path createLogFile(String moduleName) throws Exception {
        // 根据模块深度尝试不同的相对路径
        Path logDir = null;
        if (Files.exists(Paths.get("../../../integration-testing"))) {
            logDir = Paths.get("../../../integration-testing/logs/integration-tests");
        } else if (Files.exists(Paths.get("../../integration-testing"))) {
            logDir = Paths.get("../../integration-testing/logs/integration-tests");
        } else if (Files.exists(Paths.get("../../../../integration-testing"))) {
            logDir = Paths.get("../../../../integration-testing/logs/integration-tests");
        } else {
            throw new RuntimeException("Could not find integration-testing directory");
        }
        
        Files.createDirectories(logDir);
        return logDir.resolve(moduleName + "-spring-boot-" + System.currentTimeMillis() + ".log");
    }
    
    // 运行 Spring Boot 应用
    public static ProcessResult runSpringBootApp(ExampleInfo cfg, Path logFile) throws Exception {
        return new ProcessExecutor()
            .command("./mvnw", "spring-boot:run", "-q")
            .timeout(cfg.timeoutSec(), TimeUnit.SECONDS)
            .redirectOutput(Files.newOutputStream(logFile))
            .redirectErrorStream(true)
            .execute();
    }
    
    // 展示完整输出
    public static void displayOutput(String output) {
        out.println("📋 Full Application Output:");
        out.println("---");
        out.println(output);
        out.println("---");
    }
    
    // 展示日志文件路径
    public static void displayLogPath(Path logFile) {
        out.println("📁 Full Spring Boot log: " + logFile.toAbsolutePath().normalize());
    }
    
    // 使用 Python 脚本执行 AI 校验
    public static boolean performAIValidation(String output, Path logFile, ExampleInfo cfg, String moduleName) throws Exception {
        AIValidationConfig aiConfig = cfg.aiValidation();
        if (aiConfig == null || !aiConfig.enabled()) {
            return true; // 未启用 AI 校验，直接返回成功
        }
        
        out.println("🤖 Running AI validation...");
        
        // 构建 Python 命令：先计算到仓库根目录的路径，再指向校验器
        Path currentDir = Path.of(".").toAbsolutePath();
        Path repoRoot = currentDir;
        
        // 向上查找仓库根目录（包含 integration-testing 目录）
        while (repoRoot != null && !repoRoot.resolve("integration-testing").toFile().exists()) {
            repoRoot = repoRoot.getParent();
        }
        
        if (repoRoot == null) {
            throw new Exception("Could not find repository root with integration-testing directory");
        }
        
        Path validatorScript = repoRoot.resolve("integration-testing/ai-validator/validate_example.py");
            
        String[] baseCmd = {
            "python3",
            validatorScript.toString(),
            "--log-path", logFile.toAbsolutePath().toString(),
            "--example-name", moduleName,
            "--output-format", "json",
            "--verbose"  // 启用详细输出，便于在 CI 中调试
        };
        
        // 构建带可选参数的动态命令
        List<String> cmdList = new ArrayList<>(Arrays.asList(baseCmd));
        
        if (aiConfig.expectedBehavior() != null && !aiConfig.expectedBehavior().isEmpty()) {
            cmdList.add("--expected-behavior");
            cmdList.add(aiConfig.expectedBehavior());
        }
        
        if (aiConfig.readmeFile() != null && !aiConfig.readmeFile().isEmpty()) {
            cmdList.add("--readme-path");
            cmdList.add(aiConfig.readmeFile());
        }
        
        if (aiConfig.components() != null && aiConfig.components().length > 0) {
            cmdList.add("--components");
            for (String component : aiConfig.components()) {
                cmdList.add(component);
            }
        }
        
        if (aiConfig.promptTemplate() != null && !aiConfig.promptTemplate().isEmpty()) {
            cmdList.add("--template");
            cmdList.add(aiConfig.promptTemplate());
        }
        
        String[] fullCmd = cmdList.toArray(new String[0]);
        
        try {
            // 执行 AI 校验
            ProcessResult result = new ProcessExecutor()
                .command(fullCmd)
                .timeout(180, TimeUnit.SECONDS) // AI 校验的 3 分钟超时
                .readOutput(true)
                .redirectErrorStream(true) // 合并 stderr 到 stdout，以便更清楚地观察错误
                .execute();

            String aiOutput = result.outputUTF8();
            int aiExitCode = result.getExitValue();

            // 检查退出码
            if (aiExitCode != 0) {
                err.println("AI validation output: " + aiOutput);
                throw new Exception("AI validation script failed with exit code: " + aiExitCode);
            }
            
            // 静默模式下，输出应是干净的 JSON
            String jsonOutput = aiOutput.trim();
            
            if (jsonOutput.isEmpty()) {
                throw new Exception("No output from AI validation");
            }
            
            // 直接解析 JSON 响应（静默模式下无需复杂过滤）
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> aiResult;
            
            try {
                aiResult = mapper.readValue(jsonOutput, Map.class);
            } catch (Exception e) {
                // 如果直接解析失败，则尝试提取 JSON 作为兜底
                String extractedJson = extractJsonFromOutput(aiOutput);
                if (extractedJson != null && !extractedJson.trim().isEmpty()) {
                    aiResult = mapper.readValue(extractedJson, Map.class);
                } else {
                    throw new Exception("Could not parse AI validation JSON response: " + e.getMessage());
                }
            }
            
            boolean aiSuccess = (Boolean) aiResult.get("success");
            Double confidence = (Double) aiResult.get("confidence");
            String reasoning = (String) aiResult.get("reasoning");
            
            // 展示 AI 校验结果
            out.println("🤖 AI Validation Result:");
            out.println("  Success: " + aiSuccess);
            out.println("  Confidence: " + String.format("%.2f", confidence));
            out.println("  Reasoning: " + reasoning);
            
            // 如有成本信息则展示
            @SuppressWarnings("unchecked")
            Map<String, Object> costInfo = (Map<String, Object>) aiResult.get("cost_info");
            if (costInfo != null) {
                out.println("  💰 Cost Information:");
                
                Object inputTokens = costInfo.get("input_tokens");
                Object outputTokens = costInfo.get("output_tokens");
                Object totalTokens = costInfo.get("total_tokens");
                Object cacheCreation = costInfo.get("cache_creation_input_tokens");
                Object cacheRead = costInfo.get("cache_read_input_tokens");
                Object duration = costInfo.get("duration_seconds");
                
                if (inputTokens != null) out.println("    Input Tokens: " + inputTokens);
                if (outputTokens != null) out.println("    Output Tokens: " + outputTokens);
                if (totalTokens != null) out.println("    Total Tokens: " + totalTokens);
                if (cacheCreation != null) out.println("    Cache Creation Tokens: " + cacheCreation);
                if (cacheRead != null) out.println("    Cache Read Tokens: " + cacheRead);
                if (duration != null) out.println("    Duration: " + String.format("%.2f", (Double) duration) + " seconds");
            }
            
            @SuppressWarnings("unchecked")
            List<String> functionalityDemo = (List<String>) aiResult.get("functionality_demonstrated");
            if (functionalityDemo != null && !functionalityDemo.isEmpty()) {
                out.println("  Functionality Demonstrated:");
                for (String item : functionalityDemo) {
                    out.println("    - " + item);
                }
            }
            
            @SuppressWarnings("unchecked")
            List<String> issues = (List<String>) aiResult.get("issues_found");
            if (issues != null && !issues.isEmpty()) {
                out.println("  Issues Found:");
                for (String issue : issues) {
                    out.println("    - " + issue);
                }
            }
            
            return aiSuccess;
            
        } catch (Exception e) {
            err.println("⚠️  AI validation failed: " + e.getMessage());
            
            // 根据校验模式进行处理
            String mode = aiConfig.validationMode();
            if ("primary".equals(mode)) {
                throw new Exception("AI validation failed and is required: " + e.getMessage());
            } else if ("hybrid".equals(mode)) {
                err.println("⚠️  AI validation failed, falling back to regex patterns only");
                return true; // 交由正则校验处理
            } else { // fallback mode
                err.println("⚠️  AI validation failed, using regex validation result");
                return true; // 使用正则校验的结果
            }
        }
    }
    
    // 从混合输出中提取 JSON（过滤掉日志行）
    public static String extractJsonFromOutput(String output) {
        String[] lines = output.split("\n");
        StringBuilder jsonBuilder = new StringBuilder();
        boolean inJsonBlock = false;
        int braceCount = 0;
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // 跳过日志行
            if (trimmedLine.startsWith("INFO:") || 
                trimmedLine.startsWith("DEBUG:") || 
                trimmedLine.startsWith("WARNING:") ||
                trimmedLine.startsWith("ERROR:")) {
                continue;
            }
            
            // 查找 JSON 起始位置
            if (trimmedLine.startsWith("{") && !inJsonBlock) {
                inJsonBlock = true;
                braceCount = 0;
                jsonBuilder.setLength(0); // Reset
                jsonBuilder.append(line).append("\n");
                braceCount += line.chars().map(c -> c == '{' ? 1 : c == '}' ? -1 : 0).sum();
            } else if (inJsonBlock) {
                jsonBuilder.append(line).append("\n");
                braceCount += line.chars().map(c -> c == '{' ? 1 : c == '}' ? -1 : 0).sum();
                
                // 检查 JSON 块是否完整
                if (braceCount <= 0 && trimmedLine.endsWith("}")) {
                    break;
                }
            }
        }
        
        return inJsonBlock ? jsonBuilder.toString().trim() : null;
    }
    
    // 主测试执行流程
    public static void runIntegrationTest(String moduleName) throws Exception {
        // 加载配置
        ExampleInfo cfg = loadConfig();
        
        // 校验环境
        verifyEnvironment(cfg);
        
        try {
            // 构建应用
            buildApplication(moduleName);
            
            // 在构建之后运行安装命令，避免 clean 删除测试文件
            runSetupCommands(cfg);
            
            // 创建日志文件
            Path logFile = createLogFile(moduleName.toLowerCase().replace(" ", "-"));
            
            // 运行应用
            out.println("🚀 Running " + moduleName + "...");
            ProcessResult result = runSpringBootApp(cfg, logFile);
            int exitCode = result.getExitValue();
            
            // 读取输出
            String output = Files.readString(logFile);
            
            // 校验模式并展示日志路径
            out.println("✅ Verifying output patterns...");
            out.println("📁 Full Spring Boot log: " + logFile.toAbsolutePath().normalize());
            
            // 先展示完整输出
            displayOutput(output);
            
            // 首先检查 AI 校验是否已启用且处于 primary 模式
            AIValidationConfig aiConfig = cfg.aiValidation();
            boolean regexValidationNeeded = true;
            boolean aiValidationPassed = true;
            
            if (aiConfig != null && aiConfig.enabled() && "primary".equals(aiConfig.validationMode())) {
                // Primary AI 校验模式 - 跳过正则模式
                out.println("🤖 Using AI validation as primary validation method");
                regexValidationNeeded = false;
                aiValidationPassed = performAIValidation(output, logFile, cfg, moduleName);
            } else {
                // 先运行正则校验
                int failedPatterns = 0;
                for (String pattern : cfg.successRegex()) {
                    if (output.matches("(?s).*" + pattern + ".*")) {
                        out.println("  ✓ Found: " + pattern);
                    } else {
                        err.println("  ❌ Missing: " + pattern);
                        failedPatterns++;
                    }
                }
                
                // 如已配置则运行 AI 校验
                if (aiConfig != null && aiConfig.enabled()) {
                    if ("hybrid".equals(aiConfig.validationMode())) {
                        // Hybrid 模式 - 两者都必须通过
                        out.println("🤖 Running additional AI validation (hybrid mode)");
                        aiValidationPassed = performAIValidation(output, logFile, cfg, moduleName);
                    } else if ("fallback".equals(aiConfig.validationMode()) && failedPatterns > 0) {
                        // Fallback 模式 - 仅当正则失败时才运行 AI
                        out.println("🤖 Regex validation failed, trying AI validation (fallback mode)");
                        aiValidationPassed = performAIValidation(output, logFile, cfg, moduleName);
                        if (aiValidationPassed) {
                            out.println("🎉 AI validation succeeded, overriding regex failure!");
                            failedPatterns = 0; // AI 校验通过，重置失败的模式计数
                        }
                    }
                }
                
                // 如需要则检查正则结果
                if (regexValidationNeeded && failedPatterns > 0 && !aiValidationPassed) {
                    err.println("❌ Failed pattern verification: " + failedPatterns + " patterns missing");
                    exit(1);
                }
            }
            
            // 保留日志文件以便调试 - 不要删除
            out.println("\n📁 Spring Boot log preserved: " + logFile.toAbsolutePath().normalize());
            
            // 最终校验检查
            // 对使用 AI 校验的交互式应用允许非零退出码
            boolean allowNonZeroExit = aiConfig != null && aiConfig.enabled() && 
                aiConfig.successCriteria() != null && 
                aiConfig.successCriteria().containsKey("requiresUserInteraction") &&
                Boolean.TRUE.equals(aiConfig.successCriteria().get("requiresUserInteraction"));
                
            if (exitCode != 0 && !allowNonZeroExit) {
                err.println("❌ Application exited with code: " + exitCode);
                exit(exitCode);
            } else if (exitCode != 0 && allowNonZeroExit) {
                out.println("ℹ️  Application exited with code " + exitCode + " (expected for interactive application)");
            }
            
            if (!aiValidationPassed) {
                err.println("❌ AI validation failed");
                exit(1);
            }
            
            out.println("🎉 Integration test completed successfully!");
            
        } finally {
            // 运行清理命令
            runCleanupCommands(cfg);
        }
    }
}