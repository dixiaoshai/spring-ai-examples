package com.example.tickets.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.modern.client.transport.McpHttpRequest;
import io.modelcontextprotocol.modern.client.transport.McpHttpRequestCustomizer;
import io.modelcontextprotocol.modern.client.transport.McpHttpRequestHeaders;
import reactor.core.publisher.Mono;

/**
 * 打印 modern 连接发送的每个请求，并按 MCP 方法统计请求次数，以便示例展示工具列表在
 * 什么情况下无需询问服务器即可提供。
 */
class WireLog implements McpHttpRequestCustomizer {

	private final Map<String, AtomicInteger> requests = new ConcurrentHashMap<>();

	@Override
	public Mono<Void> customize(McpHttpRequest request, McpHttpRequestHeaders headers, McpTransportContext context) {
		this.requests.computeIfAbsent(request.mcpMethod(), method -> new AtomicInteger()).incrementAndGet();
		Object role = context.get(ModernClientConfiguration.ROLE);
		System.out.println("    [http] " + request.mcpMethod() + (request.mcpName() != null ? " " + request.mcpName() : "")
				+ (role != null ? " (X-Role: " + role + ")" : ""));
		return Mono.empty();
	}

	/** 到目前为止某个 MCP 方法（例如 {@code tools/list}）已发送的请求数。 */
	int count(String mcpMethod) {
		AtomicInteger count = this.requests.get(mcpMethod);
		return count != null ? count.get() : 0;
	}

}
