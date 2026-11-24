#!/usr/bin/env bash
# 打开 subscriptions/listen 并打印事件，直到中断。在另一个 shell 中运行：
#   curl -X POST http://localhost:8080/admin/tools/hello
exec "$(dirname "$0")/mcp.sh" subscriptions/listen '{"notifications":{"toolsListChanged":true}}'
