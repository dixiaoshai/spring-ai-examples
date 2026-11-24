# ============================================================================
# 预加载 Flow Judge 模型的 Ollama
# ============================================================================
#
# 此镜像包含已预下载 flow-judge 模型的 Ollama，
# 用于不便下载大型模型的 CI/CD 流水线。
#
# 模型：avcodes/flowaicom-flow-judge:q4（约 2.5GB）
# 基础镜像：ollama/ollama:latest
#
# 构建：
#   docker build -t ollama-flow-judge .
#
# 运行：
#   docker run -d -p 11434:11434 ollama-flow-judge
#
# ============================================================================

FROM ollama/ollama:latest

# 设置环境变量
ENV OLLAMA_HOST=0.0.0.0
ENV OLLAMA_MODELS=/root/.ollama/models

# 创建模型目录
RUN mkdir -p /root/.ollama/models

# 在构建期间下载 flow-judge 模型
# 这会后台运行 ollama、拉取模型，然后停止
RUN ollama serve & \
    sleep 5 && \
    ollama pull avcodes/flowaicom-flow-judge:q4 && \
    pkill ollama

# 暴露 Ollama API 端口
EXPOSE 11434

# 默认命令 - 启动 Ollama 服务器
ENTRYPOINT ["/bin/ollama"]
CMD ["serve"]
