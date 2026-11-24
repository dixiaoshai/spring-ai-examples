# JBang 集成测试模式 - 标准指南

## 概览

本文档确立了在 Spring AI Examples 仓库中创建 JBang 集成测试脚本的标准模式。自第 3a.6 阶段起，所有 JBang 脚本都必须使用集中式工具类，以消除代码重复。

## 标准模式

### 新 JBang 脚本的模板

```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.zeroturnaround:zt-exec:1.12
//DEPS com.fasterxml.jackson.core:jackson-databind:2.17.1
//JAVA 17
//FILES ExampleInfo.json
//SOURCES [RELATIVE_PATH]/integration-testing/jbang-lib/IntegrationTestUtils.java

/*
 * Integration test launcher for [MODULE_NAME]
 * Uses centralized utilities from IntegrationTestUtils
 */

public class Run[ModuleName] {
    
    public static void main(String... args) throws Exception {
        IntegrationTestUtils.runIntegrationTest("[module-name]");
    }
}
```

## 关键组成部分

### 1. JBang 头
- 必须包含准确的依赖（zt-exec 与 jackson-databind）
- Java 17 是标准版本
- 始终将 `ExampleInfo.json` 作为文件依赖包含进来

### 2. SOURCES 指令
`IntegrationTestUtils.java` 的路径因模块深度而异：

```
Module at: kotlin/kotlin-hello-world (2 levels deep)
Use: //SOURCES ../../../integration-testing/jbang-lib/IntegrationTestUtils.java

Module at: models/chat/helloworld (3 levels deep)  
Use: //SOURCES ../../../../integration-testing/jbang-lib/IntegrationTestUtils.java

Module at: model-context-protocol/client/starter/example (4 levels deep)
Use: //SOURCES ../../../../../integration-testing/jbang-lib/IntegrationTestUtils.java
```

### 3. 类命名约定
- 类名：`Run[ModuleName]`（PascalCase）
- 文件名：`Run[ModuleName].java`
- 示例：
  - `RunHelloworld.java`
  - `RunKotlinHelloWorld.java`
  - `RunStarterDefaultClient.java`

### 4. 模块名参数
将模块名传给 `runIntegrationTest()`：
- 使用目录名或描述性标识符
- 用于日志记录与进度报告
- 示例：
  - `"helloworld"`
  - `"kotlin-hello-world"`
  - `"starter-default-client"`

## 不应做的事项

### ❌ 不要复制旧模式
```java
// DON'T DO THIS - Old pattern with inline logic
public class RunExample {
    public static void main(String... args) throws Exception {
        // 100+ lines of test logic here
        // Building, running, verifying, etc.
    }
}
```

### ❌ 不要修改单个脚本
- 不要向单个测试脚本添加自定义逻辑
- 不要为特定测试覆盖行为
- 所有定制都应放在 `ExampleInfo.json` 中

### ❌ 不要使用错误路径
```java
// WRONG - Missing '../' levels
//SOURCES integration-testing/jbang-lib/IntegrationTestUtils.java

// WRONG - Absolute path
//SOURCES /home/user/projects/spring-ai-examples/integration-testing/jbang-lib/IntegrationTestUtils.java
```

## 该模式的优势

1. **一致性**：所有测试行为完全一致
2. **可维护性**：只需在一处修复缺陷
3. **简单性**：创建新测试非常容易
4. **可靠性**：经过验证的测试执行逻辑
5. **可调试性**：标准化的日志与错误报告

## 创建新的集成测试

### 第 1 步：创建目录
```bash
mkdir -p your-module/integration-tests
```

### 第 2 步：创建配置
```bash
cat > your-module/integration-tests/ExampleInfo.json << 'EOF'
{
  "timeoutSec": 300,
  "successRegex": [
    "BUILD SUCCESS",
    "Started.*Application"
  ],
  "requiredEnv": ["OPENAI_API_KEY"]
}
EOF
```

### 第 3 步：创建 JBang 脚本
1. 计算模块的目录深度
2. 使用上面的模板
3. 将 `[RELATIVE_PATH]` 替换为正确数量的 `../`
4. 适当替换 `[MODULE_NAME]` 与 `[ModuleName]`
5. 保存为 `RunYourModule.java`

### 第 4 步：本地测试
```bash
cd your-module/integration-tests
jbang RunYourModule.java
```

## 故障排查

### 找不到 “IntegrationTestUtils.java”
- 计算模块的目录深度
- 添加正确数量的 `../` 以到达仓库根目录
- 然后追加 `integration-testing/jbang-lib/`

### 找不到符号 “IntegrationTestUtils”
- 确认 SOURCES 指令存在
- 检查路径是否正确
- 确保类名中没有拼写错误

### 行为与预期不符
- 检查 `ExampleInfo.json` 配置
- 所有行为定制都通过配置完成
- 不要修改 JBang 脚本本身

## 迁移指南

更新旧脚本时：

1. **备份**旧脚本（供参考）
2. 使用模板**创建**新脚本
3. **核实** `ExampleInfo.json` 中的配置
4. 在本地**测试**以确保其可用
5. 确认可用后**删除**备份

## 未来增强

所有对集成测试的增强都应在 `IntegrationTestUtils.java` 中进行：
- 新功能
- 缺陷修复
- 性能改进
- 额外的校验

各个 JBang 脚本应保持精简且不做改动。

## 总结

集中式 JBang 模式对所有新的集成测试都是强制要求。这可确保整个测试套件的一致性、可维护性与可靠性。如有疑问，可参考 `RunHelloworld.java` 等现有示例。