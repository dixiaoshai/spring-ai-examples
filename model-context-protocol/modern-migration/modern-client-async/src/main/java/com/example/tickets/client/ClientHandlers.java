package com.example.tickets.client;

import java.util.List;

import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.ElicitResult;
import io.modelcontextprotocol.spec.McpSchema.ProgressNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.annotation.McpElicitation;
import org.springframework.ai.mcp.annotation.McpProgress;
import org.springframework.ai.mcp.annotation.McpToolListChanged;
import org.springframework.ai.mcp.annotation.context.StructuredElicitResult;
import org.springframework.stereotype.Component;

/**
 * 旧版客户端的处理器，但为异步客户端类型编写：注解相同、旧版 schema 类型也相同，
 * 但每个方法返回 {@code Mono}。一个方法同时服务于它所指定的旧版连接和 modern 连接。
 * <p>
 * 在 {@code spring.ai.mcp.client.type=ASYNC} 下只会调用响应式方法。指定了连接的同步方法
 * 会在启动时被报告为警告。
 * <p>
 * {@code legacy} 连接仅在 {@code mixed} profile 下存在。
 */
@Component
public class ClientHandlers {

	private static final Logger logger = LoggerFactory.getLogger(ClientHandlers.class);

	public record Confirmation(boolean confirmed) {
	}

	/**
	 * 应答两种协议的表单引导。在 modern 连接上，SDK 客户端会在服务器请求输入时调用它，
	 * 然后重试工具调用。可选的 {@link ToolContext} 是该引导所属工具调用的上下文，
	 * 因此方法知道该询问谁。应用可以在这里询问用户，并用应答完成该 {@code Mono}。
	 */
	@McpElicitation(clients = { "tickets", "legacy" })
	public Mono<StructuredElicitResult<Confirmation>> confirm(McpSchema.ElicitRequest request,
			ToolContext toolContext) {
		Object user = toolContext != null ? toolContext.getContext().get("user") : null;
		logger.info("ELICITATION{}: {}", user != null ? " for " + user : "", request.message());
		return Mono.just(new StructuredElicitResult<>(ElicitResult.Action.ACCEPT, new Confirmation(true), null));
	}

	/** 异步类型的进度方法返回 {@code Mono<Void>}。 */
	@McpProgress(clients = { "tickets", "legacy" })
	public Mono<Void> progress(ProgressNotification notification) {
		return Mono.fromRunnable(() -> logger.info("PROGRESS {}: {} {}", notification.progressToken(),
				notification.progress(), notification.message()));
	}

	/**
	 * 当设置了 {@code listen=true} 的 modern 连接的服务器报告工具变更时调用，
	 * 监听流在结束之后重新建立时也会调用。modern 服务器在变更时不发送列表，
	 * 因此会先列出工具，且是面向应用而不是某个调用者。
	 */
	@McpToolListChanged(clients = { "tickets", "legacy" })
	public Mono<Void> toolsChanged(List<McpSchema.Tool> tools) {
		return Mono.fromRunnable(
				() -> logger.info("TOOLS LIST CHANGED: {}", tools.stream().map(McpSchema.Tool::name).toList()));
	}

}
