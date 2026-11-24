package com.example.tickets;


import io.modelcontextprotocol.spec.McpSchema.ElicitResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.ai.mcp.annotation.context.McpSyncRequestContext;
import org.springframework.ai.mcp.annotation.context.StructuredElicitResult;
import org.springframework.stereotype.Component;

/**
 * 通过会话回调客户端的工具：日志、进度、引导和采样。这些方法在迁移到 modern 服务器时
 * 必须重写，因为 modern 协议没有可用于回调的会话。
 */
@Component
public class InteractiveTicketProvider {

	public record Confirmation(boolean confirmed) {
	}

	private final TicketService tickets;

	public InteractiveTicketProvider(TicketService tickets) {
		this.tickets = tickets;
	}

	@McpTool(name = "delete_ticket", description = "Deletes a ticket after asking the user to confirm")
	public String deleteTicket(McpSyncRequestContext ctx, @McpToolParam(description = "Ticket id") String id) {
		ctx.info("delete_ticket called for " + id); // 发送给客户端的日志消息
		ctx.progress(p -> p.percentage(10).message("asking for confirmation"));

		// 阻塞式往返：服务器询问，客户端应答，处理器继续执行。
		StructuredElicitResult<Confirmation> answer = ctx.elicit(e -> e.message("Delete ticket " + id + "?"),
				Confirmation.class);
		if (answer.action() != ElicitResult.Action.ACCEPT || answer.structuredContent() == null
				|| !answer.structuredContent().confirmed()) {
			return "Kept " + id;
		}
		ctx.progress(p -> p.percentage(100).message("deleting"));
		return this.tickets.delete(id) ? "Deleted " + id : "No such ticket: " + id;
	}

	@McpTool(name = "summarize_tickets", description = "Asks the client's model to summarise the ticket list")
	public String summarizeTickets(McpSyncRequestContext ctx) {
		var result = ctx.sample(s -> s.message("Summarise these tickets in one sentence: " + this.tickets.toJson(this.tickets.list())));
		return result.content() instanceof TextContent text ? text.text() : String.valueOf(result.content());
	}

}
