package suy.sk8.coach.dto;

import lombok.Data;
import java.util.List;

@Data
public class EmbeddingResponse {
    private List<EmbeddingData> data;
    
    @Data
    public static class EmbeddingData {
        private List<Float> embedding;
    }
}