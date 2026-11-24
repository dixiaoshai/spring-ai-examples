# Spring AI Hello World 聊天应用

一个简单的命令行聊天应用，演示 Spring AI 的 ChatClient 与 AI 模型配合使用的能力。

## 前置条件
- Java 17 或更高版本
- Maven

本示例使用 OpenAI 作为模型提供方。

在使用 AI 命令之前，请确保你拥有来自 OpenAI 的开发者 token。

在 [OpenAI Signup](https://platform.openai.com/signup) 创建账户，并在 [API Keys](https://platform.openai.com/account/api-keys) 生成 token。

Spring AI 项目定义了名为 `spring.ai.openai.api-key` 的配置属性，你应将其设置为从 OpenAI 获取的 API 密钥值。

导出环境变量是设置该配置属性的一种方式：

```shell
export SPRING_AI_OPENAI_API_KEY=<INSERT KEY HERE>
```



## 运行应用
1. 克隆仓库
2. 进入项目目录
3. 使用 Maven wrapper 运行应用：
   `./mvnw spring-boot:run`

## 交互示例
启动后，你会看到 Spring Boot 横幅，然后：

```text
Let's chat!
USER: tell me a joke
ASSISTANT: Why don't skeletons fight each other?
They don't have the guts.
```

## 技术细节

应用使用 Spring AI 的 ChatClient 与 AI 模型交互。以下演示代码展示了在 Spring Boot 应用中创建聊天交互的基本设置：

```java
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    CommandLineRunner cli(ChatClient.Builder builder) {
        return args -> {
            var chat = builder.build();
            var scanner = new Scanner(System.in);
            System.out.println("\nLet's chat!");
            while (true) {
                System.out.print("\nUSER: ");
                System.out.println("ASSISTANT: " +
                        chat.prompt(scanner.nextLine()).call().content());
            }
        };
    }
}
```

# 说明
1. 应用入口：

* `Application` 类使用 `@SpringBootApplication` 注解，将其标记为 Spring Boot 应用的主入口。
* `main` 方法通过调用 SpringApplication.run 启动应用。

2. 命令行运行器：
* 定义了 `CommandLineRunner` Bean，在应用启动后启动聊天功能。
* `ChatClient.Builder` 用于创建 ChatClient 实例，它是 AI 交互的核心接口。

3. 用户交互：
* 使用 `Scanner` 捕获来自命令行的用户输入。
* 在一个持续循环中，应用从用户读取一行输入（``USER``），并将其作为提示词发送给 `ChatClient`。
* 助手的响应通过 `chat.prompt(...).call().content()` 获取并显示为 `ASSISTANT'` 的回复。
* 这个简单的循环会一直持续，直到用户终止应用。