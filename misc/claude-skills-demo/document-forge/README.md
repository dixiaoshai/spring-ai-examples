# Document Forge

**基于 Spring AI 构建的 AI 驱动文档创作工作室**

*"描述它，下载它。"*

## 概述

Document Forge 是一个演示应用，展示 [Claude Skills](https://platform.claude.com/docs/en/agents-and-tools/agent-skills/overview) 与 [Spring AI](https://docs.spring.io/spring-ai/reference/api/chat/anthropic-chat.html#_skills) 的集成。
用自然语言描述文档，即可获得由 Claude 生成的真实、可下载的文件。

### 支持的文档类型

| 类型 | 扩展名 | 说明 |
|------|-----------|-------------|
| Excel | `.xlsx` | 包含数据、公式和格式的电子表格 |
| PowerPoint | `.pptx` | 包含幻灯片和版式的演示文稿 |
| Word | `.docx` | 带格式和结构的文档 |
| PDF | `.pdf` | 便携式文档格式 |

## 截图

![Main Form](screenshots/01-main-form.png)
*选择文档类型并描述你想要的内容*

![Generating](screenshots/02-generating.png)
*Claude 使用 Skills 生成你的文档*

![Result](screenshots/03-result.png)
*下载生成的文档*

## 快速开始

### 前置条件

- Java 21+
- Maven 3.9+
- Anthropic API 密钥

### 运行

```bash
export ANTHROPIC_API_KEY=your-api-key-here
./mvnw spring-boot:run
```

打开 http://localhost:8080

## 特性

- **自然语言输入** - 用普通英文描述你想要的内容
- **真实文件生成** - 获得真正可下载的文件，而不只是文字描述
- **源文档上传** - 上传 PDF、TXT、CSV、JSON、XML 或 MD 文件，供 Claude 转换
- **自定义 Skills 支持** - 通过 Anthropic 自定义 skills 应用自定义品牌或格式
- **历史侧边栏** - 查看历史生成记录并重新下载文件
- **异步生成** - PowerPoint 演示文稿在后台运行，完成后发出通知

## 自定义 Skills 集成

Document Forge 演示如何将预构建的 Anthropic skills 与自定义 skills 结合使用。
`custom-skills/watermark/SKILL.md` 中包含一个示例自定义 skill。

### 设置自定义 Skill

1. **将 skill 上传到 Anthropic：**

```bash
cd custom-skills/watermark

curl -X POST "https://api.anthropic.com/v1/skills" \
  -H "x-api-key: $ANTHROPIC_API_KEY" \
  -H "anthropic-version: 2023-06-01" \
  -H "anthropic-beta: skills-2025-10-02" \
  -F "display_title=Document Watermark" \
  -F "files[]=@SKILL.md;filename=document-watermark/SKILL.md"
```

2. **从响应中复制 skill ID**（例如 `skill_01AbCdEfGhIjKlMnOpQrStUv`）

3. **设置环境变量：**

```bash
export CUSTOM_SKILL_ID=skill_01AbCdEfGhIjKlMnOpQrStUv
```

4. **运行应用** - UI 中现在会显示 "Apply custom branding" 复选框

### 不使用自定义 Skills

如果未设置 `CUSTOM_SKILL_ID`，应用仍可正常工作，只是仅使用预构建的 skills。
自定义品牌复选框不会显示。

更多细节请参阅 [Spring AI 自定义 Skills 文档](https://docs.spring.io/spring-ai/reference/api/chat/anthropic-chat.html#_custom_skills)。

## 用法示例

**Excel - 销售报告**
> "为 2024 年第四季度创建一份销售报告，包含月度收入、支出和利润列。"

**PowerPoint - 融资演示**
> "为一家名为 'GreenTech' 的初创公司创建一份 5 页幻灯片，该公司生产可持续包装。"

**Word - 求职信**
> "为软件工程师职位撰写一封专业的求职信。候选人拥有 5 年 Java 和 Spring Boot 经验。"

**PDF - 发票**
> "为 40 小时的咨询工作创建一张发票，每小时 150 美元，采用标准付款条款。"

**转换文档**

上传一份会议记录文本文件并输入提示词：
> "创建一个行动项跟踪表格，包含任务、负责人、截止日期和状态列。"

## 技术栈

| 层级 | 技术 |
|-------|------------|
| 后端 | Spring Boot 4.1 |
| AI | Spring AI 2.1.0-SNAPSHOT + Anthropic Claude Skills |
| 模板 | Thymeleaf |
| 交互 | HTMX |
| 样式 | Tailwind CSS |

## 配置

```yaml
spring:
  ai:
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
      chat:
        options:
          model: claude-sonnet-4-5-20250514
```

## 工作原理

```
User Prompt → Spring AI → Claude Skills → File Generated → Download
```

1. 用户选择文档类型并输入自然语言提示词
2. Spring AI 携带已启用的相应 skill 将请求发送给 Claude
3. 可选地，添加自定义 skill 用于品牌或格式
4. Claude 使用代码执行能力生成实际的文档文件
5. `AnthropicSkillsResponseHelper` 从响应中提取文件 ID
6. 文件通过 Anthropic 的 Files API 下载并提供给用户

---

基于 Spring AI 构建。
