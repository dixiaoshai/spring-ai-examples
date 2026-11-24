# 工单服务器，STREAMABLE（有状态）

`spring.ai.mcp.server.protocol=STREAMABLE`：基于 Streamable HTTP 的会话。

增加了 `InteractiveTicketProvider`，其工具通过会话回调客户端：`delete_ticket` 记录日志、报告进度，并用 `ctx.elicit(...)` 请求确认；`summarize_tickets` 用 `ctx.sample(...)` 询问客户端的模型。`AdminController` 通过 `McpSyncServer.addTool` 在运行时添加工具，该方法会向已连接的会话发送 `notifications/tools/list_changed`（`POST /admin/tools/{name}`）。

用 [legacy-client](../legacy-client/) 驱动它。与 [modern-server-from-streamable](../modern-server-from-streamable/) 对比。

属于 [modern-migration](../README.md)。用 `./mvnw spring-boot:run` 运行；服务器监听 `http://localhost:8080`。
