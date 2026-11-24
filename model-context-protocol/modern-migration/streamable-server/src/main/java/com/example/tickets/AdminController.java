package com.example.tickets;

import java.util.Map;
import java.util.function.Supplier;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.function.SingletonSupplier;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 在运行时添加工具。在基于会话的服务器上，这会通过 {@link McpSyncServer#addTool} 完成，
 * 该方法还会向每个已连接的会话发送 {@code notifications/tools/list_changed}。
 */
@RestController
public class AdminController {

	// 延迟解析且只解析一次：直接注入 McpSyncServer 会在装配该控制器时、在 @McpTool Bean
	// 被扫描之前就创建它，服务器会以空工具列表启动。
	// ObjectProvider 推迟查找；
	// SingletonSupplier 缓存其结果。
	private final Supplier<McpSyncServer> server;

	public AdminController(ObjectProvider<McpSyncServer> server) {
		this.server = SingletonSupplier.of(server::getObject);
	}

	@PostMapping("/admin/tools/{name}")
	public String addEchoTool(@PathVariable String name) {
		String toolName = "echo_" + name;
		Tool tool = Tool.builder()
			.name(toolName)
			.description("Echoes its 'text' argument, registered at runtime")
			.inputSchema(Map.of("type", "object", "properties", Map.of("text", Map.of("type", "string"))))
			.build();
		this.server.get().addTool(McpServerFeatures.SyncToolSpecification.builder()
			.tool(tool)
			.callHandler((exchange, request) -> CallToolResult.builder()
				.addTextContent(toolName + ": " + (request.arguments() != null ? request.arguments().get("text") : null))
				.build())
			.build());
		return "Registered " + toolName + "\n";
	}

}
