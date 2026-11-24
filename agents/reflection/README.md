# Spring AI 反思智能体应用

本项目演示使用 Spring AI 构建一个可自我改进的代码生成系统。反思智能体（Reflection Agent）在迭代循环中使用两个 ChatClient 实例 —— 一个用于生成，一个用于评审 —— 以产出高质量的 Java 代码。

它基于仓库 https://github.com/neural-maze/agentic_patterns 中的代码。

该应用实现了一个基于反思的系统，其中**反思智能体**：

- 使用**生成 ChatClient** 根据用户提示词创建代码
- 使用**评审 ChatClient** 审查生成的代码
- 通过将评审反馈回**生成 ChatClient** 来迭代改进代码
- 持续该循环，直到**评审 ChatClient** 对质量满意


## 前置条件
- Java 17 或更高版本
- Maven

本示例使用 OpenAI 作为模型提供方。

在使用 AI 命令之前，请确保你拥有 OpenAI 的开发者 token。

在 [OpenAI Signup](https://platform.openai.com/signup) 创建账户，并在 [API Keys](https://platform.openai.com/account/api-keys) 生成 token。

Spring AI 项目定义了一个名为 `spring.ai.openai.api-key` 的配置属性，你应将其设置为从 OpenAI 获取的 API 密钥值。

导出环境变量是设置该配置属性的一种方式：

```shell
export SPRING_AI_OPENAI_API_KEY=<INSERT KEY HERE>
```



## 运行应用
1. 克隆仓库
2. 进入项目目录
3. 使用 Maven wrapper 运行应用：
   `./mvnw spring-boot:run`

## 项目结构
### 主要组件

* `Application.java`：提供命令行界面的 Spring Boot 主应用
* `ReflectionAgent.java`：在生成与评审之间管理迭代的核心组件


## 工作原理

### 初始设置

反思智能体创建两个 `ChatClient` 实例：

- `generateChatClient`：用于根据用户请求生成 Java 代码
- `critiqueChatClient`：用于审查和评审生成的代码

## 生成过程

- 用户输入请求
- 生成 `ChatClient` 创建初始代码
- 评审 `ChatClient` 审查代码
- 如果需要改进，生成 `ChatClient` 创建修订版本
- 该过程持续至多 `maxIterations` 次，或直到评审 `ChatClient` 批准（`<OK>`）
- 
## ChatClient 配置

- **生成 ChatClient** 的系统提示词：

```text
You are a Java programmer tasked with generating high quality
Java code. Your task is to generate the best content possible
for the user's request.
```
- **评审 ChatClient** 的系统提示词：
```text
You are tasked with generating critique and recommendations for
the user's generated content. If the user content has something
wrong or something to be improved, output a list of
recommendations and critiques.
```

## 示例运行

在此示例运行中，用户请求为 `Person` 类编写一个 JUnit 5 测试。实际输出见文件 `JacksonTestAgent.md`。

### 初始生成

- 生成 `ChatClient` 创建了一个基础的 `Person` 类和测试类
- 包含序列化/反序列化功能
- 实现了基本测试用例

### 评审阶段

评审 ChatClient 发现了若干改进点：

- 更好的错误处理
- 改进代码可读性
- 需要边界情况测试
- 更好的测试结构
- 扩大测试覆盖范围
- 增强 Java 类结构
- 使用现代 Java 特性

### 最终结果

生成 `ChatClient` 创建了改进后的代码，包含：

- 分离的测试方法，以获得更好的模块化
- 带详细消息的增强错误处理
- 增加了针对 null 值的边界情况测试
- 改进的代码结构与可读性
- 更好的测试覆盖

## MergeSort

文件 `AgentMergeSort.md` 展示了一次类似的运行，用于创建归并排序算法。该文件中还包含对生成代码的 JUnit 测试，以证明其可用。
