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
import io.modelcontextprotocol.modern.server.McpAsyncNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.ai.mcp.annotation.McpClientCapability;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.ai.mcp.annotation.context.McpModernRequestContext;
import org.springframework.stereotype.Component;

/**
 * {@code modern-server-from-streamable} 的两个多轮往返工具，为 {@code type=ASYNC} 编写。
 * 流程相同：第一次调用返回一个 {@link InputRequiredResult}，客户端携带
 * {@code inputResponses} 和密封的 {@code requestState} 重试。
 * <p>
 * {@code deleteTicket} 声明了 SDK 的 {@link McpAsyncNotifier}，因此以一段简短的
 * {@code text/event-stream} 应答；{@code progress(...)} 返回一个 {@code Mono<Void>}，
 * 方法会将它组合进结果。{@code summarizeTickets} 只声明上下文
 * {@link McpModernRequestContext}（两种 API 类型相同），并以普通 JSON 应答。
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
	public Mono<CallToolOutcome> deleteTicket(McpAsyncNotifier notifier, McpModernRequestContext ctx,
			@McpToolParam(description = "Ticket id") String id) {
		logger.info("delete_ticket called for {}", id);
		ElicitResponse answer = ctx.inputResponse("confirm", ElicitResponse.class);
		if (answer == null) {
			// 第一轮往返：询问。工单 id 通过密封的请求状态传递。
			return notifier.progress(10, 100.0, "asking for confirmation")
				.then(Mono.fromSupplier(() -> InputRequiredResult.builder()
					.elicit("confirm", ElicitFormRequest.builder("Delete ticket " + id + "?", CONFIRM_SCHEMA).build())
					.requestState(id)
					.build()));
		}
		// 第二轮往返：对密封进请求状态的工单执行操作；
		// 不信任重试时的 id 参数。
		String sealedId = ctx.requestState();
		boolean confirmed = "accept".equals(answer.action()) && answer.content() != null
				&& Boolean.TRUE.equals(answer.content().get("confirmed"));
		if (!confirmed) {
			return Mono.just(text("Kept " + sealedId));
		}
		return notifier.progress(100, 100.0, "deleting")
			.then(Mono.fromSupplier(() -> text(this.tickets.delete(sealedId) ? "Deleted " + sealedId : "No such ticket: " + sealedId)));
	}

	@McpTool(name = "summarize_tickets", description = "Asks the client's model to summarise the ticket list",
			requiredCapabilities = McpClientCapability.SAMPLING)
	public Mono<CallToolOutcome> summarizeTickets(McpModernRequestContext ctx) {
		Map<?, ?> sampled = ctx.inputResponse("summary", Map.class);
		if (sampled == null) {
			String prompt = "Summarise these tickets in one sentence: " + this.tickets.toJson(this.tickets.list());
			return Mono.just(InputRequiredResult.builder()
				.createMessage("summary", CreateMessageRequest
					.builder(List.of(SamplingMessage.builder(Role.USER, TextContent.builder(prompt).build()).build()), 300)
					.build())
				.build());
		}
		Object content = sampled.get("content");
		Object summary = content instanceof Map<?, ?> map ? map.get("text") : content;
		return Mono.just(text(String.valueOf(summary)));
	}

	private static CallToolOutcome text(String text) {
		return CallToolResult.builder().addContent(TextContent.builder(text).build()).build();
	}

}
