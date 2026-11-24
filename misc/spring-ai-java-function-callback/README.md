# 基于 OpenAI 的 Spring AI Java 工具回调演示
这个 Spring Boot 应用演示如何使用 OpenAI 的 API 与 Spring AI 实现工具回调。应用通过一个模拟天气服务来模拟不同城市的天气信息检索。
## 概述
应用展示了如何：
- 将 OpenAI 的工具调用能力与 Spring Boot 集成
- 使用模拟天气服务处理与天气相关的查询
- 处理并响应自然语言的天气信息请求
## 组件
### 主应用（SpringAiJavaFunctionCallbackApplication）
主应用类包含：
- Spring Boot 配置
- 用于执行演示的命令行运行器
- 模拟天气服务实现
- 温度单位枚举（Celsius/Fahrenheit）
### 天气请求（WeatherRequest）
天气请求的数据模型，包含以下字段：
- location：城市与州（例如 "San Francisco, CA"）
- lat：纬度坐标
- lon：经度坐标
- unit：温度单位（C/F）
### 天气响应（WeatherResponse）
天气信息的数据模型，包含：
- temp：当前温度
- feels_like：体感温度
- temp_min：最低温度
- temp_max：最高温度
- pressure：大气压
- humidity：湿度百分比
- unit：温度单位（C/F）
## 模拟天气服务
模拟服务为三个城市提供模拟天气数据：
- San Francisco (30.0°C)
- Tokyo (10.0°C)
- Paris (15.0°C)
## 用法示例
应用通过命令行运行器演示用法：
```java
ToolCallback weatherFunctionInfo = FunctionToolCallback.builder("WeatherInfo", currentWeather)
    .description("Find the weather conditions, forecasts, and temperatures for a location, like a city or state.")
    .inputType(WeatherRequest.class)
    .build();

ChatResponse response = chatClient
    .prompt("What are the weather conditions in San Francisco, Tokyo, and Paris? Find the temperature in Celsius for each of the three locations.")
    .tools(weatherFunctionInfo)
    .call()
    .chatResponse();
```
## 依赖
项目使用 Maven 管理依赖。所有必需的依赖都包含在 pom.xml 文件中：
- Spring Boot Starter (spring-boot-starter)
- Spring AI OpenAI Starter (spring-ai-starter-model-openai)
- Spring Boot Starter Test (spring-boot-starter-test)
- 用于 JSON 处理的 Jackson 库
- 其他 Spring 框架依赖
  关于确切的版本和完整的依赖列表，请参阅项目根目录下的 pom.xml 文件。
## 安装
克隆仓库
在应用配置中设置 OpenAI API 密钥：
```properties
spring.ai.openai.api-key=your_api_key_here
```
使用以下命令运行应用：
```bash
./mvnw spring-boot:run
```

## 说明
- 这是一个使用模拟数据的演示应用
- 真实实现需要与实际的天气 API 集成
- 温度值是为演示目的而硬编码的
