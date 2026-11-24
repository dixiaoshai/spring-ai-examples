package com.example.documentforge.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.http.HttpResponse;
import com.anthropic.models.beta.files.BetaFileMetadata;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.AnthropicSkillsResponseHelper;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

import com.example.documentforge.model.DocumentType;
import com.example.documentforge.model.GenerationRequest;
import com.example.documentforge.model.GenerationResult;
import com.example.documentforge.model.GenerationResult.GeneratedFile;

/**
 * 使用 Claude Skills 生成文档的服务。
 */
@Service
public class DocumentGenerationService {

	private static final Logger logger = LoggerFactory.getLogger(DocumentGenerationService.class);

	private final AnthropicChatModel chatModel;
	private final AnthropicClient anthropicClient;
	private final String customSkillId;

	// 生成历史记录的内存存储（真实应用中应基于会话）
	private final Map<String, GenerationResult> history = new ConcurrentHashMap<>();

	private final Executor asyncExecutor = Executors.newVirtualThreadPerTaskExecutor();

	public DocumentGenerationService(
			AnthropicChatModel chatModel,
			@org.springframework.beans.factory.annotation.Value("${document-forge.custom-skill-id:}") String customSkillId) {
		this.chatModel = chatModel;
		this.anthropicClient = chatModel.getAnthropicClient();
		this.customSkillId = customSkillId;
	}

	/**
	 * 检查某文档类型是否默认使用异步生成。
	 */
	public boolean shouldUseAsync(DocumentType type) {
		return type == DocumentType.PPTX;
	}

	/**
	 * 使用 Claude Skills 生成文档。
	 */
	public GenerationResult generate(GenerationRequest request) {
		String id = UUID.randomUUID().toString();
		logger.info("Generating {} document with id: {}", request.documentType(), id);

		try {
			// 构建包含 skill 专用指令的提示词
			String enhancedPrompt = buildEnhancedPrompt(request);

			// 使用相应的 skill 配置选项
			AnthropicChatOptions.Builder optionsBuilder = AnthropicChatOptions.builder()
					.skill(request.documentType().getSkill())
					.maxTokens(8192);

			// 如果请求并已配置，则添加自定义 skill
			if (request.useWatermark() && hasCustomSkill()) {
				logger.info("Adding custom skill: {}", customSkillId);
				optionsBuilder.skill(customSkillId);
			}

			AnthropicChatOptions options = optionsBuilder.build();

			Prompt prompt = new Prompt(List.of(new UserMessage(enhancedPrompt)), options);

			// 携带 Skills 调用 Claude
			ChatResponse response = chatModel.call(prompt);

			String responseText = response.getResult().getOutput().getText();
			logger.debug("Response text: {}", responseText);

			// 从响应中提取文件 ID
			List<String> fileIds = AnthropicSkillsResponseHelper.extractFileIds(response);
			logger.info("Extracted {} file(s) from response", fileIds.size());

			// 获取每个文件的元数据
			List<GeneratedFile> files = new ArrayList<>();
			for (String fileId : fileIds) {
				try {
					BetaFileMetadata metadata = anthropicClient.beta().files().retrieveMetadata(fileId);
					files.add(new GeneratedFile(fileId, metadata.filename(), metadata.sizeBytes(), metadata.mimeType()));
					logger.info("File: {} ({} bytes)", metadata.filename(), metadata.sizeBytes());
				}
				catch (Exception e) {
					logger.warn("Failed to get metadata for file {}: {}", fileId, e.getMessage());
					files.add(new GeneratedFile(fileId, "document." + request.documentType().getExtension(), 0L,
							"application/octet-stream"));
				}
			}

			GenerationResult result = GenerationResult.success(id, request.documentType(),
					request.prompt(), responseText, files);

			// 存入历史记录
			history.put(id, result);

			return result;
		}
		catch (Exception e) {
			logger.error("Failed to generate document: {}", e.getMessage(), e);
			GenerationResult result = GenerationResult.failure(id, request.documentType(),
					request.prompt(), e.getMessage());
			history.put(id, result);
			return result;
		}
	}

