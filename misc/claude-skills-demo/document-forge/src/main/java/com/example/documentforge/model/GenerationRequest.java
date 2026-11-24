package com.example.documentforge.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 生成文档的请求。
 */
public record GenerationRequest(
		@NotNull(message = "Document type is required")
		DocumentType documentType,

		@NotBlank(message = "Prompt is required")
		String prompt,

		boolean useWatermark
) {
	/**
	 * 不带水印选项的构造函数（默认为 false）。
	 */
	public GenerationRequest(DocumentType documentType, String prompt) {
		this(documentType, prompt, false);
	}
}
