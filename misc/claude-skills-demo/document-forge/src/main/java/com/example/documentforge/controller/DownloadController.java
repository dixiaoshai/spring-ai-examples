package com.example.documentforge.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.anthropic.models.beta.files.BetaFileMetadata;

import com.example.documentforge.service.DocumentGenerationService;

/**
 * 文件下载控制器。
 */
@Controller
@RequestMapping("/download")
public class DownloadController {

	private static final Logger logger = LoggerFactory.getLogger(DownloadController.class);

	private final DocumentGenerationService generationService;

	public DownloadController(DocumentGenerationService generationService) {
		this.generationService = generationService;
	}

	@GetMapping("/{fileId}")
	public ResponseEntity<byte[]> download(@PathVariable String fileId) {
		logger.info("Download request for file: {}", fileId);

		try {
			// 获取文件元数据
			BetaFileMetadata metadata = generationService.getFileMetadata(fileId);

			// 下载文件内容
			byte[] content = generationService.downloadFile(fileId);

			// 构建带相应响应头的响应
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType(metadata.mimeType()));
			headers.setContentDisposition(ContentDisposition.attachment()
					.filename(metadata.filename())
					.build());
			headers.setContentLength(content.length);

			logger.info("Serving file: {} ({} bytes)", metadata.filename(), content.length);

			return ResponseEntity.ok()
					.headers(headers)
					.body(content);
		}
		catch (Exception e) {
			logger.error("Download failed for file {}: {}", fileId, e.getMessage());
			return ResponseEntity.notFound().build();
		}
	}

}
