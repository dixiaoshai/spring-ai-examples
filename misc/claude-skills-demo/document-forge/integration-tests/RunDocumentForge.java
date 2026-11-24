///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.zeroturnaround:zt-exec:1.12
//DEPS com.fasterxml.jackson.core:jackson-databind:2.17.1
//JAVA 17
//FILES ExampleInfo.json
//SOURCES ../../../../integration-testing/jbang-lib/IntegrationTestUtils.java
//SOURCES ../../../../integration-testing/jbang-lib/WebServerTestUtils.java

/*
 * document-forge 的集成测试启动器
 * 测试 Claude Skills 文档生成 Web 应用
 */

import java.util.List;
import java.util.Map;

public class RunDocumentForge {

    public static void main(String... args) throws Exception {
        // 检查必需的环境变量
        if (System.getenv("ANTHROPIC_API_KEY") == null) {
            System.err.println("❌ Missing required environment variable: ANTHROPIC_API_KEY");
            System.exit(1);
        }

        // 定义 Web UI 的端点测试
        // 该模式使用非捕获组以实现正确的分支选择
        var endpointTest = new WebServerTestUtils.EndpointTest(
            "http://localhost:8080/",
            "GET",
            "(?:Document Forge|document-forge|Generate|Claude)",
            Map.of()
        );

        // 配置 Web 服务器测试
        var config = new WebServerTestUtils.WebServerConfig(
            "http://localhost:8080/",  // 健康检查 URL
            20,  // 启动超时时间（秒）
            List.of(endpointTest)  // 要测试的端点
        );

        // 运行测试
        WebServerTestUtils.runWebServerTest("document-forge", config);
    }
}
