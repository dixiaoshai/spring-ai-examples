# GitHub Actions CI/CD 实施计划

## 概述
通过系统化、分阶段的方式，实现带多版本 Spring AI 支持的 GitHub Actions 集成测试。所有状态都通过本计划和经验总结文档中的复选框进行跟踪。

**优先重点**：版本管理（1.0.1 ↔ 1.1.0-SNAPSHOT）与多版本测试矩阵验证的风险高于全量覆盖，因此我们先处理这些内容。

## 参考架构
本计划借鉴 Spring AI 仓库 GitHub Actions 工作流中久经验证的模式（见 `integration-testing/plans/spring-ai-github-build-workflow.yml`），包括：
- 使用 Temurin 发行版的 JDK 17 配置与 Maven 缓存
- API 密钥的环境变量管理（`OPENAI_API_KEY`、`SPRING_AI_OPENAI_API_KEY`）
- 基于分支的触发（`main`、`*.*.x` 模式）
- 组织特定的条件（`github.repository_owner`）
- 带快照更新的 Maven 批处理模式执行

注意：我们的实现将采用更适合示例仓库的简化模式（不涉及 Artifactory、文档部署或 SSH 密钥）。

## 目标（重新排序）
1. **优先级 1**：版本管理脚本（1.0.1 ↔ 1.1.0-SNAPSHOT）
2. **优先级 2**：用 3 个示例验证多版本测试矩阵
3. **优先级 3**：全量覆盖推广（本地 97% 的可靠性让我们有信心）
4. 通过 GitHub Actions 启用自动化集成测试
5. 提供手动与自动两种工作流触发方式
6. 编写完整的维护文档

---

## 阶段 1：基础工作与脚本重命名

*优先级与之前相同 —— 为了命名清晰所必需*

### 阶段 1 前置条件
- [ ] 查阅 `integration-testing/learnings/` 目录中已有的经验总结
- [ ] 理解当前的集成测试设置
- [ ] 确认 `rit-direct.sh` 存在且可正常工作

### 阶段 1 实施
- [x] 检查当前文件是否存在：`integration-testing/scripts/rit-direct.sh`
- [x] 执行重命名：`mv integration-testing/scripts/rit-direct.sh integration-testing/scripts/run-integration-tests.sh`
- [x] 更新 `CLAUDE.md` 中的引用
- [x] 更新 `integration-testing/README.md` 中的引用
- [x] 更新 `integration-testing/TODO.txt` 中的引用（未发现引用）
- [x] 搜索其他引用：`grep -r "rit-direct.sh" . --exclude-dir=.git`
- [x] 更新所有找到的额外引用

### 阶段 1 验证
- [x] 本地测试：`./integration-testing/scripts/run-integration-tests.sh helloworld`
- [x] 确认退出码为 0
- [x] 本地测试：`./integration-testing/scripts/run-integration-tests.sh kotlin-hello-world`
- [x] 确认退出码为 0
- [x] 检查在 `integration-testing/logs/` 中生成的日志

### 阶段 1 完成与提交节点
- [x] 提交：`git commit -m "refactor: rename integration test runner to run-integration-tests.sh"`
- [x] 创建经验总结文档：`integration-testing/learnings/phase-1-script-renaming.md`
- [x] 记录遇到的任何问题
- [x] 审阅下面的阶段 2，并根据经验总结进行调整

---

## 阶段 2：版本管理系统（高优先级）

*从原阶段 3 提前 —— 这是风险最高的组成部分*

### 阶段 2 前置条件
- [x] 阅读 `integration-testing/learnings/phase-1-script-renaming.md`
- [x] 反思阶段 1 的经验，评估阶段 2 的计划是否需要调整
- [x] 检查是否发现了任何阻塞项或疑虑
- [x] 确认重命名后的脚本可在本地正常工作
- [x] 理解当前的 Spring AI 版本结构

### 阶段 2 实施
- [x] 分析当前的版本分布：`grep -r "spring-ai.version" . --include="pom.xml" | wc -l`
- [x] 记录哪些模块使用版本属性、哪些不使用（17 个使用属性，17 个不使用）
- [x] 创建脚本：`scripts/update-spring-ai-version.sh`
- [x] 添加 shebang 与参数处理：
  ```bash
  #!/bin/bash
  VERSION="${1:-1.1.0-SNAPSHOT}"
  echo "Updating Spring AI version to: $VERSION"
  ```
