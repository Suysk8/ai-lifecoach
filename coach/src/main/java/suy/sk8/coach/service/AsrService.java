package suy.sk8.coach.service;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;

@Slf4j
@Service
public class AsrService {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Value("${dashscope.api-key}")
    private String apiKey;
    
    public String transcribe(String filePath, String model, String format, Integer sampleRate) {
        try {
            File file = new File(filePath);
            if (!file.exists() || !file.canRead()) {
                throw new RuntimeException("Audio file not found or not readable: " + filePath);
            }
            
            Recognition recognition = new Recognition();
            RecognitionParam param = RecognitionParam.builder()
                    .model(model)
                    .format(format)
                    .sampleRate(sampleRate)
                    .apiKey(apiKey)
                    .build();
            
            String jsonResult = recognition.call(param, file);
            log.info("ASR result for file {}: {}", filePath, jsonResult);
            
            // 解析JSON结果，提取完整文本
            JsonNode root = objectMapper.readTree(jsonResult);
            JsonNode sentences = root.path("sentences");
            
            StringBuilder fullText = new StringBuilder();
            for (JsonNode sentence : sentences) {
                String text = sentence.path("text").asText();
                if (!text.isEmpty()) {
                    fullText.append(text);
                }
            }
            
            return fullText.toString();
        } catch (Exception e) {
            log.error("ASR transcription failed for file: {}", filePath, e);
            throw new RuntimeException("ASR failed: " + e.getMessage(), e);
        }
    }
}