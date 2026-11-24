package com.example.jinferdemo;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionAdvisors;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 普通的 Spring AI 代码：这里没有任何 jinfer 特有的内容。starter 会在注入的
 * {@link ChatClient.Builder} 背后自动配置 {@code ChatModel}（一个在此 JVM 内运行的本地 LLM）。
 */
@RestController
class ChatController {

    record Incident(String severity, String summary, boolean pageOnCall) {}

    private final ChatClient chat;
    private final ChatClient memoryChat;

    ChatController(ChatClient.Builder builder, SessionService sessionService) {
        this.chat = builder.defaultSystem("You are a concise, helpful assistant.").build();
        // 带会话顾问的客户端副本，这些顾问负责存储并重放历史记录
        this.memoryChat = this.chat.mutate().defaultAdvisors(SessionAdvisors.builder(sessionService).build()).build();
    }

    /** 简单的阻塞式调用。 */
    @GetMapping("/chat")
    String chat(@RequestParam(defaultValue = "Tell me a joke about Java.") String message) {
        return chat.prompt(message).call().content();
    }

    /** 以服务器发送事件形式进行 token 流式传输。 */
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<String> stream(@RequestParam(defaultValue = "Write a haiku about rivers.") String message) {
        return chat.prompt(message).stream().content();
    }

    /** 多轮对话：会话顾问按会话 id 存储并重放历史记录。 */
    @GetMapping("/chat/memory")
    String memory(@RequestParam String message, @RequestParam(defaultValue = "default") String conversationId) {
        return memoryChat.prompt(message)
                .advisors(SessionAdvisors.session(conversationId))
                .call()
                .content();
    }

    /** 工具调用：模型决定调用 {@link WeatherTools}，由 Spring AI 执行。 */
    @GetMapping("/weather")
    String weather(@RequestParam(defaultValue = "Sofia") String city) {
        return chat.prompt("Use the weather tool to check the current weather in " + city + ".")
                .tools(new WeatherTools())
                .call()
                .content();
    }

    /**
     * 结构化输出：Spring AI 根据 record 推导出 JSON schema，jinfer 将 token 采样约束到该 schema，
     * 因此响应始终可以解析。
     */
    @PostMapping(value = "/incident", consumes = MediaType.TEXT_PLAIN_VALUE)
    Incident incident(@RequestBody String report) {
        return chat.prompt("Extract this incident as a JSON object with severity (low, medium or high), "
                        + "summary (one short sentence) and pageOnCall (a boolean). Report: " + report)
                .call()
                .entity(Incident.class, ChatClient.EntityParamSpec::useProviderStructuredOutput);
    }
}