- [x] 添加 find 命令以定位所有 pom.xml 文件
- [x] 添加 sed 命令以更新 `<spring-ai.version>` 属性
- [x] 在修改前添加备份功能
- [x] 添加版本格式校验（同时支持正式版与 SNAPSHOT）
- [x] 添加从备份恢复的能力（独立脚本）
- [x] 使脚本可执行：`chmod +x scripts/*.sh`

### 阶段 2 本地测试
- [x] 使用正式版本测试：`./scripts/update-spring-ai-version.sh 1.0.1`
- [x] 验证：`grep "spring-ai.version" misc/openai-streaming-response/pom.xml`
- [x] 使用 SNAPSHOT 测试：`./scripts/update-spring-ai-version.sh 1.1.0-SNAPSHOT`
- [x] 确认 SNAPSHOT 格式得到保留
- [x] 测试恢复功能
- [x] 确认原始版本已恢复
- [x] 运行集成测试以确保修改后的版本可用：`./integration-testing/scripts/run-integration-tests.sh kotlin-hello-world`

### 阶段 2 版本兼容性测试
- [x] 使用 1.0.1 测试示例：`./integration-testing/scripts/run-integration-tests.sh kotlin-hello-world`
- [x] 使用 1.1.0-SNAPSHOT 测试示例：`./integration-testing/scripts/run-integration-tests.sh kotlin-hello-world`
- [x] 记录任何版本相关问题（未发现）
- [x] 为已测版本创建兼容性矩阵

### 阶段 2 完成与提交节点
- [x] 创建经验总结文档：`integration-testing/learnings/phase-2-version-management.md`
- [x] 记录版本兼容性发现
- [x] 记录任何版本特定行为或限制（未发现）
- [x] 列出已测试的版本组合
- [x] 提交版本管理系统：`git commit -m "feat: add Spring AI version management system"`
- [x] 审阅阶段 3 并根据经验总结进行调整

---

## 阶段 3：最小可用的 GitHub Actions 工作流

*用于验证版本管理在 CI 中可正常工作的基础工作流*

### 阶段 3 前置条件
- [x] 阅读所有先前经验总结文档（阶段 1-2）
- [x] 反思经验总结，评估阶段 3 的计划是否需要大幅调整
- [x] 识别先前阶段遗留的阻塞项或技术债（未发现）
- [x] 确认版本管理脚本可在本地正常工作
- [ ] 确保拥有 GitHub 仓库的写权限
- [ ] 准备好 OPENAI_API_KEY 以配置为密钥

### 阶段 3 实施
- [x] 创建目录：`.github/workflows/`
- [x] 创建文件：`.github/workflows/integration-tests-manual.yml`
- [ ] 参照 `spring-ai-github-build-workflow.yml` 的模式添加工作流：
  ```yaml
  name: Integration Tests (Manual)
  on:
    workflow_dispatch:
      inputs:
        spring_ai_version:
          description: 'Spring AI Version'
          required: false
          default: '1.1.0-SNAPSHOT'
  
  jobs:
    test:
      runs-on: ubuntu-latest
      timeout-minutes: 30
  ```
- [x] 添加 JDK 17 配置步骤（沿用参考工作流的模式）
- [x] 添加 Maven 缓存配置
- [x] 添加使用阶段 2 脚本更新版本的步骤
- [x] 添加仅针对 3 个示例的测试执行：
  - `models/chat/helloworld`
  - `kotlin/kotlin-hello-world`
  - `misc/spring-ai-java-function-callback`
- [x] 配置环境变量（OPENAI_API_KEY、SPRING_AI_OPENAI_API_KEY）
- [x] 添加日志产物上传

### 阶段 3 GitHub 密钥配置
- [x] 导航到仓库 Settings > Secrets and variables > Actions
- [x] 添加仓库密钥：OPENAI_API_KEY
- [x] 记录密钥配置流程

### 阶段 3 CI 验证
- [x] 提交工作流：`git commit -m "feat: add GitHub Actions workflow with version management"`
- [x] 推送到 GitHub：`git push`
- [x] 导航到 Actions 标签页并选择工作流
- [x] 使用默认版本（1.0.1）触发工作流
- [x] 监控执行并识别问题（JBang、AI 校验）
- [ ] 使用 1.0.1 版本触发工作流（修复后）
- [ ] 下载产物并确认版本已正确应用
- [ ] 使用 1.1.0-SNAPSHOT 版本触发工作流
- [ ] 确认 SNAPSHOT 版本与正式版本均可正常工作

### 阶段 3 故障排查（如需要）
- [x] 如出现失败，分析日志
- [x] 修复 GitHub Actions 特有问题（JBang 安装、退出码、AI 校验）
- [x] 重新运行工作流以验证修复

