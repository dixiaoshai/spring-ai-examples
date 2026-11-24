# 工单客户端，2026-07-28，基于 Spring AI starter，异步

[modern-client-from-legacy](../modern-client-from-legacy/) 采用异步客户端类型，即 `spring.ai.mcp.client.type=ASYNC`。
连接、监听流和工具过滤器都以相同方式配置。
变化的是调用者如何到达服务器，以及处理器方法返回 `Mono`。
适用于本集合中的每个 modern 服务器。

连接的工具以 `ToolCallback` 的形式从 `AsyncMcpToolCallbackProvider` Bean 到达，该 Bean 与为旧版异步客户端提供服务的是同一个。
带有模型的应用通过 `ChatClient.builder(chatModel).defaultToolCallbacks(provider)` 注册它；本示例没有模型，因此自己调用这些回调。
`WireLog` 以 `[http] <method>` 行打印连接发送的每个请求，`ListenLog` 以 `[listen]` 行打印监听流报告的内容。

`AsyncTicketsClientApplication` 会先执行一遍通用流程，然后执行服务器允许的场景：

* **每个服务器** - 解析工具回调（`tickets_open_ticket`，命名为 `<connection>_<tool>`），调用其中一个，然后再次解析以查看服务器是否被再次询问。连接的异步 SDK 客户端随后以一条响应式管线同时发送 `server/discover`、一次资源读取、提示词以及各项补全。
* **每个服务器** - 在可能阻塞的线程 `Schedulers.parallel()` 上解析工具回调，这会立即失败并给出说明该如何做的消息；然后在 `Schedulers.boundedElastic()` 上解析，可以正常工作。
* **modern-server-from-streamable** 和 **modern-server-async** - `tickets_delete_ticket`，其引导和进度会到达响应式的 `@McpElicitation` 和 `@McpProgress` 方法（`ClientHandlers` 的）；另有一个运行时工具添加，监听流会将其报告给响应式的 `@McpToolListChanged` 方法。
* **modern-server-repositories** - 分别为具有 `admin` 角色和不具有该角色的调用者解析工具回调。调用者提供者 Bean 在解析工具时读取角色，`roleHeader` 将其作为 `X-Role` 发送，管理员回调在角色离开线程后仍然以管理员身份调用。

示例中的类：

* **`ClientHandlers`** - 旧版客户端的各个方法，仍然使用旧版 schema 类型，但每个方法都返回 `Mono`。使用异步客户端类型时只会调用响应式方法，而一个指定了连接的同步方法会在启动时被报告为警告。一个连接只能有一个此类型的 `@McpElicitation` 方法。
* **`ModernClientConfiguration`** - 一个 `AsyncModernMcpTransportContextProvider`，为每个请求提供调用者；一个添加请求头的 `McpHttpRequestCustomizer`；工具过滤器；以及一个为每个角色保留一个客户端的 `AsyncModernMcpClientScope`。
* **`ListenLog`** - 一个 `AsyncModernMcpChangeListener` Bean，它会监听监听流在工具回调提供者和注解方法之外报告的内容。它的方法返回 `Mono`，并在完成时通知下一个监听器。

## 与同步客户端相比有哪些变化

| 同步客户端类型 | 异步客户端类型 |
|---|---|
| `SyncMcpToolCallbackProvider` | `AsyncMcpToolCallbackProvider` |
| 将调用者绑定到每个客户端的 `McpClientCustomizer<McpClient.SyncBuilder>` | 在解析工具时被询问的 `AsyncModernMcpTransportContextProvider` Bean |
| `SyncModernMcpClientScope` | `AsyncModernMcpClientScope` |
| `SyncModernMcpChangeListener` | `AsyncModernMcpChangeListener` |
| 处理器方法返回其结果或 `void` | 处理器方法返回 `Mono<...>` 或 `Mono<Void>` |

`McpModernToolFilter`、`McpModernToolNamePrefixGenerator` 和 `McpHttpRequestCustomizer` Bean 对两种类型都适用。

## 请求的调用者

异步客户端不持有调用者，因此由 `AsyncModernMcpTransportContextProvider` Bean 提供。
每次解析工具时都会在解析它们的线程上询问它，而 `ChatClient` 会在每个请求时询问一次。
工具列表会按该调用者列出，工具回调会保留它，因此之后运行的调用，或在流式响应线程上运行的调用，仍然以该调用者的身份发出。

该 Bean 是调用者的唯一来源。
聊天调用 Reactor 上下文中的 `McpTransportContext` 不会被使用，因为它可能属于其他人，例如同一应用的某个 MCP 服务器正在服务的请求。

## 阻塞调用

无论哪种客户端类型，工具回调约定都是阻塞式的：解析工具会从服务器列出工具，而工具调用会等待结果。
因此，响应式应用必须在可能阻塞的线程上发起其聊天调用：

```java
Flux<String> answer = Flux.defer(() -> chatClient.prompt(question).stream().content())
    .subscribeOn(Schedulers.boundedElastic());
```

在非阻塞线程上（例如 WebFlux 的事件循环），解析工具会立即失败并抛出 `IllegalStateException`，且不会发送任何内容。

## 监听流

`spring.ai.mcp.client.streamable-http.connections.tickets.listen=true` 会与服务器保持一条监听流打开，与同步示例中一样。
该流在应用启动时于后台打开，面向应用而不是某个调用者：不会询问调用者提供者 Bean，请求也不携带 `X-Role` 请求头。
当服务器报告工具变更时，已保留的工具列表会立即被丢弃，并以应用所见的工具调用响应式的 `@McpToolListChanged` 方法。
一个连接的报告会按到达顺序处理，因此某个监听器或方法的 `Mono` 若永不完成，会阻塞后面的报告：请为可能挂起的调用设置超时。

## 可以尝试的事情

* **同时运行旧版服务器和 modern 服务器。** 在 8081 端口运行旧版服务器，在 8080 端口运行 modern 服务器，并用 `mixed` profile 启动客户端，该 profile 会添加一个不带 `protocol` 属性的连接。使用 `type=ASYNC` 时，旧版连接获得旧版异步客户端，一个提供者同时返回旧版服务器的 `open_ticket` 和 modern 服务器的 `tickets_open_ticket`，而 `ClientHandlers` 的方法同时服务于两个连接。

  ```bash
  (cd ../stateless-server             && ./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081)
  (cd ../modern-server-from-stateless && ./mvnw spring-boot:run)
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=mixed
  ```

* **无法访问的 modern 服务器。** 只在旧版服务器运行的情况下启动 `mixed` profile。监听流和提供者各记录一条警告，`tickets_*` 工具被排除，并且在 `unreachable-cool-down` 时间内不会再次向该服务器请求其工具；旧版工具仍然可用。
* **每个角色一个客户端。** 设置 `spring.ai.mcp.client.streamable-http.connections.tickets.client-scope=CUSTOM`（在 `application.properties` 中）。`AsyncModernMcpClientScope` Bean 为每个角色保留一个客户端，每个客户端保留其角色的私有列表：第二次解析不会发送任何内容，并且针对 `modern-server-repositories` 时管理员仍然能获得管理员工具。
* **共享客户端。** 改为设置 `client-scope=SHARED`。启动时会记录一条警告，因为共享客户端会为每个调用者保留私有工具列表，无论调用者提供者 Bean 返回什么。针对 `modern-server-repositories` 时，管理员工具不再出现：从第一个请求（没有角色）保留的列表也会为管理员使用。

属于 [modern-migration](../README.md)。在某个 modern 服务器运行时用 `./mvnw spring-boot:run` 运行。