	/**
	 * 使用 Claude Skills 并附带源文件生成文档。
	 */
	public GenerationResult generateWithFile(GenerationRequest request, MultipartFile sourceFile) {
		String id = UUID.randomUUID().toString();
		logger.info("Generating {} document with id: {} from source file: {}", request.documentType(), id,
				sourceFile.getOriginalFilename());

		try {
			// 构建包含文件上下文的提示词
			String enhancedPrompt = buildEnhancedPromptWithFile(request, sourceFile);

			// 使用相应的 skill 配置选项
			AnthropicChatOptions.Builder optionsBuilder = AnthropicChatOptions.builder()
					.skill(request.documentType().getSkill())
					.maxTokens(8192);

			// 如果请求并已配置，则添加自定义 skill
			if (request.useWatermark() && hasCustomSkill()) {
				logger.info("Adding custom skill: {}", customSkillId);
				optionsBuilder.skill(customSkillId);
			}

			AnthropicChatOptions options = optionsBuilder.build();

			// 创建消息 - 文本文件将内容包含在提示词中
			// PDF 则作为媒体附件
			UserMessage userMessage;
			String contentType = sourceFile.getContentType();
			String filename = sourceFile.getOriginalFilename();

			if (isPdfFile(filename, contentType)) {
				// 将 PDF 作为媒体附件
				MimeType mimeType = MimeType.valueOf("application/pdf");
				ByteArrayResource resource = new ByteArrayResource(sourceFile.getBytes());
				Media media = new Media(mimeType, resource);
				userMessage = UserMessage.builder()
						.text(enhancedPrompt)
						.media(media)
						.build();
				logger.debug("Attached PDF file as media");
			}
			else {
				// 对于文本类文件，将内容包含在提示词中
				userMessage = new UserMessage(enhancedPrompt);
			}

			Prompt prompt = new Prompt(List.of(userMessage), options);

			// 携带 Skills 调用 Claude
			ChatResponse response = chatModel.call(prompt);

			String responseText = response.getResult().getOutput().getText();
			logger.debug("Response text: {}", responseText);

			// 从响应中提取文件 ID
			List<String> fileIds = AnthropicSkillsResponseHelper.extractFileIds(response);
			logger.info("Extracted {} file(s) from response", fileIds.size());

			// 获取每个文件的元数据
			List<GeneratedFile> files = new ArrayList<>();
			for (String fileId : fileIds) {
				try {
					BetaFileMetadata metadata = anthropicClient.beta().files().retrieveMetadata(fileId);
					files.add(new GeneratedFile(fileId, metadata.filename(), metadata.sizeBytes(), metadata.mimeType()));
					logger.info("File: {} ({} bytes)", metadata.filename(), metadata.sizeBytes());
				}
				catch (Exception e) {
					logger.warn("Failed to get metadata for file {}: {}", fileId, e.getMessage());
					files.add(new GeneratedFile(fileId, "document." + request.documentType().getExtension(), 0L,
							"application/octet-stream"));
				}
			}

			GenerationResult result = GenerationResult.success(id, request.documentType(), request.prompt(),
					responseText, files);

			history.put(id, result);
			return result;
		}
		catch (Exception e) {
			logger.error("Failed to generate document: {}", e.getMessage(), e);
			GenerationResult result = GenerationResult.failure(id, request.documentType(), request.prompt(),
					e.getMessage());
			history.put(id, result);
			return result;
		}
	}

