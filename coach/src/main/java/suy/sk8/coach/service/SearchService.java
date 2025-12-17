package suy.sk8.coach.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import suy.sk8.coach.dto.SearchRequest;
import suy.sk8.coach.dto.SearchResponse;
import suy.sk8.coach.entity.DocumentChunk;
import suy.sk8.coach.repository.DocumentChunkRepository;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final DocumentChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;

    public SearchResponse searchKeyword(SearchRequest request) {
        List<DocumentChunk> chunks = chunkRepository.searchByKeyword(request.getQ(), request.getTopK());
        return new SearchResponse("keyword", toHits(chunks));
    }

    public SearchResponse searchSemantic(SearchRequest request) {
        List<Double> queryVector = embeddingService.embed(request.getQ(), "text-embedding-v4", 1024);
        String vectorStr = embeddingService.formatVector(queryVector);

        List<DocumentChunk> chunks = chunkRepository.searchBySemantic(vectorStr, request.getTopK());
        return new SearchResponse("semantic", toHits(chunks));
    }

    private List<SearchResponse.SearchHit> toHits(List<DocumentChunk> chunks) {
        return chunks.stream()
                .map(chunk -> new SearchResponse.SearchHit(
                        chunk.getId(), chunk.getJobId(), chunk.getChunkIndex(), 0.0, chunk.getChunkText()))
                .collect(Collectors.toList());
    }
}
