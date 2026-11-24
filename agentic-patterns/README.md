# 智能体模式

本项目演示了用于构建高效 LLM 系统的多种工作流模式的实践实现，参考 [Anthropic 关于构建高效智能体的研究](https://www.anthropic.com/research/building-effective-agents)。

## 概述

本项目提供了五种基础工作流模式的具体实现，可用于构建高效的 LLM 系统。每种模式都实现为一个独立的模块，各自具有特定的用例和优势。

## 工作流模式

### 1. 链式工作流
[chain-workflow/](chain-workflow/)

通过提示词链将任务分解为一系列 LLM 调用，其中每一步都处理上一步的输出。适用于可以清晰拆分为固定子任务的任务。

**使用场景：**
- 具有明确顺序步骤的任务
- 希望以延迟换取更高准确度的场景
- 每一步都基于上一步输出构建的场景

**示例应用：**
- 数据转换流水线
- 多步骤文本处理
- 具有结构化步骤的文档生成

### 2. 并行化工作流
[parallelization-worflow/](parallelization-worflow/)

支持多个 LLM 操作并发处理，包含两种关键变体：
- **分段（Sectioning）**：将任务拆分为并行运行的独立子任务
- **投票（Voting）**：多次运行同一任务以获得多样化的输出

**使用场景：**
- 处理大量相似但相互独立的条目
- 需要多个独立视角的任务
- 处理时间至关重要且任务可并行化的场景

**示例应用：**
- 文档的批处理
- 多视角内容分析
- 并行校验检查

### 3. 路由工作流
[routing-workflow/](routing-workflow/)

实现了一个分类系统，将输入引导至专门的后续任务，从而实现关注点分离，并针对不同类型的输入优化处理。

**使用场景：**
- 输入类别划分明确的复杂任务
- 不同输入需要专门处理的场景
- 分类可以准确完成的场景

**示例应用：**
- 客户支持工单路由
- 内容审核系统
- 基于复杂度的查询优化

### 4. 编排器—工作者
[orchestrator-workers/](orchestrator-workers/)

实现了一个灵活的系统，其中中央 LLM 负责任务分解的编排，并委派给专门的工作者 LLM。

**使用场景：**
- 子任务无法预先预测的复杂任务
- 需要不同方法或视角的任务
- 需要自适应问题解决的场景

**示例应用：**
- 复杂的代码生成任务
- 多来源研究任务
- 自适应内容创作

### 5. 评估器—优化器
[evaluator-optimizer/](evaluator-optimizer/)

实现了一个迭代精炼过程，其中一个 LLM 生成解决方案，另一个 LLM 提供评估和反馈。

**使用场景：**
- 存在明确的评估标准
- 迭代精炼能带来可衡量的价值
- 任务受益于多轮评审

**示例应用：**
- 代码审查与改进
- 内容质量优化
- 翻译精炼
- 复杂搜索任务

## 实现细节

所有工作流均使用以下技术实现：
- Spring AI 用于 LLM 交互
- Spring Boot 作为应用框架
- Java 17+ 以利用现代语言特性

每个工作流模块包含：
- 核心工作流实现
- 使用示例
- 自定义选项
- 单元测试

## 快速开始

1. 克隆仓库
2. 为你的用例选择合适的工作流模式
3. 查看各模块的 README 了解具体实现细节

## Spring AI 特性

本项目利用了 Spring AI 的若干关键特性来高效实现这些工作流模式：

1. **ChatClient 接口**：与 LLM 模型交互的核心抽象
   - 跨不同 LLM 提供方的一致 API
   - 用于构建提示词的流式接口
   - 内置重试与错误处理

2. **模型支持**：广泛支持各类 LLM 提供方
   - OpenAI（GPT 模型）
   - Anthropic（Claude 模型）
   - Ollama（本地模型）
   - 以及更多……

3. **结构化输出**：对 LLM 响应进行类型安全处理
   - 将 JSON 响应转换为 Java 对象
   - 强类型的响应处理
   - 校验与错误处理

4. **提示词管理**：灵活的提示词处理
   - 基于模板的提示词
   - 系统消息与用户消息分离
   - 上下文管理

这些特性为构建可靠且易于维护的 LLM 应用提供了坚实基础。

### Spring AI 模型可移植性

本项目中的工作流与模型无关，可与 [Spring AI 支持的任何聊天模型](https://docs.spring.io/spring-ai/reference/api/chat/comparison.html)配合使用。要在不同模型之间切换：

1. 在 `pom.xml` 中替换特定模型的 starter 依赖：

```xml
<!-- OpenAI -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>

<!-- Anthropic -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-anthropic</artifactId>
</dependency>

<!-- Ollama -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-ollama</artifactId>
</dependency>
```

2. 在 `application.properties` 中配置特定模型的属性：

```properties
# OpenAI Configuration
spring.ai.openai.api-key=your-api-key
spring.ai.openai.chat.model=gpt-4o

# Anthropic Configuration
spring.ai.anthropic.api-key=your-api-key
spring.ai.anthropic.chat.model=claude-sonnet-4-5

# Ollama Configuration
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.model=llama3.2:latest
```

每种模型的详细配置选项可参阅 [Spring AI Chat Models 文档](https://docs.spring.io/spring-ai/reference/api/chatmodel.html)。


### Spring AI 结构化输出

本项目中的若干模式使用 Spring AI 的 [结构化输出转换器](https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html) 来处理来自 LLM 的结构化响应。该特性允许：
- 将 LLM 响应转换为强类型的 Java 对象
- 确保一致的响应格式
- 对 LLM 输出进行类型安全的处理

#### 示例用法

```java
// Define a record for structured output
public record EvaluationResponse(Evaluation evaluation, String feedback) {
    public enum Evaluation {
        PASS, NEEDS_IMPROVEMENT, FAIL
    }
}

// Use the entity() method to convert response to your type
EvaluationResponse response = chatClient.prompt(prompt)
    .call()
    .entity(EvaluationResponse.class);

// Access typed fields
if (response.evaluation() == Evaluation.PASS) {
    // Handle passing evaluation
}
```

#### 实现示例

该特性在多个工作流模式中使用：

1. **评估器—优化器工作流**：以通过/失败状态和反馈来构造评估响应
```java
record EvaluationResponse(Evaluation evaluation, String feedback)
```

2. **路由工作流**：以推理和选择来构造路由决策
```java
record RoutingResponse(String reasoning, String selection)
```

3. **链式工作流**：构造中间转换结果
```java
record TransformationResult(String output, List<String> steps)
```


## 参考资料

- [Building Effective Agents（Anthropic 研究）](https://www.anthropic.com/research/building-effective-agents)
- [Spring AI 文档](https://docs.spring.io/spring-ai/reference/api/chatclient.html)
- [Spring AI 聊天模型](https://docs.spring.io/spring-ai/reference/api/chatmodel.html)
- [Spring AI 结构化输出](https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html)
