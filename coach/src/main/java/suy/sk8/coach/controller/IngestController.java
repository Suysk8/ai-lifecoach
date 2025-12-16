package suy.sk8.coach.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import suy.sk8.coach.dto.ApiResponse;
import suy.sk8.coach.dto.IngestRequest;
import suy.sk8.coach.dto.IngestResponse;
import suy.sk8.coach.entity.IngestJob;
import suy.sk8.coach.service.IngestService;

@RestController
@RequestMapping("/api/v1/ingest")
@RequiredArgsConstructor
public class IngestController {
    
    private final IngestService ingestService;
    
    @PostMapping
    public ApiResponse<IngestResponse> ingest(@Valid @RequestBody IngestRequest request) {
        try {
            IngestResponse response = ingestService.ingest(request);
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error("INGEST_FAILED", e.getMessage());
        }
    }
    
    @GetMapping("/{jobId}")
    public ApiResponse<IngestJob> getJob(@PathVariable Long jobId) {
        try {
            IngestJob job = ingestService.getJob(jobId);
            return ApiResponse.success(job);
        } catch (Exception e) {
            return ApiResponse.error("JOB_NOT_FOUND", e.getMessage());
        }
    }
}
