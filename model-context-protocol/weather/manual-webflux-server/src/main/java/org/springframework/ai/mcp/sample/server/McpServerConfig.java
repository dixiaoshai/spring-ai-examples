package org.springframework.ai.mcp.sample.server;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProvider;

import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.mcp.server.webflux.transport.WebFluxSseServerTransportProvider;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;

@Configuration
public class McpServerConfig {

	// STDIO 传输
	@Bean
	@ConditionalOnProperty(prefix = "transport", name = "mode", havingValue = "stdio")
	public StdioServerTransportProvider stdioServerTransportProvider() {
		return new StdioServerTransportProvider(McpJsonDefaults.getMapper());
	}

	// SSE 传输
	@Bean
	@ConditionalOnProperty(prefix = "transport", name = "mode", havingValue = "sse")
	public WebFluxSseServerTransportProvider sseServerTransportProvider() {
		return WebFluxSseServerTransportProvider.builder().messageEndpoint("/mcp/message").build();
	}

	// SSE 传输的路由函数，Spring WebFlux 用它启动 HTTP
	// 服务器。
	@Bean
	@ConditionalOnProperty(prefix = "transport", name = "mode", havingValue = "sse")
	public RouterFunction<?> mcpRouterFunction(WebFluxSseServerTransportProvider transportProvider) {
		return transportProvider.getRouterFunction();
	}

	@Bean
	public WeatherApiClient weatherApiClient() {
		return new WeatherApiClient();
	}

	@Bean
	public McpSyncServer mcpServer(McpServerTransportProvider transportProvider, WeatherApiClient weatherApiClient) { // @formatter:off

		// 配置服务器能力，并支持资源
		var capabilities = McpSchema.ServerCapabilities.builder()
			.tools(true) // 工具支持，带列表变更通知
			.logging() // 日志支持
			.build();

		// 创建同时具有工具和资源能力的服务器
		McpSyncServer server = McpServer.sync(transportProvider)
			.serverInfo("MCP Demo Weather Server", "1.0.0")
			.capabilities(capabilities)
			.tools(McpToolUtils.toSyncToolSpecifications(ToolCallbacks.from(weatherApiClient))) // 添加 @Tools
			.build();
		
		return server; // @formatter:on
	} // @formatter:on

}
