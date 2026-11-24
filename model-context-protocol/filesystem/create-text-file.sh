#!/bin/bash

# 获取脚本所在目录
SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

# 定义脚本目录下的文件路径
FILE_PATH="$SCRIPT_DIR/target/spring-ai-mcp-overview.txt"

echo "Creating a text file in $FILE_PATH..."

# 创建文件
cat << 'EOF' > "$FILE_PATH"

Model Context Protocol (MCP) Java SDK

A Java implementation of the Model Context Protocol specification, providing both synchronous and 
asynchronous clients for MCP server interactions.

Overview: This SDK implements the Model Context Protocol, enabling seamless integration with AI models and tools through 
a standardized interface. It supports both synchronous and asynchronous communication patterns, making it suitable for 
various use cases and integration scenarios.

Features

 Synchronous and Asynchronous client implementations
 Standard MCP operations support:
	 Tool discovery and execution
	 Resource management and templates
	 Prompt handling and management
	 Resource subscription system
	 Server initialization and ping
 Stdio-based server transport
 Reactive programming support using Project Reactor

EOF

echo "The file has been created in $FILE_PATH."
