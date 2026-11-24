package com.example.documentforge.model;

import java.time.Instant;
import java.util.List;

/**
 * 文档生成请求的结果。
 */
public record GenerationResult(
		String id,
		DocumentType documentType,
		String prompt,
		String responseText,
		List<GeneratedFile> files,
		Instant createdAt,
		Status status,
		String errorMessage
) {

	/**
	 * 生成请求的状态。
	 */
	public enum Status {
		PENDING,
		COMPLETED,
		FAILED
	}

	/**
	 * 由 Claude Skills 生成的文件。
	 */
	public record GeneratedFile(
			String fileId,
			String filename,
			long size,
			String mimeType
	) {
	}

	/**
	 * 检查生成是否成功。
	 */
	public boolean success() {
		return status == Status.COMPLETED;
	}

	/**
	 * 检查生成是否仍在进行中。
	 */
	public boolean pending() {
		return status == Status.PENDING;
	}

	public static GenerationResult success(String id, DocumentType type, String prompt,
			String responseText, List<GeneratedFile> files) {
		return new GenerationResult(id, type, prompt, responseText, files, Instant.now(), Status.COMPLETED, null);
	}

	public static GenerationResult failure(String id, DocumentType type, String prompt, String errorMessage) {
		return new GenerationResult(id, type, prompt, null, List.of(), Instant.now(), Status.FAILED, errorMessage);
	}

	public static GenerationResult pending(String id, DocumentType type, String prompt) {
		return new GenerationResult(id, type, prompt, "Generating in background...", List.of(), Instant.now(), Status.PENDING, null);
	}

}