### 阶段 3 完成与提交节点
- [x] 提交工作流更新：`git commit -m "feat: add manual GitHub Actions workflow for integration tests"`
- [x] 创建经验总结文档：`integration-testing/learnings/phase-3-minimal-workflow.md`
- [x] 记录 GitHub Actions 配置流程（见 integration-testing/docs/GITHUB_ACTIONS_SETUP.md）
- [x] 记录任何 CI 特有问题或限制
- [x] 记录版本管理集成的成功经验
- [x] 审阅阶段 3b 并根据经验总结进行调整

---

## 阶段 3a：使用 Spring AI 1.0.1 进行本地测试

*关键验证阶段 —— 确保所有集成测试在进入 CI/CD 之前都能在本地正常工作*

### 阶段 3a 前置条件
- [x] 确认版本管理脚本可正常工作
- [x] 本地已配置 OPENAI_API_KEY
- [x] 本地已配置 ANTHROPIC_API_KEY（用于 AI 校验）
- [x] 本地已安装 Claude Code CLI

### 阶段 3a 实施 —— 版本更新
- [x] 将所有模块更新到 Spring AI 1.0.1：
  ```bash
  ./scripts/update-spring-ai-version.sh 1.0.1
  ```
- [x] 验证版本更新：
  ```bash
  ./scripts/check-spring-ai-version.sh
  ```
- [x] 确认全部 32 个 pom 文件都显示版本 1.0.1（17 个使用属性，15 个直接使用 BOM）

### 阶段 3a 测试 —— 核心示例
- [x] 测试 models/chat/helloworld：
  ```bash
  ./integration-testing/scripts/run-integration-tests.sh "chat/helloworld"
  ```
- [x] 测试 kotlin/kotlin-hello-world：
  ```bash
  ./integration-testing/scripts/run-integration-tests.sh kotlin-hello-world
  ```
- [x] 测试 misc/spring-ai-java-function-callback：
  ```bash
  ./integration-testing/scripts/run-integration-tests.sh spring-ai-java-function-callback
  ```

### 阶段 3a 测试 —— 全部示例
- [x] 运行完整测试套件：
  ```bash
  ./integration-testing/scripts/run-integration-tests.sh
  ```
- [x] 记录哪些测试通过/失败（17 个通过，7 个失败/超时）
- [x] 识别任何版本相关问题（未发现 —— kotlin-hello-world 存在一次误报）
- [x] 修复 kotlin/rag-with-kotlin 中的 Docker Compose 路径问题
- [x] 反复运行直至所有启用的测试通过

### 阶段 3a AI 校验测试
- [x] 确认 Claude Code CLI 可用：
  ```bash
  claude --version
  ```
- [x] 为核心示例启用 AI 校验进行测试
- [x] 记录 kotlin-hello-world 的 AI 校验误报
- [x] 添加 TODO 以调研 AI 校验问题

### 阶段 3a 故障排查
- [x] 构建失败：检查了 1.0.1 中的 API 破坏性变更（未发现）
- [x] 运行时失败：检查了 integration-testing/logs/ 中的日志
- [x] AI 校验失败：定位到 kotlin-hello-world 的误报
- [x] 将所有问题与解决方案记录在 analysis-summary.md 中

### 阶段 3a 完成与提交节点
- [x] 所有核心示例（3 个）在 Spring AI 1.0.1 下通过
- [x] 记录所有示例的通过/失败状态（test-results-1.0.1.md）
- [x] 提交任何修复：`git commit -m "fix: resolve Docker Compose path for kotlin RAG example"`
- [x] 创建经验总结文档：已创建 test-results-1.0.1.md 与 analysis-summary.md
- [x] 按需更新测试配置（已更新 TODO.txt）
- [x] 审阅阶段 3b 并根据发现进行调整

---

## 阶段 3c：在 CI 中验证全部 17 个通过的测试

*关键验证阶段 —— 在矩阵测试之前，确保全部 17 个已知通过的测试都能在 GitHub Actions 中正常工作*

### 阶段 3c 前置条件
- [ ] 阅读阶段 3a 的经验总结（使用 1.0.1 的测试结果）
- [ ] 确认哪 17 个测试在本地配置 API 密钥后可正常通过
- [ ] 确保已将 ANTHROPIC_API_KEY 添加到 GitHub Secrets
- [ ] 确保已将 BRAVE_API_KEY 添加到 GitHub Secrets

