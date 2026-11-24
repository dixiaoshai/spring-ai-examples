package com.example.transcription;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import com.qxotic.jinfer.Transcription;
import com.qxotic.jinfer.spring.ai.JinferTranscriptionModel;

import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
class TranscriptionController {

    record TranscriptResponse(String text, long elapsedMillis) {}

    record WordTiming(String text, double startSeconds, double endSeconds, double confidence) {}

    record DetailedTranscriptResponse(String text, List<WordTiming> words, double audioSeconds,
            long elapsedMillis, double realTimeFactor) {}

    private final TranscriptionModel transcriptionModel;

    private final JinferTranscriptionModel jinferTranscriptionModel;

    // 两个参数是同一个自动配置的 Bean，分别通过可移植的 Spring AI 接口
    // 和增加了词级时间戳的 jinfer 类型来访问。
    TranscriptionController(TranscriptionModel transcriptionModel, JinferTranscriptionModel jinferTranscriptionModel) {
        this.transcriptionModel = transcriptionModel;
        this.jinferTranscriptionModel = jinferTranscriptionModel;
    }

    /** 可移植的 Spring AI API：可与任何 TranscriptionModel 提供方配合使用，无需改动。 */
    @PostMapping("/transcribe")
    TranscriptResponse transcribe(@RequestParam("file") MultipartFile file) {
        long start = System.nanoTime();
        String text = this.transcriptionModel.transcribe(file.getResource());
        return new TranscriptResponse(text, millisSince(start));
    }

    /** jinfer 专用 API：返回完整的 Transcription，包含逐词时间跨度和置信度。 */
    @PostMapping("/transcribe/words")
    DetailedTranscriptResponse transcribeWithWords(@RequestParam("file") MultipartFile file) throws IOException {
        long start = System.nanoTime();
        Transcription transcription = this.jinferTranscriptionModel.transcribe(file.getBytes());
        long elapsed = millisSince(start);

        List<WordTiming> words = transcription.words()
            .stream()
            .map(w -> new WordTiming(w.text(), seconds(w.start()), seconds(w.end()), round(w.confidence())))
            .toList();
        // 最后一个 token 的结束时间即语音结束时间，是音频时长的下界
        double audioSeconds = transcription.tokens().isEmpty() ? 0
                : seconds(transcription.tokens().getLast().end());
        double realTimeFactor = elapsed == 0 ? 0 : round(audioSeconds * 1000 / elapsed);
        return new DetailedTranscriptResponse(transcription.text(), words, audioSeconds, elapsed, realTimeFactor);
    }

    private static long millisSince(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private static double seconds(Duration d) {
        return round(d.toNanos() / 1e9);
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }
}
