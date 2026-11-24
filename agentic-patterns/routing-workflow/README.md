# 路由工作流模式

本项目实现了 Anthropic 在 [Building Effective Agents](https://www.anthropic.com/research/building-effective-agents) 中描述的路由工作流模式。该模式基于内容分类，将输入智能地路由到专门的处理程序。

![路由工作流](https://www.anthropic.com/_next/image?url=https%3A%2F%2Fwww-cdn.anthropic.com%2Fimages%2F4zrzovbb%2Fwebsite%2F5c0c0e9fe4def0b584c04d37849941da55e5e71c-2401x1000.png&w=3840&q=75)

## 概述

路由工作流模式专为复杂任务而设计，其中不同类型的输入更适合由专门化的流程处理。它使用 LLM 分析输入内容，并将其路由到最合适的专门化提示词或处理程序。

### 主要优势

- **关注点分离**：每条路由都可以针对特定类型的输入进行优化
- **提升准确度**：专门化的提示词可以发挥其最佳能力
- **可扩展架构**：易于添加新的路由与专门化处理程序
- **更好的资源利用**：将较简单的任务路由到更轻量的模型

### 使用场景

该工作流在以下情况下特别有效：
- 存在需要不同处理方式的明确输入类别
- 分类可以由 LLM 准确完成
- 不同类型的输入需要不同的专业知识或处理方法

## 用法示例

```java
@Autowired
private ChatClient chatClient;

// Create the workflow
RoutingWorkflow workflow = new RoutingWorkflow(chatClient);

// Define specialized prompts for different types of input
Map<String, String> routes = Map.of(
    "billing", "You are a billing specialist. Help resolve billing issues...",
    "technical", "You are a technical support engineer. Help solve technical problems...",
    "general", "You are a customer service representative. Help with general inquiries..."
);

// Process input
String input = "My account was charged twice last week";
String response = workflow.route(input, routes);
```

## 常见用例

1. **客户支持**
   - 将查询路由到相应部门（计费、技术、综合）
   - 将紧急问题转交优先处理
   - 将复杂案例转交专家团队

2. **内容审核**
   - 将内容路由到相应的审核流程
   - 将敏感内容转交人工审核员
   - 将常规内容送入自动检查

3. **查询优化**
   - 将简单问题路由到更小、更快的模型
   - 将复杂查询转交能力更强的模型
   - 将专业主题转交领域特定的处理程序

## 实现细节

该实现包含两个主要组件：

1. `RoutingWorkflow`：实现路由逻辑的主类
   - 使用 LLM 分析输入
   - 选择适当的路由
   - 使用专门化提示词处理输入

2. `RouteResponse`：封装路由决策的 record 类
   - 存储路由选择背后的推理
   - 维护所选路由的信息

## 参考资料

- [Building Effective Agents](https://www.anthropic.com/research/building-effective-agents) - Anthropic Research
- [Spring AI 文档](https://docs.spring.io/spring-ai/reference/api/chatclient.html)
