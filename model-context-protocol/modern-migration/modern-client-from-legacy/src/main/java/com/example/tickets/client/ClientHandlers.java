package com.example.tickets.client;

import java.util.List;

import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.ElicitResult;
import io.modelcontextprotocol.spec.McpSchema.ProgressNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.annotation.McpElicitation;
import org.springframework.ai.mcp.annotation.McpProgress;
import org.springframework.ai.mcp.annotation.McpToolListChanged;
import org.springframework.ai.mcp.annotation.context.StructuredElicitResult;
import org.springframework.stereotype.Component;

/**
 * 迁移后的旧版客户端 {@code ClientHandlers}。这些方法保留其注解和旧版 schema 类型：
 * 一个方法同时服务于它所指定的旧版连接和 modern 连接。只有连接名发生了变化，
 * 另外少了两个方法：{@code @McpSampling} 和 {@code @McpLogging}，因为 modern 协议
 * 既没有采样也没有客户端日志。
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
	 * 然后重试工具调用。{@link ToolContext} 参数是新增的可选参数：它是该引导所属工具
	 * 调用的上下文，因此方法知道该询问谁。
	 */
	@McpElicitation(clients = { "tickets", "legacy" })
	public StructuredElicitResult<Confirmation> confirm(McpSchema.ElicitRequest request, ToolContext toolContext) {
		Object user = toolContext != null ? toolContext.getContext().get("user") : null;
		logger.info("ELICITATION{}: {}", user != null ? " for " + user : "", request.message());
		return new StructuredElicitResult<>(ElicitResult.Action.ACCEPT, new Confirmation(true), null);
	}

	@McpProgress(clients = { "tickets", "legacy" })
	public void progress(ProgressNotification notification) {
		logger.info("PROGRESS {}: {} {}", notification.progressToken(), notification.progress(), notification.message());
	}

	/**
	 * 当设置了 {@code listen=true} 的 modern 连接的服务器报告工具变更时调用，
	 * 监听流在结束之后重新建立时也会调用。modern 服务器在变更时不发送列表，
	 * 因此会先列出工具，且是面向应用而不是某个调用者。
	 */
	@McpToolListChanged(clients = { "tickets", "legacy" })
	public void toolsChanged(List<McpSchema.Tool> tools) {
		logger.info("TOOLS LIST CHANGED: {}", tools.stream().map(McpSchema.Tool::name).toList());
	}

}