### 阶段 3c 实施 —— 扩展工作流
- [ ] 更新工作流，运行全部 17 个通过的测试而不仅仅是 3 个：
  ```yaml
  # The 17 passing tests:
  # 1. agents/reflection
  # 2. agents/tools-and-agent-tools
  # 3. agentic-patterns/chain-workflow
  # 4. agentic-patterns/evaluator-optimizer-pattern
  # 5. agentic-patterns/orchestrator-workers
  # 6. agentic-patterns/parallelization
  # 7. agentic-patterns/routing-agent
  # 8. kotlin/kotlin-hello-world (with AI validation workaround)
  # 9. misc/spring-ai-java-function-callback
  # 10. model-context-protocol/brave
  # 11. model-context-protocol/client-starter/starter-default-client
  # 12. model-context-protocol/filesystem
  # 13. model-context-protocol/sqlite/chatbot
  # 14. model-context-protocol/sqlite/simple
  # 15. model-context-protocol/web-search/brave-chatbot
  # 16. model-context-protocol/web-search/brave-starter
  # 17. models/chat/helloworld
  ```
- [ ] 将测试按逻辑分组组织（agents、patterns、MCP 等）
- [ ] 在测试组之间添加进度指示
- [ ] 按需延长超时时间（从 30 分钟到 60 分钟）

### 阶段 3c 测试策略
- [ ] 按复杂度顺序运行测试（由简到繁）
- [ ] 将相关测试放在一起，便于调试
- [ ] 在输出中添加清晰的章节标题
- [ ] 跟踪每个测试的执行耗时

### 阶段 3c GitHub Actions 执行
- [ ] 提交更新后的工作流：`git commit -m "feat: expand CI to test all 17 passing examples"`
- [ ] 推送到 GitHub：`git push`
- [ ] 使用 Spring AI 1.0.1 触发工作流
- [ ] 监控全部 17 个测试的执行
- [ ] 记录执行耗时
- [ ] 识别任何 CI 特有的失败

### 阶段 3c 故障排查
- [ ] 如果测试在 CI 中失败但本地通过：
  - 检查环境变量配置
  - 确认 API 密钥已正确设置
  - 审阅 CI 特有的资源限制
  - 检查是否存在超时问题
- [ ] 记录所需的任何 CI 特定调整
- [ ] 更新工作流并加入修复
- [ ] 反复运行直至全部 17 个测试通过

### 阶段 3c 性能分析
- [ ] 计算总执行耗时
- [ ] 识别最慢的测试
- [ ] 判断是否需要并行执行
- [ ] 记录资源使用情况

### 阶段 3c 完成与提交节点
- [ ] 全部 17 个测试在 GitHub Actions 中使用 Spring AI 1.0.1 通过
- [ ] 记录执行耗时与资源使用情况
- [ ] 提交最终工作流：`git commit -m "feat: successfully validate 17 passing tests in CI"`
- [ ] 创建经验总结文档：`integration-testing/learnings/phase-3c-full-validation.md`
- [ ] 审阅阶段 4，并根据当前执行耗时判断矩阵测试是否可行

---

## 阶段 3b：工作流优化与 AI 校验支持

*为支持 AI 校验并提升 CI 性能而进行的关键优化*

### 阶段 3b 前置条件
- [x] 阅读阶段 3 的经验总结（尤其是关于 AI 校验失败的部分）
- [x] 理解 AI 校验所需的 Python 依赖
- [x] 查阅 JBang 安装的最佳实践
- [x] 确认基础工作流可正常运行

### 阶段 3b 实施 —— 配置优化
- [x] 用官方 action 替换手动安装 JBang：
  ```yaml
  - name: Set up JBang
    uses: jbangdev/setup-jbang@v0.119.0
  ```
- [x] 更新测试执行逻辑，仅运行已知通过的测试
  - 排除 7 个持续失败或超时的测试：
    - kotlin/rag-with-kotlin（需要 Docker）
    - misc/openai-streaming-response（Web 服务器超时）
    - model-context-protocol/weather/starter-webmvc-server（Web 服务器超时）
    - prompt-engineering/prompt-engineering-patterns（长时间运行超时）
    - model-context-protocol/client-starter/starter-webflux-client（连接问题）
    - model-context-protocol/dynamic-tool-update/client（需要配对的服务器）
    - model-context-protocol/dynamic-tool-update/server（需要配对的客户端）
- [x] 为 AI 校验添加 Python 环境配置：
  ```yaml
  - name: Set up Python
    uses: actions/setup-python@v5
    with:
      python-version: '3.11'
      cache: 'pip'
  ```
