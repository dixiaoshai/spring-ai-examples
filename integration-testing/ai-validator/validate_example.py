#!/usr/bin/env python3
"""
AI-powered validation for Spring AI example applications.

This script analyzes logs to determine if example applications ran successfully 
and demonstrated their intended functionality using Claude Code.
"""

import json
import sys
import argparse
from pathlib import Path
from typing import Dict, List, Optional, Any
from dataclasses import dataclass

# 将 lib 目录加入导入路径
sys.path.insert(0, str(Path(__file__).parent / "lib"))

try:
    from claude_code_wrapper import ClaudeCodeWrapper
except ImportError as e:
    print(f"Error importing claude_code_wrapper: {e}")
    print("Make sure claude_code_wrapper.py is in the lib/ directory")
    sys.exit(1)

@dataclass
class ValidationRequest:
    """Configuration for validation request"""
    log_path: str
    example_name: str
    readme_path: Optional[str] = None
    expected_behavior: Optional[str] = None
    components: Optional[List[str]] = None
    prompt_template: str = "example_validation"
    success_criteria: Optional[Dict[str, Any]] = None

@dataclass 
class ValidationResult:
    """Result of validation analysis"""
    success: bool
    confidence: float
    reasoning: str
    components_validated: List[str]
    functionality_demonstrated: List[str]
    issues_found: List[str]
    recommendations: List[str]
    raw_response: str = ""
    cost_info: Optional[Dict[str, Any]] = None
    
    def to_dict(self) -> Dict[str, Any]:
        result = {
            "success": self.success,
            "confidence": self.confidence,
            "reasoning": self.reasoning,
            "components_validated": self.components_validated,
            "functionality_demonstrated": self.functionality_demonstrated,
            "issues_found": self.issues_found,
            "recommendations": self.recommendations
        }
        
        # 如可用则包含成本信息
        if self.cost_info:
            result["cost_info"] = self.cost_info
            
        return result

