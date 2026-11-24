package com.example.kotlin_hello_world

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.boot.CommandLineRunner
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@TestPropertySource(properties = [
    "spring.ai.openai.api-key=test-key-for-context-loading"
])
class KotlinHelloWorldApplicationTests {

	@MockitoBean
	private lateinit var jokeRunner: CommandLineRunner

	@Test
	fun contextLoads() {
		// 简单的冒烟测试 - 应用上下文应能无错误加载
		// 用于验证所有 Spring AI 配置都已正确设置
		// CommandLineRunner 被 mock，以避免测试期间发起真实的 API 调用
	}

}