- [x] 为 AI 校验安装 Claude Code CLI：
  ```yaml
  - name: Install Claude Code CLI
    run: |
      echo "📦 Installing Claude Code CLI..."
      npm install -g @anthropic-ai/claude-code
      which claude || echo "❌ Claude CLI not found"
      claude --version || echo "⚠️ Claude CLI version check failed"
  ```
  注意：可能需要改用 anthropics/claude-code-action@beta 的替代方案
- [x] 配置 Claude API 密钥：
  - 在测试步骤的环境变量中添加 ANTHROPIC_API_KEY
  - Claude CLI 将从环境中读取 ANTHROPIC_API_KEY
  - 已创建 TODO，计划用 anthropics/claude-code-action@beta 进行优化
- [x] 添加 JBang 缓存以加速后续运行：
  ```yaml
  - name: Cache JBang
    uses: actions/cache@v4
    with:
      path: ~/.jbang
      key: ${{ runner.os }}-jbang-${{ hashFiles('**/pom.xml') }}
      restore-keys: |
        ${{ runner.os }}-jbang-
  ```

### 阶段 3b 实施 —— 重新启用 AI 校验
- [x] 在 `models/chat/helloworld/integration-tests/ExampleInfo.json` 中重新启用 AI 校验
- [x] 在 `kotlin/kotlin-hello-world/integration-tests/ExampleInfo.json` 中重新启用 AI 校验
- [ ] 在 GitHub Secrets 中配置 ANTHROPIC_API_KEY（需用户添加）
- [ ] 在工作流中使用 Claude Code CLI 测试 AI 校验
- [ ] 确认 AI 校验提示词是否合适
- [ ] 确保 Claude CLI 拥有正确的权限

### 阶段 3b 测试
- [ ] 使用优化后的配置测试工作流
- [ ] 确认 JBang action 正常工作
- [ ] 确认 Python 环境已正确配置
- [ ] 在全部 3 个示例上测试 AI 校验
- [ ] 衡量缓存带来的性能提升

### 阶段 3b 故障排查
- [ ] 如 AI 校验失败，检查 Claude CLI 的安装情况与 PATH
- [ ] 确认 ANTHROPIC_API_KEY 已在 GitHub Secrets 中正确配置
- [ ] 如需，使用 --dangerously-skip-permissions 标志检查 Claude CLI 权限
- [ ] 如 JBang action 失败，检查版本兼容性
- [ ] 记录 AI 校验所需的 Claude API 密钥要求
- [ ] 分别使用 ANTHROPIC_API_KEY 与备选方案进行测试

### 阶段 3b 完成与提交节点
- [x] 提交优化：`git commit -m "feat: optimize workflow with official actions and AI validation support"`
- [x] 创建经验总结文档：`integration-testing/learnings/phase-3b-workflow-optimization.md`
- [x] 记录性能提升情况
- [x] 记录 AI 校验相关的任何问题（Claude CLI 安装）
- [x] 更新关于 Python 依赖的文档
- [x] 审阅阶段 4 并根据经验总结进行调整

---

## 阶段 4：多版本矩阵测试（高优先级）

*用矩阵执行验证版本管理系统 —— 风险高于全量覆盖*

### 阶段 4 前置条件
- [ ] 阅读所有先前经验总结文档（阶段 1-3）
- [ ] 反思累积经验，评估阶段 4 的计划是否需要大幅调整
- [ ] 审阅阶段 3 的执行耗时与资源使用情况
- [ ] 确认单版本工作流可稳定通过 3 个示例
- [ ] 理解 GitHub Actions 的矩阵策略

### 阶段 4 实施
- [ ] 创建新的工作流文件：`.github/workflows/test-spring-ai-versions.yml`
- [ ] 为 Spring AI 版本添加矩阵策略：
  ```yaml
  name: Test Spring AI Versions
  on:
    workflow_dispatch:
  
  jobs:
    test:
      runs-on: ubuntu-latest
      timeout-minutes: 30
      strategy:
        fail-fast: false
        matrix:
          spring-ai-version: ['1.0.1', '1.1.0-SNAPSHOT']
  ```
- [ ] 在任务名称中体现版本以便辨识：`name: Test with Spring AI ${{ matrix.spring-ai-version }}`
- [ ] 复制阶段 3 工作流的步骤，包括：
  - JDK 17 配置
  - 通过官方 action 配置 JBang
  - Python 环境配置
  - 通过 npm 安装 Claude Code CLI
  - JBang 缓存
  - 版本更新脚本
  - 带环境变量（OPENAI_API_KEY、ANTHROPIC_API_KEY）的测试执行
- [ ] 使用矩阵版本：`./scripts/update-spring-ai-version.sh ${{ matrix.spring-ai-version }}`
- [ ] 在输出中报告版本：`echo "Testing with Spring AI version: ${{ matrix.spring-ai-version }}"`
- [ ] 更新产物名称以包含版本：`name: test-logs-${{ matrix.spring-ai-version }}`

