# 工单服务器，STATELESS

`spring.ai.mcp.server.protocol=STATELESS`：每个 POST 都是独立的，没有 `Mcp-Session-Id`。

`TicketProvider` 暴露 `list_tickets`、`open_ticket`、`resolve_ticket`，资源 `tickets://all` 和 `tickets://{id}`，提示词 `daily-summary`，以及为 `owner`（`daily-summary` 的）和 `id`（`tickets://{id}` 的）提供的补全。这里没有任何内容需要会话，因此同一个类可以在每种协议上不加修改地运行。

用 [legacy-client](../legacy-client/) 驱动它。

属于 [modern-migration](../README.md)。用 `./mvnw spring-boot:run` 运行；服务器监听 `http://localhost:8080`。
