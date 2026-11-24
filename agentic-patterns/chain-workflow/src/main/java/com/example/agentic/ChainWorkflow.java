package com.example.agentic;

import org.springframework.ai.chat.client.ChatClient;

/**
 * 实现提示词链式工作流模式，用于将复杂任务分解为
 * 一系列 LLM 调用，其中每一步都
 * 处理上一步的输出。
 * 
 * <p>
 * 本实现演示了处理
 * 文本中数值数据的四步工作流：
 * <ol>
 * <li>提取数值与指标</li>
 * <li>标准化为百分比格式</li>
 * <li>按降序排序</li>
 * <li>格式化为 Markdown 表格</li>
 * </ol>
 * 
 * <p/>
 * 何时使用该工作流：当任务可以轻松、清晰地
 * 分解为固定子任务时，该工作流最为理想。其主要目标
 * 是以延迟换取更高的准确度，通过让每次 LLM 调用
 * 都成为更简单的任务。
 * 
 * @author Christian Tzolov
 * @see org.springframework.ai.chat.client.ChatClient
 * @see <a href="https://docs.spring.io/spring-ai/reference/1.0/api/chatclient.html">Spring AI ChatClient</a>
 * @see <a href=
 *      "https://www.anthropic.com/research/building-effective-agents">Building
 *      Effective Agents</a>
 */
public class ChainWorkflow {

	/**
	 * 定义链中各个转换步骤的系统提示词数组。
	 * 每个提示词都充当门控，在进入下一步之前
	 * 校验并转换输出。
	 */
	private static final String[] DEFAULT_SYSTEM_PROMPTS = {

			// 第 1 步
			"""
					Extract only the numerical values and their associated metrics from the text.
					Format each as'value: metric' on a new line.
					Example format:
					92: customer satisfaction
					45%: revenue growth""",
			// 第 2 步
			"""
					Convert all numerical values to percentages where possible.
					If not a percentage or points, convert to decimal (e.g., 92 points -> 92%).
					Keep one number per line.
					Example format:
					92%: customer satisfaction
					45%: revenue growth""",
			// 第 3 步
			"""
					Sort all lines in descending order by numerical value.
					Keep the format 'value: metric' on each line.
					Example:
					92%: customer satisfaction
					87%: employee satisfaction""",
			// 第 4 步
			"""
					Format the sorted data as a markdown table with columns:
					| Metric | Value |
					|:--|--:|
					| Customer Satisfaction | 92% | """
	};

	private final ChatClient chatClient;

	private final String[] systemPrompts;

	/**
	 * 使用指定的聊天客户端
	 * 和默认系统提示词构造提示词链式工作流的新实例。
	 * 
	 * @param chatClient 用于发起 LLM 调用的 Spring AI 聊天客户端
	 */
	public ChainWorkflow(ChatClient chatClient) {
		this(chatClient, DEFAULT_SYSTEM_PROMPTS);
	}

	/**
	 * 使用指定的聊天客户端
	 * 和系统提示词构造提示词链式工作流的新实例。
	 * 
	 * @param chatClient 用于发起 LLM 调用的 Spring AI 聊天客户端
	 * @param systemPrompts 定义链中各个转换步骤的系统提示词
	 * （提示词数组）
	 */
	public ChainWorkflow(ChatClient chatClient, String[] systemPrompts) {
		this.chatClient = chatClient;
		this.systemPrompts = systemPrompts;
	}

	/**
	 * 通过一系列 LLM 调用处理输入文本，执行
	 * 提示词链式工作流：每次调用的输出
	 * 都会成为下一步的输入。
	 * 
	 * <p>
	 * 该方法在每一步之后打印中间结果，以展示
	 * 转换在链中的推进过程。
	 *
	 * @param chatClient 用于发起 LLM 调用的 Spring AI 聊天客户端
	 * @param userInput 待处理的、包含数值数据的输入文本
	 * @return 所有步骤执行完毕后的最终输出
	 */
	public String chain(String userInput) {

		int step = 0;
		String response = userInput;
		System.out.println(String.format("\nSTEP %s:\n %s", step++, response));

		for (String prompt : systemPrompts) {

			// 1. 使用上一步的响应组合输入。
			String input = String.format("{%s}\n {%s}", prompt, response);

			// 2. 用新输入调用聊天客户端并获取新的响应。
			response = chatClient.prompt(input).call().content();

			System.out.println(String.format("\nSTEP %s:\n %s", step++, response));
		}

		return response;
	}
}
