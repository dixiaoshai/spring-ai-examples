#!/usr/bin/env python3
"""
Claude Code Wrapper - Robust utility for Claude Code CLI integration

Provides a reliable interface for calling Claude Code from Python scripts with proper
error handling, file I/O, and debugging capabilities.
"""

import subprocess
import os
import tempfile
import time
from datetime import datetime
from pathlib import Path
from typing import Dict, Any, Optional
from animated_progress import AnimatedProgress

class ClaudeCodeWrapper:
    """Robust wrapper for Claude Code CLI interactions"""
    
    def __init__(self, claude_binary_path: str = None, logs_dir: Path = None):
        if claude_binary_path is None:
            # 尝试在 PATH 中查找 Claude Code
            try:
                result = subprocess.run(['which', 'claude'], capture_output=True, text=True, check=True)
                self.claude_binary_path = result.stdout.strip()
            except subprocess.CalledProcessError:
                # 退而使用 'claude'，由系统自行查找
                self.claude_binary_path = 'claude'
        else:
            self.claude_binary_path = claude_binary_path
        
        # 设置日志目录以便调试
        if logs_dir is None:
            # 默认使用当前工作目录下的 logs
            self.logs_dir = Path.cwd() / "logs"
        else:
            self.logs_dir = Path(logs_dir)
        
        # 确保日志目录存在
        self.logs_dir.mkdir(parents=True, exist_ok=True)
    
    def is_available(self) -> bool:
        """Check if Claude Code is available"""
        try:
            # 直接使用 PATH 中的 'claude' 命令
            result = subprocess.run(['claude', '--version'],
                         capture_output=True, check=True, timeout=10)
            return True
        except (subprocess.CalledProcessError, FileNotFoundError, subprocess.TimeoutExpired) as e:
            # 调试：输出实际异常以便排查
            print(f"DEBUG: Claude Code availability check failed: {type(e).__name__}: {e}")
            if hasattr(e, 'stdout') and e.stdout:
                print(f"DEBUG: stdout: {e.stdout}")
            if hasattr(e, 'stderr') and e.stderr:
                print(f"DEBUG: stderr: {e.stderr}")
            return False
        except Exception as e:
            # 捕获其他任何意外异常
            print(f"DEBUG: Unexpected exception in is_available(): {type(e).__name__}: {e}")
            return False
    
    def get_version(self) -> Optional[str]:
        """Get Claude Code version"""
        try:
            result = subprocess.run(['claude', '--version'],
                                  capture_output=True, text=True, check=True, timeout=10)
            return result.stdout.strip()
        except Exception:
            return None
    
    def analyze_from_file(self, prompt_file_path: str, output_file_path: Optional[str] = None, 
                         timeout: int = 300, use_json_output: bool = True, show_progress: bool = True, quiet: bool = False) -> Dict[str, Any]:
        """
        Analyze using Claude Code by having it read the prompt file directly
        
        Args:
            prompt_file_path: Path to file containing the prompt
            output_file_path: Optional path to save the response
            timeout: Timeout in seconds (default 300 = 5 minutes)
            use_json_output: Use --output-format json for structured responses
            show_progress: Show animated progress during execution (default True)
            quiet: Suppress logging output for cleaner JSON parsing (default False)
            
        Returns:
            Dict containing success status, response, error info, etc.
        """
        if not self.is_available():
            return {
                'success': False,
                'error': 'Claude Code is not available',
                'stderr': None,
                'response': None
            }
        
        prompt_path = Path(prompt_file_path)
        if not prompt_path.exists():
            return {
                'success': False,
                'error': f'Prompt file not found: {prompt_file_path}',
                'stderr': None,
                'response': None
            }
        
        try:
            # 构建命令，可选 JSON 输出格式并跳过权限检查
            cmd = ['claude', '-p', '--dangerously-skip-permissions']
            if not quiet:
                cmd.append('--verbose')
            if use_json_output:
                cmd.extend(['--output-format', 'json'])
            
            # 让 Claude Code 直接读取并分析该文件
            file_prompt = f"Please read the file {prompt_path.absolute()} and follow the instructions contained within it."
            cmd.append(file_prompt)
            
            # 记录提示词大小并估算 token（仅在非静默模式下）
            if not quiet:
                try:
                    prompt_size = prompt_path.stat().st_size
                    prompt_size_kb = prompt_size / 1024
                    # Token 估算：每个 token 约 5 字节，或每 KB 约 200 tokens
                    estimated_tokens_method1 = prompt_size / 5  # 每个 token 约 5 字节
                    estimated_tokens_method2 = prompt_size_kb * 200  # 每 KB 约 200 tokens
                    
                    # 同时输出到 stdout 并加入响应，以便调试
                    import logging
                    logging.basicConfig(level=logging.INFO)
                    logger = logging.getLogger(__name__)
                    
                    logger.info(f"🔍 Claude Code prompt file: {prompt_path}")
                    logger.info(f"🔍 Prompt size: {prompt_size:,} bytes ({prompt_size_kb:.1f}KB)")
                    logger.info(f"🔍 Estimated tokens (5 bytes/token): {estimated_tokens_method1:,.0f}")
                    logger.info(f"🔍 Estimated tokens (200/KB): {estimated_tokens_method2:,.0f}")
                    
                    # 提示是否可能引发问题（Claude 支持约 200K tokens）
                    if prompt_size_kb > 400:  # 按每 KB 200 tokens 计约 80K tokens
                        logger.warning(f"⚠️  Large prompt detected - may cause Claude Code processing issues")
                    if estimated_tokens_method1 > 150000:  # 接近 200K 上限
                        logger.warning(f"⚠️  Prompt approaching Claude's 200K token limit")
                        
                except Exception as e:
                    print(f"🔍 Could not get prompt size: {e}")
            
            # 调用 Claude Code，并可显示进度动画
            try:
                start_time = time.time()
                if not quiet:
                    logger.info(f"🔍 Starting Claude Code execution...")
                
                # 如需要则启动进度动画
                progress = None
                if show_progress and not quiet:
                    progress = AnimatedProgress("🧠 Claude Code analyzing")
                    progress.start()
                
                try:
                    # 使用 Popen 进行非阻塞执行，以便展示进度
                    process = subprocess.Popen(
                        cmd,
                        stdout=subprocess.PIPE,
                        stderr=subprocess.PIPE,
                        text=True
                    )
                    
                    # 带超时等待完成
                    stdout, stderr = process.communicate(timeout=timeout)
                    returncode = process.returncode
                    
                finally:
                    # 始终停止进度动画
                    if progress:
                        progress.stop()
                
                end_time = time.time()
                duration = end_time - start_time
                if not quiet:
                    logger.info(f"🔍 Claude Code completed in {duration:.1f} seconds")
                
                # 检查命令是否成功
                if returncode != 0:
                    raise subprocess.CalledProcessError(returncode, cmd, stdout, stderr)
                
            except subprocess.TimeoutExpired:
                return {
                    'success': False,
                    'error': f'Claude Code analysis timed out after {timeout} seconds',
                    'stderr': None,
                    'response': None
                }
            except subprocess.CalledProcessError as e:
                return {
                    'success': False,
                    'error': f'Claude Code failed with return code {e.returncode}',
                    'stderr': e.stderr,
                    'response': None
                }
            
            # 如需要则保存输出
            if output_file_path:
                output_path = Path(output_file_path)
                output_path.parent.mkdir(parents=True, exist_ok=True)
                with open(output_path, 'w', encoding='utf-8') as f:
                    f.write(f"Return code: {returncode}\n")
                    f.write(f"Stdout length: {len(stdout)}\n")
                    f.write(f"Stderr: {stderr or 'None'}\n")
                    f.write(f"Response:\n{stdout}")
            
            # 根据输出格式处理响应
            if use_json_output:
                # 从 Claude Code 的 JSON 响应中提取实际内容
                try:
                    import json
                    claude_response = json.loads(stdout)
                    
                    # 检查这是否是错误响应
                    if isinstance(claude_response, dict) and claude_response.get('subtype') == 'error_during_execution':
                        return {
                            'success': False,
                            'error': 'Claude Code execution error - likely timeout or processing failure',
                            'stderr': stderr,
                            'response': None
                        }
                    
                    # 从 Claude Code 的 JSON 响应中提取实际内容与成本信息
                    actual_content = None
                    cost_info = {}
                    
                    try:
                        # 调试日志，用于查看当前处理的数据
                        import logging
                        logger = logging.getLogger(__name__)
                        logger.info(f"Claude response type: {type(claude_response)}")
                        if isinstance(claude_response, list):
                            logger.info(f"List length: {len(claude_response)}")
                            if len(claude_response) > 0:
                                logger.info(f"First item type: {type(claude_response[0])}")
                        
                        # 处理数组格式（对话消息）
                        if isinstance(claude_response, list):
                            # 查找最后一条带内容的 assistant 消息
                            for i, message in enumerate(reversed(claude_response)):
                                logger.info(f"Processing message {i}: type={type(message)}")
                                if not isinstance(message, dict):
                                    continue
                                logger.info(f"Message keys: {message.keys()}")
                                if message.get('type') == 'assistant':
                                    msg_data = message.get('message', {})
                                    logger.info(f"Message data type: {type(msg_data)}")
                                    if isinstance(msg_data, dict) and 'content' in msg_data:
                                        content_list = msg_data['content']
                                        logger.info(f"Content list type: {type(content_list)}")
                                        if isinstance(content_list, list) and len(content_list) > 0:
                                            # 从最后一个内容项中获取文本内容
                                            last_content = content_list[-1]
                                            logger.info(f"Last content type: {type(last_content)}")
                                            if isinstance(last_content, dict) and 'text' in last_content:
                                                actual_content = last_content['text']
                                                logger.info(f"Found content: {actual_content[:100] if actual_content else 'None'}...")
                                                break
                                                
                                # 从 usage 元数据中提取成本信息
                                elif message.get('type') == 'usage':
                                    usage_data = message.get('usage', {})
                                    if usage_data:
                                        cost_info.update({
                                            'input_tokens': usage_data.get('input_tokens'),
                                            'output_tokens': usage_data.get('output_tokens'),
                                            'total_tokens': usage_data.get('input_tokens', 0) + usage_data.get('output_tokens', 0),
                                            'cache_creation_input_tokens': usage_data.get('cache_creation_input_tokens'),
                                            'cache_read_input_tokens': usage_data.get('cache_read_input_tokens')
                                        })
                                        logger.info(f"Found usage info: {cost_info}")
                        
                        # 处理单个对象格式
                        elif isinstance(claude_response, dict):
                            if 'result' in claude_response:
                                actual_content = claude_response['result']
                            elif 'content' in claude_response:
                                actual_content = claude_response['content']
                            elif 'response' in claude_response:
                                actual_content = claude_response['response']
                            
                            # 从单个对象格式中提取 usage/成本信息
                            if 'usage' in claude_response:
                                usage_data = claude_response['usage']
                                cost_info.update({
                                    'input_tokens': usage_data.get('input_tokens'),
                                    'output_tokens': usage_data.get('output_tokens'),
                                    'total_tokens': usage_data.get('input_tokens', 0) + usage_data.get('output_tokens', 0),
                                    'cache_creation_input_tokens': usage_data.get('cache_creation_input_tokens'),
                                    'cache_read_input_tokens': usage_data.get('cache_read_input_tokens')
                                })
                                logger.info(f"Found usage info in single object: {cost_info}")
                        
                    except Exception as parse_error:
                        # 记录解析错误，但继续使用兜底方案
                        import logging
                        logger = logging.getLogger(__name__)
                        logger.error(f"Error parsing Claude Code JSON response: {parse_error}")
                        import traceback
                        logger.error(f"Traceback: {traceback.format_exc()}")
                    
                    # 如果无法提取内容，则将原始 JSON 作为字符串兜底
                    if actual_content is None:
                        actual_content = json.dumps(claude_response) if isinstance(claude_response, (dict, list)) else stdout
                    
                    # 将执行耗时加入成本信息
                    cost_info['duration_seconds'] = duration
                    
                    return {
                        'success': True,
                        'response': actual_content,
                        'output_file': output_file_path,
                        'stderr': stderr,
                        'error': None,
                        'cost_info': cost_info if cost_info else None
                    }
                    
                except json.JSONDecodeError:
                    # 如果不是合法 JSON，则原样返回（可能是纯文本响应）
                    return {
                        'success': True,
                        'response': stdout,
                        'output_file': output_file_path,
                        'stderr': stderr,
                        'error': None,
                        'cost_info': {'duration_seconds': duration}
                    }
            else:
                # 纯文本响应
                return {
                    'success': True,
                    'response': stdout,
                    'output_file': output_file_path,
                    'stderr': stderr,
                    'error': None,
                    'cost_info': {'duration_seconds': duration}
                }
            
        except Exception as e:
            stderr_val = stderr if 'stderr' in locals() else None
            return {
                'success': False,
                'error': f'Unexpected error: {str(e)}',
                'stderr': stderr_val,
                'response': None
            }
    
    def extract_json_from_response(self, text_content: str) -> Optional[Dict[str, Any]]:
        """
        Extract JSON from Claude Code response text using multiple fallback strategies.
        
        This centralizes all JSON extraction logic that was previously duplicated
        across backport_assessor.py, solution_assessor.py, ai_conversation_analyzer.py, etc.
        
        Args:
            text_content: Raw text content from Claude Code response
            
        Returns:
            Parsed JSON dict if successful, None if extraction fails
        """
        import re
        import json
        import logging
        
        logger = logging.getLogger(__name__)
        
        if not text_content:
            logger.warning("🔍 extract_json_from_response: Empty text content provided")
            return None
        
        logger.info(f"🔍 Extracting JSON from response ({len(text_content)} chars)")
        
        # 策略 1：尝试从 markdown 代码块中提取 JSON
        try:
            json_pattern = r'```json\s*\n(.*?)\n```'
            json_match = re.search(json_pattern, text_content, re.DOTALL)
            
            if json_match:
                json_str = json_match.group(1).strip()
                logger.info(f"🔍 Found JSON in markdown code block ({len(json_str)} chars)")
                
                try:
                    parsed_json = json.loads(json_str)
                    logger.info("✅ Successfully parsed JSON from markdown code block")
                    return parsed_json
                except json.JSONDecodeError as e:
                    logger.warning(f"⚠️  JSON code block parse error: {e}")
                    # 继续尝试下一种策略
        except Exception as e:
            logger.warning(f"⚠️  Markdown code block extraction failed: {e}")
        
        # 策略 2：尝试直接解析 JSON（查找 { ... } 块）
        try:
            logger.info("🔍 Trying direct JSON parsing from response...")
            lines = text_content.split('\n')
            json_lines = []
            in_json = False
            brace_count = 0
            
            for line in lines:
                line_stripped = line.strip()
                
                # 开始检测 JSON 块
                if line_stripped.startswith('{') and not in_json:
                    in_json = True
                    brace_count = 0
                    json_lines = [line]  # 重置并重新开始
                elif in_json:
                    json_lines.append(line)
                
                # 统计花括号数量以处理嵌套对象
                if in_json:
                    brace_count += line.count('{') - line.count('}')
                    
                    # JSON 块结束
                    if brace_count == 0 and line_stripped.endswith('}'):
                        break
            
            if json_lines and in_json:
                json_str = '\n'.join(json_lines).strip()
                logger.info(f"🔍 Found direct JSON block ({len(json_str)} chars)")
                
                try:
                    parsed_json = json.loads(json_str)
                    logger.info("✅ Successfully parsed JSON from direct parsing")
                    return parsed_json
                except json.JSONDecodeError as e:
                    logger.warning(f"⚠️  Direct JSON parse error: {e}")
                    logger.warning(f"⚠️  Problematic JSON preview: {json_str[:200]}...")
                    # 继续尝试下一种策略
        except Exception as e:
            logger.warning(f"⚠️  Direct JSON parsing failed: {e}")
        
        # 策略 3：使用简单的括号匹配查找 JSON
        try:
            logger.info("🔍 Trying bracket-based JSON extraction...")
            
            # 查找第一个 { 与最后一个 }
            first_brace = text_content.find('{')
            last_brace = text_content.rfind('}')
            
            if first_brace != -1 and last_brace != -1 and last_brace > first_brace:
                json_str = text_content[first_brace:last_brace+1].strip()
                logger.info(f"🔍 Found bracket-based JSON ({len(json_str)} chars)")
                
                try:
                    parsed_json = json.loads(json_str)
                    logger.info("✅ Successfully parsed JSON using bracket matching")
                    return parsed_json
                except json.JSONDecodeError as e:
                    logger.warning(f"⚠️  Bracket-based JSON parse error: {e}")
        except Exception as e:
            logger.warning(f"⚠️  Bracket-based JSON extraction failed: {e}")
        
        # 所有策略均失败
        logger.error("❌ All JSON extraction strategies failed")
        logger.error(f"❌ Text preview: {text_content[:500]}...")
        return None
    
    def analyze_from_file_with_json(self, prompt_file_path: str, output_file_path: Optional[str] = None,
                                   timeout: int = 300, show_progress: bool = True, quiet: bool = False) -> Dict[str, Any]:
        """
        Analyze from file and automatically extract JSON from the response.
        
        This is a convenience method that combines analyze_from_file() with JSON extraction.
        
        Args:
            prompt_file_path: Path to the prompt file
            output_file_path: Optional path to save the response
            timeout: Timeout in seconds
            show_progress: Show animated progress
            quiet: Suppress logging output for cleaner JSON parsing
            
        Returns:
            Dict with keys: success, response (raw text), json_data (parsed), error, etc.
        """
        # 使用 JSON 输出模式获取原始响应
        result = self.analyze_from_file(
            prompt_file_path=prompt_file_path,
            output_file_path=output_file_path,
            timeout=timeout,
            use_json_output=True,  # 强制 JSON 模式以便更好地解析
            show_progress=show_progress,
            quiet=quiet
        )
        
        # 如果调用失败，则原样返回
        if not result.get('success'):
            return result
        
        # 从响应中提取 JSON
        raw_response = result.get('response', '')
        json_data = self.extract_json_from_response(raw_response)
        
        # 将 JSON 数据加入结果
        result['json_data'] = json_data
        result['json_extraction_success'] = json_data is not None
        
        return result
    
    def analyze_from_text_with_json(self, prompt_text: str, output_file_path: Optional[str] = None,
                                   timeout: int = 300, show_progress: bool = True, quiet: bool = False) -> Dict[str, Any]:
        """
        Analyze from text and automatically extract JSON from the response.
        
        This is a convenience method that combines analyze_from_text() with JSON extraction.
        
        Args:
            prompt_text: The prompt text to analyze
            output_file_path: Optional path to save the response
            timeout: Timeout in seconds
            show_progress: Show animated progress
            quiet: Suppress logging output for cleaner JSON parsing
            
        Returns:
            Dict with keys: success, response (raw text), json_data (parsed), error, etc.
        """
        # 使用 JSON 输出模式获取原始响应
        result = self.analyze_from_text(
            prompt_text=prompt_text,
            output_file_path=output_file_path,
            timeout=timeout,
            use_json_output=True,  # 强制 JSON 模式以便更好地解析
            show_progress=show_progress,
            quiet=quiet
        )
        
        # 如果调用失败，则原样返回
        if not result.get('success'):
            return result
        
        # 从响应中提取 JSON
        raw_response = result.get('response', '')
        json_data = self.extract_json_from_response(raw_response)
        
        # 将 JSON 数据加入结果
        result['json_data'] = json_data
        result['json_extraction_success'] = json_data is not None
        
        return result
    
    def analyze_from_text(self, prompt_text: str, output_file_path: Optional[str] = None,
                         timeout: int = 300, use_json_output: bool = True, show_progress: bool = True, quiet: bool = False) -> Dict[str, Any]:
        """
        Analyze using Claude Code with prompt text directly
        
        Args:
            prompt_text: The prompt text to analyze
            output_file_path: Optional path to save the response
            timeout: Timeout in seconds (default 300 = 5 minutes)
            use_json_output: Use --output-format json for structured responses
            show_progress: Show animated progress during execution (default True)
            quiet: Suppress logging output for cleaner JSON parsing (default False)
            
        Returns:
            Dict containing success status, response, error info, etc.
        """
        # 创建持久化的提示词文件以便调试
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        prompt_file_path = self.logs_dir / f"claude-prompt-{timestamp}.md"
        
        with open(prompt_file_path, 'w', encoding='utf-8') as f:
            f.write(prompt_text)
        
        temp_file_path = str(prompt_file_path)
        
        try:
            # 使用基于文件的分析
            result = self.analyze_from_file(temp_file_path, output_file_path, timeout, use_json_output, show_progress, quiet)
            # 保留提示词文件以便调试 - 不要删除
            return result
        except Exception as e:
            # 记录错误，但保留提示词文件以便调试
            import logging
            logger = logging.getLogger(__name__)
            logger.error(f"Error in analyze_from_text, prompt saved to: {prompt_file_path}")
            raise


