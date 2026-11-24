# 集成测试故障排查指南

## 速查表

| 现象 | 最可能的原因 | 快速修复 |
|---------|-------------------|-----------|
| ❌ 8080 端口被占用 | 上一个测试未清理 | `lsof -ti:8080 \| xargs -r kill` |
| ❌ NoSuchElementException | 交互式应用，缺少配置 | 添加 `"requiresUserInteraction": true` |
| ❌ Docker Compose 错误 | 路径/工作目录问题 | 检查 compose.yaml 的位置 |
| ❌ 缺少环境变量 | 未设置 API 密钥 | `export OPENAI_API_KEY=your-key` |
| ❌ AI 校验失败 | 应用未正常启动 | 检查 Spring Boot 日志中的实际错误 |
| ❌ 未找到模式 | 输出已变化或正则错误 | 更新 successRegex 模式 |

## 常见问题与解决方案

### 1. 端口冲突

#### 现象
```
Port 8080 was already in use
org.springframework.boot.web.server.PortInUseException
```

#### 根本原因
之前的 Spring Boot 进程未干净退出

#### 解决方案
```bash
# Quick fix - kill processes on port 8080
lsof -ti:8080 | xargs -r kill

# Comprehensive cleanup (used in run-integration-tests.sh)
./integration-testing/scripts/run-integration-tests.sh --clean-logs

# Check what's using the port
lsof -i :8080
```

#### 预防措施
- 使用自带自动端口清理的 `run-integration-tests.sh`
- 允许进程在测试之间优雅关闭
- 考虑为并行测试使用动态端口分配

### 2. 交互式应用失败

#### 现象
```
java.util.NoSuchElementException: No line found
Application exited with code: 1
```

#### 根本原因
使用 `Scanner.nextLine()` 的应用在自动化测试中没有可用输入时会失败

#### 解决方案
配置测试，允许交互式应用返回非零退出码：

```json
{
  "aiValidation": {
    "enabled": true,
    "validationMode": "fallback",
    "successCriteria": {
      "requiresUserInteraction": true
    }
  }
}
```

#### 工作原理
- 测试校验应用启动与就绪标志
- 将退出码 1 视为预期行为而允许
- 关注“已准备好交互”而不是“已完成交互”

#### 示例
- `agents/reflection` - 显示 “Let's chat!” 后在 Scanner.nextLine() 处失败
- `sqlite/chatbot` - 显示 “Starting interactive chat session” 后失败
- `brave-chatbot` - 显示 “I am your AI assistant” 后失败

### 3. Docker Compose 路径问题

#### 现象
```
Docker Compose file 'kotlin/rag-with-kotlin/compose.yaml' does not exist
```

#### 根本原因
应用相对于项目根目录查找 Docker Compose 文件，但实际从模块目录运行

#### 解决方案

**选项 1：安装命令**
```json
{
  "setupCommands": [
    "docker-compose up -d",
    "sleep 10"
  ],
  "cleanupCommands": [
    "docker-compose down"
  ]
}
```

**选项 2：修正工作目录**
```json
{
  "setupCommands": [
    "bash -c 'cd ../../ && docker-compose -f kotlin/rag-with-kotlin/compose.yaml up -d'"
  ]
}
```

**选项 3：应用配置**
更新应用的 Docker Compose 配置，改用相对于模块目录的路径

### 4. 环境变量问题

#### 现象
```
❌ Missing required environment variable: OPENAI_API_KEY
```

#### 解决方案
```bash
# Set for current session
export OPENAI_API_KEY="your-api-key-here"

# For Anthropic examples
export ANTHROPIC_API_KEY="your-anthropic-key-here"

# For Brave Search examples  
export BRAVE_API_KEY="your-brave-key-here"

# Verify environment variables are set
env | grep -E "(OPENAI|ANTHROPIC|BRAVE)_API_KEY"
```

#### 配置
确保 `ExampleInfo.json` 列出了所有必需的环境变量：
```json
{
  "requiredEnv": [
    "OPENAI_API_KEY",
    "BRAVE_API_KEY"
  ]
}
```

### 5. AI 校验失败

#### 现象
```
⚠️ AI validation failed: AI validation script failed with exit code: 1
```

#### 诊断步骤
1. 在 Spring Boot 日志中**检查应用的实际输出**
2. **确认应用已成功启动**（查找 “Started Application”）
3. **检查应用日志中的真实错误**
4. **确保环境变量已正确设置**

#### 常见原因
- **应用启动失败**：真实的 Spring Boot 错误被 AI 校验掩盖
- **缺少依赖**：外部服务不可用
- **配置问题**：API 密钥或配置值错误
- **网络问题**：无法访问外部 API

#### 解决方案
```bash
# Run test individually to see full output
cd module-directory
jbang integration-tests/RunModule.java

# Check the detailed Spring Boot log
cat integration-testing/logs/integration-tests/module-spring-boot-*.log

# Test the application manually
./mvnw spring-boot:run
```

### 6. 模式匹配问题

#### 现象
```
❌ Missing: Started.*Application
❌ Missing: Expected output pattern
```

#### 诊断
1. 在展示的应用日志中**检查实际输出**
2. **核实正则模式**对该应用是否正确
3. **留意因环境不同而出现的输出差异**

#### 解决方案

**更新成功模式**
```json
{
  "successRegex": [
    "Started.*Application",          // Standard Spring Boot
    "Your specific pattern here"     // Application-specific
  ]
}
```

**测试正则模式**
```bash
# Test regex against actual output
grep -E "Started.*Application" log-file.txt
```

**改用 AI 校验**
对于输出不可预测的应用：
```json
{
  "aiValidation": {
    "enabled": true,
    "validationMode": "primary"
  }
}
```

