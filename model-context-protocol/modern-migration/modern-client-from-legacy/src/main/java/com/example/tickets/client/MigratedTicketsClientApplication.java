package com.example.tickets.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.modern.McpSchema.PromptReference;
import io.modelcontextprotocol.modern.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.modern.McpSchema.ResourceReference;
import io.modelcontextprotocol.modern.McpSchema.ResourceTemplate;
import io.modelcontextprotocol.modern.client.McpSyncClient;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.mcp.modern.client.SyncModernMcpConnection;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * 迁移到 modern 协议之后的旧版工单客户端，基于 Spring AI MCP 客户端 starter。
 * modern 连接的工具以 {@link ToolCallback} 的形式从同一个 {@link SyncMcpToolCallbackProvider}
 * Bean 到达，该 Bean 也为旧版连接提供服务；而 {@code ChatClient} 会通过
 * {@code defaultToolCallbacks(provider)} 使用该 Bean。本示例没有模型，因此由它自己调用
 * 这些回调。
 * <p>
 * 每个服务器都会执行通用流程。之后的场景仅在服务器提供它们所需的内容时运行：
 * {@code modern-server-from-streamable} 和 {@code modern-server-async} 的交互式工具，
 * 以及 {@code modern-server-repositories} 的按调用者区分的工具列表。
 */
