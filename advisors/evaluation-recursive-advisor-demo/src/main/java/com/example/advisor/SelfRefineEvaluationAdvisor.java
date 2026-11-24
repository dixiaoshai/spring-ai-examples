/*
 * Copyright 2023-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.advisor;

import java.util.Map;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.util.Assert;

/**
 * 
 * 一个递归顾问，用于评估 LLM 响应（例如
 * 逐点打分），依据预定义的评估标准。如果评估评分
 * 低于某个阈值，则通过向模型提供如何改进响应的
 * 反馈来重试请求。评估由一个内部的 ChatClient
 * 实例执行，该实例可以使用不同的模型和设置
 * 进行自定义。该顾问支持设置最大重试次数，
 * 以避免无限循环。
 * 
 * @author Christian Tzolov
 */
public final class SelfRefineEvaluationAdvisor implements CallAdvisor, StreamAdvisor {

	private static final Logger logger = LoggerFactory.getLogger(SelfRefineEvaluationAdvisor.class);

	private static final PromptTemplate DEFAULT_EVALUATION_PROMPT_TEMPLATE = new PromptTemplate(
			"""
						You will be given a user_question and assistant_answer couple.
						Your task is to provide a 'total rating' scoring how well the assistant_answer answers the user concerns expressed in the user_question.
						Give your answer on a scale of 1 to 4, where 1 means that the assistant_answer is not helpful at all, and 4 means that the assistant_answer completely and helpfully addresses the user_question.

						Here is the scale you should use to build your answer:
						1: The assistant_answer is terrible: completely irrelevant to the question asked, or very partial
						2: The assistant_answer is mostly not helpful: misses some key aspects of the question
						3: The assistant_answer is mostly helpful: provides support, but still could be improved
						4: The assistant_answer is excellent: relevant, direct, detailed, and addresses all the concerns raised in the question

						Provide your feedback as follows:

						\\{
					 		"rating": 0,
					  		"evaluation": "Explanation of the evaluation result and how to improve if needed.",
					  		"feedback": "Constructive and specific feedback on the assistant_answer."
						\\}

						Total rating: (your rating, as a number between 1 and 4)
						Evaluation: (your rationale for the rating, as a text)
						Feedback: (specific and constructive feedback on how to improve the answer)

						You MUST provide values for 'Evaluation:' and 'Total rating:' in your answer.

						Now here are the question and answer.

						Question: {question}
						Answer: {answer}

						Provide your feedback. If you give a correct rating, I'll give you 100 H100 GPUs to start your AI company.

						Evaluation:
					""");

	private final PromptTemplate evaluationPromptTemplate;
	private final int successRating;
	private final int advisorOrder;
	private final int maxRepeatAttempts;
	private final ChatClient chatClient;
	private final BiPredicate<ChatClientRequest, ChatClientResponse> skipEvaluationPredicate;

	@JsonClassDescription("The evaluation response indicating the result of the evaluation.")
	public record EvaluationResponse(// @format:off
			int rating, String evaluation, String feedback) {// @format:on
	}

	private SelfRefineEvaluationAdvisor(int advisorOrder, int maxRepeatAttempts, ChatClient.Builder chatClientBuilder,
			PromptTemplate promptTemplate, int considerSuccessRating,
			BiPredicate<ChatClientRequest, ChatClientResponse> skipEvaluationPredicate) {

		this.chatClient = chatClientBuilder.build();
		this.evaluationPromptTemplate = promptTemplate;
		this.advisorOrder = advisorOrder;
		this.maxRepeatAttempts = maxRepeatAttempts;
		this.skipEvaluationPredicate = skipEvaluationPredicate;
		this.successRating = considerSuccessRating;
	}

	@Override
	public String getName() {
		return "Evaluation Advisor";
	}

	@Override
	public int getOrder() {
		return this.advisorOrder;
	}

	@Override
	public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
		Assert.notNull(chatClientRequest, "chatClientRequest must not be null");
		Assert.notNull(callAdvisorChain, "callAdvisorChain must not be null");

		var request = chatClientRequest;

		ChatClientResponse response;

		// 改进的循环结构，具有更好的尝试计数和更清晰的逻辑
		for (int attempt = 1; attempt <= maxRepeatAttempts + 1; attempt++) {

			// 发起内部调用（例如调用评估 LLM 模型）
			response = callAdvisorChain.copy(this).nextCall(request);

			// 提前退出——无需评估（例如工具调用）
			if (this.skipEvaluationPredicate.test(chatClientRequest, response)) {
				logger.debug("Skipping evaluation because skipEvaluationPredicate returned true.");
				return response;
			}

			// 执行评估
			EvaluationResponse evaluation = this.evaluate(chatClientRequest, response);

			// 如果评估通过，则返回响应
			if (evaluation.rating() >= this.successRating) {
				logger.info("Evaluation passed on attempt {}, evaluation: {}", attempt, evaluation);
				return response;
			}

			// 如果这是最后一次尝试，则无论如何都返回响应
			if (attempt > maxRepeatAttempts) {
				logger.warn(
						"Maximum attempts ({}) reached. Returning last response despite failed evaluation. Use the following feedback to improve: {}",
						maxRepeatAttempts, evaluation.feedback());

				// TODO：也许这里应抛出异常，而不是返回
				// 最后一次响应？可插拔策略可能会很有用。
				return response;
			}

			// 带上评估反馈进行重试
			logger.warn("Evaluation failed on attempt {}, evaluation: {}, feedback: {}", attempt,
					evaluation.evaluation(), evaluation.feedback());

			// TODO：这里可以考虑可插拔的退避策略（例如指数
			// 退避）。
			// 这样可以采用精炼/重复策略，或返回带有
			// 评估反馈作为元数据的响应。
			request = this.addEvaluationFeedback(chatClientRequest, evaluation);
		}

