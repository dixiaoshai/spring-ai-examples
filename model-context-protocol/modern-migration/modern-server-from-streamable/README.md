# 工单服务器，MODERN（从 STREAMABLE 迁移）

`spring.ai.mcp.server.protocol=MODERN`，并把绑定会话的工具改写为多轮往返请求。

`InteractiveTicketProvider.deleteTicket` 返回一个 `InputRequiredResult`，携带一个引导表单，以及作为 `requestState` 的工单 id；客户端携带 `inputResponses` 重试，第二次调用通过 `ctx.inputResponse("confirm", ElicitResponse.class)` 读取应答，并通过 `ctx.requestState()` 读取工单 id（服务器已校验过）；封存覆盖工具和调用者，但不覆盖参数，因此重试时的 `id` 参数不被信任。
`summarizeTickets` 用 `createMessage` 做同样的事。
`deleteTicket` 还声明 SDK 的 `McpSyncNotifier` 并通过它报告进度，这使其响应成为一段简短的 `text/event-stream`；`summarizeTickets` 只声明上下文，并用普通 JSON 应答。
两个工具都声明了它们缺少就无法工作的能力：`requiredCapabilities = McpClientCapability.ELICITATION_FORM` 和 `SAMPLING`。
如果客户端没有在请求 `_meta` 中声明该能力，就会在方法运行之前收到 JSON-RPC 错误 `-32021`，即便是 `deleteTicket` 也以普通 JSON 响应返回。
客户端日志没有对应项，已由服务器端日志取代。

`AdminController` 在运行时添加一个 `McpSyncTool`，通过 `InMemorySyncToolRepository.add`，它会向 `notifications/tools/list_changed` 广播到 `subscriptions/listen` 流。
它通过 `ObjectProvider` 解析仓库，并用 `SingletonSupplier` 包装：查找被推迟到注解工具扫描之后，并且只发生一次。

用 [modern-client](../modern-client/) 驱动它。与 [streamable-server](../streamable-server/) 对比。

属于 [modern-migration](../README.md)。用 `./mvnw spring-boot:run` 运行；服务器监听 `http://localhost:8080`。
