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

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.util.Assert;

/**
 * 工作流：<b>评估器—优化器</b>
 * <p/>
 * 实现面向大型语言模型（LLM）
 * 交互的评估器—优化器工作流模式。该工作流编排一个双 LLM 流程：
 * 一个模型生成响应，另一个模型
 * 则在迭代循环中提供评估与反馈，
 * 这一过程类似于
 * 人类作者的迭代精炼过程。
 *
 * <p>
 * 该工作流包含两个主要组件：
 * <ul>
 * <li>生成器 LLM：生成初始响应，并基于
 * 反馈对其进行精炼</li>
 * <li>评估器 LLM：分析响应并提供详细的
 * 改进反馈</li>
 * </ul>
 *
 * <b>使用标准</b>
 * 该工作流在满足以下条件的场景中
 * 特别有效：
 * <ul>
 * <li>存在用于评估响应质量的明确评估标准</li>
 * <li>迭代精炼能为输出带来可衡量的价值</li>
 * <li>任务受益于多轮评审与改进</li>
 * </ul>
 *
 * <b>适用性指标</b>
 * 两个关键指标表明该工作流是合适的：
 * <ul>
 * <li>当反馈被清晰表述时，LLM 的响应能够
 * 得到可证明的改进</li>
 * <li>评估器 LLM 能提供实质性且可操作的反馈</li>
 * </ul>
 *
 * <b>示例应用</b>
 * <ul>
 * <li>需要捕捉细微差别并通过迭代精炼的
 * 文学翻译</li>
 * <li>需要多轮搜索与分析的
 * 复杂搜索任务</li>
 * <li>可通过系统性审查提升质量的
 * 代码生成</li>
 * <li>需要多份草稿与针对性改进的内容创作</li>
 * </ul>
 * 
 * @author Christian Tzolov
 * @see <a href=
 *      "https://www.anthropic.com/research/building-effective-agents">Building
 *      effective agents</a>
 */
@SuppressWarnings("null")
public class EvaluatorOptimizer {

	public static final String DEFAULT_GENERATOR_PROMPT = """
			Your goal is to complete the task based on the input. If there are feedback
			from your previous generations, you should reflect on them to improve your solution.

			CRITICAL: Your response must be a SINGLE LINE of valid JSON with NO LINE BREAKS except those explicitly escaped with \\n.
			Here is the exact format to follow, including all quotes and braces:

			{"thoughts":"Brief description here","response":"public class Example {\\n    // Code here\\n}"}

			Rules for the response field:
			1. ALL line breaks must use \\n
			2. ALL quotes must use \\"
			3. ALL backslashes must be doubled: \\
			4. NO actual line breaks or formatting - everything on one line
			5. NO tabs or special characters
			6. Java code must be complete and properly escaped

			Example of properly formatted response:
			{"thoughts":"Implementing counter","response":"public class Counter {\\n    private int count;\\n    public Counter() {\\n        count = 0;\\n    }\\n    public void increment() {\\n        count++;\\n    }\\n}"}

			Follow this format EXACTLY - your response must be valid JSON on a single line.
			""";

	public static final String DEFAULT_EVALUATOR_PROMPT = """
			Evaluate this code implementation for correctness, time complexity, and best practices.
			Ensure the code have proper javadoc documentation.
			Respond with EXACTLY this JSON format on a single line:

			{"evaluation":"PASS, NEEDS_IMPROVEMENT, or FAIL", "feedback":"Your feedback here"}

			The evaluation field must be one of: "PASS", "NEEDS_IMPROVEMENT", "FAIL"
			Use "PASS" only if all criteria are met with no improvements needed.
			""";

	/**
	 * 表示一个解决方案生成步骤。包含模型的思考内容与
	 * 所提议的解决方案。
	 * 
	 * @param thoughts 模型对任务与反馈的理解
	 * @param response 模型提议的解决方案
	 */
	public static record Generation(String thoughts, String response) {
	}

	/**
	 * 表示一个评估响应。包含评估结果与
	 * 详细反馈。
	 * 
	 * @param evaluation 评估结果（PASS、NEEDS_IMPROVEMENT 或 FAIL）
	 * @param feedback 用于改进的详细反馈
	 */
	public static record EvaluationResponse(Evaluation evaluation, String feedback) {

		public enum Evaluation {
			PASS, NEEDS_IMPROVEMENT, FAIL
		}
	}

	/**
	 * 表示最终精炼后的响应。包含最终解决方案以及
	 * 展示解决方案演进过程的思维链。
	 * 
	 * @param solution 最终解决方案
	 * @param chainOfThought 展示解决方案
	 * 演进过程的思维链
	 */
	public static record RefinedResponse(String solution, List<Generation> chainOfThought) {
	}

	private final ChatClient chatClient;

	private final String generatorPrompt;

	private final String evaluatorPrompt;

	public EvaluatorOptimizer(ChatClient chatClient) {
		this(chatClient, DEFAULT_GENERATOR_PROMPT, DEFAULT_EVALUATOR_PROMPT);
	}

