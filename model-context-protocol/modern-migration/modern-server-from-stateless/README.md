# 工单服务器，MODERN（从 STATELESS 迁移）

`spring.ai.mcp.server.protocol=MODERN`：2026-07-28 协议，单个 POST 端点，用 `server/discover` 代替 `initialize`。

Java 代码就是 [stateless-server](../stateless-server/) 的代码，另加 `ttlMs` 和 `cacheScope`（用于 `tickets://all` 资源）。属性中添加了列表与发现的缓存提示以及分页大小。

用 [modern-client](../modern-client/) 或 `modern-client/scripts` 中的 curl 脚本驱动它。

属于 [modern-migration](../README.md)。用 `./mvnw spring-boot:run` 运行；服务器监听 `http://localhost:8080`。
