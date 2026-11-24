# 使用 jinfer 的 Tool Search Tool 自动配置

在完全本地化的技术栈上演示 Spring AI 的 [Tool Search Tool](https://docs.spring.io/spring-ai/reference/api/tools.html)
顾问。**聊天模型**和**嵌入模型**都通过 [Qxotic jinfer](https://qxotic.ai) 在应用的 JVM 内运行。无需 API 密钥，也无需外部服务。

应用注册了 26 个工具：3 个 `MyTools` 和 23 个 `DummyTools`。Spring AI 的
`ToolSearchToolCallingAdvisor` 只向模型展示 `toolSearchTool`。当模型执行搜索时，顾问会
为下一轮添加匹配的工具。工具索引是一个 `SimpleVectorStore`，由 jinfer 的
`EmbeddingModel` 计算其嵌入。

```
USER   TOOLS: ["toolSearchTool"]
ASSIST TOOL-CALL: toolSearchTool ({"query":"get current time and date"})
USER   TOOLS: ["currentTime","weather","toolSearchTool"]
ASSIST TOOL-CALL: currentTime ({"location":"Landsmeer, NL"})
ASSIST TOOL-CALL: toolSearchTool ({"query":"clothing shops in Landsmeer, NL"})
USER   TOOLS: ["toolSearchTool","clothing"]
ASSIST TOOL-CALL: clothing ({"location":"Landsmeer, NL","openAtTime":"2026-10-19T16:06:43"})
ASSIST Based on the current time in Landsmeer, NL, here are the clothing shops that are open: Foo, Bar, Baz ...
```

## 运行

要求：JDK 25 和 Maven 3.9+。示例使用 Spring Boot 4.1.1 和 Spring AI 2.1.0-SNAPSHOT。

```bash
sdk use java 25.0.3-graal
mvn spring-boot:run
```

首次启动时会下载两个模型，并缓存到 `~/Library/Caches/jinfer`（Linux 上为 `~/.cache/jinfer`）：

| 角色 | 模型 | 大小 |
|------|-------|------|
| 聊天 | `unsloth/gemma-4-E2B-it-GGUF` (Q8_0) | 4.8 GB |
| 嵌入 | `LiquidAI/LFM2.5-Embedding-350M-GGUF:Q8_0` | 362 MB |

无需修改文件即可覆盖任一模型：

```bash
JINFER_CHAT_MODEL=LiquidAI/LFM2.5-8B-A1B-GGUF:Q8_0 \
JINFER_EMBEDDING_MODEL=Qwen/Qwen3-Embedding-0.6B-GGUF:Q8_0 mvn spring-boot:run
```

模型的选择很重要。工具搜索需要一个愿意调用从未在提示词中描述过的工具的模型。`LFM2.5-1.2B-Instruct` 回答 "I don't have access to real-time data"，并且从未调用 `toolSearchTool`。`gemma-4-E2B-it` 和 `LFM2.5-8B-A1B`（MoE，约 10 亿激活参数，8.5 GB）都能完成完整的闭环。贪心解码（`temperature=0.0`）使多轮工具循环具有可复现性。

使用 `spring.ai.chat.client.tool-search-advisor.tool-index-type` 切换索引类型：

- `vector`：使用 jinfer 嵌入的语义搜索（默认）
- `lucene`：关键词搜索
- `regex`：模式匹配
