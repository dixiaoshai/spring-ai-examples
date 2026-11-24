# 工单服务器，MODERN（自定义仓库）

`spring.ai.mcp.server.protocol=MODERN`，并把四个仓库中的两个替换为应用 Bean。
该示例展示了 modern 服务器如何自定义它对外提供的内容，而基于会话的服务器只能通过在 `McpSyncServer` 上添加和移除规格来做到这一点。

* **编程式工具** - `AdminToolsConfiguration` 定义一个 `List<McpSyncTool>` Bean，与注解扫描器使用的约定相同。`admin_purge_resolved` 是直接针对 SDK 编写的多轮往返工具：它将表单引导声明为必需的客户端能力（`McpSyncTool.of(tool, requiredCapabilities, handler)`，即 `requiredCapabilities` 注解属性的编程式形式），请求确认，并将其询问过的 id 封存进 `requestState`。
* **包装工具仓库** - `RoleFilteringRepositoryConfiguration` 从 `List<McpSyncTool>` Bean 构建内存工具仓库并包装它。`admin_*` 工具仅对带有 `X-Role: admin` 请求头的请求可见和可调用；这个头之所以能到达仓库，是因为同一个类中的 `McpTransportContextExtractor` Bean 会复制它；传输上下文默认为空，因此请求中的其他内容不会进入。
* **自定义资源仓库** - `TicketResourceRepository` 实现 `McpSyncResourceRepository`。它从 `TicketService` 为每张工单列出一个资源，每次分页两个，服务 `tickets://{id}` 模板并为其 `id` 提供补全（`findTemplate` 返回带有补全处理器的模板，`supportsCompletion()` 用于选择加入），并将 `TicketChanged` Spring 事件转换为 `resources/list_changed` 和 `resources/updated` 通知（在 `subscriptions/listen` 上）。

* **定义属性** - `TicketProvider` 放了一个 `@McpIcon` 在 `open_ticket` 上，并放了一个显示用 `title` 在 `owner` 参数上（`daily-summary` 的）。它们是注解属性，而不是仓库特性，会像在任何类型的服务器上一样出现在 `tools/list` 和 `prompts/list` 中。

提示词仓库保持自动配置；补全没有自己的仓库，它们随其补全的提示词或模板一起提供。
因为服务器定义了自己的资源仓库，`@McpResource` 方法将不会被服务，所以 `TicketProvider` 中没有这类方法。

注意：`spring.ai.mcp.server.modern.page-size` 保持未设置。
包装器会在内存仓库完成分页之后再过滤一页，因此如果设置了页大小，匿名调用者可能会收到一个很短或空的一页以及一个 `nextCursor`。

用 [modern-client](../modern-client/) 或 curl 驱动它：

```bash
../modern-client/scripts/tools-list.sh                                  # three tools
MCP_HEADER='X-Role: admin' ../modern-client/scripts/tools-list.sh       # five tools
MCP_HEADER='X-Role: admin' MCP_CAPABILITIES='{}' \
  ../modern-client/scripts/tools-call.sh admin_purge_resolved '{}'          # -32021
../modern-client/scripts/mcp.sh resources/list '{}'                     # nextCursor=t-2
../modern-client/scripts/mcp.sh subscriptions/listen \
  '{"notifications":{"resourcesListChanged":true,"resourceSubscriptions":["tickets://t-1"]}}'
```

属于 [modern-migration](../README.md)。用 `./mvnw spring-boot:run` 运行；服务器监听 `http://localhost:8080`。
