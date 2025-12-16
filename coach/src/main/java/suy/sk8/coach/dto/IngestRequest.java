package suy.sk8.coach.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class IngestRequest {
    @NotBlank
    private String path;
    
    private AsrConfig asr = new AsrConfig();
    private ChunkConfig chunk = new ChunkConfig();
    private EmbeddingConfig embedding = new EmbeddingConfig();
    
    @Data
    public static class AsrConfig {
        private String model = "paraformer-realtime-v2";
        private String format = "mp3";
        private Integer sampleRate = 16000;
    }
    
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
