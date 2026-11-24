package com.example.tickets;

import java.util.List;

import io.modelcontextprotocol.modern.McpException;
import io.modelcontextprotocol.modern.McpSchema.CacheScope;
import io.modelcontextprotocol.modern.McpSchema.CompleteResult;
import io.modelcontextprotocol.modern.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.modern.McpSchema.Resource;
import io.modelcontextprotocol.modern.McpSchema.ResourceTemplate;
import io.modelcontextprotocol.modern.McpSchema.TextResourceContents;
import io.modelcontextprotocol.modern.server.McpRequestContext;
import io.modelcontextprotocol.modern.server.McpSyncResponse;
import io.modelcontextprotocol.modern.server.feature.McpChangeBroadcaster;
import io.modelcontextprotocol.modern.server.feature.McpSyncResource;
import io.modelcontextprotocol.modern.server.feature.McpSyncResourceRepository;
import io.modelcontextprotocol.modern.server.feature.McpSyncResourceTemplate;
import io.modelcontextprotocol.modern.server.feature.ResourceTemplatesPage;
import io.modelcontextprotocol.modern.server.feature.ResourcesPage;
import io.modelcontextprotocol.modern.server.feature.ServerChange;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 通过实现 SDK 接口自定义资源仓库。每张工单都是一个资源：{@code tickets://t-1}、
 * {@code tickets://t-2} 等等，每次请求都从 {@link TicketService} 列出：没有需要注册、
 * 添加或移除的内容。列表按工单 id 分页。
 * <p>
 * 工单变更以 Spring 事件到达，并以 {@code subscriptions/listen} 上的 MCP 通知发出：
 * 工单打开或删除时发送 {@code resources/list_changed}，工单变更时针对其 URI 发送
 * {@code resources/updated}。
 * <p>
 * {@code tickets://{id}} 模板补全其 {@code id} 变量：仓库从 {@code findTemplate} 返回带有
 * 补全处理器的模板，并通过 {@code supportsCompletion()} 选择加入，两者都是 SDK 接口的
 * 默认方法。
 */
@Component
public class TicketResourceRepository implements McpSyncResourceRepository {

	private static final String SCHEME = "tickets://";

	private static final ResourceTemplate TEMPLATE = ResourceTemplate.builder(SCHEME + "{id}", "Ticket")
		.description("One ticket as JSON")
		.mimeType("application/json")
		.build();

	/** 刻意设置得很小，以便客户端能看到 {@code nextCursor}。 */
	private static final int PAGE_SIZE = 2;

	private final TicketService tickets;

	private final McpChangeBroadcaster changes;

	public TicketResourceRepository(TicketService tickets, McpChangeBroadcaster changes) {
		this.tickets = tickets;
		this.changes = changes;
	}

	@Override
	public ResourcesPage list(McpRequestContext ctx, String cursor) {
		// 游标是上一页最后一张工单的 id；id 按其数字比较，
		// 因此 t-10 排在 t-9 之后。
		int afterNumber;
		try {
			afterNumber = cursor == null ? 0 : TicketService.number(cursor);
		}
		catch (IllegalArgumentException ex) {
			throw McpException.invalidParams("Unknown cursor: " + cursor);
		}
		List<TicketService.Ticket> after = this.tickets.list()
			.stream()
			.filter(ticket -> TicketService.number(ticket.id()) > afterNumber)
			.toList();
		List<Resource> page = after.stream().limit(PAGE_SIZE).map(TicketResourceRepository::resource).toList();
		String nextCursor = after.size() > PAGE_SIZE ? after.get(PAGE_SIZE - 1).id() : null;
		return new ResourcesPage(page, nextCursor, null, null);
	}

	@Override
	public ResourceTemplatesPage listTemplates(McpRequestContext ctx, String cursor) {
		return ResourceTemplatesPage.of(List.of(TEMPLATE));
	}

	// 针对 ref/resource tickets://{id} 的 completion/complete：SDK 已检查该参数
	// 是模板的变量。
	@Override
	public McpSyncResourceTemplate findTemplate(McpRequestContext ctx, String uriTemplate) {
		if (!TEMPLATE.uriTemplate().equals(uriTemplate)) {
			return null; // 作为未知模板应答
		}
		return McpSyncResourceTemplate.of(TEMPLATE).withCompletionHandler((completeCtx, request) -> {
			List<String> ids = this.tickets.list()
				.stream()
				.map(TicketService.Ticket::id)
				.filter(id -> id.startsWith(request.argument().value()))
				.toList();
			return CompleteResult.of(new CompleteResult.Completion(ids));
		});
	}

	@Override
	public boolean supportsCompletion() {
		return true;
	}

	@Override
	public McpSyncResource find(McpRequestContext ctx, String uri) {
		if (!uri.startsWith(SCHEME) || this.tickets.find(uri.substring(SCHEME.length())).isEmpty()) {
			return null; // 作为未知资源应答
		}
		return McpSyncResource.of((readCtx, request) -> {
			String json = this.tickets.find(uri.substring(SCHEME.length()))
				.map(ticket -> this.tickets.toJson(List.of(ticket)))
				.orElse("null");
			return McpSyncResponse.result(
					ReadResourceResult.builder(List.of(TextResourceContents.builder(uri, json).mimeType("application/json").build()))
						.ttlMs(5_000)
						.cacheScope(CacheScope.PRIVATE)
						.build());
		});
	}

	@Override
	public boolean supportsSubscribe() {
		return true; // 声明 resources.subscribe，使客户端可以监听单个 URI
	}

	@EventListener
	void onTicketChanged(TicketService.TicketChanged event) {
		if (event.kind() == TicketService.TicketChanged.Kind.UPDATED) {
			this.changes.broadcast(new ServerChange.ResourceUpdated(SCHEME + event.id()));
		}
		else {
			// 打开或删除：列表发生了变化；
			// 已删除 URI 的订阅者不会收到可供重新读取的内容。
			this.changes.broadcast(new ServerChange.ResourcesListChanged());
		}
	}

	private static Resource resource(TicketService.Ticket ticket) {
		return Resource.builder(SCHEME + ticket.id(), ticket.title())
			.description(ticket.resolved() ? "Resolved ticket" : "Open ticket")
			.mimeType("application/json")
			.build();
	}

}
