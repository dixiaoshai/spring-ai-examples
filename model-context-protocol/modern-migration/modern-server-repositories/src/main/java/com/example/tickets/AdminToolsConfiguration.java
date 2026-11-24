package com.example.tickets;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.modern.McpException;
import io.modelcontextprotocol.modern.McpSchema.CallToolResult;
import io.modelcontextprotocol.modern.McpSchema.ClientCapabilities;
import io.modelcontextprotocol.modern.McpSchema.ElicitFormRequest;
import io.modelcontextprotocol.modern.McpSchema.InputRequiredResult;
import io.modelcontextprotocol.modern.McpSchema.TextContent;
import io.modelcontextprotocol.modern.McpSchema.Tool;
import io.modelcontextprotocol.modern.server.McpSyncResponse;
import io.modelcontextprotocol.modern.server.feature.McpSyncTool;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 用代码而非注解定义的工具。{@code List<McpSyncTool>} Bean 与注解扫描器使用的约定相同，
 * 因此这些工具会与注解工具一起进入工具仓库。基于会话的服务器中的
 * {@code List<McpServerFeatures.SyncToolSpecification>} Bean 就迁移到这里。
 * <p>
 * 两个工具都命名为 {@code admin_*}；{@link RoleFilteringRepositoryConfiguration} 只向发送
 * {@code X-Role: admin} 的调用者展示它们。
 */
@Configuration(proxyBeanMethods = false)
public class AdminToolsConfiguration {

	/**
	 * {@code admin_purge_resolved} 会请求确认，因此客户端必须支持表单引导。
	 * 这是 {@code @McpTool(requiredCapabilities = McpClientCapability.ELICITATION_FORM)}
	 * 的编程式等价形式。
	 */
	private static final ClientCapabilities FORM_ELICITATION = ClientCapabilities.builder()
		.elicitation(new ClientCapabilities.Elicitation(new ClientCapabilities.Elicitation.Form(), null))
		.build();

	private static final Map<String, Object> NO_ARGUMENTS = Map.of("type", "object", "properties", Map.of());

	private static final Map<String, Object> CONFIRM_SCHEMA = Map.of("type", "object", "properties",
			Map.of("confirmed", Map.of("type", "boolean")), "required", List.of("confirmed"));

	@Bean
	List<McpSyncTool> adminTools(TicketService tickets) {
		return List.of(stats(tickets), purgeDone(tickets));
	}

	private static McpSyncTool stats(TicketService tickets) {
		Tool tool = Tool.builder("admin_stats", NO_ARGUMENTS).description("Counts open and resolved tickets").build();
		return McpSyncTool.of(tool, (ctx, request) -> {
			long resolved = tickets.list().stream().filter(TicketService.Ticket::resolved).count();
			return McpSyncResponse.result(text("open=" + (tickets.list().size() - resolved) + ", resolved=" + resolved));
		});
	}

	/**
	 * 直接针对 SDK 编写的多轮往返工具。服务器会在处理器运行之前检查必需的能力：
	 * 未声明表单引导的客户端会收到 JSON-RPC 错误 {@code -32021}，而不是一个它无法回答的问题。
	 */
	private static McpSyncTool purgeDone(TicketService tickets) {
		Tool tool = Tool.builder("admin_purge_resolved", NO_ARGUMENTS)
			.description("Deletes all resolved tickets after asking the user to confirm")
			.build();
		return McpSyncTool.of(tool, FORM_ELICITATION, (ctx, request) -> {
			Object answer = request.inputResponses() != null ? request.inputResponses().get("confirm") : null;
			if (answer == null) {
				// 第一轮往返：询问，并将要确认的 id 密封进请求状态，
				// 这样期间新解决的工单就不会在未经确认的情况下被删除。
				List<String> resolved = tickets.list().stream().filter(TicketService.Ticket::resolved).map(TicketService.Ticket::id).toList();
				if (resolved.isEmpty()) {
					return McpSyncResponse.result(text("Nothing to purge"));
				}
				return McpSyncResponse.result(InputRequiredResult.builder()
					.elicit("confirm", ElicitFormRequest.builder("Delete resolved tickets " + resolved + "?", CONFIRM_SCHEMA).build())
					.requestState(String.join(",", resolved))
					.build());
			}
			// 第二轮往返：服务器已校验并解封了请求状态。
			if (request.requestState() == null) {
				throw McpException.invalidParams("A retry of admin_purge_resolved must carry the requestState");
			}
			if (!confirmed(answer)) {
				return McpSyncResponse.result(text("Kept all tickets"));
			}
			List<String> deleted = Arrays.stream(request.requestState().split(",")).filter(tickets::delete).toList();
			return McpSyncResponse.result(text("Deleted " + deleted));
		});
	}

	private static boolean confirmed(Object answer) {
		return answer instanceof Map<?, ?> response && "accept".equals(response.get("action"))
				&& response.get("content") instanceof Map<?, ?> content && Boolean.TRUE.equals(content.get("confirmed"));
	}

	private static CallToolResult text(String text) {
		return CallToolResult.builder().addContent(TextContent.builder(text).build()).build();
	}

}
