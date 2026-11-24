# 将 Spring AI MCP 服务器与客户端迁移到 2026-07-28（“modern”）协议

这些示例展示了同一个小的工单应用在三种 MCP 协议上的实现、把基于会话的服务器迁移到 modern 协议时有哪些变化、modern 服务器如何自定义为其工具和资源提供服务的仓库、同一个服务器用异步 API 类型编写的样子，以及迁移后的 Spring AI 客户端（同步与异步两种客户端类型）。

**版本：** Spring Boot `4.1.1` · Spring AI `2.1.0-SNAPSHOT` · MCP Java SDK `2.2.0`

重要：modern 服务器随 Spring AI 2.1.0-RC2 发布，spring-projects/spring-ai#7349 增加了 `spring.ai.mcp.server.type=ASYNC`，并把补全功能迁移到提示词和资源模板上。
[modern-client-from-legacy](modern-client-from-legacy/) 使用的 Spring AI 客户端 modern 连接，由 spring-projects/spring-ai#7385 在 MCP Java SDK `2.2.0` 的 modern 客户端之上加入，而 spring-projects/spring-ai#7392 又增加了它们的异步客户端类型，[modern-client-async](modern-client-async/) 使用的就是这一类型。
两者都包含在 Spring AI `2.1.0-SNAPSHOT` 构建中，示例从 Spring 快照仓库获取该构建。

## 模块

| 模块 | 协议 | 展示内容 |
|---|---|---|
| [stateless-server](stateless-server/) | `STATELESS` | 无会话的 Streamable HTTP 服务器上的 `@McpTool`、`@McpResource`（静态与模板）以及 `@McpPrompt` |
| [streamable-server](streamable-server/) | `STREAMABLE` | 同样是这些内容，外加使用会话的工具：客户端日志、进度、引导、采样，以及一个触发 `tools/list_changed` 的运行时工具添加 |
| [modern-server-from-stateless](modern-server-from-stateless/) | `MODERN` | 迁移后的无状态服务器：一个属性变更和两个注解属性（`ttlMs`、`cacheScope`） |
| [modern-server-from-streamable](modern-server-from-streamable/) | `MODERN` | 迁移后的 streamable 服务器：引导与采样变为多轮往返请求，日志转到服务器端，运行时变更通过工具仓库和 `subscriptions/listen` 进行 |
| [modern-server-repositories](modern-server-repositories/) | `MODERN` | 自定义仓库：一个编程式的 `List<McpSyncTool>` Bean、一个按调用者过滤的包装工具仓库，以及一个由领域模型支撑的资源仓库，它把 Spring 事件转换为 `resources/updated` 通知并为其模板提供补全；另外还有定义中的一个 `@McpIcon` 和一个参数 `title` |
| [modern-server-async](modern-server-async/) | `MODERN`、`type=ASYNC` | 用异步 API 类型在 WebFlux 上编写的 streamable 迁移版本：`Mono`、`CompletableFuture` 与普通返回值，`McpAsyncNotifier` 进度，以及通过 `InMemoryAsyncToolRepository` 进行的运行时变更 |
| [legacy-client](legacy-client/) | Streamable HTTP | 带有 `@McpElicitation`、`@McpSampling`、`@McpLogging`、`@McpProgress` 和 `@McpToolListChanged` 处理器的 Spring AI MCP 客户端；对两个旧版服务器均可用 |
| [modern-client](modern-client/) | 2026-07-28 | 一个最小化的 JSON-RPC 驱动，展示线上格式，另有 curl 脚本：`server/discover`、请求头、按请求声明的客户端能力、`input_required` 重试循环、`completion/complete`、`subscriptions/listen` |
| [modern-client-from-legacy](modern-client-from-legacy/) | 2026-07-28，可选带旧版连接 | 迁移后的旧版客户端，基于 Spring AI 客户端 starter：一个 `protocol=MODERN` 连接，其工具以工具回调的形式到达；每个调用者一个客户端；保持不变的 `@McpElicitation`、`@McpProgress` 和 `@McpToolListChanged` 方法；一个监听流；一个工具过滤器；一个可选的 scope，每个角色一个客户端；以及一个提供者背后同时接入的旧版服务器和 modern 服务器 |
| [modern-client-async](modern-client-async/) | 2026-07-28、`type=ASYNC`，可选带旧版连接 | 使用异步客户端类型的迁移后客户端：用调用者提供者 Bean 代替客户端定制器；响应式的 `@McpElicitation`、`@McpProgress` 和 `@McpToolListChanged` 方法；一个 `AsyncModernMcpChangeListener`；在一个响应式管线中使用的异步 SDK 客户端；以及在可能不允许阻塞的线程上解析工具时会发生什么 |

