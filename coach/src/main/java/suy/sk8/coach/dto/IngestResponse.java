package suy.sk8.coach.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class IngestResponse {
    private Long jobId;
    private String status;
    private Long transcriptId;
    private Integer chunks;
}
