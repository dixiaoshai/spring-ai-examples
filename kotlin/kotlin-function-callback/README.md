# Spring AI Kotlin 工具回调演示

本项目演示如何使用 Kotlin 在 Spring AI 中实现工具回调，特别是展示如何集成 OpenAI 的 API 来处理与天气相关的查询。

## 概述

应用展示了如何：
- 使用 Kotlin 配置 Spring AI
- 为天气信息实现工具回调
- 使用模拟天气服务进行演示
- 通过 Spring AI 完成 OpenAI API 集成

## 前置条件

- JDK 17 或更高版本
- Kotlin
- Spring Boot
- Spring AI 依赖
- OpenAI API 密钥（在应用配置中设置）

## 关键组件

### 1. 主应用类

`KotlinFunctionCallbackApplication` 类初始化 Spring Boot 应用，并配置聊天客户端以与 OpenAI 的 API 交互。它包含一个简单的演示，用于查询 San Francisco、Tokyo 和 Paris 的天气状况。

### 2. 配置

`Config` 类为工具回调配置必要的 Bean：
- 配置天气工具信息
- 配置模拟天气服务
- 定义工具回调结构

### 3. 天气服务

`MockKotlinWeatherService` 提供一个简单的模拟实现，为不同城市返回预定义的天气数据：

### 4. 数据模型

- `WeatherRequest`：表示天气查询参数
    - location（城市名）
    - latitude（纬度）
    - longitude（经度）
    - temperature unit（温度单位，C/F）

- `WeatherResponse`：包含天气信息
    - 当前温度
    - 体感温度
    - 最低温度
    - 最高温度
    - 气压
    - 湿度
    - 单位（C/F）

## 用法

1. 将你的 OpenAI API 密钥添加到 `application.properties`：
```properties
spring.ai.openai.api-key=your-api-key-here
```

2. 运行应用：
```bash
mvn spring-boot:run
```

应用将执行一次针对 San Francisco、Tokyo 和 Paris 的示例天气查询，演示工具回调如何在 Spring AI 中工作。

## 示例输出

```
Response: The current weather conditions in the three cities are:

San Francisco: 30°C
Tokyo: 10°C
Paris: 15°C
```

## 实现细节

1. 应用使用 Spring AI 的 `ChatClient` 与 OpenAI 的 API 交互
2. 通过 `FunctionToolCallback.builder()` 注册工具回调，返回 `ToolCallback` 实例
3. 天气查询由模拟服务处理，仅用于演示
4. 应用使用 Kotlin data class 实现类型安全的请求/响应处理

## 自定义

要修改模拟天气服务：
1. 更新 `MockKotlinWeatherService` 中的温度值
2. 按需添加新的城市天气状况
3. 用真实的天气 API 集成替换模拟服务
