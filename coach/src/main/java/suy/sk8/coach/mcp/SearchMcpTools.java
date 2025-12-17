package suy.sk8.coach.mcp;

import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;
import suy.sk8.coach.dto.SearchRequest;
import suy.sk8.coach.dto.SearchResponse;
import suy.sk8.coach.service.SearchService;

@Service
@RequiredArgsConstructor
public class SearchMcpTools {

    private final SearchService searchService;

    @McpTool(description = "语义检索：根据 query 在向量/语义索引中召回相关 chunk，返回排序后的结果")
    public SearchResponse semanticSearch(
            @McpToolParam(description = "查询文本", required = true) String query,
            @McpToolParam(description = "返回条数，默认 10") Integer topK,
            @McpToolParam(description = "任务ID，可选") Long jobId) {
        SearchRequest req = new SearchRequest();
        req.setQ(query);
        req.setTopK(topK == null ? 10 : topK);
        req.setJobId(jobId);

        return searchService.searchSemantic(req);
    }
}
