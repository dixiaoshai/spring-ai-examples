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

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.util.Assert;

/**
 * 实现路由工作流模式：对输入进行分类，并将其引导至
 * 专门化的
 * 后续任务。该工作流通过将不同类型的输入路由到专门化的提示词，
 * 以及针对特定类别优化的流程，
 * 从而实现关注点分离。
 * 
 * 
 * <p>
 * 路由工作流在以下复杂任务中特别有效：
 * <ul>
 * <li>存在更适合单独处理的
 * 明确输入类别</li>
 * <li>分类可以由 LLM 或传统
 * 分类模型准确完成</li>
 * <li>不同类型的输入需要不同的专门化处理或
 * 专业知识</li>
 * </ul>
 * 
 * <p>
 * 常见用例包括：
 * <ul>
 * <li>客户支持系统路由不同类型的查询（计费、
 * 技术等）</li>
 * <li>内容审核系统将内容路由到适当的审核
 * 流程</li>
 * <li>通过将简单/复杂问题路由到不同模型能力实现的
 * 查询优化</li>
 * </ul>
 * 
 * <p>
 * 本实现支持基于内容分类的动态路由，
 * 其中每条路由都有自己的、
 * 针对特定输入类型优化的专门化提示词。
 * 
 * 
 * <p/>
 * 本实现使用 <a href=
 * "https://docs.spring.io/spring-ai/reference/1.0/api/structured-output-converter.html">Spring
 * AI Structure Output</a>，将聊天客户端的响应转换为结构化的
 * {@link RoutingResponse} 对象。
 * 
 * @author Christian Tzolov
 * @see org.springframework.ai.chat.client.ChatClient
 * @see <a href=
 *      "https://docs.spring.io/spring-ai/reference/1.0/api/chatclient.html">Spring
 *      AI ChatClient</a>
 * @see <a href=
 *      "https://www.anthropic.com/research/building-effective-agents">Building
 *      Effective Agents</a>
 * @see <a href=
 *      "https://docs.spring.io/spring-ai/reference/1.0/api/structured-output-converter.html">Spring
 *      AI Structure Output</a>
 * 
 */
public class RoutingWorkflow {

    private final ChatClient chatClient;

    public RoutingWorkflow(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 基于内容分类将输入路由到专门化的提示词。
     * 该方法首先分析输入以确定最合适的路由，
     * 然后使用该路由对应的专门化提示词处理输入。
     * 
     * 
     * 
     * <p>
     * 路由过程包括：
     * <ol>
     * <li>通过内容分析确定适当的类别</li>
     * <li>选择针对该类别优化的专门化提示词</li>
     * <li>使用所选提示词处理输入</li>
     * </ol>
     *
     * <p>
     * 该方法支持：
     * <ul>
     * <li>更好地处理多样化的输入类型</li>
     * <li>针对特定类别优化提示词</li>
     * <li>通过专门化处理提升准确度</li>
     * </ul>
     *
     * @param input 待路由和处理的输入文本
     * @param routes 路由名称到其对应专门化提示词的映射
     * @return 来自所选专门化路由的处理后响应
     */
    public String route(String input, Map<String, String> routes) {
        Assert.notNull(input, "Input text cannot be null");
        Assert.notEmpty(routes, "Routes map cannot be null or empty");

        // 确定为输入选择适当的路由
        String routeKey = determineRoute(input, routes.keySet());

        // 从路由映射中获取所选提示词
        String selectedPrompt = routes.get(routeKey);

        if (selectedPrompt == null) {
            throw new IllegalArgumentException("Selected route '" + routeKey + "' not found in routes map");
        }

        // 使用所选提示词处理输入
        return chatClient.prompt(selectedPrompt + "\nInput: " + input).call().content();
    }

    /**
     * 分析输入内容，并基于内容分类确定最合适的
     * 路由。分类过程会考虑输入中的关键词、
     * 上下文，
     * 以及模式，以选择最优路由。
     * 
     * <p>
     * 该方法使用 LLM 来：
     * <ul>
     * <li>分析输入内容与上下文</li>
     * <li>考虑可用的路由选项</li>
     * <li>为路由决策提供推理</li>
     * <li>选择最合适的路由</li>
     * </ul>
     *
     * @param input 用于路由分析的输入文本
     * @param availableRoutes 可用路由选项集合
     * @return 基于内容分析所选择的路由键
     */
    @SuppressWarnings("null")
    private String determineRoute(String input, Iterable<String> availableRoutes) {
        System.out.println("\nAvailable routes: " + availableRoutes);

        String selectorPrompt = String.format("""
                Analyze the input and select the most appropriate support team from these options: %s
                First explain your reasoning, then provide your selection in this JSON format:

                \\{
                    "reasoning": "Brief explanation of why this ticket should be routed to a specific team.
                                Consider key terms, user intent, and urgency level.",
                    "selection": "The chosen team name"
                \\}

                Input: %s""", availableRoutes, input);

        RoutingResponse routingResponse = chatClient.prompt(selectorPrompt).call().entity(RoutingResponse.class);

        System.out.println(String.format("Routing Analysis:%s\nSelected route: %s",
                routingResponse.reasoning(), routingResponse.selection()));

        return routingResponse.selection();
    }
}