所有服务器都监听 `http://localhost:8080`，因此请一次只运行一个。

## 成对运行

```bash
# 1. stateless
(cd stateless-server && ./mvnw spring-boot:run)          # terminal 1
(cd legacy-client    && ./mvnw spring-boot:run)          # terminal 2

# 2. stateful streamable: watch the handler log lines for logging, progress, elicitation, sampling, list_changed
(cd streamable-server && ./mvnw spring-boot:run)
(cd legacy-client     && ./mvnw spring-boot:run)

# 3. modern, from stateless: same calls, now with ttlMs/cacheScope on lists and the resource
(cd modern-server-from-stateless && ./mvnw spring-boot:run)
(cd modern-client                && ./mvnw spring-boot:run)

# 4. modern, from streamable: watch the "input_required round" lines, the -32021 error and the listen events
(cd modern-server-from-streamable && ./mvnw spring-boot:run)
(cd modern-client                 && ./mvnw spring-boot:run)

# 5. modern, custom repositories: per-caller tool lists, paged resources, resources/updated events
(cd modern-server-repositories && ./mvnw spring-boot:run)
(cd modern-client              && ./mvnw spring-boot:run)

# 6. modern, asynchronous API type on WebFlux: the output of pair 4, plus count_open_tickets
(cd modern-server-async && ./mvnw spring-boot:run)
(cd modern-client       && ./mvnw spring-boot:run)

# 7. any modern server with the migrated Spring AI client: tool callbacks, and the scenario the server supports
(cd modern-server-repositories && ./mvnw spring-boot:run)
(cd modern-client-from-legacy  && ./mvnw spring-boot:run)

# 8. a legacy and a modern server behind one tool callback provider
(cd stateless-server             && ./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081)
(cd modern-server-from-stateless && ./mvnw spring-boot:run)
(cd modern-client-from-legacy    && ./mvnw spring-boot:run -Dspring-boot.run.profiles=mixed)

# 9. any modern server with the migrated client on the asynchronous client type
(cd modern-server-from-streamable && ./mvnw spring-boot:run)
(cd modern-client-async           && ./mvnw spring-boot:run)
```

modern 客户端会针对每个服务器执行一遍通用流程，然后执行服务器所支持的场景（根据其工具列表检测）。
无需 Java 的快速检查：`modern-client/scripts/discover.sh`、`tools-list.sh`、`tools-call.sh open_ticket '{"title":"x"}'` 和 `listen.sh`。
`MCP_HEADER='X-Role: admin'` 会添加一个请求头，`MCP_CAPABILITIES='{}'` 会改变声明的客户端能力。

## 迁移中有哪些变化

### 从无状态到 modern：只改配置

```diff
-spring.ai.mcp.server.protocol=STATELESS
-spring.ai.mcp.server.streamable-http.mcp-endpoint=/mcp
+spring.ai.mcp.server.protocol=MODERN
+spring.ai.mcp.server.modern.mcp-endpoint=/mcp
+spring.ai.mcp.server.modern.list-ttl=30s
+spring.ai.mcp.server.modern.list-cache-scope=PRIVATE
+spring.ai.mcp.server.modern.discover-ttl=5m
+spring.ai.mcp.server.modern.page-size=50
```

```diff
-@McpResource(uri = "tickets://all", mimeType = "application/json")
+@McpResource(uri = "tickets://all", mimeType = "application/json", ttlMs = 30_000, cacheScope = CacheScope.PUBLIC)
```

除此之外，`TicketProvider` 在无状态服务器、streamable 服务器以及两个迁移后的服务器中都完全相同。

### 从 streamable 到 modern：重写绑定会话的方法

