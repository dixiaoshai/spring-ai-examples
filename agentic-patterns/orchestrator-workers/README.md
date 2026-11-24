# 编排器—工作者工作流模式

本项目演示用于构建高效 LLM 系统的编排器—工作者工作流模式，参考 [Anthropic 关于构建高效智能体的研究](https://www.anthropic.com/research/building-effective-agents)。

![编排工作流](https://www.anthropic.com/_next/image?url=https%3A%2F%2Fwww-cdn.anthropic.com%2Fimages%2F4zrzovbb%2Fwebsite%2F8985fc683fae4780fb34eab1365ab78c7e51bc8e-2401x1000.png&w=3840&q=75)

## 概述

编排器—工作者模式是一种灵活的方法，用于处理需要动态任务分解和专门化处理的复杂任务。它包含三个主要组件：

- **编排器（Orchestrator）**：一个中央 LLM，负责分析任务并确定所需的子任务
- **工作者（Workers）**：执行特定子任务的专门化 LLM
- **合成器（Synthesizer）**：将工作者输出组合为最终结果的组件

## 使用场景

该模式在以下情况下特别有效：

- 子任务无法预先预测的复杂任务
- 需要不同方法或视角的任务
- 需要自适应问题解决的场景
- 受益于专门化处理的任务


## 实现

本实现使用 Spring AI 的 ChatClient 进行 LLM 交互，包含：

```java
public class OrchestratorWorkers {
    public WorkerResponse process(String taskDescription) {
        // 1. Orchestrator analyzes task and determines subtasks
        OrchestratorResponse orchestratorResponse = // ...

        // 2. Workers process subtasks in parallel
        List<String> workerResponses = // ...

        // 3. Results are combined into final response
        return new WorkerResponse(/*...*/);
    }
}
```

### 用法示例

```java
ChatClient chatClient = // ... initialize chat client
OrchestratorWorkers agent = new OrchestratorWorkers(chatClient);

// Process a task
WorkerResponse response = agent.process(
    "Generate both technical and user-friendly documentation for a REST API endpoint"
);

// Access results
System.out.println("Analysis: " + response.analysis());
System.out.println("Worker Outputs: " + response.workerResponses());
```

## 自定义

该模式可以通过以下方式进行自定义：

1. **自定义提示词**：为编排器与工作者提供专门的提示词
```java
agent = new OrchestratorWorkers(
    chatClient,
    customOrchestratorPrompt,
    customWorkerPrompt
);
```

2. **默认模板**：为常见用例修改默认提示词
   - `DEFAULT_ORCHESTRATOR_PROMPT`：用于任务分析的模板
   - `DEFAULT_WORKER_PROMPT`：用于工作者处理的模板

## 依赖

- Spring AI
- Spring Boot
- Java 17 或更高版本

## 参考资料

- [Building Effective Agents（Anthropic 研究）](https://www.anthropic.com/research/building-effective-agents)
