# 工单客户端，2026-07-28，基于 Spring AI starter

[legacy-client](../legacy-client/) 迁移后的版本：Spring AI MCP 客户端 starter（`spring-ai-starter-mcp-client`），带一个使用 modern 协议的连接。
迁移只涉及一个属性：`spring.ai.mcp.client.streamable-http.connections.tickets.protocol=MODERN`。
除了连接名之外，带注解的处理器方法保持不变。
适用于本集合中的每个 modern 服务器。

连接的工具以 `ToolCallback` 的形式从 `SyncMcpToolCallbackProvider` Bean 到达，该 Bean 与为旧版连接提供服务的是同一个。
带有模型的应用通过 `ChatClient.builder(chatModel).defaultToolCallbacks(provider)` 注册它；本示例没有模型，因此自己调用这些回调。
`WireLog` 以 `[http] <method>` 行打印连接发送的每个请求，`ListenLog` 以 `[listen]` 行打印监听流报告的内容。

`MigratedTicketsClientApplication` 会先执行一遍通用流程，然后执行服务器允许的场景：

* **每个服务器** - 解析工具回调（`tickets_open_ticket`，命名为 `<connection>_<tool>`），调用其中一个，再次解析以查看服务器是否被再次询问，并使用该连接的 SDK 客户端访问 `server/discover`、一个资源、提示词以及各项补全。
* **modern-server-from-streamable** 和 **modern-server-async** - `tickets_delete_ticket`，其引导和进度会到达 `@McpElicitation` 和 `@McpProgress` 方法（`ClientHandlers` 的）；另有一个运行时工具添加，监听流会将其报告给 `@McpToolListChanged` 方法。`summarize_tickets` 需要采样，而 modern SDK 客户端不支持采样，因此用一个 `McpModernToolFilter` Bean 将其隐藏。
* **modern-server-repositories** - 分别为具有 `admin` 角色和不具有该角色的调用者解析工具回调。每次解析都会为其调用者创建一个客户端，`roleHeader` 将角色作为 `X-Role` 发送，管理员回调在角色离开线程后仍然以管理员身份调用。

示例中的类：

* **`ClientHandlers`** - 旧版客户端的类，带有其 `@McpElicitation`、`@McpProgress` 和 `@McpToolListChanged` 方法，仍然使用旧版 schema 类型。一个方法同时服务于它所指定的旧版连接和 modern 连接。引导方法新增了一个可选的 `ToolContext` 参数，它是该引导所属工具调用的上下文。`@McpSampling` 和 `@McpLogging` 方法已移除，因为 modern 协议中既没有采样也没有客户端日志。
* **`ModernClientConfiguration`** - 一个将调用者绑定到每个客户端的 `McpClientCustomizer<McpClient.SyncBuilder>`，一个添加请求头的 `McpHttpRequestCustomizer`，工具过滤器，以及一个为每个角色保留一个客户端的 `SyncModernMcpClientScope`。
* **`ListenLog`** - 一个 `SyncModernMcpChangeListener` Bean，它会监听监听流在工具回调提供者和注解方法之外报告的内容。

## 监听流

`spring.ai.mcp.client.streamable-http.connections.tickets.listen=true` 会与服务器保持一条监听流打开。
该流在应用启动时于后台打开，面向应用而不是某个调用者：请求不携带 `X-Role` 请求头。
当服务器报告工具变更时，已保留的工具列表会立即被丢弃，并以应用所见的工具调用 `@McpToolListChanged` 方法。
结束的流会以逐步增长的间隔重新打开，随后会再次调用这些方法，因为服务器在这段时间内可能已经发生变化。

## 可以尝试的事情

* **同时运行旧版服务器和 modern 服务器。** 在 8081 端口运行旧版服务器，在 8080 端口运行 modern 服务器，并用 `mixed` profile 启动客户端，该 profile 会添加一个不带 `protocol` 属性的连接。一个提供者同时返回旧版服务器的 `open_ticket` 和 modern 服务器的 `tickets_open_ticket`，而 `ClientHandlers` 的方法同时服务于两个连接。

  ```bash
  (cd ../stateless-server             && ./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081)
  (cd ../modern-server-from-stateless && ./mvnw spring-boot:run)
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=mixed
  ```

* **无法访问的 modern 服务器。** 只在旧版服务器运行的情况下启动 `mixed` profile。监听流和提供者各记录一条警告，`tickets_*` 工具被排除，并且在 `unreachable-cool-down` 时间内不会再次向该服务器请求其工具；旧版工具仍然可用。
* **本可以保留的列表。** 示例服务器发送工具列表时带有 `ttlMs`，但将其标记为 `private`，因此只有列出它们的客户端可以保留它们。使用默认的 `PER_REQUEST` scope 时，第二次解析会再次发送 `tools/list`，客户端会记录一次提示：该列表本可以保留。
* **保留的公共列表。** 用 `-Dspring-boot.run.arguments=--spring.ai.mcp.server.modern.list-cache-scope=PUBLIC` 启动服务器，提供者会在服务器的 `list-ttl` 期间为所有调用者保留该列表：第二次解析不会发送任何内容，而工具调用仍然会到达服务器。
* **每个角色一个客户端。** 设置 `spring.ai.mcp.client.streamable-http.connections.tickets.client-scope=CUSTOM`（在 `application.properties` 中）。`SyncModernMcpClientScope` Bean（`ModernClientConfiguration` 的）为每个角色保留一个客户端，每个客户端保留其角色的私有列表：第二次解析不会发送任何内容，并且针对 `modern-server-repositories` 时管理员仍然能获得管理员工具。
* **共享客户端。** 改为设置 `client-scope=SHARED`。一个客户端服务于每个请求，甚至保留私有列表，这适合以单一身份调用服务器的应用。针对 `modern-server-repositories` 时，管理员工具不再出现，因为共享客户端是在启动时构建的，没有调用者。

属于 [modern-migration](../README.md)。在某个 modern 服务器运行时用 `./mvnw spring-boot:run` 运行。