| Streamable（`InteractiveTicketProvider`） | Modern（`InteractiveTicketProvider`） |
|---|---|
| `McpSyncRequestContext ctx` | `McpModernSyncRequestContext ctx` |
| 向客户端发送 `ctx.info("...")` | 在服务器端使用 `logger.info(...)`；客户端日志在 2026-07-28 修订版中已废弃且不再暴露 |
| `ctx.progress(p -> p.percentage(10))` | 声明 SDK 的 `McpSyncNotifier` 并调用 `notifier.progress(10, 100.0, "...")`；正是这个参数（且只有这个参数）使响应成为一段简短的 SSE 流 |
| `ctx.elicit(e -> e.message(...), Confirmation.class)` 会阻塞直到客户端应答 | 返回 `InputRequiredResult.builder().elicit("confirm", form).requestState(id).build()`；重试时通过 `ctx.inputResponse("confirm", ElicitResponse.class)` 读取应答，通过 `ctx.requestState()` 读取工单 id |
| `ctx.sample(s -> s.message(...))` | 返回 `InputRequiredResult.builder().createMessage("summary", request).build()`；重试时通过 `ctx.inputResponse("summary", Map.class)` 读取应答 |
| 返回 `String` | 返回 `CallToolOutcome`（一个 `CallToolResult` 或一个 `InputRequiredResult`） |
| `ctx.elicit` 与 `ctx.sample` 检查客户端在 `initialize` 时发送的能力 | `@McpTool(requiredCapabilities = McpClientCapability.ELICITATION_FORM)` 声明该工具缺少哪些能力就无法工作 |
| `McpSyncServer.addTool(SyncToolSpecification)` 通知每个会话 | `InMemorySyncToolRepository.add(McpSyncTool.of(tool, handler))` 广播到每条 `subscriptions/listen` 流 |