@SpringBootApplication
public class MigratedTicketsClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(MigratedTicketsClientApplication.class, args).close();
	}

	@Bean
	CommandLineRunner demo(SyncMcpToolCallbackProvider tools, List<SyncModernMcpConnection> connections, WireLog wire,
			ListenLog listen, @Value("${demo.admin-url:http://localhost:8080}") String adminUrl) {
		return args -> {
			// listen=true：该流在后台打开，面向应用而不是某个调用者。
			// 等待它可以保持下面的输出顺序。
			System.out.println(">>> wait for the listen stream of the modern connection");
			if (!listen.awaitEstablished(Duration.ofSeconds(3))) {
				System.out.println("    -> not established: the server cannot be reached, it is tried again");
			}
			ToolCallback[] callbacks = common(tools, wire);
			for (SyncModernMcpConnection connection : connections) {
				if (has(callbacks, connection.info().name() + "_open_ticket")) {
					sdkClient(connection);
				}
				else {
					// 提供者已记录一条警告，在 unreachable-cool-down 时间过去之前
					// 不会再次询问该服务器。
					System.out.println(">>> the server of the connection '" + connection.info().name()
							+ "' cannot be reached: its tools are left out, the other tools stay usable");
				}
			}
			if (has(callbacks, "tickets_count_open_tickets")) {
				System.out.println(">>> tickets_count_open_tickets (a CompletableFuture on the server) -> "
						+ call(callbacks, "tickets_count_open_tickets", "{}"));
			}
			if (has(callbacks, "tickets_delete_ticket")) {
				interactive(tools, callbacks, adminUrl);
			}
			System.out.println(">>> resolve the tool callbacks for a caller with the admin role, and for one without");
			ToolCallback[] adminCallbacks = CurrentRole.callAs("admin", tools::getToolCallbacks);
			ToolCallback[] userCallbacks = tools.getToolCallbacks();
			if (adminCallbacks.length > userCallbacks.length) {
				roles(userCallbacks, adminCallbacks);
			}
			else {
				System.out.println("    -> the same tools: this server does not filter its tools by caller");
			}
		};
	}

	/** 来自同一个提供者 Bean 的所有连接（旧版和 modern）的工具。 */
	private static ToolCallback[] common(SyncMcpToolCallbackProvider tools, WireLog wire) {
		System.out.println(">>> resolve the tool callbacks, as a ChatClient does once for each request");
		ToolCallback[] callbacks = tools.getToolCallbacks();
		System.out.println("    -> " + names(callbacks));

		for (ToolCallback callback : callbacks) {
			// modern 连接上是 tickets_open_ticket，旧版连接上是 open_ticket
			if (callback.getToolDefinition().name().endsWith("open_ticket")) {
				System.out.println(">>> " + callback.getToolDefinition().name() + " -> "
						+ callback.call("{\"title\":\"Printer offline\"}"));
			}
		}

		System.out.println(">>> resolve the tool callbacks again");
		int listRequests = wire.count("tools/list");
		tools.getToolCallbacks();
		// 使用示例服务器的默认配置时为 1：私有列表且没有 ttlMs。当列表被保留时为 0：
		// 由提供者为带 cacheScope=PUBLIC 的 ttlMs 保留，或由生命周期长于请求的客户端
		// （client-scope=SHARED 或 CUSTOM）为 ttlMs 保留。
		System.out.println("    -> " + (wire.count("tools/list") - listRequests) + " tools/list request(s) sent");
		return callbacks;
	}

	/**
	 * 不属于工具的内容会通过连接的 modern SDK 客户端发出：没有 modern 客户端 Bean，
	 * 连接会给出当前请求对应的客户端。
	 */
	private static void sdkClient(SyncModernMcpConnection connection) {
		McpSyncClient client = connection.client();
		System.out.println(">>> SDK client of the connection '" + connection.info().name() + "'");
		System.out.println("    server/discover -> " + client.discover().supportedVersions());

		String uri = client.resources().list().resources().get(0).uri();
		ReadResourceResult resource = client.resource(uri).read();
		System.out.println("    resources/read " + uri + " (ttlMs=" + resource.ttlMs() + ", cacheScope="
				+ resource.cacheScope() + ") -> " + resource.contents());
		System.out.println("    prompts/get daily-summary -> "
				+ client.prompt("daily-summary").arguments(Map.of("owner", "Ana")).get().messages());
		System.out.println("    completion/complete daily-summary owner=\"A\" -> "
				+ client.completion(new PromptReference("daily-summary"), "owner", "A").complete().completion().values());
		List<String> templates = client.resourceTemplates()
			.list()
			.resourceTemplates()
			.stream()
			.map(ResourceTemplate::uriTemplate)
			.toList();
		if (templates.contains("tickets://{id}")) {
			System.out.println("    completion/complete tickets://{id} id=\"t-\" -> " + client
				.completion(new ResourceReference("tickets://{id}"), "id", "t-")
				.complete()
				.completion()
				.values());
		}
	}

	/**
	 * modern-server-from-streamable 和 modern-server-async：引导和进度通过
	 * ClientHandlers 的注解方法处理，另有一个运行时工具添加，由连接的监听流报告。
	 */
	private static void interactive(SyncMcpToolCallbackProvider tools, ToolCallback[] callbacks, String adminUrl)
			throws Exception {
		System.out.println(">>> tickets_delete_ticket (@McpElicitation answers, the SDK client retries the call)");
		// 调用的工具上下文会到达处理器方法，因此处理器知道是哪个用户。
		ToolContext toolContext = new ToolContext(Map.of("user", "ana"));
		System.out.println("    -> " + callback(callbacks, "tickets_delete_ticket").call("{\"id\":\"t-1\"}", toolContext));

		System.out.println(">>> add a tool at runtime: the listen stream reports it to @McpToolListChanged");
		HttpClient.newHttpClient()
			.send(HttpRequest.newBuilder(URI.create(adminUrl + "/admin/tools/hello"))
				.POST(HttpRequest.BodyPublishers.noBody())
				.build(), HttpResponse.BodyHandlers.ofString());
		Thread.sleep(1500);
		System.out.println(">>> tool callbacks after the runtime add -> " + names(tools.getToolCallbacks()));
	}

	/**
	 * modern-server-repositories：服务器只向发送 {@code X-Role: admin} 的调用者展示管理员工具。
	 * 每次解析工具都会为其调用者创建一个客户端。
	 */
	private static void roles(ToolCallback[] callbacks, ToolCallback[] adminCallbacks) {
		System.out.println("    -> admin:   " + names(adminCallbacks));
		System.out.println("    -> no role: " + names(callbacks));

		// 该线程上已不再设置角色：回调保留了它们解析时所对应的调用者，
		// 这正是它们能在流式响应的线程上使用的原因。
		System.out.println(">>> tickets_admin_stats, called after the admin request has ended -> "
				+ call(adminCallbacks, "tickets_admin_stats", "{}"));
		System.out.println(">>> tickets_resolve_ticket -> " + call(callbacks, "tickets_resolve_ticket", "{\"id\":\"t-1\"}"));
		System.out.println(">>> tickets_admin_purge_resolved (elicitation on a programmatic tool, @McpElicitation)");
		System.out.println("    -> " + call(adminCallbacks, "tickets_admin_purge_resolved", "{}"));
	}

	private static String call(ToolCallback[] callbacks, String name, String arguments) {
		return callback(callbacks, name).call(arguments);
	}

	private static ToolCallback callback(ToolCallback[] callbacks, String name) {
		return Arrays.stream(callbacks)
			.filter(callback -> callback.getToolDefinition().name().equals(name))
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("No tool named " + name));
	}

	private static boolean has(ToolCallback[] callbacks, String name) {
		return names(callbacks).contains(name);
	}

	private static List<String> names(ToolCallback[] callbacks) {
		return Arrays.stream(callbacks).map(callback -> callback.getToolDefinition().name()).sorted().toList();
	}

}
