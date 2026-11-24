# 用于 PR 集成测试搭建的本地工具

## 概述
本文档规划了一个本地命令行工具：维护者在审查 PR 时可用它为新贡献的 Spring AI 示例自动生成集成测试。

## 问题陈述
当贡献者向 spring-ai-examples 提交新示例时，通常会提供：
- 核心应用代码（src/main/java 或 src/main/kotlin）
- 含使用说明的 README.md
- 含依赖的 pom.xml
- 用于配置的 application.properties
- 基础单元测试（有时提供）

集成测试方面尚缺的内容：
- 集成测试配置（ExampleInfo.json）
- JBang 启动脚本（Run*.java）
- 环境变量需求说明
- 成功校验模式
- AI 校验配置

## 建议方案：本地维护者工具

### 核心概念
创建一个本地命令行工具，维护者在审查 PR 时运行它，即可为新贡献的示例自动生成集成测试。该工具将分析贡献的代码并生成所有必要的集成测试文件。

## 工具设计：`analyze-pr-example.py`

### 使用流程
```bash
# When reviewing a PR with a new example
cd spring-ai-examples
git checkout pr-branch

# Run the analysis tool
python3 integration-testing/scripts/analyze-pr-example.py path/to/new-example

# Tool automatically:
# 1. Analyzes the example structure
# 2. Generates integration test files
# 3. Runs the test locally
# 4. Shows results to maintainer
```

### 主要功能

#### 1. 从现有文件自动检测
- **README 解析**：提取预期行为、用法示例与前决条件
- **pom.xml 解析**：检测依赖，判定复杂度级别
- **application.properties**：识别必需的环境变量
- **源代码**：查找主类，检测所使用的 Spring AI 组件

#### 2. 智能测试生成
- 自动判定复杂度级别（simple/complex/mcp）
- 生成合适的 ExampleInfo.json 配置
- 使用集中式工具类创建 JBang 启动器
- 根据示例特征推荐 AI 校验模式
- 从 README 示例中提取成功模式

#### 3. 交互式完善
- 向维护者展示生成的配置
- 允许在定稿前快速编辑
- 可选择立即运行测试以进行校验
- 生成可直接提交的文件

### 实现细节

#### 阶段 1：增强 `scaffold_integration_test.py`
为现有的脚手架工具增加新能力：
```python
--auto-detect     # Analyze existing files to determine configuration
--from-readme     # Extract expected behavior from README
--analyze-deps    # Determine complexity from dependencies
--suggest-patterns # Generate success patterns from README examples
```

#### 阶段 2：新增分析模块
创建 `integration-testing/scripts/example_analyzer.py`：

##### README 解析器
- 提取 “Expected Output” 或 “Example Output” 章节
- 识别关键功能描述
- 提取用法示例以生成模式
- 查找前决条件与环境要求

##### POM 分析器
- 根据依赖评估复杂度：
  - 简单：基础 Spring Boot + 一个 Spring AI 模块
  - 复杂：多个 AI 模块、工作流或智能体
  - MCP：Model Context Protocol 依赖
- 识别所使用的 Spring AI 组件
- 检测数据库或外部服务需求

##### 模式建议器
- 从 README 示例生成正则表达式模式
- 基于 Spring AI 组件类型的常见模式：
  - 聊天模型：响应生成模式
  - 嵌入：向量操作模式
  - 函数：工具调用模式
  - 智能体：工作流执行模式

##### 环境检测器
- 解析 application.properties/yml 中的占位符
- 识别常见的 API 密钥模式
- 检测数据库连接需求
- 查找服务端点配置

### 工具输出示例
```
🔍 Analyzing: kotlin/new-rag-example
📄 Found README with usage examples
📦 Detected dependencies: spring-ai-openai, spring-ai-vectordb
🔧 Found required env: OPENAI_API_KEY, POSTGRES_CONNECTION
📊 Complexity: complex (vector DB + multi-step workflow)

✅ Generated integration test configuration:
- Timeout: 180s (complex workflow)
- AI Validation: hybrid mode (recommended for RAG)
- Success patterns: 3 extracted from README
- Environment vars: 2 required

📁 Files created:
- integration-tests/ExampleInfo.json
- integration-tests/RunNewRagExample.java

🧪 Run test now? [Y/n]
```

### 配置模板

#### 简单示例模板
```json
{
  "timeoutSec": 120,
  "successRegex": ["BUILD SUCCESS", "Started.*Application"],
  "requiredEnv": ["OPENAI_API_KEY"],
  "aiValidation": {
    "enabled": true,
    "validationMode": "hybrid",
    "expectedBehavior": "[Extracted from README]",
    "promptTemplate": "example_validation"
  }
}
```

#### 复杂示例模板
```json
{
  "timeoutSec": 300,
  "successRegex": ["BUILD SUCCESS", "Started.*Application", "[Pattern from README]"],
  "requiredEnv": ["OPENAI_API_KEY", "[Additional from properties]"],
  "aiValidation": {
    "enabled": true,
    "validationMode": "primary",
    "readmeFile": "../README.md",
    "expectedBehavior": "[Extracted from README]",
    "promptTemplate": "workflow_validation",
    "successCriteria": {
      "expectedSteps": 3,
      "evaluationRequired": true
    }
  }
}
```

## 优势
1. **即时**：可在 PR 审查过程中使用，无需等待 CI
2. **本地可控**：维护者拥有完全的可见性与控制权
3. **迭代式**：可立即调整并测试
4. **简单**：一条命令即可完成分析与生成
5. **有教育意义**：让维护者了解什么样的测试才算好
6. **一致**：确保所有示例都有集成测试

## 实施阶段

### 阶段 1：核心功能（第 1 周）
- [ ] 创建 example_analyzer.py 模块
- [ ] 实现 README 解析以提取行为
- [ ] 添加 POM 依赖分析
- [ ] 基于 README 的基础模式建议

### 阶段 2：增强检测（第 2 周）
- [ ] 从 properties 中检测环境变量
- [ ] 复杂度评分算法
- [ ] AI 校验模式推荐
- [ ] 成功模式生成

### 阶段 3：集成（第 3 周）
- [ ] 与 scaffold_integration_test.py 集成
- [ ] 添加交互式完善模式
- [ ] 创建 analyze-pr-example.py 封装脚本
- [ ] 用近期 PR 示例进行测试

### 阶段 4：文档（第 4 周）
- [ ] 创建维护者工作流指南
- [ ] 记录模式提取逻辑
- [ ] 添加故障排查指南
- [ ] 创建视频演示

## 成功标准
- 工具能够成功分析 90% 的新 PR 示例
- 生成的测试首次运行通过率达到 80%
- 新增集成测试的耗时从 15 分钟缩短到 2 分钟
- 维护者更倾向于使用工具而非手动创建

## 未来增强
- 集成 GitHub CLI，直接检出 PR
- 批量处理多个示例
- 从历史测试配置中学习
- 与 PR 审查评论集成
- 自动更新 PR 并附带生成的测试

## 备注
- 继续将 JBang 作为主要的测试执行框架
- JUnit5 框架仅作为进阶的维护者工具保留
- 专注于零摩擦的贡献者体验
- 保持与现有测试的向后兼容

---

*本计划创建于 2025-08-08，实施前应重新审阅。*