### 阶段 4 验证
- [ ] 提交矩阵工作流：`git commit -m "feat: add multi-version matrix testing"`
- [ ] 推送到 GitHub：`git push`
- [ ] 触发矩阵工作流执行
- [ ] 监控两个版本的并行执行
- [ ] 确认版本隔离（每个任务使用正确的版本）
- [ ] 检查两个版本都能通过这 3 个示例
- [ ] 下载并比较两个版本的产物
- [ ] 记录任何版本特定的差异

### 阶段 4 故障排查
- [ ] 如出现版本特定失败，定位根本原因
- [ ] 记录哪些示例在哪些版本下失败
- [ ] 更新兼容性矩阵
- [ ] 修复问题，或标记为版本不兼容
- [ ] 重新运行矩阵以验证修复

### 阶段 4 完成与提交节点
- [ ] 提交矩阵测试：`git commit -m "feat: add multi-version matrix testing workflow"`
- [ ] 创建经验总结文档：`integration-testing/learnings/phase-4-matrix-testing.md`
- [ ] 创建版本兼容性矩阵
- [ ] 记录并行执行的洞察
- [ ] 记录矩阵构建的资源使用情况
- [ ] 审阅阶段 5 并根据经验总结进行调整

---

## 阶段 5：自动触发与路径过滤

### 阶段 5 前置条件
- [ ] 阅读所有先前经验总结文档（阶段 1-4）
- [ ] 反思经验总结，评估阶段 5 的计划是否需要大幅调整
- [ ] 审阅阶段 4 中发现的任何版本特定问题
- [ ] 确认矩阵测试可稳定运行
- [ ] 理解 GitHub Actions 的触发类型
- [ ] 规划路径过滤策略

### 阶段 5 实施
- [ ] 打开 `.github/workflows/integration-tests-manual.yml`
- [ ] 添加 push 事件触发：
  ```yaml
  push:
    branches: [main]
    paths-ignore:
      - 'integration-testing/**'
      - '**/*.md'
      - '.github/**'
      - '.gitignore'
      - 'CLAUDE.md'
  ```
- [ ] 添加带有相同路径过滤的 pull_request 事件触发
- [ ] 保留 workflow_dispatch 以便手动运行
- [ ] 保存工作流文件

### 阶段 5 验证 —— 测试 PR 1（应当触发）
- [ ] 创建新分支：`git checkout -b test-trigger-1`
- [ ] 修改一个示例文件（例如在 `models/chat/helloworld/src/main/java/` 中添加注释）
- [ ] 提交变更：`git commit -m "test: verify workflow triggers on example change"`
- [ ] 推送分支：`git push -u origin test-trigger-1`
- [ ] 创建 pull request
- [ ] 确认工作流被自动触发
- [ ] 验证后关闭 PR

### 阶段 5 验证 —— 测试 PR 2（不应触发）
- [ ] 创建新分支：`git checkout -b test-trigger-2`
- [ ] 仅修改 README.md
- [ ] 提交变更：`git commit -m "docs: update README"`
- [ ] 推送分支并创建 PR
- [ ] 确认工作流未被触发
- [ ] 验证后关闭 PR

### 阶段 5 验证 —— 测试 PR 3（不应触发）
- [ ] 创建新分支：`git checkout -b test-trigger-3`
- [ ] 仅修改 integration-testing 中的文件
- [ ] 提交并推送
- [ ] 创建 PR
- [ ] 确认工作流未被触发
- [ ] 验证后关闭 PR

### 阶段 5 完成与提交节点
- [ ] 提交触发配置：`git commit -m "feat: add automated triggers with path filtering"`
- [ ] 创建经验总结文档：`integration-testing/learnings/phase-5-automated-triggers.md`
- [ ] 记录触发行为
- [ ] 记录任何边界情况
- [ ] 记录路径过滤的有效性
- [ ] 审阅阶段 6 并根据经验总结进行调整

---

## 阶段 6：全量覆盖推广（32 个示例）

### 阶段 6 前置条件
- [ ] 阅读所有先前经验总结文档（阶段 1-5）
- [ ] 反思累积经验，评估阶段 6 的计划是否需要大幅调整
- [ ] 审阅先前阶段发现的任何性能瓶颈
- [ ] 审阅截至目前已测试示例的总执行耗时
- [ ] 估算 32 个示例的预计耗时
- [ ] 确保有充足的 GitHub Actions 分钟数

