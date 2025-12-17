package suy.sk8.coach.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TextIngestRequest {
    @NotBlank
    private String text;
    
    private ChunkConfig chunk = new ChunkConfig();
    private EmbeddingConfig embedding = new EmbeddingConfig();
    
    @Data
    public static class ChunkConfig {
        private Integer maxChars = 800;
        private Integer overlapChars = 120;
    }
    
    @Data
    public static class EmbeddingConfig {
        private String model = "text-embedding-v4";
        private Integer dimensions = 1024;
    }
}
