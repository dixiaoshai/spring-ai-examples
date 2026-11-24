package com.example.tickets;

import java.util.List;

import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpComplete;
import org.springframework.ai.mcp.annotation.McpIcon;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * 其他示例中的注解工具和提示词。注解扫描器会将它们转换为构建仓库所用的
 * {@code List<McpSyncTool>} 和 {@code List<McpSyncPrompt>} Bean。
 * <p>
 * 这里没有 {@code @McpResource} 方法：该服务器定义了自己的资源仓库
 * （{@link TicketResourceRepository}），有了它，带注解的资源将不会被提供。
 * <p>
 * {@code open_ticket} 和该提示词还带有定义属性：一个图标（客户端可以在工具旁渲染），
 * 以及提示词参数的显示标题。它们都是普通的注解属性，在每种服务器上行为相同。
 */
@Component
public class TicketProvider {

	/** 每日摘要所面向的人员；工单本身没有所有者。 */
	private static final List<String> OWNERS = List.of("Ana", "Andrei", "Ben", "Chloe");

	private final TicketService tickets;

	public TicketProvider(TicketService tickets) {
		this.tickets = tickets;
	}

	@McpTool(name = "list_tickets", description = "Lists all tickets as JSON")
	public String listTickets() {
		return this.tickets.toJson(this.tickets.list());
	}

	@McpTool(name = "open_ticket", description = "Opens a ticket and returns its id",
			icons = @McpIcon(src = "https://example.com/icons/ticket-add.svg", mimeType = "image/svg+xml", sizes = "any"))
	public String openTicket(@McpToolParam(description = "Title of the ticket") String title) {
		return this.tickets.open(title).id();
	}

	@McpTool(name = "resolve_ticket", description = "Marks a ticket as resolved")
	public String resolveTicket(@McpToolParam(description = "Ticket id, e.g. t-1") String id) {
		return this.tickets.resolve(id).map(ticket -> "Resolved " + ticket.id()).orElse("No such ticket: " + id);
	}

	@McpPrompt(name = "daily-summary", description = "Asks the model to summarise the open tickets for an owner")
	public String dailySummary(
			@McpArg(name = "owner", title = "Owner", description = "Who the summary is for", required = true) String owner) {
		return "Summarise the open tickets for " + owner + " in two sentences: " + this.tickets.toJson(this.tickets.list());
	}

	// 补全 daily-summary 的 owner 参数，modern 服务器会将它附加到该提示词上。
	@McpComplete(prompt = "daily-summary")
	public List<String> completeOwner(String prefix) {
		return OWNERS.stream().filter(owner -> owner.toLowerCase().startsWith(prefix.toLowerCase())).toList();
	}

}
