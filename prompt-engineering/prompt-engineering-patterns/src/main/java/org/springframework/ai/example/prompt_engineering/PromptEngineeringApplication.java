package org.springframework.ai.example.prompt_engineering;

import tools.jackson.databind.json.JsonMapper;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * @author Christian Tzolov
 */
@SpringBootApplication
public class PromptEngineeringApplication {

	JsonMapper jsonMapper = new JsonMapper();

	public static void main(String[] args) {
		SpringApplication.run(PromptEngineeringApplication.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner(ChatClient.Builder chatClientBuilder,
			ConfigurableApplicationContext context) {
		return args -> {
			try {
				ChatClient chatClient = chatClientBuilder.build();

				// 检查是否通过命令行参数请求了特定模式
				String pattern = args.length > 0 ? args[0] : "all";

				switch (pattern.toLowerCase()) {
					case "basic":
						runBasicPatterns(chatClient);
						break;
					case "system":
						runSystemPatterns(chatClient);
						break;
					case "advanced":
						runAdvancedPatterns(chatClient);
						break;
					case "all":
					default:
						runAllPatterns(chatClient);
						break;
				}

				System.out.println("Prompt engineering patterns demonstration completed!");
				context.close();
			}
			catch (Exception e) {
				System.err.println("Error during prompt engineering execution: " + e.getMessage());
				e.printStackTrace();
				context.close();
			}
		};
	}

	private void runBasicPatterns(ChatClient chatClient) {
		System.out.println("=== Running Basic Patterns ===");
		// 2.1 通用提示 / 零样本
		pt_zero_shot(chatClient);
		// 2.2 单样本与少样本
		pt_ones_shot_few_shots(chatClient);
	}

	private void runSystemPatterns(ChatClient chatClient) {
		System.out.println("=== Running System/Role Patterns ===");
		// 2.3.1 系统提示
		pt_system_prompting_1(chatClient);
		pt_system_prompting_2_springai_style(chatClient);
		// 2.3.2 角色提示
		pt_role_prompting_1(chatClient);
	}

	private void runAdvancedPatterns(ChatClient chatClient) {
		System.out.println("=== Running Advanced Patterns ===");
		// 2.6 自洽性（简化版 - 仅一次迭代）
		pt_self_consistency_simple(chatClient);
	}

	private void runAllPatterns(ChatClient chatClient) {
		//@formatter:off
		// 2. 提示技术（第 13 页）

			// 2.1 通用提示 / 零样本（第 15 页）
			pt_zero_shot(chatClient);

			// 2.2 单样本与少样本
			pt_ones_shot_few_shots(chatClient);

			// 2.3 系统提示、上下文提示与角色提示

				// 2.3.1 系统提示
				pt_system_prompting_1(chatClient);
				pt_system_prompting_2(chatClient);
				pt_system_prompting_2_springai_style(chatClient);

				// 2.3.2 角色提示
				pt_role_prompting_1(chatClient);
				pt_role_prompting_2(chatClient); // 以幽默风格。

				// 2.3.3 上下文提示
				pt_contextual_prompting(chatClient);

			// 2.4 退步提示
			pt_step_back_prompting(chatClient.mutate());

			// 2.5 思维链（CoT）
			pt_chain_of_thought_zero_shot(chatClient);
			pt_chain_of_thought_singleshot_fewshots(chatClient);

			// 2.6 自洽性
			pt_self_consistency(chatClient);

			// 2.7 思维树（ToT）
			pt_tree_of_thoughts_game(chatClient);
			pt_tree_of_thoughts_problem(chatClient);

			// 2.8 自动提示词工程
			pt_automatic_prompt_engineering(chatClient);

			// 2.9 代码提示

				// 2.9.1 用于编写代码的提示
				pt_code_prompting_writing_code(chatClient);

				// 2.9.2 用于解释代码的提示
				pt_code_prompting_explaining_code(chatClient);

				// 2.9.3 用于翻译代码的提示
				pt_code_prompting_translating_code(chatClient);
		//@formatter:on
	}

	// 2.1 通用提示 / 零样本（第 15 页）
	public void pt_zero_shot(ChatClient chatClient) {
		// 通用提示 / 零样本（第 15 页）

		enum Sentiment {

			POSITIVE, NEUTRAL, NEGATIVE

		}

		Sentiment reviewSentiment = chatClient.prompt("""
				Classify movie reviews as POSITIVE, NEUTRAL or NEGATIVE.
				Review: "Her" is a disturbing study revealing the direction
				humanity is headed if AI is allowed to keep evolving,
				unchecked. I wish there were more movies like this masterpiece.
				Sentiment:
				""")
			.options(ChatOptions.builder().temperature(0.1).maxTokens(5))
			.call()
			.entity(Sentiment.class);

		System.out.println("Output: " + reviewSentiment);
	}

	// 2.2 单样本与少样本
	public void pt_ones_shot_few_shots(ChatClient chatClient) {

		// 单样本与少样本
		// 为 AI 模型编写提示词时，提供示例会很有帮助。
		// 这些示例有助于模型理解你的需求。
		// - one-shot 提示：向模型提供单个示例。
		// - few-shot 提示：向模型提供多个示例。

		// 一个 few-shot 提示示例：沿用之前相同的 gemini-pro 模型
		// 配置，只是提高 token 上限以
		// 满足
		// 更长响应的需求。（第 16 页）

		String pizzaOrder = chatClient.prompt("""
				Parse a customer's pizza order into valid JSON

				EXAMPLE 1:
				I want a small pizza with cheese, tomato sauce, and pepperoni.
				JSON Response:
				```
				{
					"size": "small",
					"type": "normal",
					"ingredients": ["cheese", "tomato sauce", "peperoni"]
				}
				```

				EXAMPLE 2:
				Can I get a large pizza with tomato sauce, basil and mozzarella.
				JSON Response:
				```
				{
					"size": "large",
					"type": "normal",
					"ingredients": ["tomato sauce", "basil", "mozzarella"]
				}
				```

				Now, I would like a large pizza, with the first half cheese and mozzarella.
				And the other tomato sauce, ham and pineapple.

				""")
			.options(ChatOptions.builder()
				.temperature(0.1)
				// 提高 token 上限，以满足
				// 生成更长响应的需求。
				.maxTokens(250)
				)
			.call()
			.content();

		System.out.println("Output: " + pizzaOrder);

		// 注意：few-shot 提示所需的示例数量取决于多个
		// 因素，
		// 包括任务的复杂度、示例的质量，以及
		// 你所用生成式 AI
		// （gen AI）模型的能力。一般经验法则是，
		// 对于 few-shot 提示，
		// 至少使用三到五个示例。
		//
		// 要生成对各种输入都稳健的输出，重要的一点是
		// 在示例中包含
		// 边界情况。边界情况是不寻常或意外的输入，
		// 但模型仍应能够处理。
	}

	// 设置语言模型的整体上下文和目的。它
	// 定义模型应完成的"大图景"，例如翻译
	// 一门语言、对评论进行分类等。

	// 2.3.1 系统提示（1）
	public void pt_system_prompting_1(ChatClient chatClient) {

		String movieReview = chatClient.prompt()
			.system("Classify movie reviews as positive, neutral or negative. Only return the label in uppercase.")
			.user("""
					Review: "Her" is a disturbing study revealing the direction
					humanity is headed if AI is allowed to keep evolving,
					unchecked. It's so disturbing I couldn't watch it.

					Sentiment:
					""")
			.options(ChatOptions.builder().temperature(1.0).topK(40).maxTokens(5))
			.call()
			.content();

		System.out.println("Output: " + movieReview);

		// 我们提高了 temperature 以获得更高的创造性，并
		// 指定了更高的 token 上限。不过，由于我对
		// 输出格式有明确指令，模型没有返回多余文本
	}

	// 你可以使用系统提示生成与特定编程语言兼容的
	// 代码片段，也可以使用系统提示
	// 返回某种结构，例如 JSON 格式的输出。

	// 2.3.1 系统提示（2）
	public void pt_system_prompting_2(ChatClient chatClient) {

		String movieReview = chatClient.prompt().system("""
				Classify movie reviews as positive, neutral or negative. Return
				valid JSON.

				Use the Schema:

				```
				MOVIE:
				{
				"sentiment": String "POSITIVE" | "NEGATIVE" | "NEUTRAL",
				"name": String
				}
				MOVIE REVIEWS:
				{
				"movie_reviews": [MOVIE]
				}
				```
				""").user("""
				Review: "Her" is a disturbing study revealing the direction
				humanity is headed if AI is allowed to keep evolving,
				unchecked. It's so disturbing I couldn't watch it.

				JSON Response:
				""").options(ChatOptions.builder().temperature(1.0).topK(40).maxTokens(1024)).call().content();

		System.out.println("Output: " + movieReview);

	}

	// 2.3.1（3）spring-ai 系统提示 - SpringAI 风格
	public void pt_system_prompting_2_springai_style(ChatClient chatClient) {

		record MovieReviews(Movie[] movie_reviews) {
			enum Sentiment {

				POSITIVE, NEUTRAL, NEGATIVE

			}

			record Movie(Sentiment sentiment, String name) {
			}
		}

		MovieReviews movieReviews = chatClient.prompt()
			.system("""
					Classify movie reviews as positive, neutral or negative. Return
					valid JSON.
					""")
			.user("""
					Review: "Her" is a disturbing study revealing the direction
					humanity is headed if AI is allowed to keep evolving,
					unchecked. It's so disturbing I couldn't watch it.

					JSON Response:
					""")
			.options(ChatOptions.builder().temperature(1.0).topK(40).maxTokens(1024))
			.call()
			.entity(MovieReviews.class);

		System.out.println("Output: " + jsonMapper.writeValueAsString(movieReviews));
	}

	// 2.3.2 角色提示（1）
	public void pt_role_prompting_1(ChatClient chatClient) {

		// 目标：充当旅行指南并提供 3 条旅行建议

		String movieReview = chatClient.prompt().system("""
				I want you to act as a travel guide. I will write to you
				about my location and you will suggest 3 places to visit near
				me. In some cases, I will also give you the type of places I
				will visit.
				""").user("""
				My suggestion: "I am in Amsterdam and I want to visit only museums."
				Travel Suggestions:
				""").options(ChatOptions.builder().temperature(1.0).topK(40).maxTokens(1024)).call().content();

		System.out.println("Output: " + movieReview);

	}

	// 2.3.2 角色提示（2）
	public void pt_role_prompting_2(ChatClient chatClient) {

		// 目标：充当旅行指南并提供 3 条旅行建议
		//
		// 以幽默风格。

		String movieReview = chatClient.prompt().system("""
				I want you to act as a travel guide. I will write to you about
				my location and you will suggest 3 places to visit near me in
				a humorous style.
				""").user("""
				My suggestion: "I am in Amsterdam and I want to visit only museums."
				Travel Suggestions:
				""").options(ChatOptions.builder().temperature(1.0).topK(40).maxTokens(1024)).call().content();

		System.out.println("Output: " + movieReview);
	}

	// 2.3.3 上下文提示
	public void pt_contextual_prompting(ChatClient chatClient) {

		// 目标：为关于复古游戏的博客建议文章主题

		String movieReview = chatClient.prompt()
			.user(u -> u.text("""
					Suggest 3 topics to write an article about with a few lines of
					description of what this article should contain.

					Context: {context}
					""").param("context", "You are writing for a blog about retro 80's arcade video games."))
			.options(ChatOptions.builder().temperature(1.0).topK(40).maxTokens(1024))
			.call()
			.content();

		System.out.println("Output: " + movieReview);

	}

	// 2.4 退步提示
	public void pt_step_back_prompting(ChatClient.Builder chatClientBuilder) {

		// （SpringAI 提示）为聊天客户端设置通用选项。
		var chatClient = chatClientBuilder
			.defaultOptions(ChatOptions.builder().temperature(1.0).topK(40).maxTokens(1024))
			.build();

		// 目标：为第一人称射击游戏的某一关卡编写故事情节。

		String stepBack = chatClient.prompt("""
				Based on popular first-person shooter action games, what are
				5 fictional key settings that contribute to a challenging and
				engaging level storyline in a first-person shooter video game?
				""").call().content();

		System.out.println("StepBack Output: " + stepBack + "\n");

		String story = chatClient.prompt().user(u -> u.text("""
				Write a one paragraph storyline for a new level of a first-
				person shooter video game that is challenging and engaging.

				Context: {step-back}
				""").param("step-back", stepBack)).call().content();

		System.out.println("Output: " + story);

	}

	// 2.5 思维链（CoT）

	// “零样本”思维链。
	public void pt_chain_of_thought_zero_shot(ChatClient chatClient) {

		String output = chatClient.prompt("""
				When I was 3 years old, my partner was 3 times my age. Now,
				I am 20 years old. How old is my partner?

				Let's think step by step.
				""").call().content();

		System.out.println("Output: " + output + "\n");

	}

	public void pt_chain_of_thought_singleshot_fewshots(ChatClient chatClient) {

		String output = chatClient.prompt("""
				Q: When my brother was 2 years old, I was double his age. Now
				I am 40 years old. How old is my brother? Let's think step
				by step.
				A: When my brother was 2 years, I was 2 * 2 = 4 years old.
				That's an age difference of 2 years and I am older. Now I am 40
				years old, so my brother is 40 - 2 = 38 years old. The answer
				is 38.
				Q: When I was 3 years old, my partner was 3 times my age. Now,
				I am 20 years old. How old is my partner? Let's think step
				by step.
				A:
				""").call().content();

		System.out.println("Output: " + output + "\n");

	}

	// 2.6 自洽性（为基础测试简化）
	public void pt_self_consistency_simple(ChatClient chatClient) {
		String email = """
				Hi,
				I have seen you use Wordpress for your website. A great open
				source content management system. I have used it in the past
				too. It comes with lots of great user plugins. And it's pretty
				easy to set up.
				I did notice a bug in the contact form, which happens when
				you select the name field. See the attached screenshot of me
				entering text in the name field. Notice the JavaScript alert
				box that I inv0k3d.
				But for the rest it's a great website. I enjoy reading it. Feel
				free to leave the bug in the website, because it gives me more
				interesting things to read.
				Cheers,
				Harry the Hacker.
				""";

		record EmailClassification(Classification classification, String reasoning) {
			enum Classification {

				IMPORTANT, NOT_IMPORTANT

			}
		}

		// 简化版本 - 基础测试仅做一次分类
		EmailClassification output = chatClient.prompt()
			.user(u -> u.text("""
					Email: {email}
					Classify the above email as IMPORTANT or NOT IMPORTANT. Let's
					think step by step and explain why.
					""").param("email", email))
			.options(ChatOptions.builder().temperature(1.0).topK(60).maxTokens(1024))
			.call()
			.entity(EmailClassification.class);

		System.out.println("Classification: [" + output.classification() + "], Reason: " + output.reasoning());
		System.out.println("The email is " + output.classification() + ".");
	}

	// 2.6 自洽性（完整版本）
	public void pt_self_consistency(ChatClient chatClient) {

		String email = """
				Hi,
				I have seen you use Wordpress for your website. A great open
				source content management system. I have used it in the past
				too. It comes with lots of great user plugins. And it's pretty
				easy to set up.
				I did notice a bug in the contact form, which happens when
				you select the name field. See the attached screenshot of me
				entering text in the name field. Notice the JavaScript alert
				box that I inv0k3d.
				But for the rest it's a great website. I enjoy reading it. Feel
				free to leave the bug in the website, because it gives me more
				interesting things to read.
				Cheers,
				Harry the Hacker.
				""";

		record EmailClassification(Classification classification, String reasoning) {
			enum Classification {

				IMPORTANT, NOT_IMPORTANT

			}
		}

		int importantCount = 0;
		int notImportantCount = 0;

		for (int i = 0; i < 5; i++) {

			EmailClassification output = chatClient.prompt()
				.user(u -> u.text("""
						Email: {email}
						Classify the above email as IMPORTANT or NOT IMPORTANT. Let's
						think step by step and explain why.
						""").param("email", email))
				.options(ChatOptions.builder().temperature(1.0).topK(60).maxTokens(1024))
				.call()
				.entity(EmailClassification.class);

			if (output.classification() == EmailClassification.Classification.IMPORTANT) {
				importantCount++;
			}
			else {
				notImportantCount++;
			}

			System.out
				.println("Classification: [" + output.classification() + "], Reason: " + output.reasoning() + "\n");
		}

		if (importantCount > notImportantCount) {
			System.out.println("The email is IMPORTANT. Count: " + importantCount);
		}
		else {
			System.out.println("The email is NOT IMPORTANT. Count: " + notImportantCount);
		}

	}

	// 2.7 思维树（ToT）
	// 第 2.7 节实现：思维树（ToT）- 游戏求解示例
	public void pt_tree_of_thoughts_game(ChatClient chatClient) {
		// 步骤 1：生成多个初始走法
		String initialMoves = chatClient.prompt("""
				You are playing a game of chess. The board is in the starting position.
				Generate 3 different possible opening moves. For each move:
				1. Describe the move in algebraic notation
				2. Explain the strategic thinking behind this move
				3. Rate the move's strength from 1-10
				""").options(ChatOptions.builder().temperature(0.7)).call().content();

		System.out.println("Initial Moves: " + initialMoves + "\n");

		// 步骤 2：评估并选出最有希望的走法
		String bestMove = chatClient.prompt().user(u -> u.text("""
				Analyze these opening moves and select the strongest one:
				{moves}

				Explain your reasoning step by step, considering:
				1. Position control
				2. Development potential
				3. Long-term strategic advantage

				Then select the single best move.
				""").param("moves", initialMoves)).call().content();

		System.out.println("Best Move: " + bestMove + "\n");

		// 步骤 3：从最佳走法出发探索未来的对局状态
		String gameProjection = chatClient.prompt().user(u -> u.text("""
				Based on this selected opening move:
				{best_move}

				Project the next 3 moves for both players. For each potential branch:
				1. Describe the move and counter-move
				2. Evaluate the resulting position
				3. Identify the most promising continuation

				Finally, determine the most advantageous sequence of moves.
				""").param("best_move", bestMove)).call().content();

		System.out.println("Game Projection: " + gameProjection + "\n");
	}

	// 第 2.7 节实现：思维树（ToT）- 问题求解
	public void pt_tree_of_thoughts_problem(ChatClient chatClient) {
		String problem = "Design a system to recommend movies to users based on their viewing history.";

		// 步骤 1：生成多种解决方案思路
		String approaches = chatClient.prompt().user(u -> u.text("""
				Problem: {problem}

				Generate 3 different approaches to solve this problem:
				1. A content-based filtering approach
				2. A collaborative filtering approach
				3. A hybrid approach

				For each approach, describe:
				- The core algorithm/technique
				- Key data requirements
				- Potential advantages and limitations
				""").param("problem", problem)).call().content();

		System.out.println("Approaches: " + approaches + "\n");

		// 步骤 2：评估各方案并选出最有希望的
		String bestApproach = chatClient.prompt().user(u -> u.text("""
				Evaluate these solution approaches:
				{approaches}

				Compare them based on:
				1. Scalability
				2. Accuracy
				3. Implementation complexity
				4. Cold-start handling

				Select the most promising approach with detailed reasoning.
				""").param("approaches", approaches)).call().content();

		System.out.println("Best Approach: " + bestApproach + "\n");

		// 步骤 3：细化并展开所选方案
		String refinedSolution = chatClient.prompt().user(u -> u.text("""
				Based on the selected approach:
				{best_approach}

				Develop a detailed implementation plan:
				1. System architecture
				2. Data processing pipeline
				3. Algorithm implementation details
				4. Evaluation methodology

				Address any limitations identified earlier and propose solutions.
				""").param("best_approach", bestApproach)).call().content();

		System.out.println("Refined Solution: " + refinedSolution + "\n");
	}
	// 2.8 自动提示词工程

	public void pt_automatic_prompt_engineering(ChatClient chatClient) {

		String orderVariants = chatClient.prompt("""
				We have a band merchandise t-shirt webshop, and to train a
				chatbot we need various ways to order: "One Metallica t-shirt
				size S". Generate 10 variants, with the same semantics but keep
				the same meaning.
				""").options(ChatOptions.builder().temperature(1.0).topK(40).maxTokens(1024)).call().content();

		System.out.println("Variants: " + orderVariants + "\n");

		String output = chatClient.prompt()
			.user(u -> u.text("""
					Please perform BLEU (Bilingual Evaluation Understudy) evaluation on the following variants:
					----
					{variants}
					----

					Select the instruction candidate with the highest evaluation score.
					""").param("variants", orderVariants))
			.options(ChatOptions.builder().temperature(1.0).topK(40).maxTokens(1024))
			.call()
			.content();

		System.out.println("Output: " + output + "\n");

	}

	// 2.9.1 用于编写代码的提示
	public void pt_code_prompting_writing_code(ChatClient chatClient) {

		// 目标：编写一个提示，用 Bash 重命名文件夹中的文件。

		String output = chatClient.prompt("""
				Write a code snippet in Bash, which asks for a folder name.
				Then it takes the contents of the folder and renames all the
				files inside by prepending the name draft to the file name.
				""").options(ChatOptions.builder().temperature(0.1).maxTokens(1024)).call().content();

		System.out.println("Output: " + output + "\n");

	}

	// 2.9.2 用于解释代码的提示
	public void pt_code_prompting_explaining_code(ChatClient chatClient) {

		// 目标：编写一个用于解释 Bash 代码的提示。

		String code = """
				#!/bin/bash
				echo "Enter the folder name: "
				read folder_name
				if [ ! -d "$folder_name" ]; then
				echo "Folder does not exist."
				exit 1
				fi
				files=( "$folder_name"/* )
				for file in "${files[@]}"; do
				new_file_name="draft_$(basename "$file")"
				mv "$file" "$new_file_name"
				done
				echo "Files renamed successfully."
				""";

		String output = chatClient.prompt()
			.user(u -> u.text("""
					Explain to me the below Bash code:
					```
					{code}
					```
					""").param("code", code))
			.options(ChatOptions.builder().temperature(0.1).maxTokens(1024))
			.call()
			.content();

		System.out.println("Output: " + output + "\n");

	}

	// 2.9.3 用于翻译代码的提示
	public void pt_code_prompting_translating_code(ChatClient chatClient) {

		// 目标：编写一个把 Bash 代码翻译为 Python 的提示

		String bashCode = """
				```bash
				#!/bin/bash
				echo "Enter the folder name: "
				read folder_name
				if [ ! -d "$folder_name" ]; then
				echo "Folder does not exist."
				exit 1
				fi
				files=( "$folder_name"/* )
				for file in "${files[@]}"; do
				new_file_name="draft_$(basename "$file")"
				mv "$file" "$new_file_name"
				done
				echo "Files renamed successfully."
				```
				""";

		String output = chatClient.prompt()
			.user(u -> u.text("""
					Translate the below Bash code to a Python snippet:
					{code}
					""").param("code", bashCode))
			.options(ChatOptions.builder().temperature(0.1).maxTokens(1024))
			.call()
			.content();

		System.out.println("Output: " + output + "\n");

	}

}