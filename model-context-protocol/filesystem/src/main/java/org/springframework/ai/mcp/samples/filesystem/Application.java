package org.springframework.ai.mcp.samples.filesystem;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonDefaults;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * 跨平台的 Spring AI MCP 文件系统演示应用。
 *
 * <p>本应用演示与 MCP Filesystem 服务器的集成，可在 Windows 与 Linux/Mac 平台上运行，
 * 并自动检测操作系统。
 *
 * <h2>配置方式</h2>
 *
 * <h3>方式一：编程式配置（默认）</h3>
 * 如果 application.properties 中未指定 JSON 配置，{@link #mcpClient()} Bean 会以编程方式
 * 创建 MCP 客户端，并自动检测操作系统：
 * <ul>
 *   <li><b>Windows：</b>使用 {@code cmd.exe /c npx} 包装器（npx 执行所必需）</li>
 *   <li><b>Linux/Mac：</b>直接使用 {@code npx}</li>
 * </ul>
 *
 * <h3>方式二：JSON 配置（自动配置）</h3>
 * 在 application.properties 中启用：
 * <pre>
 * # Windows 平台：
 * spring.ai.mcp.client.stdio.servers-configuration=classpath:/mcp-servers-config-windows.json
 *
 * # Linux/Mac 平台：
 * spring.ai.mcp.client.stdio.servers-configuration=classpath:/mcp-servers-config-linux.json
 * </pre>
 *
 * 启用 JSON 配置后，由 Spring Boot 自动配置创建 MCP 客户端，
 * 而 {@link #mcpClient()} Bean 会通过 {@code @ConditionalOnMissingBean} 跳过。
 *
 * <h2>为什么 Windows 需要 cmd.exe 包装器</h2>
 *
 * 在 Windows 上，{@code npx} 并非原生可执行文件，而是批处理文件。Java 的
 * {@code ProcessBuilder}（由 {@code StdioClientTransport} 使用）无法直接执行批处理文件，
 * 需要借助 {@code cmd.exe /c} 包装器来启动这类进程。
 *
 * @see <a href="https://github.com/modelcontextprotocol/servers/tree/main/src/filesystem">
 *      MCP Filesystem Server</a>
 */
@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	/**
	 * 针对 MCP 文件系统服务器运行预定义问题。
	 *
	 * <p>该 Bean 同时适用于编程式与基于 JSON 的客户端配置：
	 * <ul>
	 *   <li>编程式：单个 {@code McpSyncClient} Bean</li>
	 *   <li>JSON 配置：由自动配置提供的 {@code McpSyncClient} Bean 列表</li>
	 * </ul>
	 */
	@Bean
	public CommandLineRunner predefinedQuestions(ChatClient.Builder chatClientBuilder,
			@Autowired(required = false) List<McpSyncClient> mcpSyncClients,
			@Autowired(required = false) McpSyncClient mcpClient,
			ConfigurableApplicationContext context) {

		return args -> {
			// 确定使用哪些客户端：自动配置的列表，或编程式的单个客户端
			List<McpSyncClient> clients = (mcpSyncClients != null && !mcpSyncClients.isEmpty())
					? mcpSyncClients
					: (mcpClient != null ? List.of(mcpClient) : List.of());

			if (clients.isEmpty()) {
				System.err.println("No MCP clients available. Check your configuration.");
				context.close();
				return;
			}

			var chatClient = chatClientBuilder
					.defaultTools(new SyncMcpToolCallbackProvider(clients))
					.build();

			System.out.println("Running predefined questions with AI model responses:\n");

			// 问题 1
			String question1 = "Can you explain the content of the target/spring-ai-mcp-overview.txt file?";
			System.out.println("QUESTION: " + question1);
			System.out.println("ASSISTANT: " + chatClient.prompt(question1).call().content());

			// 问题 2
			String question2 = "Please summarize the content of the target/spring-ai-mcp-overview.txt file and store it in a new target/summary.md as Markdown format?";
			System.out.println("\nQUESTION: " + question2);
			System.out.println("ASSISTANT: " +
					chatClient.prompt(question2).call().content());

			context.close();
		};
	}

	/**
	 * 创建带自动操作系统检测的 MCP 客户端，以兼容跨平台。
	 *
	 * <p>仅当自动配置未提供 MCP 客户端时才会创建该 Bean
	 * （即未启用 JSON 配置时）。若存在 JSON 配置，
	 * 则以 Spring Boot 自动配置优先。
	 *
	 * <h3>平台相关行为：</h3>
	 * <ul>
	 *   <li><b>Windows：</b>命令：{@code cmd.exe}，参数：{@code /c npx -y @modelcontextprotocol/server-filesystem target}</li>
	 *   <li><b>Linux/Mac：</b>命令：{@code npx}，参数：{@code -y @modelcontextprotocol/server-filesystem target}</li>
	 * </ul>
	 *
	 * <p><b>路径处理：</b>使用相对路径 {@code "target"}，以兼容跨平台。
	 * MCP 服务器会基于当前工作目录解析该路径。
	 *
	 * @return 已配置的 MCP 同步客户端
	 * @see <a href="https://github.com/modelcontextprotocol/servers/tree/main/src/filesystem">
	 *      MCP Filesystem Server Documentation</a>
	 */
	@Bean(destroyMethod = "close")
	@ConditionalOnMissingBean(McpSyncClient.class)
	public McpSyncClient mcpClient() {

		ServerParameters stdioParams;

		if (isWindows()) {
			// Windows：npx 是批处理文件，需要 cmd.exe 才能执行
			System.out.println("Detected Windows OS - using cmd.exe wrapper for npx");
			var winArgs = new ArrayList<>(Arrays.asList(
					"/c", "npx", "-y", "@modelcontextprotocol/server-filesystem", "target"));
			stdioParams = ServerParameters.builder("cmd.exe")
					.args(winArgs)
					.build();
		} else {
			// Linux/Mac：npx 可以直接执行
			System.out.println("Detected Unix-like OS - using npx directly");
			stdioParams = ServerParameters.builder("npx")
					.args("-y", "@modelcontextprotocol/server-filesystem", "target")
					.build();
		}

		var mcpClient = McpClient.sync(new StdioClientTransport(stdioParams, McpJsonDefaults.getMapper()))
				.requestTimeout(Duration.ofSeconds(10))
				.build();

		var init = mcpClient.initialize();
		System.out.println("MCP Initialized: " + init);

		return mcpClient;
	}

	/**
	 * 检测当前操作系统是否为 Windows。
	 *
	 * @return 若运行在 Windows 上则返回 true，否则返回 false
	 */
	private static boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().contains("win");
	}

}
