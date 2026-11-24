# 结合 jinfer 使用 Spring AI（本地 JVM 内模型）

这些示例通过 [Qxotic jinfer](https://qxotic.ai)——一个面向 GGUF 模型的 Spring AI 提供方——在**应用自身的 JVM 内部**运行开放模型。无需 Ollama、无需 Docker，也无需 Python 和 API 密钥。模型在首次使用时从 Hugging Face 下载并缓存。

应用代码就是普通的 Spring AI。jinfer 仅作为提供方出现在 POM 和 `application.properties` 中。

| 示例 | 展示内容 |
|---------|---------------|
| [jinfer-spring-ai-demo](jinfer-spring-ai-demo) | 通过 REST 提供聊天、流式、工具调用、结构化输出和会话记忆 |
| [jinfer-tool-search-demo](jinfer-tool-search-demo) | 配合本地聊天模型和本地嵌入模型使用的 Tool Search Tool 顾问 |
| [jinfer-transcription-demo](jinfer-transcription-demo) | 使用 NVIDIA Parakeet 的语音转文字，并提供用于录音或上传音频的浏览器页面 |

## 环境要求

- **JDK 25**：jinfer 使用 FFM 和 Vector API。
- Maven 3.9+
- Spring Boot 4.1.1、Spring AI 2.1.0-SNAPSHOT 和 jinfer 0.3.1，均由 POM 解析。

每个运行 jinfer 的 JVM 都需要以下标志：

```
--add-modules jdk.incubator.vector --enable-native-access=ALL-UNNAMED
```

各 POM 会把它们传给 `mvn spring-boot:run`。若直接使用 `java -jar` 以及 IDE 运行配置，则需要你自行传入。在 VS Code 中，请将它们加入 `.vscode/settings.json`，这样每次 Run/Debug 启动都会带上：

```json
"java.debug.settings.vmArgs": "--add-modules jdk.incubator.vector --enable-native-access=ALL-UNNAMED"
```

缺少这些标志时，jinfer 的模型 Bean 将无法启动。

## 构建

```bash
sdk use java 25.0.3-graal   # or any JDK 25
cd jinfer
./mvnw clean package
```

仓库根构建仅在 JDK 25 或更高版本上运行时（`java25` profile）才会包含此文件夹。

## 模型缓存

形如 `LiquidAI/LFM2.5-1.2B-Instruct-GGUF:Q8_0` 的模型引用只会下载一次，并缓存在 macOS 的 `~/Library/Caches/jinfer` 或 Linux 的 `~/.cache/jinfer` 中。设置 `JINFER_MODELS` 可改用其他目录，或设置 `JINFER_OFFLINE=1` 仅使用缓存的模型。
