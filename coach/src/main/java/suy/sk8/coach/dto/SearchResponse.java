package suy.sk8.coach.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class SearchResponse {
    private String mode;
    private List<SearchHit> hits;
    
    @Data
    @AllArgsConstructor
    public static class SearchHit {
        private Long chunkId;
        private Long jobId;
        private Integer chunkIndex;
        private Double score;
        private String text;
    }
}
