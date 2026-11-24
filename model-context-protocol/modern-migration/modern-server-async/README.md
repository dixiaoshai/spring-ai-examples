# 工单服务器，MODERN，使用异步 API 类型

在 WebFlux starter 上使用 `spring.ai.mcp.server.protocol=MODERN` 和 `spring.ai.mcp.server.type=ASYNC`：[modern-server-from-streamable](../modern-server-from-streamable/) 用异步编程模型编写。
工具、资源、提示词和补全，以及客户端看到的内容都相同；只有方法签名发生变化。

* **返回类型** - `TicketProvider` 在大多数方法中返回 `Mono`，以及一个 `CompletableFuture`（来自 `count_open_tickets`）；两者都是单值，通过 Spring 的 `ReactiveAdapterRegistry` 解析。`resolve_ticket` 保留普通 `String`，服务器会在 Reactor 的 `boundedElastic` 调度器上运行它，因此它可以阻塞。`Flux` 会在启动时被拒绝，因为一个 MCP 请求只有一个结果。
* **多轮往返与进度** - `InteractiveTicketProvider` 来自 streamable 迁移版本，使用 `McpModernRequestContext`（两种 API 类型共用的上下文）和 SDK 的 `McpAsyncNotifier`，其 `progress(...)` 返回一个 `Mono<Void>`，`delete_ticket` 会将它组合进结果。声明该通知器仍然会使响应成为一段简短的 `text/event-stream`。
* **补全** - `TicketProvider.completeOwner` 和 `completeTicketId` 返回 `Mono<List<String>>`，并附加到它们所指名的 `daily-summary` 提示词和 `tickets://{id}` 模板上。
* **运行时变更** - `AdminController` 添加一个 `McpAsyncTool`，通过 `InMemoryAsyncToolRepository.add`（`POST /admin/tools/{name}`），它会向 `notifications/tools/list_changed` 广播到 `subscriptions/listen` 流。

在 WebFlux 上，请求的服务过程不会阻塞事件循环；在 WebMvc starter 上同样的代码也能工作，传输会在容器线程上等待结果。

用 [modern-client](../modern-client/) 驱动它。与 [modern-server-from-streamable](../modern-server-from-streamable/) 对比。

属于 [modern-migration](../README.md)。用 `./mvnw spring-boot:run` 运行；服务器监听 `http://localhost:8080`。
