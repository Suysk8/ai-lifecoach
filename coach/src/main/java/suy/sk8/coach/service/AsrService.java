package suy.sk8.coach.service;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.File;

@Slf4j
@Service
public class AsrService {

    @Value("${dashscope.api-key}")
    private String apiKey;

    public String transcribe(String filePath, String model, String format, Integer sampleRate) {
        Recognition recognition = new Recognition();
        try {
            File file = new File(filePath);
            if (!file.exists() || !file.canRead()) {
                throw new RuntimeException("Audio file not found or not readable: " + filePath);
            }

            RecognitionParam param = RecognitionParam.builder()
                    .model(model)
                    .format(format)
                    .sampleRate(sampleRate)
                    .apiKey(apiKey)
                    .build();

            String jsonResult = recognition.call(param, file);
            log.info("ASR result for file {}: {}", filePath, jsonResult);

            return jsonResult;
        } catch (Exception e) {
            log.error("ASR transcription failed for file: {}", filePath, e);
            throw new RuntimeException("ASR failed: " + e.getMessage(), e);
        } finally {
            recognition.getDuplexApi().close(1000, "bye");
        }
    }
}