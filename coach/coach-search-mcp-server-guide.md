# 基于 Spring AI 搭建 `mcp-server`：将 `/api/v1/search/semantic` 暴露为 MCP Tool

> 目标：把现有 `SearchService.searchSemantic(SearchRequest)` 封装成 **MCP Tool**，并通过 Spring AI 的 **MCP Server Boot
Starter** 启动一个 `mcp-server`，让你的 AI 插件（作为 MCP Client）可以通过 **tools/list** 发现工具、通过 **tools/call**
> 调用语义检索。

---

## 1. 总体方案（推荐架构）

### 1.1 推荐：MCP Server 直接调用 Service（同 JVM 内）

- **mcp-server**：一个独立的 Spring Boot 应用/模块，负责对外暴露 MCP 协议（SSE / Streamable-HTTP / Stateless 等）。
- **工具实现**：用 `@McpTool` 暴露一个 Java 方法，方法内部直接调用 `SearchService.searchSemantic(...)`。
- **AI 插件**：作为 MCP Client，通过 MCP 协议调用 `semanticSearch` 工具。

**优点**：

- 不需要“再绕 HTTP 调自己”
- 延迟更低、类型更清晰
- 更容易加鉴权/审计/限流

## 2. 创建 `mcp-server` 工程

建议做成**独立 Spring Boot 工程**（或单独模块），方便你后续单独部署、单独做网关与权限。

### 2.1 选择传输协议（SSE vs Streamable-HTTP）

- **SSE**：兼容性最好（很多 MCP Client 都支持）。
- **Streamable-HTTP**：更现代的传输协议（如果你的 MCP Client 支持，推荐）。

> 你可以先用 SSE 跑通，再升级为 Streamable-HTTP。

---

## 3. 依赖配置（Gradle 示例）

> 以下以 **WebMVC** 为例（你当前项目也是 Spring MVC）。如果你是 WebFlux，请替换为 webflux starter。

```gradle
dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web")

  // MCP Server（WebMVC 传输）
  implementation("org.springframework.ai:spring-ai-starter-mcp-server-webmvc")

}
```

> 提示：生产建议用 Spring AI BOM 管理版本，避免依赖漂移。

---

## 4. 配置文件（application.yml）

下面示例使用 Streamable-HTTP（若你的客户端不支持，可切回 SSE）：

```yaml

spring:
  ai:
    mcp:
      server:
        name: "coach-search-mcp"

        # 推荐：STREAMABLE（若 client 支持）
        protocol: STREAMABLE

        # 可选：自定义端点路径（便于网关转发/统一前缀）
        sse-endpoint: /sse
        sse-message-endpoint: /mcp/message
```

---

## 5. 将语义检索暴露为 MCP Tool（核心）

### 5.1 Tool 的输入/输出设计建议

- **输入参数要稳定**：尽量避免把复杂/易变的内部 DTO 直接暴露给外部插件。
- **输出建议返回结构化 JSON**：既方便模型理解，也便于插件消费。

建议为 MCP 单独建一个更稳定的入参：

```java
public record McpSearchRequest(
        String query,
        Integer topK,
        Double minScore,
        String userId
) {
}
```

### 5.2 Tool 实现（直接复用 SearchService）

```java
import org.springframework.stereotype.Service;
import org.springframework.ai.mcp.server.annotation.McpTool;
import org.springframework.ai.mcp.server.annotation.McpToolParam;

@Service
public class SearchMcpTools {

    private final SearchService searchService;

    public SearchMcpTools(SearchService searchService) {
        this.searchService = searchService;
    }

    @McpTool(description = "语义检索：根据 query 在向量/语义索引中召回相关 chunk，返回排序后的结果")
    public SearchResponse semanticSearch(
            @McpToolParam(description = "查询文本", required = true) String query,
            @McpToolParam(description = "返回条数，默认 10") Integer topK,
            @McpToolParam(description = "最小相似度阈值，可选") Double minScore,
            @McpToolParam(description = "用户ID，可用于个性化/审计") String userId
    ) {
        SearchRequest req = new SearchRequest();
        req.setQuery(query);
        req.setTopK(topK == null ? 10 : topK);
        req.setMinScore(minScore);
        req.setUserId(userId);

        return searchService.searchSemantic(req);
    }
}
```

> 说明：上面 `SearchRequest/SearchResponse` 需要按你项目真实字段调整。

---

## 6.（可选）保留原 REST Controller

你原来的 Controller 可以保留：

- 继续给内部系统/调试使用
- MCP Tool 作为“给 AI 插件使用的标准化入口”

但推荐 MCP Tool **直接调 Service**，不要再绕 HTTP。

---