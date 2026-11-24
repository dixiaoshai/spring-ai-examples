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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.util.Assert;

/**
 * 实现并行化工作流模式，用于对多个 LLM 操作进行
 * 高效的并发处理。该模式支持并行执行 LLM 调用，
 * 并自动聚合输出，可显著提升
 * 批处理场景的吞吐量。
 * 
 * <p>该模式体现为两种关键变体：</p>
 * 
 * <ul>
 * <li><b>分段（Sectioning）</b>：将复杂任务分解为可以
 * 并发处理的独立子任务。例如，同时分析
 * 文档的不同部分。</li>
 * <li><b>投票（Voting）</b>：并行多次执行相同的提示词，
 * 以收集多样化视角或实现多数投票机制。这对于
 * 校验或建立共识类任务特别有用。</li>
 * </ul>
 *
 * <p><b>主要优势：</b></p>
 * <ul>
 * <li>通过并发处理提升吞吐量</li>
 * <li>更好地利用 LLM API 容量的资源</li>
 * <li>缩短批处理操作的整体处理时间</li>
 * <li>通过多视角提升结果质量（在投票场景中）</li>
 * </ul>
 *
 * <p><b>使用场景：</b></p>
 * <ul>
 * <li>处理大量相似但相互独立的条目</li>
 * <li>需要多个独立视角或校验的任务</li>
 * <li>处理时间至关重要且任务可并行化的场景</li>
 * <li>可分解为独立子任务的复杂操作</li>
 * </ul>
 *
 * <p><b>实现注意事项：</b></p>
 * <ul>
 * <li>确保任务真正相互独立，以避免一致性问题</li>
 * <li>确定并行执行容量时考虑 API 速率限制</li>
 * <li>扩展并行操作时监控资源使用（内存、CPU）</li>
 * <li>为并行任务失败实现适当的错误处理</li>
 * </ul>
 *
 * @author Christian Tzolov
 * @see org.springframework.ai.chat.client.ChatClient
 * @see <a href="https://docs.spring.io/spring-ai/reference/1.0/api/chatclient.html">Spring AI ChatClient</a>
 * @see <a href=
 *      "https://www.anthropic.com/research/building-effective-agents">Building
 *      Effective Agents</a>
 */
public class ParallelizationlWorkflow {

	private final ChatClient chatClient;

	public ParallelizationlWorkflow(ChatClient chatClient) {
		this.chatClient = chatClient;
	}

	/**
	 * 使用固定线程池和相同的提示词模板并发处理多个输入。
	 * 该方法保持结果顺序与输入顺序一致。
	 *
	 * @param prompt 用于每个输入的提示词模板。输入将追加到该提示词之后。
	 * 不能为 null。示例："Translate the following text to French:"
	 * @param inputs 待处理的输入字符串列表。每个输入都将被独立
	 * 并行处理。不能为 null 或空。示例：["Hello", "World", "Good morning"]
	 * @param nWorkers 要使用的并发工作线程数。它控制
	 * 同时进行的 LLM API 调用上限。必须大于 0。设置该值时应
	 * 考虑 API 速率限制。
	 * @return 按输入顺序排列的处理结果列表。每个结果包含
	 * 对应输入的 LLM 响应。
	 * @throws IllegalArgumentException 如果 prompt 为 null、inputs 为 null/空，或 nWorkers <= 0
	 * @throws RuntimeException 如果任一输入处理失败，其 cause 包含
	 * 具体的错误详情
	 */
	public List<String> parallel(String prompt, List<String> inputs, int nWorkers) {
		Assert.notNull(prompt, "Prompt cannot be null");
		Assert.notEmpty(inputs, "Inputs list cannot be empty");
		Assert.isTrue(nWorkers > 0, "Number of workers must be greater than 0");

		ExecutorService executor = Executors.newFixedThreadPool(nWorkers);
		try {
			List<CompletableFuture<String>> futures = inputs.stream()
					.map(input -> CompletableFuture.supplyAsync(() -> {
						try {
							return chatClient.prompt(prompt + "\nInput: " + input).call().content();
						} catch (Exception e) {
							throw new RuntimeException("Failed to process input: " + input, e);
						}
					}, executor))
					.collect(Collectors.toList());

			// 等待所有任务完成
			CompletableFuture<Void> allFutures = CompletableFuture.allOf(
					futures.toArray(CompletableFuture[]::new));
			allFutures.join();

			return futures.stream()
					.map(CompletableFuture::join)
					.collect(Collectors.toList());

		} finally {
			executor.shutdown();
		}
	}

}
