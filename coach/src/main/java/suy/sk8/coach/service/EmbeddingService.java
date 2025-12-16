package suy.sk8.coach.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import retrofit2.Response;
import suy.sk8.coach.api.DashScopeApi;
import suy.sk8.coach.dto.EmbeddingRequest;
import suy.sk8.coach.dto.EmbeddingResponse;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {
    
    @Value("${dashscope.api-key}")
    private String apiKey;
    
    private final DashScopeApi dashScopeApi;
    
    public List<Float> embed(String text, String model, Integer dimensions) {
        try {
            EmbeddingRequest request = new EmbeddingRequest(model, text, dimensions);
            Response<EmbeddingResponse> response = dashScopeApi.createEmbedding(
                "Bearer " + apiKey, request
            ).execute();
            
            if (!response.isSuccessful()) {
                throw new RuntimeException("Embedding API failed: " + response.code());
            }
            
            return response.body().getData().get(0).getEmbedding();
        } catch (Exception e) {
            log.error("Embedding generation failed", e);
            throw new RuntimeException("Embedding failed: " + e.getMessage(), e);
        }
    }
    
    public String formatVector(List<Float> vector) {
        return "[" + vector.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(",")) + "]";
    }
}