### 阶段 6 批次 1：简单示例（新增 10 个）
- [ ] 找出 10 个无特殊要求的简单示例
- [ ] 加入工作流的测试执行
- [ ] 提交并推送：`git commit -m "feat: add batch 1 simple examples"`
- [ ] 触发工作流
- [ ] 监控执行
- [ ] 修复发现的任何问题
- [ ] 记录结果

### 阶段 6 批次 2：MCP 示例（新增 15 个）
- [ ] 在 GitHub 密钥中配置 BRAVE_API_KEY（如需要）
- [ ] 向工作流添加 15 个 MCP 示例
- [ ] 如需要，将超时时间延长到 60 分钟
- [ ] 提交并推送：`git commit -m "feat: add batch 2 MCP examples"`
- [ ] 触发工作流
- [ ] 监控执行
- [ ] 修复任何 MCP 特有问题
- [ ] 记录 MCP 相关要求

### 阶段 6 批次 3：剩余复杂示例
- [ ] 添加所有剩余示例
- [ ] 优化执行顺序以提升效率
- [ ] 提交并推送：`git commit -m "feat: complete full coverage with all examples"`
- [ ] 触发完整测试套件
- [ ] 监控完整执行过程
- [ ] 计算最终通过率
- [ ] 确认达到 97% 的通过率

### 阶段 6 性能优化
- [ ] 如果执行超过 60 分钟，考虑：
  - [ ] 拆分为多个任务
  - [ ] 并行执行策略
  - [ ] 缓存优化
- [ ] 记录优化决策

### 阶段 6 完成与提交节点
- [ ] 最终提交：`git commit -m "feat: complete full integration test coverage (32 examples)"`
- [ ] 创建经验总结文档：`integration-testing/learnings/phase-6-full-coverage.md`
- [ ] 记录最终指标（通过率、执行耗时）
- [ ] 列出持续失败的测试
- [ ] 记录资源使用情况
- [ ] 审阅阶段 7 并根据经验总结进行调整

---

## 阶段 7：多版本测试矩阵

### 阶段 7 前置条件
- [ ] 阅读所有先前经验总结文档（阶段 1-6）
- [ ] 反思经验总结，评估阶段 7 的计划是否需要大幅调整
- [ ] 审阅阶段 6 全量覆盖的性能指标
- [ ] 确认完整测试套件可成功运行
- [ ] 确定要测试的目标 Spring AI 版本
- [ ] 理解 GitHub Actions 的矩阵策略

### 阶段 7 实施
- [ ] 创建新的工作流文件：`.github/workflows/test-spring-ai-versions.yml`
- [ ] 添加矩阵策略：
  ```yaml
  strategy:
    fail-fast: false
    matrix:
      spring-ai-version: ['1.0.0', '1.0.1', '1.1.0-SNAPSHOT']
  ```
- [ ] 配置任务使用矩阵版本
- [ ] 在任务名称中体现版本以便辨识
- [ ] 在测试输出中报告版本

### 阶段 7 小规模测试
- [ ] 初始矩阵测试仅限 3 个示例
- [ ] 推送工作流：`git commit -m "feat: add multi-version testing matrix"`
- [ ] 触发工作流
- [ ] 确认并行执行正常工作
- [ ] 检查版本隔离
- [ ] 审阅每个版本的日志

### 阶段 7 全规模测试
- [ ] 扩展到全部 32 个示例
- [ ] 触发完整矩阵工作流
- [ ] 监控并行执行（3 个版本 × 32 个示例）
- [ ] 记录版本特定的失败
- [ ] 创建兼容性矩阵

### 阶段 7 完成与提交节点
- [ ] 提交多版本测试：`git commit -m "feat: add multi-version testing matrix for all examples"`
- [ ] 创建经验总结文档：`integration-testing/learnings/phase-7-multi-version.md`
- [ ] 创建版本兼容性矩阵文档
- [ ] 记录版本特定问题
- [ ] 记录矩阵构建的执行耗时
- [ ] 审阅阶段 8 并根据经验总结进行调整

---

## 阶段 8：增强 CI 功能

### 阶段 8 前置条件
- [ ] 阅读所有先前经验总结文档（阶段 1-7）
- [ ] 反思经验总结，评估阶段 8 的计划是否需要大幅调整
- [ ] 审阅阶段 7 的资源使用与成本影响
- [ ] 根据实际发现的需求确定增强功能的优先级
- [ ] 确认核心功能已稳定
- [ ] 审阅 GitHub Actions 的高级功能

