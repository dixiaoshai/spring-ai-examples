# jinfer Spring AI 演示

一个 Spring Boot + Spring AI 应用，通过 [Qxotic jinfer](https://qxotic.ai) 在**自身的 JVM 内部**运行本地 LLM。无需 Ollama、Docker、Python，也不需要 API 密钥。模型是一个 GGUF 文件，首次启动时从 Hugging Face 下载并缓存。

应用代码就是普通的 Spring AI（`ChatClient`、`@Tool`、`.entity(...)`、会话记忆）。jinfer 仅作为提供方出现在 POM 和 `application.properties` 中。

## 端点

| 端点 | 展示内容 |
|----------|---------------|
| `GET /chat?message=...` | 阻塞式聊天调用 |
| `GET /chat/stream?message=...` | token 流式传输（SSE） |
| `GET /chat/memory?message=...&conversationId=...` | 使用[会话记忆](https://docs.spring.io/spring-ai/reference/api/session.html)进行多轮聊天（`SessionAdvisors`） |
| `GET /weather?city=...` | 工具调用（`@Tool` 位于 `WeatherTools` 中） |
| `POST /incident`（文本请求体） | 结构化输出到 Java record，采样受文法约束 |

## 运行

要求：**JDK 25**（jinfer 使用 FFM 和 Vector API）以及 Maven 3.9+。

本示例使用 Spring Boot 4.1.1 和 Spring AI 2.1.0-SNAPSHOT。

```bash
sdk use java 25.0.3-graal          # or any JDK 25; check with: java -version
mvn spring-boot:run
```

首次启动会将 `LiquidAI/LFM2.5-1.2B-Instruct-GGUF:Q8_0`（约 1.2 GB）下载到 macOS 的 `~/Library/Caches/jinfer` 或 Linux 的 `~/.cache/jinfer` 中。之后的启动只需几秒。

```bash
curl "localhost:8080/chat?message=What%20is%20the%20capital%20of%20Bulgaria%3F"
curl -N "localhost:8080/chat/stream?message=Write%20a%20haiku%20about%20rivers"
curl "localhost:8080/weather?city=Sofia"
curl -X POST -H 'Content-Type: text/plain' \
     --data 'Checkout has returned HTTP 503 for ten minutes. Wake someone up!' localhost:8080/incident
curl "localhost:8080/chat/memory?conversationId=c1&message=Hi,%20my%20name%20is%20Christian."
curl "localhost:8080/chat/memory?conversationId=c1&message=What%20is%20my%20name%3F"
```

若要使用其他模型，请修改 `spring.ai.jinfer.chat.model` 或设置环境变量：

```bash
JINFER_CHAT_MODEL=unsloth/Qwen3.5-2B-GGUF:Q8_0 mvn spring-boot:run
```

若要直接运行 jar，请自行传入 JVM 标志：

```bash
mvn package
java --add-modules jdk.incubator.vector --enable-native-access=ALL-UNNAMED \
     -jar target/jinfer-spring-ai-demo-0.0.1-SNAPSHOT.jar
```

---

## 从零搭建你自己的 jinfer + Spring AI 项目

**这些构件发布在 Maven Central 上**，group 为 `com.qxotic`。当前版本是 `0.3.1`
（例如 [jinfer-bom](https://repo1.maven.org/maven2/com/qxotic/jinfer-bom/)）。你无需
构建 `qxotic` 仓库，也无需添加额外的 Maven 仓库。

### 1. 生成 Spring Boot 项目

使用 [start.spring.io](https://start.spring.io)（或 `spring init`），配置如下：

- **Spring Boot 4.1.x** 和 **Java 25**
- 如果需要 REST 端点，添加 **Spring Web** 依赖
- **不要**添加 Spring AI 模型 starter。jinfer 未在 start.spring.io 中列出，因此需要手动添加。

jinfer 0.3.1 基于 Spring Boot 4.1.1 和 Spring AI 2.0.1 构建并测试，也可在 Spring AI 2.1.0-SNAPSHOT 上运行，本示例使用的正是后者。

### 2. 导入两个 BOM

```xml
<properties>
  <java.version>25</java.version>
</properties>

<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.ai</groupId>
      <artifactId>spring-ai-bom</artifactId>
      <version>2.0.1</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
    <dependency>
      <groupId>com.qxotic</groupId>
      <artifactId>jinfer-bom</artifactId>
      <version>0.3.1</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

### 3. 添加 starter、至少一个模型家族，以及可选的快速内核

```xml
<dependency>
  <groupId>com.qxotic</groupId>
  <artifactId>jinfer-spring-ai-spring-boot-starter</artifactId>
</dependency>
<!-- architecture code only, no weights; or pick one: jinfer-lfm2, jinfer-qwen35, jinfer-gemma4, jinfer-llama, jinfer-gptoss ... -->
<dependency>
  <groupId>com.qxotic</groupId>
  <artifactId>jinfer-models-all</artifactId>
</dependency>
<!-- optional matmul acceleration: native SIMD, with a pure-Java Vector API fallback -->
<dependency>
  <groupId>com.qxotic</groupId>
  <artifactId>jam-native</artifactId>
  <scope>runtime</scope>
</dependency>
<dependency>
  <groupId>com.qxotic</groupId>
  <artifactId>jam-vector</artifactId>
  <scope>runtime</scope>
</dependency>
```

starter 会引入 `spring-ai-client-chat`、`spring-ai-rag`，以及针对
`ChatClient.Builder`、工具调用和 `ChatMemory` 的自动配置。

### 4. 传入必需的 JVM 标志

jinfer 需要 `--add-modules jdk.incubator.vector --enable-native-access=ALL-UNNAMED`：

```xml
<plugin>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-maven-plugin</artifactId>
  <configuration>
    <optimizedLaunch>false</optimizedLaunch>   <!-- keep the C2 JIT; it matters for inference speed -->
    <jvmArguments>--add-modules jdk.incubator.vector --enable-native-access=ALL-UNNAMED</jvmArguments>
  </configuration>
</plugin>
```

对于测试，请将同样的标志传给 `maven-surefire-plugin`（`<argLine>`），并传给 `java -jar`
以及你的 IDE 运行配置。

### 5. 配置模型

```properties
spring.ai.model.chat=jinfer
spring.ai.jinfer.chat.model=LiquidAI/LFM2.5-1.2B-Instruct-GGUF:Q8_0   # a HF ref or a local /path/model.gguf
spring.ai.jinfer.chat.context-capacity=8192
spring.ai.jinfer.chat.max-tokens=512
```

然后注入 `ChatClient.Builder`（或 `ChatModel`），编写常规的 Spring AI 代码。

其他 `spring.ai.jinfer.*` 能力也以相同方式工作：

| 能力 | 选择方式 | 模型属性 |
|------------|-----------|----------------|
| 聊天 | `spring.ai.model.chat=jinfer` | `spring.ai.jinfer.chat.model` |
| 嵌入 | `spring.ai.model.embedding=jinfer` | `spring.ai.jinfer.embedding.model`（例如 `Qwen/Qwen3-Embedding-0.6B-GGUF:Q8_0`） |
| 重排序 | 配置后即启用 | `spring.ai.jinfer.rerank.model` |
| 文本转语音 | 配置后即启用 | `spring.ai.jinfer.speech.model` |
| 转录 | 配置后即启用 | `spring.ai.jinfer.transcription.model` |

多模态模型会附带一个投影器作为伴随组件，例如
`spring.ai.jinfer.chat.companions.media=unsloth/gemma-4-E2B-it-GGUF/mmproj-F32.gguf`。

### 有用的环境变量

- `JINFER_MODELS`：覆盖模型缓存目录
- `JINFER_OFFLINE=1`：仅使用缓存的模型，从不下载

### `qxotic` 仓库中的延伸阅读

- `qxotic/jinfer/jinfer-spring-ai/README.md`：完整的 Spring AI 集成指南（视觉、嵌入、
  重排序、提示词缓存、TTS、转录）
- `qxotic/jinfer/jinfer-example-local-rag`：一个完整的 JVM 内 RAG 应用
- `qxotic/jinfer/jinfer-example-judge-advisor`：一个在自我改进顾问中运行的本地文法约束评判器