# 直接运行时的测试函数
if __name__ == "__main__":
    """Test the ClaudeCodeWrapper with the saved prompt"""
    claude = ClaudeCodeWrapper()
    
    print("Claude Code Wrapper Test")
    print("=" * 40)
    
    if claude.is_available():
        version = claude.get_version()
        print(f"✅ Claude Code available: {version}")
        
        # 使用已保存的提示词进行测试
        prompt_file = '/home/mark/project-mgmt/spring-ai-project-mgmt/pr-review/logs/claude-prompt-ai-analyzer.txt'
        output_file = '/home/mark/project-mgmt/spring-ai-project-mgmt/pr-review/logs/claude-response-wrapper-test.txt'
        
        if Path(prompt_file).exists():
            print(f"🧪 Testing with prompt file: {prompt_file}")
            result = claude.analyze_from_file(prompt_file, output_file)
            
            if result['success']:
                print("✅ Analysis completed successfully!")
                print(f"📁 Output saved to: {result['output_file']}")
                print(f"📊 Response length: {len(result['response'])} chars")
                print(f"🔍 Response preview: {result['response'][:200]}...")
            else:
                print(f"❌ Error: {result['error']}")
                if result.get('stderr'):
                    print(f"stderr: {result['stderr']}")
        else:
            print(f"❌ Prompt file not found: {prompt_file}")
    else:
        print("❌ Claude Code is not available")