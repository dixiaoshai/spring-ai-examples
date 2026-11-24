# Spring AI 示例的 AI 校验器

本目录包含用于 Spring AI 集成测试的 AI 驱动校验系统。它使用 Claude Code 智能分析应用日志，判断示例应用是否成功展示了预期功能。

## 组件

- `lib/` - 核心库（claude_code_wrapper.py、animated_progress.py）
- `templates/` - 针对不同示例类型的校验提示词模板
- `validate_example.py` - 主校验脚本（待实现）
- `requirements.txt` - Python 依赖

## 用法

AI 校验器由 Java 集成测试调用，用于校验示例应用：
1. 运行时没有异常
2. 展示了预期功能
3. 多组件示例也能正常工作

## 状态

🚧 **开发中** - 属于 plans/ai-validation-plan.md 中第 1 阶段实现的一部分