package org.springframework.ai.mcp.sample.server;

import org.springframework.ai.mcp.sample.server.providers.SpringAiToolProvider;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class McpServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(McpServerApplication.class, args);
	}

	// 注意：这与 MCP Annotations 无关，但演示了如何将 Spring AI 的工具 
	// 回调 provider 与 @McpTool 一起注册为 MCP 工具
	@Bean
	ToolCallbackProvider weatherTools(SpringAiToolProvider weatherService) {
		return MethodToolCallbackProvider.builder().toolObjects(weatherService).build();
	}
}
