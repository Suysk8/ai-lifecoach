package suy.sk8.coach.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SearchRequest {
    @NotBlank
    private String q;
    private Integer topK = 10;
    private Long jobId;
}