	/**
	 * 启动异步生成（立即返回处于 pending 状态的结果）。
	 */
	public GenerationResult generateAsync(GenerationRequest request) {
		String id = UUID.randomUUID().toString();
		logger.info("Starting async generation for {} document with id: {}", request.documentType(), id);

		// 创建 pending 状态的结果
		GenerationResult pendingResult = GenerationResult.pending(id, request.documentType(), request.prompt());
		history.put(id, pendingResult);

		// 在后台运行生成
		CompletableFuture.runAsync(() -> {
			try {
				GenerationResult result = doGenerate(id, request, null);
				history.put(id, result);
				logger.info("Async generation completed for id: {}", id);
			}
			catch (Exception e) {
				logger.error("Async generation failed for id: {}", id, e);
				history.put(id, GenerationResult.failure(id, request.documentType(), request.prompt(), e.getMessage()));
			}
		}, asyncExecutor);

		return pendingResult;
	}

	/**
	 * 启动带文件的异步生成（立即返回处于 pending 状态的结果）。
	 */
	public GenerationResult generateWithFileAsync(GenerationRequest request, byte[] fileContent, String fileName,
			String contentType) {
		String id = UUID.randomUUID().toString();
		logger.info("Starting async generation for {} document with id: {} from file: {}", request.documentType(), id,
				fileName);

		// 创建 pending 状态的结果
		GenerationResult pendingResult = GenerationResult.pending(id, request.documentType(), request.prompt());
		history.put(id, pendingResult);

		// 在后台运行生成
		CompletableFuture.runAsync(() -> {
			try {
				GenerationResult result = doGenerateWithFile(id, request, fileContent, fileName, contentType);
				history.put(id, result);
				logger.info("Async generation completed for id: {}", id);
			}
			catch (Exception e) {
				logger.error("Async generation failed for id: {}", id, e);
				history.put(id, GenerationResult.failure(id, request.documentType(), request.prompt(), e.getMessage()));
			}
		}, asyncExecutor);

		return pendingResult;
	}

	/**
	 * 执行生成的内部方法（同步和异步路径共用）。
	 */
	private GenerationResult doGenerate(String id, GenerationRequest request, Void unused) {
		// 委托给现有的生成逻辑，但使用指定的 ID
		try {
			String enhancedPrompt = buildEnhancedPrompt(request);

			AnthropicChatOptions.Builder optionsBuilder = AnthropicChatOptions.builder()
					.skill(request.documentType().getSkill())
					.maxTokens(8192);

			// 如果请求并已配置，则添加自定义 skill
			if (request.useWatermark() && hasCustomSkill()) {
				logger.info("Adding custom skill (async): {}", customSkillId);
				optionsBuilder.skill(customSkillId);
			}

			AnthropicChatOptions options = optionsBuilder.build();

			Prompt prompt = new Prompt(List.of(new UserMessage(enhancedPrompt)), options);
			ChatResponse response = chatModel.call(prompt);

			String responseText = response.getResult().getOutput().getText();
			List<String> fileIds = AnthropicSkillsResponseHelper.extractFileIds(response);

			List<GeneratedFile> files = extractFiles(fileIds, request.documentType());

			return GenerationResult.success(id, request.documentType(), request.prompt(), responseText, files);
		}
		catch (Exception e) {
			logger.error("Generation failed: {}", e.getMessage(), e);
			return GenerationResult.failure(id, request.documentType(), request.prompt(), e.getMessage());
		}
	}

