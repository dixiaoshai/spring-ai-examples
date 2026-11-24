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

/**
 * 表示路由分类过程响应的 record。
 * 
 * <p>
 * 该 record 被 {@link RoutingWorkflow} 用于
 * 捕获并传达 LLM 分类器做出的路由决策。
 * 
 * @param reasoning 详细说明为何选择某条特定路由，
 * 其中考虑关键词、用户意图和紧急程度
 * 等因素
 * @param selection 将处理该输入的所选路由名称
 * 
 * 
 * @author Christian Tzolov
 * @see RoutingWorkflow
 */
public record RoutingResponse(
		/**
		 * 路由选择背后的推理，说明为何基于输入分析
		 * 选择了该特定路由。
		 */
		String reasoning,

		/**
		 * 基于分类分析将处理该输入的
		 * 所选路由名称。
		 */
		String selection) {
}