class SpringAIExampleValidator:
    """AI-powered validator for Spring AI examples"""
    
    def __init__(self, templates_dir: Optional[Path] = None):
        self.templates_dir = templates_dir or Path(__file__).parent / "templates"
        self.claude_wrapper = ClaudeCodeWrapper()
        
        # 核实模板目录存在
        if not self.templates_dir.exists():
            raise FileNotFoundError(f"Templates directory not found: {self.templates_dir}")
    
    def validate_example(self, request: ValidationRequest) -> ValidationResult:
        """
        Validate a Spring AI example application.
        
        Args:
            request: ValidationRequest with all necessary information
            
        Returns:
            ValidationResult with analysis outcome
        """
        try:
            # 加载日志内容
            log_content = self._load_log_file(request.log_path)
            
            # 如提供则加载 README
            readme_content = self._load_readme(request.readme_path) if request.readme_path else ""
            
            # 构建校验提示词
            prompt_content = self._build_validation_prompt(
                request, log_content, readme_content
            )
            
            # 执行 AI 校验
            ai_result = self._execute_claude_validation(prompt_content)
            
            # 解析并返回结果
            return self._parse_validation_result(ai_result, request)
            
        except Exception as e:
            return ValidationResult(
                success=False,
                confidence=0.0,
                reasoning=f"Validation failed due to error: {str(e)}",
                components_validated=[],
                functionality_demonstrated=[],
                issues_found=[f"Validation error: {str(e)}"],
                recommendations=["Check log file path and configuration"],
                cost_info=None
            )
    
    def _load_log_file(self, log_path: str) -> str:
        """Load and return log file content"""
        log_file = Path(log_path)
        if not log_file.exists():
            raise FileNotFoundError(f"Log file not found: {log_path}")
            
        try:
            with open(log_file, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # 截断过大的日志文件，以避免超出 token 限制
            max_chars = 50000  # 约 12-15K tokens
            if len(content) > max_chars:
                truncated_msg = f"\n\n[LOG TRUNCATED - Original size: {len(content)} chars, showing last {max_chars} chars]\n\n"
                content = truncated_msg + content[-max_chars:]
                
            return content
            
        except Exception as e:
            raise Exception(f"Error reading log file {log_path}: {str(e)}")
    
    def _load_readme(self, readme_path: str) -> str:
        """Load README content for context"""
        try:
            readme_file = Path(readme_path)
            if not readme_file.exists():
                return f"README file not found at {readme_path}"
                
            with open(readme_file, 'r', encoding='utf-8') as f:
                content = f.read()
                
            # 提取相关章节（前 2000 个字符足以提供上下文）
            if len(content) > 2000:
                content = content[:2000] + "\n\n[README truncated for brevity...]"
                
            return content
            
        except Exception as e:
            return f"Error reading README: {str(e)}"
    
    def _build_validation_prompt(
        self, 
        request: ValidationRequest, 
        log_content: str, 
        readme_content: str
    ) -> str:
        """Build validation prompt from template"""
        
        # 加载模板
        template_file = self.templates_dir / f"{request.prompt_template}.md"
        if not template_file.exists():
            template_file = self.templates_dir / "example_validation.md"
            
        try:
            with open(template_file, 'r', encoding='utf-8') as f:
                template = f.read()
        except Exception as e:
            raise Exception(f"Error loading template {template_file}: {str(e)}")
        
        # 准备模板变量
        components_str = ", ".join(request.components) if request.components else "Single component"
        expected_behavior = request.expected_behavior or "Demonstrate Spring AI functionality as described in README"
        readme_excerpt = readme_content[:1000] + "..." if len(readme_content) > 1000 else readme_content
        
        # 替换模板变量
        prompt_content = template.format(
            example_name=request.example_name,
            components=components_str,
            expected_behavior=expected_behavior,
            readme_excerpt=readme_excerpt,
            log_content=log_content
        )
        
        return prompt_content
    
    def _execute_claude_validation(self, prompt_content: str) -> Dict[str, Any]:
        """Execute Claude Code validation"""
        try:
            # 使用 Claude Code 封装以静默模式分析提示词，便于得到干净的 JSON
            result = self.claude_wrapper.analyze_from_text_with_json(
                prompt_text=prompt_content,
                timeout=120,  # 2 分钟应该足够
                show_progress=True,
                quiet=True  # 抑制日志输出，以便更干净地解析 JSON
            )
            
            if not result.get('success'):
                error_msg = result.get('error', 'Unknown error')
                raise Exception(f"Claude Code analysis failed: {error_msg}")
                
            return result
            
        except Exception as e:
            raise Exception(f"Claude Code execution failed: {str(e)}")
    
    def _parse_validation_result(
        self, 
        ai_result: Dict[str, Any], 
        request: ValidationRequest
    ) -> ValidationResult:
        """Parse AI response into ValidationResult"""
        
        raw_response = ai_result.get('response', '')
        json_data = ai_result.get('json_data')
        cost_info = ai_result.get('cost_info')
        
        # 如果 JSON 提取成功，则使用它
        if json_data and isinstance(json_data, dict):
            return ValidationResult(
                success=json_data.get('success', False),
                confidence=float(json_data.get('confidence', 0.8)),  # 未提供时的默认置信度
                reasoning=json_data.get('reasoning', 'No reasoning provided'),
                components_validated=json_data.get('components_validated', []),
                functionality_demonstrated=json_data.get('functionality_demonstrated', []),
                issues_found=json_data.get('issues_found', []),
                recommendations=json_data.get('recommendations', []),
                raw_response=raw_response,
                cost_info=cost_info
            )
        
        # 兜底：尝试从原始响应中手动提取 JSON
        try:
            json_data = self.claude_wrapper.extract_json_from_response(raw_response)
            if json_data:
                return ValidationResult(
                    success=json_data.get('success', False),
                    confidence=float(json_data.get('confidence', 0.0)),
                    reasoning=json_data.get('reasoning', 'No reasoning provided'),
                    components_validated=json_data.get('components_validated', []),
                    functionality_demonstrated=json_data.get('functionality_demonstrated', []),
                    issues_found=json_data.get('issues_found', []),
                    recommendations=json_data.get('recommendations', []),
                    raw_response=raw_response,
                    cost_info=cost_info
                )
        except Exception:
            pass
        
        # 最终兜底：分析原始响应中的基础成功标志
        success = self._analyze_raw_response_for_success(raw_response)
        
        return ValidationResult(
            success=success,
            confidence=0.5,  # 非 JSON 响应的置信度较低
            reasoning=f"Could not parse JSON response. Raw analysis: {raw_response[:200]}...",
            components_validated=[],
            functionality_demonstrated=[],
            issues_found=["Could not parse JSON response from AI"],
            recommendations=["Review AI response format"],
            raw_response=raw_response,
            cost_info=cost_info
        )
    
    def _analyze_raw_response_for_success(self, response: str) -> bool:
        """Basic heuristic analysis of raw response for success indicators"""
        response_lower = response.lower()
        
        # 查找正面标志
        positive_indicators = [
            'success', 'successful', 'passed', 'working', 'completed',
            'demonstrated', 'functioning', 'properly'
        ]
        
        # 查找负面标志  
        negative_indicators = [
            'failed', 'error', 'exception', 'timeout', 'crashed',
            'unsuccessful', 'not working', 'broken'
        ]
        
        positive_count = sum(1 for indicator in positive_indicators if indicator in response_lower)
        negative_count = sum(1 for indicator in negative_indicators if indicator in response_lower)
        
        return positive_count > negative_count

def main():
    """Command line interface for validation"""
    parser = argparse.ArgumentParser(
        description="AI-powered validation for Spring AI examples",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Basic validation
  python validate_example.py --log-path ./app.log --example-name helloworld
  
  # With README context and expected behavior
  python validate_example.py --log-path ./app.log --example-name chain-workflow \\
    --readme-path ../README.md --expected-behavior "Process data through 4 steps"
  
  # Client-server example
  python validate_example.py --log-path ./app.log --example-name weather-server \\
    --components server client --template client_server_validation
        """
    )
    
    parser.add_argument("--log-path", required=True,
                      help="Path to the application log file")
    parser.add_argument("--example-name", required=True,
                      help="Name of the example being validated")
    parser.add_argument("--readme-path",
                      help="Path to README file for context")
    parser.add_argument("--expected-behavior",
                      help="Description of expected behavior")
    parser.add_argument("--components", nargs='+',
                      help="List of components (e.g., server client)")
    parser.add_argument("--template", default="example_validation",
                      help="Validation template to use")
    parser.add_argument("--output-format", choices=["json", "text"], default="json",
                      help="Output format")
    parser.add_argument("--verbose", action="store_true",
                      help="Show detailed output")
    
    args = parser.parse_args()
    
    # 创建校验请求
    request = ValidationRequest(
        log_path=args.log_path,
        example_name=args.example_name,
        readme_path=args.readme_path,
        expected_behavior=args.expected_behavior,
        components=args.components,
        prompt_template=args.template
    )
    
    # 创建校验器并运行校验
    try:
        validator = SpringAIExampleValidator()

        # 调试：检查 Claude CLI 是否可用
        if args.verbose:
            import subprocess
            try:
                which_result = subprocess.run(['which', 'claude'], capture_output=True, text=True)
                print(f"DEBUG: which claude: {which_result.stdout.strip() or which_result.stderr.strip()}", file=sys.stderr)
                version_result = subprocess.run(['claude', '--version'], capture_output=True, text=True, timeout=10)
                print(f"DEBUG: claude version: {version_result.stdout.strip() or version_result.stderr.strip()}", file=sys.stderr)
            except Exception as e:
                print(f"DEBUG: Claude CLI check failed: {e}", file=sys.stderr)

        result = validator.validate_example(request)
        
        if args.output_format == "json":
            print(json.dumps(result.to_dict(), indent=2))
        else:
            print(f"Success: {result.success}")
            print(f"Confidence: {result.confidence:.2f}")
            print(f"Reasoning: {result.reasoning}")
            if result.functionality_demonstrated:
                print("Functionality Demonstrated:")
                for item in result.functionality_demonstrated:
                    print(f"  - {item}")
            if result.issues_found:
                print("Issues Found:")
                for item in result.issues_found:
                    print(f"  - {item}")
                    
        # 根据校验结果设置退出码
        sys.exit(0 if result.success else 1)
        
    except Exception as e:
        error_result = {
            "success": False,
            "confidence": 0.0,
            "reasoning": f"Validation failed: {str(e)}",
            "components_validated": [],
            "functionality_demonstrated": [],
            "issues_found": [str(e)],
            "recommendations": ["Check configuration and try again"]
        }
        
        if args.output_format == "json":
            print(json.dumps(error_result, indent=2))
        else:
            print(f"Error: {str(e)}")
            
        sys.exit(1)

if __name__ == "__main__":
    main()