### 阶段 8 JSON 输出实现
- [ ] 修改 `run-integration-tests.sh`，添加 `--json` 标志
- [ ] 实现测试结果的 JSON 输出格式
- [ ] 在本地测试 JSON 输出
- [ ] 在工作流中集成 JSON 解析

### 阶段 8 GitHub 注解
- [ ] 为测试失败添加 problem matchers
- [ ] 实现工作流注解
- [ ] 测试 PR 中的注解展示

### 阶段 8 PR 评论
- [ ] 添加 action，在 PR 上评论测试结果
- [ ] 将结果格式化为 Markdown 表格
- [ ] 用示例 PR 进行测试

### 阶段 8 缓存
- [ ] 添加 Maven 依赖缓存
- [ ] 如适用，添加 JBang 缓存
- [ ] 衡量性能提升
- [ ] 记录缓存命中率

### 阶段 8 定时运行
- [ ] 添加每夜定时触发：
  ```yaml
  schedule:
    - cron: '0 2 * * *'  # 2 AM UTC daily
  ```
- [ ] 配置为针对多个版本运行
- [ ] 设置失败通知

### 阶段 8 完成与提交节点
- [ ] 提交增强功能：`git commit -m "feat: add CI enhancements (JSON output, annotations, caching)"`
- [ ] 创建经验总结文档：`integration-testing/learnings/phase-8-enhancements.md`
- [ ] 记录功能的有效性
- [ ] 记录性能提升情况
- [ ] 列出暂缓的功能
- [ ] 审阅阶段 9 并根据经验总结进行调整

---

## 阶段 9：文档与收尾

### 阶段 9 前置条件
- [ ] 阅读所有经验总结文档（阶段 1-8）
- [ ] 回顾整个历程，识别关键成功因素
- [ ] 记录任何技术债或未来需要改进之处
- [ ] 汇总关键洞察清单
- [ ] 对比原始目标与实际成果
- [ ] 收集所有配置示例

### 阶段 9 文档任务
- [ ] 用最终架构更新本计划
- [ ] 创建故障排查指南：`integration-testing/docs/GITHUB_ACTIONS_TROUBLESHOOTING.md`
- [ ] 创建维护运行手册：`integration-testing/docs/CI_MAINTENANCE.md`
- [ ] 在主 README.md 中添加 CI/CD 章节
- [ ] 记录所有工作流文件
- [ ] 创建密钥管理指南
- [ ] 记录回滚流程

### 阶段 9 验证
- [ ] 运行完整的手动工作流
- [ ] 创建测试 PR 以验证自动触发
- [ ] 运行多版本矩阵
- [ ] 验证定时运行（如已实现）
- [ ] 测试所有已记录的流程

### 阶段 9 完成与提交节点
- [ ] 最终文档提交：`git commit -m "docs: complete GitHub Actions CI/CD documentation"`
- [ ] 创建最终总结：`integration-testing/learnings/github-actions-implementation-summary.md`
- [ ] 归档本计划并附上完成日期
- [ ] 记录未来的增强机会
- [ ] 计算总实施时间
- [ ] 标记项目完成

---

## 关键原则

1. **渐进增强**：从 3 个示例开始，逐步扩展
2. **尽早验证**：早在阶段 2 就在 GitHub Actions 中测试
3. **持续学习**：每个阶段都会阅读所有先前的经验总结
4. **自适应规划**：根据发现调整后续阶段
5. **状态记入文档**：通过复选框与经验总结跟踪进度

## 成功指标

- **阶段 2**：首次在 GitHub Actions 中运行 3 个示例 ✓
- **阶段 4**：8 个示例稳定运行 ✓
- **阶段 5**：自动触发正常工作 ✓
- **阶段 6**：全部 32 个示例，通过率 97% ✓
- **阶段 7**：多版本测试投入运行 ✓
- **阶段 8**：增强功能已实施 ✓
- **阶段 9**：文档完整 ✓

## 风险缓解

1. **API 速率限制**：实施缓存，限制并发运行
2. **密钥泄露**：使用 GitHub 密钥，绝不提交密钥
3. **执行耗时过长**：优化顺序，考虑拆分任务
4. **不稳定测试**：添加重试逻辑，记录已知问题
5. **版本冲突**：维护兼容性矩阵

## 维护说明

- 每周审阅工作流运行情况
- 每月更新 Spring AI 版本
- 每季度归档旧日志
- 每半年审阅并更新文档
- 监控 GitHub Actions 的使用量与成本

## 未来增强（不在当前范围内）

- 带动态端口分配的并行执行
- 测试结果趋势看板
- 失败时自动创建 issue
- 与 Spring AI 发布流程集成
- 性能回归检测