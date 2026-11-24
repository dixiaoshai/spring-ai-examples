package com.example.tickets.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.modern.client.McpAsyncClient;
import io.modelcontextprotocol.modern.client.transport.McpHttpRequestCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.mcp.modern.client.AsyncModernMcpClientScope;
import org.springframework.ai.mcp.modern.client.AsyncModernMcpTransportContextProvider;
import org.springframework.ai.mcp.modern.client.McpModernToolFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 异步客户端类型的 modern 连接在 {@link ClientHandlers} 的响应式处理器之外还需要的内容：
 * 每个请求的调用者、携带调用者的请求头、要隐藏的工具，以及为每个角色保留一个客户端的
 * scope。
 */
@Configuration(proxyBeanMethods = false)
class ModernClientConfiguration {

	/** 调用者角色在传输上下文中的键。 */
	static final String ROLE = "role";

	private static final Logger logger = LoggerFactory.getLogger(ModernClientConfiguration.class);

	/**
	 * 提供每个请求的调用者。异步客户端不持有调用者，因此同步示例中的客户端定制器没有
	 * 对应物：每次解析工具时都会在解析它们的线程上询问该 Bean。工具列表会按该调用者
	 * 发出，那次解析产生的工具回调会保留它，因此之后或在其他线程上运行的调用仍然以该
	 * 调用者的身份发出。它是调用者的唯一来源：聊天调用 Reactor 上下文中的传输上下文
	 * 不会被使用。监听流属于应用，不会询问它。
	 */
	@Bean
	AsyncModernMcpTransportContextProvider callerProvider() {
		return connection -> {
			String role = CurrentRole.get();
			return role != null ? McpTransportContext.create(Map.of(ROLE, role)) : McpTransportContext.EMPTY;
		};
	}

	/**
	 * 仅在连接设置 {@code client-scope=CUSTOM} 时使用：为每个角色保留一个客户端，
	 * 从而使服务器标记为私有的工具列表在该角色下保留服务器允许的时长。
	 * 一个 scope 服务于所有连接，因此键中包含连接名。
	 */
	@Bean
	AsyncModernMcpClientScope perRoleClientScope() {
		Map<String, McpAsyncClient> clients = new ConcurrentHashMap<>();
		return (connection, clientFactory) -> clients.computeIfAbsent(connection.name() + "/" + CurrentRole.get(),
				key -> {
					logger.info("CLIENT SCOPE: a new client for {}", key);
					return clientFactory.get();
				});
	}

	/** 将调用者的角色发送给服务器，就像发送访问令牌那样。 */
	@Bean
	McpHttpRequestCustomizer roleHeader() {
		return McpHttpRequestCustomizer.fromSync((request, headers, context) -> {
			Object role = context.get(ROLE);
			if (role != null) {
				headers.set("X-Role", role.toString());
			}
		});
	}

	@Bean
	WireLog wireLog() {
		return new WireLog();
	}

	@Bean
	ListenLog listenLog() {
		return new ListenLog();
	}

	/**
	 * 隐藏需要采样的工具，modern SDK 客户端不支持采样；服务器会以 {@code -32021} 拒绝该调用。
	 * 旧版的 {@code McpToolFilter} 不适用于 modern 连接。
	 */
	@Bean
	McpModernToolFilter modernToolFilter() {
		return (connection, tool) -> !tool.name().equals("summarize_tickets");
	}

}
