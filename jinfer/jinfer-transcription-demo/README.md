# jinfer 转录演示

一个 Spring Boot + Spring AI 应用，使用
[NVIDIA Parakeet](https://huggingface.co/nvidia/parakeet-tdt-0.6b-v3) 将语音转换为文本。模型通过
[Qxotic jinfer](https://qxotic.ai) 在**应用的 JVM 内部**运行：无需云服务、无需 API 密钥，
也无需 Python 和 whisper.cpp。音频始终不会离开本机。

它还包含一个小型网页，你可以用麦克风录音或上传文件，并查看带有逐词时间戳和置信度的转录文本。

## 运行

要求：**JDK 25** 和 Maven 3.9+。示例使用 Spring Boot 4.1.1 和 Spring AI 2.1.0-SNAPSHOT。

```bash
sdk use java 25.0.3-graal
mvn spring-boot:run
```

然后打开 <http://localhost:8080>。

首次启动会将 `mudler/parakeet-cpp-gguf/tdt-0.6b-v3-q8_0.gguf`（897 MB）下载到 macOS 的
`~/Library/Caches/jinfer` 或 Linux 的 `~/.cache/jinfer` 中。之后的启动只需几秒。

## 端点

| 端点 | 使用的 API | 返回值 |
|----------|----------|---------|
| `POST /api/transcribe`（multipart `file`） | 可移植的 Spring AI `TranscriptionModel` | 文本和耗时 |
| `POST /api/transcribe/words`（multipart `file`） | jinfer 的 `JinferTranscriptionModel.transcribe(byte[])` | 文本、每个词的起止时间和置信度，以及相对于实时的速度倍率 |

这两个端点使用同一个自动配置的 Bean。`/api/transcribe` 可与任何 Spring AI
转录提供方配合，无需改动。`/api/transcribe/words` 使用 jinfer 更丰富的 `Transcription` 结果。

```bash
# make a test recording on macOS (or use any WAV / MP3 file)
say -o speech.wav --data-format=LEI16@16000 "Spring AI now runs speech recognition locally."

curl -F file=@speech.wav localhost:8080/api/transcribe
# {"text":"Spring AI now runs speech recognition locally.","elapsedMillis":412}

curl -F file=@speech.wav localhost:8080/api/transcribe/words
# {"text":"...","words":[{"text":"Spring","startSeconds":0.0,"endSeconds":0.24,"confidence":1.0}, ...],
#  "audioSeconds":8.72,"elapsedMillis":942,"realTimeFactor":9.26}
```

在 Apple Silicon 笔记本上，一段 8.7 秒的音频不到一秒即可完成转录，约为实时的 9 倍。

## 音频格式

jinfer 在本地将音频解码为 16 kHz 单声道：

- WAV、AIFF 和 AU 由 `javax.sound` 解码。无需额外工具，支持任意采样率。
- MP3、M4A、OGG 和 WebM 等压缩格式会回退到 `ffmpeg`，该程序必须在 `PATH` 中。
- 音频默认限制为 60 分钟。可通过 `-Djinfer.codecs.maxAudioMinutes=...` 提高限制。

网页会在浏览器中将麦克风录制为 16 kHz 16 位单声道 WAV，因此无需 `ffmpeg` 也能录音。浏览器允许在 `localhost` 上访问麦克风。若使用其他主机，请通过 HTTPS 提供该页面。

## 选择模型

设置 `spring.ai.jinfer.transcription.model`，或使用环境变量：

```bash
JINFER_TRANSCRIPTION_MODEL=mudler/parakeet-cpp-gguf/tdt_ctc-110m-q8_0.gguf mvn spring-boot:run
```

| 模型 | 语言 | 说明 |
|-------|-----------|-------|
| `tdt-0.6b-v3-q8_0.gguf`（默认） | 25 种欧洲语言，自动检测 | 质量与速度的良好平衡 |
| `tdt-0.6b-v3-q4_k.gguf` | 相同 | 最快的 0.6B 变体 |
| `tdt-0.6b-v2-q8_0.gguf` | 英语 | |
| `tdt_ctc-110m-q8_0.gguf` | 英语 | 极小：一小时的语音转录不到一分钟 |

所有 checkpoint 都位于 [mudler/parakeet-cpp-gguf](https://huggingface.co/mudler/parakeet-cpp-gguf)。

## 接线方式

```xml
<dependency>
  <groupId>com.qxotic</groupId>
  <artifactId>jinfer-spring-ai-spring-boot-starter</artifactId>
</dependency>
<dependency>
  <groupId>com.qxotic</groupId>
  <artifactId>jinfer-parakeet</artifactId>   <!-- the Parakeet architecture -->
</dependency>
```

```properties
spring.ai.jinfer.transcription.model=mudler/parakeet-cpp-gguf/tdt-0.6b-v3-q8_0.gguf
```

设置模型属性正是创建 `TranscriptionModel` Bean 的方式。pom 还导入了
`spring-ai-bom`（2.1.0-SNAPSHOT）和 `jinfer-bom`（0.3.1）。它将
`--add-modules jdk.incubator.vector --enable-native-access=ALL-UNNAMED` 通过
`spring-boot-maven-plugin` 传给 JVM。若要直接运行 jar，请传入相同的标志：

```bash
mvn package
java --add-modules jdk.incubator.vector --enable-native-access=ALL-UNNAMED \
     -jar target/jinfer-transcription-demo-0.0.1-SNAPSHOT.jar
```

`JinferTranscriptionModel` 是线程安全的：并发请求会并行转录。
