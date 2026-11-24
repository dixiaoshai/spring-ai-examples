///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.zeroturnaround:zt-exec:1.12
//DEPS com.fasterxml.jackson.core:jackson-databind:2.17.1
//JAVA 17
//FILES ExampleInfo.json
//SOURCES ../../../integration-testing/jbang-lib/IntegrationTestUtils.java
//SOURCES ../../../integration-testing/jbang-lib/WebServerTestUtils.java

/*
 * openai-streaming-response 的 Web 服务器集成测试
 * 通过发起 HTTP 请求测试流式端点
 */

import java.util.List;
import java.util.Map;

public class RunOpenaiStreamingResponseWebServer {
    
    public static void main(String... args) throws Exception {
        // 检查必需的环境变量
        if (System.getenv("OPENAI_API_KEY") == null) {
            System.err.println("❌ Missing required environment variable: OPENAI_API_KEY");
            System.exit(1);
        }

        // 定义端点测试
        // SSE 流会逐词返回包含笑话内容的 ChatResponse 对象
        var endpointTest = new WebServerTestUtils.EndpointTest(
            "http://localhost:8080/ai/generateStream?message=Tell%20me%20a%20short%20joke",
            "GET",
            // 校验收到带聊天补全和文本内容的 SSE data 事件
            // 模式匹配：data:{"metadata":...,"id":"chatcmpl-...,"text":"..."...}
            "data:.*chatcmpl.*text",
            Map.of("Accept", "text/event-stream")
        );
        
        // 配置 Web 服务器测试
        var config = new WebServerTestUtils.WebServerConfig(
            null,  // 无健康检查，只等待启动时间
            15,  // 启动超时时间（秒，给它足够时间启动）
            List.of(endpointTest)  // 要测试的端点
        );
        
        // 运行测试
        WebServerTestUtils.runWebServerTest("openai-streaming-response", config);
    }
}