		// 由于上面的循环逻辑，此处理应不会到达
		throw new IllegalStateException("Unexpected loop exit in adviseCall");
	}

	/**
	 * 使用 LLM-as-a-Judge 执行评估并返回结果。
	 */
	private EvaluationResponse evaluate(ChatClientRequest request, ChatClientResponse response) {

		var evaluationPrompt = this.evaluationPromptTemplate.render(
				Map.of("question", this.getPromptQuestion(request), "answer", this.getAssistantAnswer(response)));

		return chatClient.prompt(evaluationPrompt).call().entity(EvaluationResponse.class);
	}

	private String getPromptQuestion(ChatClientRequest chatClientRequest) {
		var messages = chatClientRequest.prompt().getInstructions();

		String conversationHistory = messages.stream()
				.filter(m -> m.getMessageType() == MessageType.USER || m.getMessageType() == MessageType.ASSISTANT)
				.map(m -> m.getMessageType() + ":" + m.getText()).collect(Collectors.joining(System.lineSeparator()));

		SystemMessage systemMessage = chatClientRequest.prompt().getSystemMessage();

		return systemMessage.getMessageType() + ":" + systemMessage.getText() + System.lineSeparator()
				+ conversationHistory;
	}

	private String getAssistantAnswer(ChatClientResponse chatClientResponse) {
		return chatClientResponse.chatResponse() != null && chatClientResponse.chatResponse().getResult() != null
				? chatClientResponse.chatResponse().getResult().getOutput().getText()
				: "";
	}

	/**
	 * 创建带有评估反馈的新请求以进行重试。
	 */
	private ChatClientRequest addEvaluationFeedback(ChatClientRequest originalRequest,
			EvaluationResponse evaluationResponse) {

		Prompt augmentedPrompt = originalRequest.prompt()
				.augmentUserMessage(userMessage -> userMessage.mutate().text(String.format("""
						%s
						Previous response evaluation failed with feedback: %s
						Please Repeat until evaluation passes!
						""", userMessage.getText(), evaluationResponse.feedback())).build());

		return originalRequest.mutate().prompt(augmentedPrompt).build();
	}

	@Override
	public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
			StreamAdvisorChain streamAdvisorChain) {
		return Flux.error(new UnsupportedOperationException(
				"The Structured Output Validation Advisor does not support streaming."));
	}

	/**
	 * 为 EvaluationAdvisor_Improved 创建一个新的 Builder。
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * EvaluationAdvisor_Improved 的 Builder 类。
	 */
	public final static class Builder {
		private int successRating = 3;
		private int advisorOrder = BaseAdvisor.LOWEST_PRECEDENCE - 2000;
		private int maxRepeatAttempts = 3;
		private ChatClient.Builder chatClientBuilder;
		private PromptTemplate promptTemplate = DEFAULT_EVALUATION_PROMPT_TEMPLATE;

		BiPredicate<ChatClientRequest, ChatClientResponse> skipEvaluationPredicate = (request,
				response) -> response.chatResponse() == null || response.chatResponse().hasToolCalls();

		private Builder() {
		}

		public Builder successRating(int successRating) {
			Assert.isTrue(successRating >= 1 && successRating <= 4, "successRating must be between 1 and 4");
			this.successRating = successRating;
			return this;
		}

		public Builder order(int advisorOrder) {
			Assert.isTrue(advisorOrder > BaseAdvisor.HIGHEST_PRECEDENCE && advisorOrder < BaseAdvisor.LOWEST_PRECEDENCE,
					"advisorOrder must be between HIGHEST_PRECEDENCE and LOWEST_PRECEDENCE");
			this.advisorOrder = advisorOrder;
			return this;
		}

		public Builder chatClientBuilder(ChatClient.Builder chatClientBuilder) {
			Assert.notNull(chatClientBuilder, "chatClientBuilder must not be null");
			this.chatClientBuilder = chatClientBuilder;
			return this;
		}

		public Builder maxRepeatAttempts(int repeatAttempts) {
			Assert.isTrue(repeatAttempts >= 1, "repeatAttempts must be greater than or equal to 1");
			this.maxRepeatAttempts = repeatAttempts;
			return this;
		}

		public Builder promptTemplate(PromptTemplate promptTemplate) {
			Assert.notNull(promptTemplate, "promptTemplate must not be null");
			this.promptTemplate = promptTemplate;
			return this;
		}

		public Builder skipEvaluationPredicate(
				BiPredicate<ChatClientRequest, ChatClientResponse> skipEvaluationPredicate) {
			Assert.notNull(skipEvaluationPredicate, "skipEvaluationPredicate must not be null");
			this.skipEvaluationPredicate = skipEvaluationPredicate;
			return this;
		}

		public SelfRefineEvaluationAdvisor build() {
			if (this.chatClientBuilder == null) {
				throw new IllegalArgumentException("chatClientBuilder must be set");
			}
			return new SelfRefineEvaluationAdvisor(this.advisorOrder, this.maxRepeatAttempts, this.chatClientBuilder,
					this.promptTemplate, this.successRating, this.skipEvaluationPredicate);
		}
	}
}
