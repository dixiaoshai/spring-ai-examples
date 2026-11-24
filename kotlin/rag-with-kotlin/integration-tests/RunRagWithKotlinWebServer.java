///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.zeroturnaround:zt-exec:1.12
//DEPS com.fasterxml.jackson.core:jackson-databind:2.17.1
//JAVA 17
//FILES ExampleInfo.json
//SOURCES ../../../integration-testing/jbang-lib/IntegrationTestUtils.java
//SOURCES ../../../integration-testing/jbang-lib/WebServerTestUtils.java

/*
 * kotlin/rag-with-kotlin 的 Web 服务器集成测试
 * 测试由 RAG 驱动的领养狗演示
 */

import static java.lang.System.*;

public class RunRagWithKotlinWebServer {

    public static void main(String... args) throws Exception {
        // 检查必需的环境变量
        if (getenv("OPENAI_API_KEY") == null && getenv("SPRING_AI_OPENAI_API_KEY") == null) {
            err.println("Missing required environment variable: OPENAI_API_KEY");
            exit(1);
        }

        WebServerTestUtils.runSimpleWebServerTest(
            WebServerTestUtils.WebServerTestConfig.withWait(
                "rag-with-kotlin",
                45,  // RAG 工作流需要时间将狗加载到向量存储中
                RunRagWithKotlinWebServer::validateRagWorkflow
            )
        );
    }

    private static boolean validateRagWorkflow() {
        try {
            // 读取服务器日志以验证 RAG 工作流已完成
            String logContent = WebServerTestUtils.readServerLog();

            boolean hasStarted = logContent.contains("Started RagWithKotlin");
            boolean hasDogsLoaded = logContent.contains("got a dog");
            boolean hasRagOutput = logContent.contains("DogAdoptionSuggestion");

            if (!hasStarted) {
                err.println("Server did not start properly");
                return false;
            }
            out.println("✅ Server started successfully");

            if (!hasDogsLoaded) {
                err.println("Dogs were not loaded into vector store");
                return false;
            }
            out.println("✅ Dogs loaded into vector store");

            if (!hasRagOutput) {
                err.println("RAG query did not produce DogAdoptionSuggestion");
                return false;
            }
            out.println("✅ RAG workflow completed");

            // 测试 /dogs 端点
            out.println("Testing /dogs endpoint...");
            var response = WebServerTestUtils.httpGet("http://localhost:8090/dogs");

            if (response.statusCode() != 200) {
                err.println("/dogs endpoint returned status " + response.statusCode());
                return false;
            }

            String body = response.body();
            if (!body.contains("name") || !body.contains("description")) {
                err.println("/dogs endpoint returned unexpected response");
                return false;
            }
            out.println("✅ /dogs endpoint returned dog data");

            return true;
        } catch (Exception e) {
            err.println("Exception: " + e.getMessage());
            return false;
        }
    }
}
