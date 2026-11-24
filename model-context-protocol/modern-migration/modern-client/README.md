# 工单客户端，2026-07-28

`ModernMcpDriver` 直接使用 JDK 的 `HttpClient` 展示线上格式，而不使用 MCP Java SDK 的 modern 客户端。
关于 Spring AI starter 上的该客户端，参见 [modern-client-from-legacy](../modern-client-from-legacy/)。
它涵盖：带有协议版本和客户端能力的 `_meta`，`Mcp-Method`、`MCP-Protocol-Version` 和 `Mcp-Name` 请求头，JSON 与 SSE 响应，`input_required` 重试循环（`callTool`），`completion/complete`（`complete`），以及一个可关闭的 `subscriptions/listen` 读取器。
`withHeader` 和 `withCapabilities` 派生出发送额外请求头或声明其他能力的驱动。

`ModernTicketsClientApplication` 会对任何 modern 工单服务器执行一遍通用流程（包括为 `daily-summary` 补全 owner，以及当服务器具有 `tickets://{id}` 模板时补全工单 id），然后执行服务器工具所允许的场景：

* **modern-server-async** - `count_open_tickets`，即服务器上的一个 `CompletableFuture`，随后是下面的交互式场景。
* **modern-server-from-streamable** 和 **modern-server-async** - 带引导和进度的 `delete_ticket`、不带客户端能力的同一调用（`-32021`）、带采样的 `summarize_tickets` 以及来自不支持采样的客户端的调用（`-32021`），以及在 `subscriptions/listen` 上看到的运行时工具添加。
* **modern-server-repositories** - 定义中的图标和参数标题、带与不带 `X-Role: admin` 的工具列表、被隐藏的工具以未知工具应答、带 `nextCursor` 的资源分页、`resources/updated` 和 `resources/list_changed` 事件，以及带与不带能力的编程式 `admin_purge_resolved`。

`scripts/` 中存放等价的 curl 脚本：`discover.sh`、`tools-list.sh`、`tools-call.sh <tool> '<json>'`、`listen.sh` 以及通用的 `mcp.sh <method> '<params>' [name]`。
用 `MCP_ENDPOINT=http://host:port/mcp` 将它们指向别处，用 `MCP_HEADER='X-Role: admin'` 添加请求头，用 `MCP_CAPABILITIES='{}'` 改变声明的能力。

属于 [modern-migration](../README.md)。在某个 modern 服务器运行时用 `./mvnw spring-boot:run` 运行。
