package suy.sk8.coach.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import suy.sk8.coach.entity.DocumentChunk;
import java.util.List;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {
    
    @Query(value = "SELECT * FROM document_chunk WHERE chunk_tsv @@ plainto_tsquery('simple', :query) ORDER BY ts_rank(chunk_tsv, plainto_tsquery('simple', :query)) DESC LIMIT :topK", nativeQuery = true)
    List<DocumentChunk> searchByKeyword(@Param("query") String query, @Param("topK") int topK);
    
    @Query(value = "SELECT *, 1 - (embedding <=> CAST(:queryVector AS vector)) AS score FROM document_chunk WHERE embedding IS NOT NULL ORDER BY embedding <=> CAST(:queryVector AS vector) LIMIT :topK", nativeQuery = true)
    List<DocumentChunk> searchBySemantic(@Param("queryVector") String queryVector, @Param("topK") int topK);
}
