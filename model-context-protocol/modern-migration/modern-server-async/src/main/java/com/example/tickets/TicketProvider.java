package com.example.tickets;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.modelcontextprotocol.modern.McpSchema.CacheScope;
import reactor.core.publisher.Mono;

import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpComplete;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * 其他服务器的工具、资源和提示词，为 {@code spring.ai.mcp.server.type=ASYNC} 编写。
 * 一个 MCP 请求只有一个结果，因此方法返回单个值：{@code Mono}、{@code CompletableFuture}
 * 或普通值。返回 {@code Flux} 的方法会在启动时被拒绝；请改为返回一个列表的 {@code Mono}。
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
	public Mono<String> listTickets() {
		return Mono.fromSupplier(() -> this.tickets.toJson(this.tickets.list()));
	}

	@McpTool(name = "open_ticket", description = "Opens a ticket and returns its id")
	public Mono<String> openTicket(@McpToolParam(description = "Title of the ticket") String title) {
		return Mono.fromSupplier(() -> this.tickets.open(title).id());
	}

	// 普通返回类型也可以：服务器会在 Reactor 的 boundedElastic 调度器上运行该方法，
	// 因此它可以阻塞而不会拖住 WebFlux 事件循环。
	@McpTool(name = "resolve_ticket", description = "Marks a ticket as resolved")
	public String resolveTicket(@McpToolParam(description = "Ticket id, e.g. t-1") String id) {
		return this.tickets.resolve(id).map(ticket -> "Resolved " + ticket.id()).orElse("No such ticket: " + id);
	}

	// 任何单值的异步类型都会通过 Spring 的 ReactiveAdapterRegistry 解析。
	@McpTool(name = "count_open_tickets", description = "Counts the tickets that are not resolved")
	public CompletableFuture<String> countOpenTickets() {
		return CompletableFuture
			.supplyAsync(() -> String.valueOf(this.tickets.list().stream().filter(ticket -> !ticket.resolved()).count()));
	}

	@McpResource(uri = "tickets://all", name = "All tickets", description = "Every ticket as a JSON array",
			mimeType = "application/json", ttlMs = 30_000, cacheScope = CacheScope.PUBLIC)
	public Mono<String> allTickets() {
		return Mono.fromSupplier(() -> this.tickets.toJson(this.tickets.list()));
	}

	@McpResource(uri = "tickets://{id}", name = "Ticket", description = "One ticket as JSON", mimeType = "application/json")
	public Mono<String> oneTicket(String id) {
		return Mono.fromSupplier(() -> this.tickets.find(id).map(ticket -> this.tickets.toJson(List.of(ticket))).orElse("null"));
	}

	// 补全 tickets://{id} 的 {id} 变量。处理器随该模板一起提供，
	// 因此它必须指定本应用的一个 @McpResource 模板。
	@McpComplete(uri = "tickets://{id}")
	public Mono<List<String>> completeTicketId(String prefix) {
		return Mono.fromSupplier(() -> this.tickets.list().stream().map(ticket -> ticket.id()).filter(id -> id.startsWith(prefix)).toList());
	}

	@McpPrompt(name = "daily-summary", description = "Asks the model to summarise the open tickets for an owner")
	public Mono<String> dailySummary(@McpArg(name = "owner", description = "Who the summary is for", required = true) String owner) {
		return Mono.fromSupplier(() -> "Summarise the open tickets for " + owner + " in two sentences: " + this.tickets.toJson(this.tickets.list()));
	}

	// 补全 daily-summary 的 owner 参数，服务器会将它附加到该提示词上。
	@McpComplete(prompt = "daily-summary")
	public Mono<List<String>> completeOwner(String prefix) {
		return Mono.just(OWNERS.stream().filter(owner -> owner.toLowerCase().startsWith(prefix.toLowerCase())).toList());
	}

}
