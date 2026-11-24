package com.example.documentforge.controller;

import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.documentforge.model.GenerationResult;
import com.example.documentforge.service.DocumentGenerationService;

/**
 * 生成历史记录管理的控制器。
 */
@Controller
@RequestMapping("/history")
public class HistoryController {

	private static final Logger logger = LoggerFactory.getLogger(HistoryController.class);

	private final DocumentGenerationService generationService;

	public HistoryController(DocumentGenerationService generationService) {
		this.generationService = generationService;
	}

	/**
	 * 以 HTMX 片段形式获取历史记录列表。
	 */
	@GetMapping
	public String getHistory(Model model) {
		List<GenerationResult> history = generationService.getHistory()
				.stream()
				.sorted(Comparator.comparing(GenerationResult::createdAt).reversed())
				.toList();

		model.addAttribute("history", history);
		return "fragments/history :: list";
	}

	/**
	 * 清空所有历史记录。
	 */
	@DeleteMapping
	public String clearHistory(Model model) {
		logger.info("Clearing all history");
		generationService.clearHistory();
		model.addAttribute("history", List.of());
		return "fragments/history :: cleared";
	}

	/**
	 * 删除指定的历史记录项。
	 */
	@DeleteMapping("/{id}")
	public String deleteHistoryItem(@PathVariable String id, Model model) {
		logger.info("Deleting history item: {}", id);
		generationService.removeFromHistory(id);

		List<GenerationResult> history = generationService.getHistory()
				.stream()
				.sorted(Comparator.comparing(GenerationResult::createdAt).reversed())
				.toList();

		model.addAttribute("history", history);
		return "fragments/history :: list";
	}

	/**
	 * 查看指定结果（用于异步任务结果）。
	 */
	@GetMapping("/{id}")
	public String viewResult(@PathVariable String id, Model model) {
		logger.info("Viewing result: {}", id);
		GenerationResult result = generationService.getResult(id);

		if (result == null) {
			model.addAttribute("error", "Result not found");
			return "fragments/response :: error";
		}

		model.addAttribute("result", result);

		if (result.pending()) {
			return "fragments/response :: pending";
		}
		else if (result.success()) {
			return "fragments/response :: success";
		}
		else {
			model.addAttribute("error", result.errorMessage());
			return "fragments/response :: error";
		}
	}

}