### 7. 构建失败

#### 现象
```
[ERROR] Failed to execute goal ... compilation failure
```

#### 解决方案
```bash
# Clean build
./mvnw clean

# Update dependencies
./mvnw dependency:resolve

# Check Java version
java -version  # Should be 17+

# Manual build test
./mvnw clean package
```

### 8. JBang 编译问题

#### 现象
```
[jbang] Error compiling script
cannot find symbol
```

#### 常见原因
- JBang `//DEPS` 指令中**缺少依赖**
- `//SOURCES` 指令中的**源路径错误**
- **Java 版本不匹配**

#### 解决方案
```bash
# Update JBang
jbang version
curl -Ls https://sh.jbang.dev | bash -s - app setup

# Clear JBang cache
jbang cache clear

# Test JBang script manually
jbang --verbose integration-tests/RunModule.java
```

## 调试工作流

### 1. 测试失败排查

```bash
# Step 1: Run individual test
cd failing-module
jbang integration-tests/RunModule.java

# Step 2: Check logs
cat integration-testing/logs/integration-tests/module-spring-boot-*.log

# Step 3: Manual application test
./mvnw spring-boot:run

# Step 4: Check configuration
cat integration-tests/ExampleInfo.json
```

### 2. 新建测试的调试

```bash
# Step 1: Scaffold test
python3 integration-testing/scripts/scaffold_integration_test.py module-path --complexity simple

# Step 2: Test immediately
cd module-path
jbang integration-tests/RunModule.java

# Step 3: Adjust configuration
# Edit integration-tests/ExampleInfo.json

# Step 4: Re-test
jbang integration-tests/RunModule.java
```

### 3. 完整测试套件调试

```bash
# Run with detailed output
./integration-testing/scripts/rit-direct.sh --verbose

# Run specific test only
./integration-testing/scripts/rit-direct.sh module-name

# Clean logs and re-run
./integration-testing/scripts/rit-direct.sh --clean-logs
```

## 配置调试

### 校验模式选择

| 应用类型 | 推荐模式 | 理由 |
|------------------|------------------|-----------|
| 交互式（Scanner） | `fallback` | 允许正则失败，用 AI 校验就绪状态 |
| 可预测输出 | `hybrid` | 用正则保证速度，用 AI 做额外校验 |
| 多变的 AI 输出 | `primary` | 跳过正则，仅使用 AI 校验 |

### 超时调优

```json
{
  "timeoutSec": 120  // Adjust based on application complexity
}
```

指南：
- **简单示例**：60-120 秒
- **交互式应用**：15-25 秒（仅启动校验）
- **复杂工作流**：300-600 秒
- **Docker 依赖**：180 秒以上（留出服务启动时间）

### 环境变量调试

```bash
# Check all environment variables
printenv | grep -E "(API_KEY|DATABASE|DOCKER)"

# Test specific variable
echo $OPENAI_API_KEY

# Set temporarily
OPENAI_API_KEY=test-key jbang integration-tests/RunModule.java
```

## 高级故障排查

### 日志分析

**Spring Boot 日志位置**
```bash
integration-testing/logs/integration-tests/module-spring-boot-timestamp.log
```

**测试执行日志位置**
```bash
integration-testing/logs/background-runs/test_module_timestamp.log
```

**日志分析命令**
```bash
# Find errors in Spring Boot log
grep -E "(ERROR|WARN|Exception)" spring-boot.log

# Check startup sequence
grep -E "(Starting|Started)" spring-boot.log

# Find specific patterns
grep -E "your-pattern" spring-boot.log
```

### 性能问题

**症状**
- 测试频繁超时
- 应用启动缓慢
- 测试期间出现内存问题

**解决方案**
```bash
# Increase timeout for specific test
# Edit ExampleInfo.json: "timeoutSec": 600

# Check system resources
free -h
df -h

# Limit parallel execution
# Currently tests run sequentially to avoid port conflicts
```

### AI 校验调试

**检查 AI 校验配置**
```json
{
  "aiValidation": {
    "enabled": true,
    "validationMode": "primary|hybrid|fallback",
    "expectedBehavior": "Clear description of what should happen"
  }
}
```

**AI 校验成本监控**
- 每次校验约花费 $0.002（400 tokens）
- 如果频繁运行，请监控用量
- 对可预测的输出考虑使用正则

## 预防性最佳实践

### 1. 创建新测试之前
- 使用 `integration-testing/learnings/templates/` 中合适的配置模板
- 提交前先在本地测试
- 核实所有环境变量均已记录
- 检查超时是否与应用复杂度匹配

### 2. 定期维护
- 每月运行完整测试套件：`./integration-testing/scripts/rit-direct.sh`
- 应用变化时更新成功模式
- 审查并更新环境变量要求
- 监控 AI 校验成本

### 3. 代码评审清单
- [ ] 配置遵循既定模板
- [ ] 超时与应用类型匹配
- [ ] 所有环境变量均已记录
- [ ] 成功模式与实际应用输出匹配
- [ ] AI 校验模式与应用类型匹配
- [ ] 提交前测试已在本地通过

## 获取帮助

1. **查阅本指南**以排查常见问题
2. **查看 `integration-testing/logs/` 中的日志**
3. **手动运行测试**：`./mvnw spring-boot:run`
4. **对照 `integration-testing/learnings/templates/` 中的模板检查配置**
5. **运行单个测试**：`jbang integration-tests/RunModule.java`

对于本指南未涵盖的问题，请查阅 `integration-testing/learnings/` 中的经验文档，以获取详细的技术洞见与模式。