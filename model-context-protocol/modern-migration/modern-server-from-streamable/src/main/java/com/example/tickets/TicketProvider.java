package com.example.tickets;

import java.util.List;

import io.modelcontextprotocol.modern.McpSchema.CacheScope;

import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpComplete;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * 不需要会话的工具、资源和提示词。这些方法在每个协议上都逐字节相同；
 * 只有下面标注的注解属性在 modern 服务器上有所不同。
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

	@McpTool(name = "open_ticket", description = "Opens a ticket and returns its id")
	public String openTicket(@McpToolParam(description = "Title of the ticket") String title) {
		return this.tickets.open(title).id();
	}

	@McpTool(name = "resolve_ticket", description = "Marks a ticket as resolved")
	public String resolveTicket(@McpToolParam(description = "Ticket id, e.g. t-1") String id) {
		return this.tickets.resolve(id).map(ticket -> "Resolved " + ticket.id()).orElse("No such ticket: " + id);
	}

	@McpResource(uri = "tickets://all", name = "All tickets", description = "Every ticket as a JSON array",
			mimeType = "application/json",
			ttlMs = 30_000, cacheScope = CacheScope.PUBLIC)
	public String allTickets() {
		return this.tickets.toJson(this.tickets.list());
	}

	@McpResource(uri = "tickets://{id}", name = "Ticket", description = "One ticket as JSON", mimeType = "application/json")
	public String oneTicket(String id) {
		return this.tickets.find(id).map(ticket -> this.tickets.toJson(List.of(ticket))).orElse("null");
	}

	// 补全 tickets://{id} 的 {id} 变量。在 modern 服务器上，处理器随该模板一起提供，
	// 因此它必须指定本应用的一个 @McpResource 模板。
	@McpComplete(uri = "tickets://{id}")
	public List<String> completeTicketId(String prefix) {
		return this.tickets.list().stream().map(ticket -> ticket.id()).filter(id -> id.startsWith(prefix)).toList();
	}

	@McpPrompt(name = "daily-summary", description = "Asks the model to summarise the open tickets for an owner")
	public String dailySummary(@McpArg(name = "owner", description = "Who the summary is for", required = true) String owner) {
		return "Summarise the open tickets for " + owner + " in two sentences: " + this.tickets.toJson(this.tickets.list());
	}

	// 补全 daily-summary 的 owner 参数，modern 服务器会将它附加到该提示词上。
	@McpComplete(prompt = "daily-summary")
	public List<String> completeOwner(String prefix) {
		return OWNERS.stream().filter(owner -> owner.toLowerCase().startsWith(prefix.toLowerCase())).toList();
	}

}
