/* 
* Copyright 2024 - 2024 the original author or authors.
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
* https://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.example.agentic;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.util.Assert;

/**
 * 模式：<b>编排器—工作者</b>
 * <p/>
 * 在该模式中，一个中央 LLM（编排器）动态地将
 * 复杂任务分解为子任务，
 * 将它们委派给工作者 LLM，并使用合成器组合
 * 它们的结果。编排器分析
 * 输入，以确定需要哪些子任务以及应如何
 * 执行，而工作者则专注于
 * 各自被分配的特定任务。最后，合成器将
 * 工作者的输出整合为一个连贯的结果。
 * <p/>
 * 关键组件：
 * <ul>
 * <li>编排器：分析任务并确定所需
 * 子任务的中央 LLM</li>
 * <li>工作者：执行特定子任务的专门化 LLM</li>
 * <li>合成器：将工作者输出组合为最终
 * 结果的组件</li>
 * </ul>
 * <p/>
 * 使用场景：该模式非常适合无法预先
 * 预测所需子任务的复杂任务。
 * 例如：
 * <ul>
 * <li>需要改动的文件数量和改动性质
 * 取决于具体请求的编码任务</li>
 * <li>需要从多个来源收集和分析
 * 信息的搜索任务</li>
 * </ul>
 * <p/>
 * 虽然结构上与并行化相似，但其关键区别在于
 * 灵活性——子任务并非
 * 预先定义，而是由编排器根据
 * 具体输入动态确定。这使其
 * 特别适用于需要自适应问题解决以及
 * 多个专门化组件之间
 * 协调的任务。
 * 
 * @author Christian Tzolov
 * @see <a href=
 *      "https://www.anthropic.com/research/building-effective-agents">Building
 *      effective agents</a>
 */
public class OrchestratorWorkers {

	private final ChatClient chatClient;
	private final String orchestratorPrompt;
	private final String workerPrompt;

	public static final String DEFAULT_ORCHESTRATOR_PROMPT = """
			Analyze this task and break it down into 2-3 distinct approaches:

			Task: {task}

			Return your response in this JSON format:
			\\{
			"analysis": "Explain your understanding of the task and which variations would be valuable.
			             Focus on how each approach serves different aspects of the task.",
			"tasks": [
				\\{
				"type": "formal",
				"description": "Write a precise, technical version that emphasizes specifications"
				\\},
				\\{
				"type": "conversational",
				"description": "Write an engaging, friendly version that connects with readers"
				\\}
			]
			\\}
			""";

	public static final String DEFAULT_WORKER_PROMPT = """
			Generate content based on:
			Task: {original_task}
			Style: {task_type}
			Guidelines: {task_description}
			""";

	/**
	 * 表示由编排器识别、需要由工作者执行的
	 * 一个子任务。
	 * 
	 * @param type 任务的类型或类别（例如 "formal"、
	 * "conversational"）
	 * @param description 工作者应完成内容的详细描述
	 */
	public static record Task(String type, String description) {
	}

	/**
	 * 来自编排器的响应，包含任务分析以及
	 * 子任务分解。
	 * 
	 * @param analysis 对任务及不同方法如何
	 * 服务于其各方面的详细说明
	 * @param tasks 由编排器识别、将由工作者
	 * 执行的子任务列表
	 */
	public static record OrchestratorResponse(String analysis, List<Task> tasks) {
	}

	/**
	 * 最终响应，包含编排器的分析以及
	 * 组合后的工作者输出。
	 * 
	 * @param analysis 编排器对原始任务的理解
	 * 与分解
	 * @param workerResponses 来自工作者的响应列表，每个响应处理
	 * 一个特定子任务
	 */
	public static record FinalResponse(String analysis, List<String> workerResponses) {
	}

	/**
	 * 使用默认提示词创建一个新的 OrchestratorWorkers。
	 * 
	 * @param chatClient 用于 LLM 交互的 ChatClient
	 */
	public OrchestratorWorkers(ChatClient chatClient) {
		this(chatClient, DEFAULT_ORCHESTRATOR_PROMPT, DEFAULT_WORKER_PROMPT);
	}

	/**
	 * 使用自定义提示词创建一个新的 OrchestratorWorkers。
	 * 
	 * @param chatClient 用于 LLM 交互的 ChatClient
	 * @param orchestratorPrompt 编排器 LLM 的自定义提示词
	 * @param workerPrompt 工作者 LLM 的自定义提示词
	 */
	public OrchestratorWorkers(ChatClient chatClient, String orchestratorPrompt, String workerPrompt) {
		Assert.notNull(chatClient, "ChatClient must not be null");
		Assert.hasText(orchestratorPrompt, "Orchestrator prompt must not be empty");
		Assert.hasText(workerPrompt, "Worker prompt must not be empty");

		this.chatClient = chatClient;
		this.orchestratorPrompt = orchestratorPrompt;
		this.workerPrompt = workerPrompt;
	}

	/**
	 * 使用编排器—工作者模式处理任务。
	 * 首先，编排器分析任务并将其分解为子任务。
	 * 然后，工作者并行执行各子任务。
	 * 最后，结果被组合为单个响应。
	 * 
	 * @param taskDescription 待处理任务的描述
	 * @return 包含编排器分析以及组合后工作者输出的
	 * WorkerResponse
	 * @throws IllegalArgumentException 如果 taskDescription 为 null 或空
	 */
	@SuppressWarnings("null")
	public FinalResponse process(String taskDescription) {
		Assert.hasText(taskDescription, "Task description must not be empty");

		// 步骤 1：获取编排器响应
		OrchestratorResponse orchestratorResponse = this.chatClient.prompt()
				.user(u -> u.text(this.orchestratorPrompt)
						.param("task", taskDescription))
				.call()
				.entity(OrchestratorResponse.class);

		System.out.println(String.format("\n=== ORCHESTRATOR OUTPUT ===\nANALYSIS: %s\n\nTASKS: %s\n",
				orchestratorResponse.analysis(), orchestratorResponse.tasks()));

		// 步骤 2：处理每个任务
		List<String> workerResponses = orchestratorResponse.tasks().stream().map(task -> this.chatClient.prompt()
				.user(u -> u.text(this.workerPrompt)
						.param("original_task", taskDescription)
						.param("task_type", task.type())
						.param("task_description", task.description()))
				.call()
				.content()).toList();

		System.out.println("\n=== WORKER OUTPUT ===\n" + workerResponses);

		return new FinalResponse(orchestratorResponse.analysis(), workerResponses);
	}

}
