package com.example.jinferdemo;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/** 模型可以调用的模拟工具；Spring AI 负责执行调用并将结果回传给模型。 */
class WeatherTools {

    @Tool(description = "Get the current weather for a city")
    String currentWeather(@ToolParam(description = "City name, for example Paris") String city) {
        return switch (city.toLowerCase()) {
            case "sofia" -> "12C, light rain in Sofia";
            case "paris" -> "18C, sunny in Paris";
            default -> "21C, partly cloudy in " + city;
        };
    }
}
