package suy.sk8.coach.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import retrofit2.Response;
import suy.sk8.coach.api.AsrApi;
import suy.sk8.coach.dto.AsrResponse;

import javax.annotation.Resource;
import java.io.File;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsrService {
    
    @Resource
    private final AsrApi asrApi;
    @Value("${dashscope.api-key}")
    private String apiKey;
    
    public String transcribe(String filePath, String model, String format, Integer sampleRate) {
        try {
            File file = new File(filePath);
            RequestBody fileBody = RequestBody.create(MediaType.parse("audio/*"), file);
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", file.getName(), fileBody);
            
            Response<AsrResponse> response = asrApi.transcribe(
                "Bearer " + apiKey,
                model,
                format,
                sampleRate,
                filePart
            ).execute();
            
            if (!response.isSuccessful()) {
                throw new RuntimeException("ASR API failed: " + response.code());
            }
            
            return response.body().getOutput().getText();
        } catch (Exception e) {
            log.error("ASR transcription failed", e);
            throw new RuntimeException("ASR failed: " + e.getMessage(), e);
        }
    }
}