	/**
	 * 执行带文件生成的内部方法。
	 */
	private GenerationResult doGenerateWithFile(String id, GenerationRequest request, byte[] fileContent,
			String fileName, String contentType) {
		try {
			String enhancedPrompt = buildEnhancedPromptWithFileContent(request, fileContent, fileName, contentType);

			AnthropicChatOptions.Builder optionsBuilder = AnthropicChatOptions.builder()
					.skill(request.documentType().getSkill())
					.maxTokens(8192);

			// 如果请求并已配置，则添加自定义 skill
			if (request.useWatermark() && hasCustomSkill()) {
				logger.info("Adding custom skill (async with file): {}", customSkillId);
				optionsBuilder.skill(customSkillId);
			}

			AnthropicChatOptions options = optionsBuilder.build();

			UserMessage userMessage;
			if (isPdfFile(fileName, contentType)) {
				MimeType mimeType = MimeType.valueOf("application/pdf");
				ByteArrayResource resource = new ByteArrayResource(fileContent);
				Media media = new Media(mimeType, resource);
				userMessage = UserMessage.builder().text(enhancedPrompt).media(media).build();
			}
			else {
				userMessage = new UserMessage(enhancedPrompt);
			}

			Prompt prompt = new Prompt(List.of(userMessage), options);
			ChatResponse response = chatModel.call(prompt);

			String responseText = response.getResult().getOutput().getText();
			List<String> fileIds = AnthropicSkillsResponseHelper.extractFileIds(response);

			List<GeneratedFile> files = extractFiles(fileIds, request.documentType());

			return GenerationResult.success(id, request.documentType(), request.prompt(), responseText, files);
		}
		catch (Exception e) {
			logger.error("Generation with file failed: {}", e.getMessage(), e);
			return GenerationResult.failure(id, request.documentType(), request.prompt(), e.getMessage());
		}
	}

	/**
	 * 从文件 ID 中提取文件元数据。
	 */
	private List<GeneratedFile> extractFiles(List<String> fileIds, DocumentType documentType) {
		List<GeneratedFile> files = new ArrayList<>();
		for (String fileId : fileIds) {
			try {
				BetaFileMetadata metadata = anthropicClient.beta().files().retrieveMetadata(fileId);
				files.add(new GeneratedFile(fileId, metadata.filename(), metadata.sizeBytes(), metadata.mimeType()));
			}
			catch (Exception e) {
				logger.warn("Failed to get metadata for file {}: {}", fileId, e.getMessage());
				files.add(new GeneratedFile(fileId, "document." + documentType.getExtension(), 0L,
						"application/octet-stream"));
			}
		}
		return files;
	}

	/**
	 * 构建包含文件内容的增强提示词（用于已有 byte[] 的异步场景）。
	 */
	private String buildEnhancedPromptWithFileContent(GenerationRequest request, byte[] fileContent, String fileName,
			String contentType) {
		StringBuilder promptBuilder = new StringBuilder();

		String typeInstruction = switch (request.documentType()) {
			case XLSX -> "Analyze the provided source document and create an Excel spreadsheet (.xlsx file). "
					+ "Extract relevant data and organize it into a well-structured spreadsheet.";
			case PPTX -> "Analyze the provided source document and create a PowerPoint presentation (.pptx file). "
					+ "Extract key points and create a professional presentation with clear slides.";
			case DOCX -> "Analyze the provided source document and create a Word document (.docx file). "
					+ "Transform the content into a well-formatted document with proper structure.";
			case PDF -> "Analyze the provided source document and create a PDF document. "
					+ "Transform the content into a professionally formatted PDF.";
		};

		promptBuilder.append(typeInstruction);
		promptBuilder.append("\n\nUser instructions: ").append(request.prompt());

		if (!isPdfFile(fileName, contentType)) {
			String textContent = new String(fileContent, StandardCharsets.UTF_8);
			promptBuilder.append("\n\n--- SOURCE DOCUMENT CONTENT (").append(fileName).append(") ---\n");
			promptBuilder.append(textContent);
			promptBuilder.append("\n--- END OF SOURCE DOCUMENT ---");
		}
		else {
			promptBuilder.append("\n\nI have attached a PDF file named '").append(fileName)
					.append("'. Please analyze its contents.");
		}

		return promptBuilder.toString();
	}

	/**
	 * 按 ID 下载文件。
	 */
	public byte[] downloadFile(String fileId) {
		logger.info("Downloading file: {}", fileId);
		try (HttpResponse httpResponse = anthropicClient.beta().files().download(fileId)) {
			return httpResponse.body().readAllBytes();
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to download file: " + fileId, e);
		}
	}