在线上，服务器不再向客户端发送引导和采样请求。
客户端应答 `inputRequests`（位于 `input_required` 结果中），并携带 `inputResponses` 和密封的 `requestState` 重试同一个调用；参见 `ModernMcpDriver.callTool`。
MCP Java SDK 的 modern 客户端自己运行该循环，并调用 Spring AI 客户端的 `@McpElicitation` 方法获取应答，参见[从旧版客户端迁移到新版](#legacy-client-to-modern-a-property-and-the-handlers-stay)。
也不再需要握手：客户端在每次请求的 `_meta` 中声明自己的能力，如果某个工具的 `requiredCapabilities` 未在请求中声明，则会在方法运行之前被以 JSON-RPC 错误 `-32021 Missing required client capability` 拒绝。

### 从同步到异步：API 类型

[modern-server-async](modern-server-async/) 就是 [modern-server-from-streamable](modern-server-from-streamable/)，只是在 WebFlux starter 上加了 `spring.ai.mcp.server.type=ASYNC`。
工具、资源、提示词和补全都相同；只有编程模型改变。

| 同步（`modern-server-from-streamable`） | 异步（`modern-server-async`） |
|---|---|
| `spring-ai-starter-mcp-server-webmvc`、`type=SYNC` | `spring-ai-starter-mcp-server-webflux`、`type=ASYNC` |
| 返回 `String`、`CallToolOutcome` | 返回 `Mono<String>`、`Mono<CallToolOutcome>` 或 `CompletableFuture`；普通返回值仍然可用，并运行在 `boundedElastic` 上 |
| `McpModernSyncRequestContext ctx` | `McpModernRequestContext ctx`，两种 API 类型使用相同的访问器 |
| `McpSyncNotifier notifier`、`notifier.progress(...)` | `McpAsyncNotifier notifier`，`notifier.progress(...)` 返回一个 `Mono<Void>`，并被组合进结果 |
| `InMemorySyncToolRepository.add(McpSyncTool.of(...))` | `InMemoryAsyncToolRepository.add(McpAsyncTool.of(...))`，处理器返回一个 `Mono` |

返回 `Flux` 的方法会在启动时被拒绝，因为一个 MCP 请求只有一个结果；请改为返回一个列表的 `Mono`。
在 `type=SYNC` 下则相反：异步返回类型的方法（包括 `CompletableFuture`）会被跳过并给出警告。

<a id="legacy-client-to-modern-a-property-and-the-handlers-stay"></a>

### 从旧版客户端迁移到新版：属性不变，处理器保持不变

[modern-client-from-legacy](modern-client-from-legacy/) 是迁移后的 [legacy-client](legacy-client/)。
协议是连接的一个属性，因此一个应用可以在 modern 连接旁保留旧版连接，而一个 `SyncMcpToolCallbackProvider` Bean 会同时返回两者的工具。

```diff
-spring.ai.mcp.client.streamable-http.connections.server1.url=http://localhost:8080
+spring.ai.mcp.client.streamable-http.connections.tickets.url=http://localhost:8080
+spring.ai.mcp.client.streamable-http.connections.tickets.protocol=MODERN
+spring.ai.mcp.client.streamable-http.connections.tickets.listen=true
```

连接被重命名，因为它的名字会成为工具名的前缀。
带注解的处理器方法保留其注解和旧版 schema 类型：一个方法同时服务于它所指定的旧版连接和 modern 连接。

```diff
-@McpElicitation(clients = "server1")
-public StructuredElicitResult<Confirmation> confirm(McpSchema.ElicitRequest request) {
+@McpElicitation(clients = { "tickets", "legacy" })
+public StructuredElicitResult<Confirmation> confirm(McpSchema.ElicitRequest request, ToolContext toolContext) {
```

| 旧版连接（`legacy-client`） | Modern 连接（`modern-client-from-legacy`） |
|---|---|
| 客户端在启动时运行 `initialize` | 启动时不发送任何内容，除了 `listen=true` 连接的监听流；第一个请求在首次解析工具时才发出 |
| 工具来自 `SyncMcpToolCallbackProvider` Bean，使用各自的名称 | 工具来自同一个 Bean，名称形如 `<connection>_<tool>`，例如 `tickets_open_ticket` |
| 为资源、提示词和补全提供 `List<McpSyncClient>` Bean | `List<SyncModernMcpConnection>`；`connection.client()` 返回当前请求对应的 SDK modern `McpSyncClient` |
| 整个应用一个客户端，一个会话 | `client-scope=PER_REQUEST`（默认值）在每次解析工具时创建一个客户端，因此每个请求都携带自己的调用者；`client-scope=SHARED` 创建一个客户端；`client-scope=CUSTOM` 询问一个 `SyncModernMcpClientScope` Bean，例如为每个用户提供一个客户端 |
| `@McpElicitation` | 同一个方法；SDK 客户端在服务器请求输入时调用它，并重试工具调用。它可以接收该工具调用的 `ToolContext`，以便知道该问谁 |
| `@McpProgress` | 同一个方法 |
| `@McpToolListChanged` | 同一个方法，用于 `listen=true` 的连接；会先列出工具，因为 modern 服务器在变更时不发送列表 |
| `@McpSampling` | modern SDK 客户端不支持；服务器会以 `-32021` 拒绝需要它的工具，因此用一个 `McpModernToolFilter` Bean 将其隐藏 |
| `@McpLogging` | 已移除，客户端日志亦然 |
| `McpToolFilter`、`McpToolNamePrefixGenerator` | `McpModernToolFilter`、`McpModernToolNamePrefixGenerator`；一个同时拥有 `McpToolFilter` Bean 和 modern 连接的应用必须也定义一个 `McpModernToolFilter` Bean |
| 来自 `McpSyncHttpClientRequestCustomizer` 的请求头 | 来自 `McpHttpRequestCustomizer` Bean 的请求头，该 Bean 从请求的 `McpTransportContext` 中读取调用者 |

两个客户端 starter 都使用 SDK 的 JDK `HttpClient` 传输为 modern 连接提供服务。

### 从同步到异步：客户端类型

[modern-client-async](modern-client-async/) 是使用 `spring.ai.mcp.client.type=ASYNC` 的迁移后客户端。
连接相关的属性保持不变；变化的是调用者的来源以及处理器方法返回什么。

```diff
+spring.ai.mcp.client.type=ASYNC
```

```diff
-@Bean
-McpClientCustomizer<McpClient.SyncBuilder> modernClientCustomizer() {
-    return (connectionName, builder) -> {
-        McpTransportContext caller = callerOf(CurrentRole.get());
-        builder.transportContextProvider(() -> caller);
-    };
-}
+@Bean
+AsyncModernMcpTransportContextProvider callerProvider() {
+    return connection -> callerOf(CurrentRole.get());
+}
```

```diff
 @McpElicitation(clients = { "tickets", "legacy" })
-public StructuredElicitResult<Confirmation> confirm(McpSchema.ElicitRequest request, ToolContext toolContext) {
+public Mono<StructuredElicitResult<Confirmation>> confirm(McpSchema.ElicitRequest request, ToolContext toolContext) {
```

工具来自 `AsyncMcpToolCallbackProvider` Bean，`SyncModernMcpClientScope` 和 `SyncModernMcpChangeListener` 变为 `AsyncModernMcpClientScope` 和 `AsyncModernMcpChangeListener`，而 `connection.client()` 返回 SDK 的异步 `McpAsyncClient`。
工具回调仍然是阻塞式的，因此响应式应用会在可能阻塞的线程上发起聊天调用，例如使用 `subscribeOn(Schedulers.boundedElastic())`。

### 编程式规格与运行时变更

modern 服务器没有 `McpSyncServer`，也没有 `McpServerFeatures.Sync*Specification` 记录。
工具、提示词和资源由仓库提供服务，而 Spring AI 集成会根据列表 Bean 构建默认的内存仓库：

| 基于会话的服务器 | Modern 服务器 |
|---|---|
| `@Bean List<McpServerFeatures.SyncToolSpecification>` | `@Bean List<McpSyncTool>`；SDK 类型由一个 `Tool` 定义、可选的必需客户端能力以及一个处理器组成 |
| `@Bean List<SyncPromptSpecification>` | `@Bean List<McpSyncPrompt>` |
| `@Bean List<SyncResourceSpecification>` / `SyncResourceTemplateSpecification` | `@Bean List<SyncResourceRegistration>` / `List<SyncResourceTemplateRegistration>`（`of(definition, handler)`） |
| `@Bean List<SyncCompletionSpecification>` | 没有自己的列表：`withCompletionHandler(...)` 调用在所要补全的 `McpSyncPrompt` 或 `SyncResourceTemplateRegistration` 上 |
| `McpSyncServer.addTool/removeTool/notifyToolsListChanged` | `InMemorySyncToolRepository.add/remove`，通过 `ObjectProvider` 解析 |
| `McpSyncServer.notifyResourcesUpdated` | `InMemorySyncResourceRepository.resourceUpdated(uri)`，或在自定义仓库中调用 `McpChangeBroadcaster.broadcast(new ServerChange.ResourceUpdated(uri))` |
| 每个调用者不同的工具列表 | 不可用；在 modern 服务器上，用一个包装内存仓库的仓库 Bean 实现 |

注解扫描器会生成同样的列表 Bean，因此带注解的原语和编程式原语最终会进入同一个仓库。
带注解的 `@McpComplete` 方法会附加到它所指定的 `@McpPrompt` 或 `@McpResource` 模板上：`TicketProvider.completeOwner` 为 `owner` 提供补全（`daily-summary` 提示词的），`completeTicketId` 为 `id` 提供补全（`tickets://{id}` 的）；如果没有带注解的提示词或模板具有该名称或 URI，应用将无法启动。
在 `type=ASYNC` 下，同样的约定使用异步对应类型：`List<McpAsyncTool>`、`List<McpAsyncPrompt>`、`List<AsyncResourceRegistration>` 和 `List<AsyncResourceTemplateRegistration>`。

## 自定义仓库

[modern-server-repositories](modern-server-repositories/) 展示了替换仓库的两种方式。
只要定义了某一种 SDK 仓库类型（`McpSyncToolRepository`、`McpSyncResourceRepository`、`McpSyncPromptRepository`，或对应的 `McpAsync*Repository` 异步类型）的任意 Bean，该原语自动配置的内存仓库就会退出。

* **委托** - `RoleFilteringRepositoryConfiguration` 从 `List<McpSyncTool>` Bean 构建内存工具仓库并将其包装。没有 `X-Role: admin` 的调用者看不到 `admin_*` 工具（在 `tools/list` 中），调用其中一个会返回 `-32602 Unknown tool`。当目录保持为固定列表、只有可见性取决于调用者时，请使用此方式。
* **实现** - `TicketResourceRepository` 直接实现 `McpSyncResourceRepository`。每张工单都是一个资源，每次请求都从领域服务列出，并按工单 id 分页；不注册、不添加、也不移除任何内容。工单变更以 Spring 事件到达，并以 `resources/list_changed` 和 `resources/updated` 通知发出。它还为它的 `id` 提供补全（`tickets://{id}` 模板的）：`findTemplate` 返回带有补全处理器的模板，`supportsCompletion()` 用于选择加入；两者都是默认方法，因此未实现它们的仓库不会提供补全。当目录位于数据库、远程服务或领域模型中时，请使用此方式。

## 线上格式（满足好奇心）

每个 modern 请求都是一个 POST，带有 `Mcp-Method` 和 `MCP-Protocol-Version` 请求头，还带有 `Mcp-Name`（用于 `tools/call`、`prompts/get` 和 `resources/read`），请求体中还有 `_meta.io.modelcontextprotocol/protocolVersion` 和 `clientCapabilities`。
没有 `initialize`：`server/discover` 是一个可缓存的请求。
与请求体不一致的请求头会被以 JSON-RPC 错误 `-32020` 拒绝。

## 注意事项

- STATELESS 服务器会跳过声明了 `McpSyncRequestContext` 的方法（提供者会记录一条警告），因此 `stateless-server` 没有 `InteractiveTicketProvider`：旧版客户端对它打印三个工具，而对 streamable 服务器打印五个。
- modern 服务器上的 `tools/list` 会带有 `ttlMs` 和 `cacheScope`；`resources/read tickets://all` 会带有注解中的值。
- 两个管理控制器都通过 `ObjectProvider` 解析服务器或仓库，并用 `SingletonSupplier` 包装，因此查找只会在第一个请求时发生一次；包装仓库是一个 `@Bean` 方法，通过 `ObjectProvider` 读取列表 Bean。直接把其中任何一个注入到组件中，可能会在注解 Bean 被扫描之前就创建它，服务器启动时便会没有任何带注解的工具。
- 响应形态由方法自己选择：`delete_ticket` 声明 `McpSyncNotifier` 并通过 `text/event-stream` 应答，其进度通知在 `input_required` 结果之前发出；`summarize_tickets` 只声明上下文，并用普通 JSON 应答。驱动会把最后一个 SSE 帧作为响应读取。
- 必需的客户端能力在处理器运行之前检查，通过注解上的 `requiredCapabilities` 或编程式工具上的 `McpSyncTool.of(tool, requiredCapabilities, handler)` 声明。没有声明这些能力却请求未声明输入的方法仍会被以 `-32021` 拒绝，但只会在其运行之后，并且服务器会记录一条推荐使用该属性的警告。
- 过滤后的工具列表取决于调用者，因此包装仓库会以 `cacheScope: private` 返回它。
- `completion/complete` 不带 `Mcp-Name` 请求头。服务器只补全提示词或模板声明的参数，对其他任何参数都返回 `-32602`，而像 `tickets://all` 这样的静态资源没有任何可补全的内容。
- 在 `modern-server-async` 上，WebFlux 传输永远不会阻塞事件循环：`Mono` 方法在订阅它们的线程上运行，而 `resolve_ticket`（返回普通 `String`）运行在 `boundedElastic` 上。
- 只有当服务器允许每个调用者都保留工具列表时（`ttlMs` 大于零且 `cacheScope: public`），Spring AI 客户端才会跨请求保留该列表。示例服务器发送的 `ttlMs` 带有 `private`，因此 `modern-client-from-legacy` 每次解析工具时都会列出工具，客户端会记录一次提示：该列表本可以保留。可以在工具对所有调用者都相同的服务器上设置 `spring.ai.mcp.server.modern.list-cache-scope=PUBLIC`，或保留一个生命周期长于请求的客户端：单身份的应使用 `client-scope=SHARED`，每个用户一个客户端时使用 `client-scope=CUSTOM`。
- 带有 `listen=true` 的连接会保持一条监听流打开，这是为应用而不是为某个调用者打开的。当服务器报告工具变更时，已保留的列表会立即被丢弃，并调用 `@McpToolListChanged` 方法。在流结束之后重新建立时也会再次调用这些方法，因为服务器在这段时间内可能已经发生变化。
- modern 连接的工具回调会绑定到其解析时所使用的客户端。`modern-client-from-legacy` 在构建该客户端时读取调用者，因此之后或在其他线程上发起的调用仍会携带调用者的 `X-Role` 请求头。`modern-client-async` 在解析工具时读取调用者，回调会保留它。
- 使用异步客户端类型时，在可能不允许阻塞的线程上解析工具会立即失败，并在消息中指出连接和线程，且不会发送任何内容；`modern-client-async` 在 `Schedulers.parallel()` 上展示了这一点。
- 无法访问的 modern 服务器不会使客户端失败：其工具会被排除并记录一条警告，并且在 `spring.ai.mcp.client.modern.unreachable-cool-down`（默认为 30 秒）期间不会再次询问该服务器。
