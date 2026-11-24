package com.example.tickets;

import java.util.Map;
import java.util.function.Supplier;

import io.modelcontextprotocol.modern.McpSchema.CallToolResult;
import io.modelcontextprotocol.modern.McpSchema.TextContent;
import io.modelcontextprotocol.modern.McpSchema.Tool;
import io.modelcontextprotocol.modern.server.McpSyncResponse;
import io.modelcontextprotocol.modern.server.feature.McpSyncTool;

import org.springframework.ai.mcp.modern.InMemorySyncToolRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.function.SingletonSupplier;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 在运行时添加工具。modern 服务器没有 {@code McpSyncServer}；工具位于仓库中，
 * 而 {@link InMemorySyncToolRepository#add} 会向每条打开的 {@code subscriptions/listen} 流
 * 广播 {@code notifications/tools/list_changed}。工具本身是 SDK 的 {@link McpSyncTool}：
 * 一个定义加上一个处理器。
 */
@RestController
public class AdminController {

	// 延迟解析且只解析一次：直接注入仓库会在装配该控制器时、在 @McpTool Bean 被扫描之前
	// 就创建它，从而以空仓库启动。
	// ObjectProvider 推迟查找；SingletonSupplier 缓存其结果。
	private final Supplier<InMemorySyncToolRepository> tools;

	public AdminController(ObjectProvider<InMemorySyncToolRepository> tools) {
		this.tools = SingletonSupplier.of(tools::getObject);
	}

	@PostMapping("/admin/tools/{name}")
	public String addEchoTool(@PathVariable String name) {
		String toolName = "echo_" + name;
		Tool tool = Tool.builder(toolName, Map.of("type", "object", "properties", Map.of("text", Map.of("type", "string"))))
			.description("Echoes its 'text' argument, registered at runtime")
			.build();
		this.tools.get().add(McpSyncTool.of(tool, (ctx, request) -> McpSyncResponse.result(CallToolResult.builder()
			.addContent(TextContent.builder(toolName + ": " + (request.arguments() != null ? request.arguments().get("text") : null)).build())
			.build())));
		return "Registered " + toolName + "\n";
	}

}
