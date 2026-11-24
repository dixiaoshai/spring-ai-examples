# 使用 Spring AI 的并行化工作流模式

本项目演示使用 Spring AI 实现并行化工作流模式，支持对多个大型语言模型（LLM）操作进行高效的并发处理。该模式特别适用于需要并行执行 LLM 调用并自动聚合输出的场景。

![并行化工作流](https://www.anthropic.com/_next/image?url=https%3A%2F%2Fwww-cdn.anthropic.com%2Fimages%2F4zrzovbb%2Fwebsite%2F406bb032ca007fd1624f261af717d70e6ca86286-2401x1000.png&w=3840&q=75)

## 概述

并行化工作流模式体现为两种关键变体：

1. **分段（Sectioning）**：将复杂任务分解为相互独立的子任务以并发处理
2. **投票（Voting）**：并行多次执行相同的提示词，以获得多样化视角或进行多数投票

## 主要优势

- 通过并发处理提升吞吐量
- 更好地利用 LLM API 容量的资源
- 缩短批处理操作的整体处理时间
- 通过多视角（在投票场景中）提升结果质量

## 前置条件

- Java 17 或更高版本
- Maven 3.6+
- Ollama（本地 LLM 服务器）

## 配置

1. 克隆仓库
2. 构建项目：
   ```bash
   mvn clean install
   ```

## 用法示例

以下是使用并行化工作流的基本示例：

```java
List<String> parallelResponse = new ParallelizationlWorkflow(chatClient)
    .parallel(
        "Analyze how market changes will impact this stakeholder group.",
        List.of(
            "Customers: ...",
            "Employees: ...",
            "Investors: ...",
            "Suppliers: ..."
        ),
        4
    );
```

该示例演示了利益相关者分析的并行处理，其中每个利益相关者群体都被并发分析。

## 实现细节

`ParallelizationlWorkflow` 类提供了核心实现，具有以下特性：

- 使用 `ExecutorService` 的固定线程池执行
- 保持与输入顺序一致的有序结果
- 可配置的工作线程数量
- 内置的错误处理与资源管理
- 与 Spring AI 的 ChatClient 集成

### 使用场景

- 处理大量相似但相互独立的条目
- 需要多个独立视角或校验的任务
- 处理时间至关重要且任务可并行化的场景
- 可分解为独立子任务的复杂操作

### 实现注意事项

- 确保任务真正相互独立，以避免一致性问题
- 确定并行执行容量时考虑 API 速率限制
- 扩展并行操作时监控资源使用（内存、CPU）
- 为并行任务失败实现适当的错误处理

## 许可证

本项目基于 Apache License 2.0 许可 - 详见 LICENSE 文件。

## 参考资料

- [Spring AI 文档](https://docs.spring.io/spring-ai/reference/api/chatclient.html)
- [Building Effective Agents - Anthropic](https://www.anthropic.com/research/building-effective-agents)
