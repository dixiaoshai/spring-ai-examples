# GitHub Actions 配置文档

## 概览
本文档介绍 Spring AI Examples 集成测试所用的 GitHub Actions CI/CD 配置。

## 工作流位置
- **文件**：`.github/workflows/integration-tests-manual.yml`
- **类型**：手动触发（workflow_dispatch）
- **用途**：使用可配置的版本测试 Spring AI 示例

## 配置过程

### 1. 前置条件
- 拥有启用了 Actions 的 GitHub 仓库访问权限
- 能够添加仓库密钥

### 2. 必需的密钥
在 Settings → Secrets and variables → Actions 中配置以下内容：
- **OPENAI_API_KEY**：示例中调用 OpenAI API 时必需
- **ANTHROPIC_API_KEY**：使用 Claude 进行 AI 校验时必需

### 3. 工作流组件

#### 环境准备
1. **JDK 17**：通过 `actions/setup-java@v4` 安装，使用 Temurin 发行版
2. **JBang**：通过 `jbangdev/setup-jbang@v0.119.0` 安装
3. **Python 3.11**：通过 `actions/setup-python@v5` 安装
4. **Claude Code CLI**：通过 npm 全局安装
5. **Maven 缓存**：由 setup-java 自动处理
6. **JBang 缓存**：通过 `actions/cache@v4` 处理

#### 版本管理
- 使用 `scripts/update-spring-ai-version.sh` 更新 Spring AI 版本
- 同时支持发行版（例如 1.0.1）与 SNAPSHOT 版本
- 更新 17 个带有 spring-ai.version 属性的 pom.xml 文件

#### 测试执行
- 默认运行 3 个具有代表性的示例：
  - models/chat/helloworld
  - kotlin/kotlin-hello-world
  - misc/spring-ai-java-function-callback
- 即使某个测试失败，也会继续运行所有测试
- 收集结果，并在最后若有测试失败时报告失败

## CI 特有的问题与限制

### 1. AI 校验
- **问题**：Claude Code CLI 需要通过 npm 安装
- **方案**：通过 `npm install -g @anthropic-ai/claude-code` 安装
- **限制**：尚未使用官方 GitHub Action
- **TODO**：借助 `anthropics/claude-code-action@beta` 进行优化

### 2. 退出码
- **问题**：脚本必须正确传递失败状态
- **方案**：在 run-integration-tests.sh 中增加退出码处理
- **注意**：测试串行运行以避免端口冲突

### 3. 依赖
- **JBang**：集成测试必须安装
- **Claude CLI**：AI 校验所需
- **Python**：已安装但目前未使用（为将来的工具预留）

### 4. 性能
- **首次运行**：约 5-10 分钟（需下载依赖）
- **有缓存时**：约 3-5 分钟（使用缓存的依赖）
- **产物存储**：日志保留 7 天

## 版本管理集成

### 成功之处
- ✅ 动态版本切换正常工作
- ✅ 同时支持 1.0.1 与 1.1.0-SNAPSHOT 版本
- ✅ 版本更新脚本无缝集成
- ✅ 支持备份/恢复（本地）

### 工作原理
1. 用户在 workflow dispatch 输入中指定版本
2. 工作流运行 update-spring-ai-version.sh
3. 更新所有 17 个相关 pom.xml 文件
4. 使用指定版本运行测试
5. 版本显示在日志与摘要中

## 用法

### 运行工作流
1. 进入 GitHub 的 Actions 标签页
2. 选择 “Integration Tests (Manual)”
3. 点击 “Run workflow”
4. 进行配置：
   - **Spring AI Version**：例如 1.0.1 或 1.1.0-SNAPSHOT
   - **Test filter**：可选，留空则使用默认的 3 个测试
5. 点击绿色的 “Run workflow” 按钮

### 监控
- 在 Actions 标签页可查看实时日志
- 任务摘要显示测试结果
- 产物中包含详细日志
- 退出码表示整体成功/失败

## 故障排查

### 测试失败
1. 检查是否配置了 ANTHROPIC_API_KEY
2. 确认 OPENAI_API_KEY 有效
3. 查看上传产物中的日志
4. 检查是否存在版本兼容性问题

### Claude CLI 问题
- 确认 npm install 执行成功
- 检查 PATH 中是否包含 npm 全局 bin 目录
- 可能需要使用 `--dangerously-skip-permissions` 标志

### 端口冲突
- 测试串行运行以避免 8080 端口冲突
- 如果问题仍然存在，请检查是否有挂起的进程

## 未来改进
- 迁移到 `anthropics/claude-code-action@beta`
- 增加使用动态端口的并行测试执行
- 实现成本跟踪
- 向默认测试集添加更多示例
- 为不同的触发方式创建独立的工作流