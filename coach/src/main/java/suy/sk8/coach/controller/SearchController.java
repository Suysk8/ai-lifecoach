package suy.sk8.coach.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import suy.sk8.coach.dto.ApiResponse;
import suy.sk8.coach.dto.SearchRequest;
import suy.sk8.coach.dto.SearchResponse;
import suy.sk8.coach.service.SearchService;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {
    
    private final SearchService searchService;
    
    @PostMapping("/keyword")
    public ApiResponse<SearchResponse> searchKeyword(@Valid @RequestBody SearchRequest request) {
        try {
            SearchResponse response = searchService.searchKeyword(request);
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error("SEARCH_FAILED", e.getMessage());
        }
    }
    
    @PostMapping("/semantic")
    public ApiResponse<SearchResponse> searchSemantic(@Valid @RequestBody SearchRequest request) {
        try {
            SearchResponse response = searchService.searchSemantic(request);
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error("SEARCH_FAILED", e.getMessage());
        }
    }
}