	/**
	 * 获取文件元数据。
	 */
	public BetaFileMetadata getFileMetadata(String fileId) {
		return anthropicClient.beta().files().retrieveMetadata(fileId);
	}

	/**
	 * 获取生成历史记录。
	 */
	public List<GenerationResult> getHistory() {
		return new ArrayList<>(history.values());
	}

	/**
	 * 获取指定的生成结果。
	 */
	public GenerationResult getResult(String id) {
		return history.get(id);
	}

	/**
	 * 清空历史记录。
	 */
	public void clearHistory() {
		history.clear();
	}

	/**
	 * 从历史记录中移除指定项。
	 */
	public void removeFromHistory(String id) {
		history.remove(id);
	}

	/**
	 * 构建包含文档类型专用指令的增强提示词。
	 */
	private String buildEnhancedPrompt(GenerationRequest request) {
		String typeInstruction = switch (request.documentType()) {
			case XLSX -> "Create an Excel spreadsheet (.xlsx file) with the following requirements. " +
					"Use appropriate formatting, formulas where relevant, and organize data clearly. ";
			case PPTX -> "Create a PowerPoint presentation (.pptx file) with the following requirements. " +
					"Use a professional design, clear slide titles, and organized content. ";
			case DOCX -> "Create a Word document (.docx file) with the following requirements. " +
					"Use proper formatting, headings, and professional layout. ";
			case PDF -> "Create a PDF document with the following requirements. " +
					"Ensure professional formatting and clear organization. ";
		};

		return typeInstruction + request.prompt();
	}

	/**
	 * 构建包含文件内容的增强提示词。
	 */
	private String buildEnhancedPromptWithFile(GenerationRequest request, MultipartFile sourceFile)
			throws IOException {
		StringBuilder promptBuilder = new StringBuilder();

		// 添加文档类型指令
		String typeInstruction = switch (request.documentType()) {
			case XLSX -> "Analyze the provided source document and create an Excel spreadsheet (.xlsx file). " +
					"Extract relevant data and organize it into a well-structured spreadsheet with appropriate " +
					"formatting and formulas where relevant. ";
			case PPTX -> "Analyze the provided source document and create a PowerPoint presentation (.pptx file). " +
					"Extract key points and create a professional presentation with clear slides, " +
					"organized content, and appropriate visual structure. ";
			case DOCX -> "Analyze the provided source document and create a Word document (.docx file). " +
					"Transform the content into a well-formatted document with proper headings, " +
					"structure, and professional layout. ";
			case PDF -> "Analyze the provided source document and create a PDF document. " +
					"Transform the content into a professionally formatted PDF with clear organization. ";
		};

		promptBuilder.append(typeInstruction);
		promptBuilder.append("\n\nUser instructions: ").append(request.prompt());

		// 对于文本类文件，将内容包含在提示词中
		String filename = sourceFile.getOriginalFilename();
		String contentType = sourceFile.getContentType();

		if (!isPdfFile(filename, contentType)) {
			String fileContent = new String(sourceFile.getBytes(), StandardCharsets.UTF_8);
			promptBuilder.append("\n\n--- SOURCE DOCUMENT CONTENT (").append(filename).append(") ---\n");
			promptBuilder.append(fileContent);
			promptBuilder.append("\n--- END OF SOURCE DOCUMENT ---");
		}
		else {
			promptBuilder.append("\n\nI have attached a PDF file named '").append(filename)
					.append("'. Please analyze its contents.");
		}

		return promptBuilder.toString();
	}

	/**
	 * 根据文件名或内容类型判断文件是否为 PDF。
	 */
	private boolean isPdfFile(String filename, String contentType) {
		return "application/pdf".equals(contentType)
				|| (filename != null && filename.toLowerCase().endsWith(".pdf"));
	}

	/**
	 * 检查是否已配置自定义 skill。
	 */
	public boolean hasCustomSkill() {
		return customSkillId != null && !customSkillId.isBlank();
	}

}