	public EvaluatorOptimizer(ChatClient chatClient, String generatorPrompt, String evaluatorPrompt) {
		Assert.notNull(chatClient, "ChatClient must not be null");
		Assert.hasText(generatorPrompt, "Generator prompt must not be empty");
		Assert.hasText(evaluatorPrompt, "Evaluator prompt must not be empty");

		this.chatClient = chatClient;
		this.generatorPrompt = generatorPrompt;
		this.evaluatorPrompt = evaluatorPrompt;
	}

	/**
	 * 为给定任务启动评估器—优化器工作流。该方法
	 * 编排生成与评估的迭代过程，
	 * 直到获得令人满意的解决方案。
	 * 
	 * <p>
	 * 该工作流遵循以下步骤：
	 * </p>
	 * <ol>
	 * <li>生成初始解决方案</li>
	 * <li>依据质量标准评估该解决方案</li>
	 * <li>如果评估通过，则返回该解决方案</li>
	 * <li>如果评估表明需要改进，则纳入反馈并
	 * 生成新的解决方案</li>
	 * <li>重复步骤 2-4，直到获得令人满意的解决方案</li>
	 * </ol>
	 * 
	 * @param task 需要通过迭代精炼解决的任务或问题
	 * @return 一个 RefinedResponse，包含最终解决方案以及
	 * 思维链
	 * （展示解决方案的演进过程）
	 */
	public RefinedResponse loop(String task) {
		List<String> memory = new ArrayList<>();
		List<Generation> chainOfThought = new ArrayList<>();

		return loop(task, "", memory, chainOfThought);
	}

	/**
	 * 评估器—优化器循环的内部递归实现。该方法
	 * 在递归精炼解决方案的过程中，
	 * 维护此前各次尝试与反馈的状态，
	 * 直至解决方案满足
	 * 评估标准。
	 * 
	 * @param task 待解决的原始任务
	 * @param context 累积的上下文，包括此前的尝试与
	 * 反馈
	 * @param memory 供参考的此前解决方案尝试列表
	 * @param chainOfThought 跟踪解决方案与推理演进过程的列表
	 * @return 包含最终解决方案及完整解决方案
	 * 历史的 RefinedResponse
	 */
	private RefinedResponse loop(String task, String context, List<String> memory,
			List<Generation> chainOfThought) {

		Generation generation = generate(task, context);
		memory.add(generation.response());
		chainOfThought.add(generation);

		EvaluationResponse evaluationResponse = evalute(generation.response(), task);

		if (evaluationResponse.evaluation().equals(EvaluationResponse.Evaluation.PASS)) {
			// 解决方案被接受！
			return new RefinedResponse(generation.response(), chainOfThought);
		}

		// 累积的新上下文，包含最近一次和此前各次
		// 尝试与反馈。
		StringBuilder newContext = new StringBuilder();
		newContext.append("Previous attempts:");
		for (String m : memory) {
			newContext.append("\n- ").append(m);
		}
		newContext.append("\nFeedback: ").append(evaluationResponse.feedback());

		return loop(task, newContext.toString(), memory, chainOfThought);
	}

	/**
	 * 基于给定任务和反馈上下文生成或精炼解决方案。
	 * 该方法代表工作流中的生成器组件，
	 * 生成可通过评估反馈迭代改进的响应。
	 * 
	 * @param task 待解决的主要任务或问题
	 * @param context 用于迭代改进的此前尝试与反馈
	 * @return 包含模型思考内容与提议解决方案的 Generation
	 */
	private Generation generate(String task, String context) {
		Generation generationResponse = chatClient.prompt()
				.user(u -> u.text("{prompt}\n{context}\nTask: {task}")
						.param("prompt", this.generatorPrompt)
						.param("context", context)
						.param("task", task))
				.call()
				.entity(Generation.class);

		System.out.println(String.format("\n=== GENERATOR OUTPUT ===\nTHOUGHTS: %s\n\nRESPONSE:\n %s\n",
				generationResponse.thoughts(), generationResponse.response()));
		return generationResponse;
	}

	/**
	 * 评估解决方案是否满足指定的要求与质量
	 * 标准。
	 * 该方法代表工作流中的评估器组件，分析
	 * 解决方案
	 * 并提供详细反馈以进一步精炼，直到达到期望的
	 * 质量
	 * 水平。
	 * 
	 * @param content 待评估的解决方案内容
	 * @param task 用于评估解决方案的原始任务
	 * @return 一个 EvaluationResponse，包含评估结果
	 * （PASS/NEEDS_IMPROVEMENT/FAIL）
	 * 以及用于改进的详细反馈
	 */
	private EvaluationResponse evalute(String content, String task) {

		EvaluationResponse evaluationResponse = chatClient.prompt()
				.user(u -> u.text("{prompt}\nOriginal task: {task}\nContent to evaluate: {content}")
						.param("prompt", this.evaluatorPrompt)
						.param("task", task)
						.param("content", content))
				.call()
				.entity(EvaluationResponse.class);

		System.out.println(String.format("\n=== EVALUATOR OUTPUT ===\nEVALUATION: %s\n\nFEEDBACK: %s\n",
				evaluationResponse.evaluation(), evaluationResponse.feedback()));
		return evaluationResponse;
	}

}
