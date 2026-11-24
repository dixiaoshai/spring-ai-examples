#!/usr/bin/env bash
# 用法：tools-call.sh <tool> '<arguments-json>'   例如 ./tools-call.sh open_ticket '{"title":"Printer offline"}'
# 流式工具会以 text/event-stream 应答；input_required 结果必须用
# inputResponses 和 requestState 重试（循环逻辑参见 ModernMcpDriver）。
TOOL=$1; ARGS=${2:-"{}"}
exec "$(dirname "$0")/mcp.sh" tools/call "{\"name\":\"$TOOL\",\"arguments\":$ARGS}" "$TOOL"
