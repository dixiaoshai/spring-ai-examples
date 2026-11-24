package com.example.tickets.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.modern.client.McpClient;
import io.modelcontextprotocol.modern.client.McpSyncClient;
import io.modelcontextprotocol.modern.client.transport.McpHttpRequestCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.mcp.customizer.McpClientCustomizer;
import org.springframework.ai.mcp.modern.client.McpModernToolFilter;
import org.springframework.ai.mcp.modern.client.SyncModernMcpClientScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * modern 连接在 {@link ClientHandlers} 的注解处理器之外还需要的内容：每个请求的调用者、
 * 携带调用者的请求头、要隐藏的工具，以及为每个角色保留一个客户端的 scope。
 */
@Configuration(proxyBeanMethods = false)
class ModernClientConfiguration {

	/** 调用者角色在传输上下文中的键。 */
	static final String ROLE = "role";

	private static final Logger logger = LoggerFactory.getLogger(ModernClientConfiguration.class);

	/**
	 * 定制每个 modern 客户端。使用默认的 {@code PER_REQUEST} 客户端 scope 时，每次解析工具
	 * 都会在解析它们的线程上创建一个客户端，因此调用者在这里被读取一次并绑定到该客户端。
	 * 之后或在其他线程上运行的调用仍然以该调用者的身份发出。监听流的客户端在启动时构建，
	 * 不在任何请求内，因此它没有调用者。
	 */
	@Bean
	McpClientCustomizer<McpClient.SyncBuilder> modernClientCustomizer() {
		return (connectionName, builder) -> {
			String role = CurrentRole.get();
			McpTransportContext caller = McpTransportContext.create(role != null ? Map.of(ROLE, role) : Map.of());
			builder.transportContextProvider(() -> caller);
		};
	}

	/**
	 * 仅在连接设置 {@code client-scope=CUSTOM} 时使用：为每个角色保留一个客户端，
	 * 从而使服务器标记为私有的工具列表在该角色下保留服务器允许的时长。工厂会在调用者的
	 * 线程上构建客户端，配置方式与上面的定制器相同。一个 scope 服务于所有连接，
	 * 因此键中包含连接名。
	 */
	@Bean
	SyncModernMcpClientScope perRoleClientScope() {
		Map<String, McpSyncClient> clients = new ConcurrentHashMap<>();
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
