package com.example.tickets;

import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.modern.McpSchema.CallToolOutcome;
import io.modelcontextprotocol.modern.McpSchema.CallToolResult;
import io.modelcontextprotocol.modern.McpSchema.CreateMessageRequest;
import io.modelcontextprotocol.modern.McpSchema.ElicitFormRequest;
import io.modelcontextprotocol.modern.McpSchema.InputRequiredResult;
import io.modelcontextprotocol.modern.McpSchema.Role;
import io.modelcontextprotocol.modern.McpSchema.SamplingMessage;
import io.modelcontextprotocol.modern.McpSchema.TextContent;
import io.modelcontextprotocol.modern.server.McpSyncNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.mcp.annotation.McpClientCapability;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.ai.mcp.annotation.context.McpModernSyncRequestContext;
import org.springframework.stereotype.Component;

/**
 * 与 STREAMABLE 服务器相同的两个交互式工具，但为 modern 协议重写。这里没有会话，
 * 因此处理器不能阻塞等待客户端：它返回描述所需内容的 {@link InputRequiredResult}，
 * 客户端携带 {@code inputResponses} 和服务器密封的 {@code requestState} 重试同一调用。
 * <p>
 * 响应形态由方法自己选择。{@code deleteTicket} 声明了 SDK 的 {@link McpSyncNotifier}，
 * 因此以一段简短的 {@code text/event-stream} 应答，并在结果之前携带其进度通知。
 * {@code summarizeTickets} 只声明上下文，并以普通 JSON 应答。
 * <p>
 * 两个工具缺少各自的客户端能力都无法工作，因此都在 {@code requiredCapabilities} 中声明了它。
 * 请求未声明该能力的客户端会在方法运行之前收到 JSON-RPC 错误 {@code -32021}，
 * 且 {@code deleteTicket} 不会打开任何流。
 */
@Component
public class InteractiveTicketProvider {

	private static final Logger logger = LoggerFactory.getLogger(InteractiveTicketProvider.class);

	/** {@code inputResponses} 中引导应答的结构。 */
	public record ElicitResponse(String action, Map<String, Object> content) {
	}

	private static final Map<String, Object> CONFIRM_SCHEMA = Map.of("type", "object", "properties",
			Map.of("confirmed", Map.of("type", "boolean")), "required", List.of("confirmed"));

	private final TicketService tickets;

	public InteractiveTicketProvider(TicketService tickets) {
		this.tickets = tickets;
	}

	@McpTool(name = "delete_ticket", description = "Deletes a ticket after asking the user to confirm",
			requiredCapabilities = McpClientCapability.ELICITATION_FORM)
	public CallToolOutcome deleteTicket(McpSyncNotifier notifier, McpModernSyncRequestContext ctx,
			@McpToolParam(description = "Ticket id") String id) {
		logger.info("delete_ticket called for {}", id); // 向客户端发送日志的方式已移除；改为在服务器端记录
		ElicitResponse answer = ctx.inputResponse("confirm", ElicitResponse.class);
		if (answer == null) {
			// 第一轮往返：询问。工单 id 通过密封的请求状态传递。
			notifier.progress(10, 100.0, "asking for confirmation");
			return InputRequiredResult.builder()
				.elicit("confirm", ElicitFormRequest.builder("Delete ticket " + id + "?", CONFIRM_SCHEMA).build())
				.requestState(id)
				.build();
		}
		// 第二轮往返：根据应答执行操作，作用于第一轮往返中密封进请求状态的工单。
		// 封存覆盖工具和调用者，但不覆盖参数，
		// 因此不信任重试时的 id 参数。
		String sealedId = ctx.requestState();
		boolean confirmed = "accept".equals(answer.action()) && answer.content() != null
				&& Boolean.TRUE.equals(answer.content().get("confirmed"));
		if (!confirmed) {
			return text("Kept " + sealedId);
		}
		notifier.progress(100, 100.0, "deleting");
		return text(this.tickets.delete(sealedId) ? "Deleted " + sealedId : "No such ticket: " + sealedId);
	}

	@McpTool(name = "summarize_tickets", description = "Asks the client's model to summarise the ticket list",
			requiredCapabilities = McpClientCapability.SAMPLING)
	public CallToolOutcome summarizeTickets(McpModernSyncRequestContext ctx) {
		Map<?, ?> sampled = ctx.inputResponse("summary", Map.class);
		if (sampled == null) {
			String prompt = "Summarise these tickets in one sentence: " + this.tickets.toJson(this.tickets.list());
			return InputRequiredResult.builder()
				.createMessage("summary", CreateMessageRequest
					.builder(List.of(SamplingMessage.builder(Role.USER, TextContent.builder(prompt).build()).build()), 300)
					.build())
				.build();
		}
		Object content = sampled.get("content");
		Object summary = content instanceof Map<?, ?> map ? map.get("text") : content;
		return text(String.valueOf(summary));
	}

	private static CallToolResult text(String text) {
		return CallToolResult.builder().addContent(TextContent.builder(text).build()).build();
	}

}
