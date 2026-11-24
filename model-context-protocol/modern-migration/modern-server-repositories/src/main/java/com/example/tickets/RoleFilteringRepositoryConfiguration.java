package com.example.tickets;

import java.util.List;

import io.modelcontextprotocol.modern.McpSchema.CacheScope;
import io.modelcontextprotocol.modern.McpSchema.Tool;
import io.modelcontextprotocol.modern.server.McpRequestContext;
import io.modelcontextprotocol.modern.server.feature.McpChangeFeed;
import io.modelcontextprotocol.modern.server.feature.McpSyncTool;
import io.modelcontextprotocol.modern.server.feature.McpSyncToolRepository;
import io.modelcontextprotocol.modern.server.feature.ToolsPage;
import io.modelcontextprotocol.server.McpTransportContextExtractor;

import org.springframework.ai.mcp.modern.InMemorySyncToolRepository;
import org.springframework.ai.mcp.modern.McpTransportContexts;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.ServerRequest;

/**
 * 通过委托自定义工具仓库。该 Bean 用自动配置本会使用的同一批 {@code List<McpSyncTool>} Bean
 * （注解工具和 {@link AdminToolsConfiguration} 中的工具）构建内存仓库并包装它。
 * 由于现在存在 {@link McpSyncToolRepository} Bean，自动配置的仓库会退出。
 * <p>
 * 没有 {@code X-Role: admin} 请求头的调用者既看不到 {@code admin_*} 工具，也无法调用它们：
 * {@code find} 返回 {@code null}，服务器会将其作为未知工具应答。
 */
@Configuration(proxyBeanMethods = false)
public class RoleFilteringRepositoryConfiguration {

	// 传输上下文默认为空：除非某个提取器 Bean 复制它，否则请求中的任何内容都不会到达仓库或工具。
	// 这个提取器只复制 X-Role 请求头，不复制其他内容。
	@Bean
	McpTransportContextExtractor<ServerRequest> roleHeaderExtractor() {
		return request -> McpTransportContexts.fromHeaders(List.of("X-Role"), request.headers().asHttpHeaders()::get);
	}

	// 这些列表通过 ObjectProvider 解析，且该仓库是 @Bean 方法而不是 @Component，
	// 以便它在注解 Bean 被扫描之后创建。
	@Bean
	McpSyncToolRepository toolRepository(ObjectProvider<List<McpSyncTool>> tools, McpChangeFeed changes) {
		InMemorySyncToolRepository inMemory = InMemorySyncToolRepository.builder()
			.tools(tools.stream().flatMap(List::stream).toList())
			.changeBroadcaster(changes)
			.build();
		return new McpSyncToolRepository() {

			@Override
			public ToolsPage list(McpRequestContext ctx, String cursor) {
				ToolsPage page = inMemory.list(ctx, cursor);
				List<Tool> visible = page.tools().stream().filter(tool -> visible(ctx, tool.name())).toList();
				// 列表取决于调用者，因此不能让共享缓存复用它。
				return new ToolsPage(visible, page.nextCursor(), page.ttlMs(), CacheScope.PRIVATE);
			}

			@Override
			public McpSyncTool find(McpRequestContext ctx, String name) {
				McpSyncTool tool = inMemory.find(ctx, name);
				return tool != null && visible(ctx, name) ? tool : null;
			}

		};
	}

	private static boolean visible(McpRequestContext ctx, String toolName) {
		// 之所以存在，只是因为 roleHeaderExtractor 复制了它；见上文。
		return !toolName.startsWith("admin_") || "admin".equals(McpTransportContexts.header(ctx, "X-Role"));
	}

}
