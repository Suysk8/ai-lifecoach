# 本地音频知识库（ASR → Chunk → Embedding → pgvector）技术方案（Java 21 + Gradle + Spring Boot）

> 目标：在本地电脑上运行一个 Spring Boot 服务，提供 **ingest** 与 **search** 两类 API：
>
> - ingest：传入本地 MP3 绝对路径 → 调用百炼 **Fun‑ASR Java SDK** 转写 → chunk 切分 → 调用百炼 **text-embedding-v4**
    生成向量 → 写入 **PostgreSQL + pgvector**。
> - search：支持 **关键词检索**（全文索引）与 **语义检索**（pgvector 向量相似度）。
>
> 本文档为“可落地工程”导向：包含接口定义、链路流转、依赖/配置、表结构与关键实现要点。

---

## 1. 参考与依赖（官方/权威资料）

- **百炼 Fun‑ASR 实时语音识别 Java SDK**：`Recognition` 类支持同步识别与流式识别；支持音频格式含 `mp3`
  ，并给出参数（model/format/sampleRate）与流式发送建议（100ms、1KB~
  16KB）。[Fun‑ASR Java SDK 文档](https://www.alibabacloud.com/help/zh/model-studio/fun-asr-realtime-java-sdk)
  citeturn20search181
- **百炼 Embedding（text-embedding-v4）OpenAI 兼容模式**：提供 `base_url=https://dashscope.aliyuncs.com/compatible-mode/v1`
  与 `POST /embeddings` 端点，并给出 Java（OpenAI 兼容 SDK）调用示例与 `dimensions` 参数（v3/v4
  支持）。[文本向量同步接口](https://help.aliyun.com/zh/model-studio/text-embedding-synchronous-api)citeturn20search199
- **百炼 Embedding OpenAI 兼容说明**：强调只需调整 `base_url/api_key/model` 即可迁移，并列出 `text-embedding-v4`
  维度与免费额度等信息。[OpenAI 兼容 Embedding](https://help.aliyun.com/zh/model-studio/embedding-interfaces-compatible-with-openai)
  citeturn20search203
- **pgvector 官方仓库**：说明 pgvector 是 Postgres 扩展，支持精确/近似最近邻、距离度量（L2/inner product/cosine 等），并支持
  HNSW / IVFFlat 索引、`CREATE EXTENSION vector` 与 `vector(n)`
  类型等。[pgvector GitHub](https://github.com/pgvector/pgvector) citeturn20search194
- **Spring Boot 系统要求（用于选型与构建工具要求）**：Spring Boot 官方给出对 Java 版本与 Gradle 版本的最低要求区间（例如
  Gradle 8.14+ / 9.x 等）。[Spring Boot System Requirements](https://docs.spring.io/spring-boot/system-requirements.html)
  citeturn20search205

---

## 2. 总体架构

### 2.1 组件

1. **Spring Boot 服务（本地）**
    - 提供 REST API：`/api/v1/ingest/*`、`/api/v1/search/*`
    - 编排整个链路：读取 MP3 → ASR → chunk → embedding → 入库

2. **百炼（DashScope / Model Studio）**
    - **Fun‑ASR Java SDK**：用于把音频转换成文本。`Recognition` 提供同步与流式识别。citeturn20search181turn20search184
    - **Embedding（text-embedding-v4）**：用于把文本 chunk 转成向量；支持 OpenAI 兼容 `POST /compatible-mode/v1/embeddings`
      。citeturn20search199turn20search203

3. **PostgreSQL + pgvector**
    - 存储文本、chunk、向量与元数据；使用 pgvector 做向量相似度检索；可选 HNSW/IVFFlat 索引。citeturn20search194

### 2.2 数据流（链路流转）

**Ingest（写入链路）**

1. Client 调用 `POST /api/v1/ingest`，传入本地 MP3 绝对路径。
2. 服务端校验文件存在、后缀/类型（mp3），创建 `ingest_job` 记录（状态 `PENDING`）。
3. 服务端读取 MP3 文件为二进制流。
4. 调用百炼 **Fun‑ASR Java SDK**（同步或流式）：
    - 同步：一次性提交本地文件并阻塞获得结果；适用于录制好的音频。citeturn20search181
    - 流式：分段发送二进制音频帧，通过回调实时得到识别结果；建议每次发送约 100ms、1KB~16KB。citeturn20search181
5. 得到完整转写文本 `transcript`，写入 `recording_transcripts`。
6. 对 `transcript` 做 chunk 切分（建议按“字符数 + 句子边界 + 重叠窗口”）。
7. 对每个 chunk 调用百炼 **text-embedding-v4** 生成向量（OpenAI 兼容 `POST /embeddings`），写入 `document_chunks`（含
   embedding）。citeturn20search199turn20search203
8. 更新 `ingest_job` 状态为 `SUCCEEDED`（或失败时 `FAILED`），记录耗时与错误信息。

**Search（读取链路）**

1. Client 调用 `POST /api/v1/search`，指定 `mode=keyword|semantic|hybrid`。
2. keyword：使用 Postgres 全文检索（FTS）在 chunk 文本上检索。
3. semantic：将 query 调用百炼 Embedding 得到 query 向量，再用 pgvector 相似度算子与索引查询 TopK。
4. hybrid：将 keyword 结果与 semantic 结果融合（例如加权重排）。

---

## 3. 环境与配置

### 3.1 运行时与构建

- JDK：**Java 21**（你当前环境）。
- 构建：Gradle（建议 8.x 以上；Spring Boot 官方对 Gradle 版本有明确支持区间，需与所选 Spring Boot
  版本匹配）。citeturn20search205

> 推荐：使用 Spring Boot 3.x（兼容 Java 21），Gradle Wrapper 8.x；如选 Spring Boot 4.x，则同样满足 Java 17+ 与 Gradle
> 8.14+。citeturn20search205

### 3.2 必要环境变量

- `DASHSCOPE_API_KEY`：百炼 API Key。官方示例使用环境变量读取 API
  Key（而非硬编码）。citeturn20search181turn20search199turn20search203

### 3.3 百炼地域与地址

- Fun‑ASR WebSocket Base URL：文档示例包含北京地域 `wss://dashscope.aliyuncs.com/api-ws/v1/inference`
  ，并说明新加坡地域需替换为对应域名。citeturn20search184
- Embedding（OpenAI 兼容）Base URL：`https://dashscope.aliyuncs.com/compatible-mode/v1`；HTTP 端点 `
  POST https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings`。citeturn20search199turn20search203

---

## 4. Gradle（Java 21）依赖建议（build.gradle）

> 说明：下面是“依赖类别”建议。具体版本号请结合你选用的 Spring Boot 版本与 BOM 管理。

**Spring Boot 关键依赖**

- `spring-boot-starter-web`：REST API
- `spring-boot-starter-validation`：参数校验
- `spring-boot-starter-data-jpa` 或 `spring-jdbc`：数据访问（建议 JPA 或 jOOQ 任选其一）
- PostgreSQL Driver：`org.postgresql:postgresql`

**百炼 SDK / OpenAI 兼容 SDK（二选一或组合）**

1) Fun‑ASR：安装最新版 **DashScope SDK** 并使用 `com.alibaba.dashscope.audio.asr.recognition.Recognition`
   。citeturn20search181turn20search184
2) Embedding：
    - 方式 A：走 OpenAI 兼容 Java SDK（文档示例使用 `com.openai` 客户端并配置 `
      baseUrl=https://dashscope.aliyuncs.com/compatible-mode/v1`）。citeturn20search199turn20search203
    - 方式 B：直接 HTTP 调 `POST /embeddings`（可用 OkHttp/Apache HC 等）；端点同上。citeturn20search199

**pgvector JDBC 映射**

- pgvector 在数据库侧是扩展与类型；Java 侧向量可按 `float[]` 或 `List<Float>` 维护，写入时用 SQL 参数化拼接为`'[...,...]'`
  形式，或使用自定义 `AttributeConverter` / `PreparedStatement` 绑定。
- pgvector 的 `vector(n)` 类型与算子由扩展提供，需在 DB 中 `CREATE EXTENSION vector;`。citeturn20search194

---

## 5. 数据库设计（PostgreSQL + pgvector）

### 5.1 扩展启用

```sql
CREATE
EXTENSION IF NOT EXISTS vector;
```

pgvector 官方说明：每个数据库需要启用一次扩展，并提供 `vector(n)` 类型与相似度查询能力。citeturn20search194

### 5.2 表结构（建议）

> 说明：下面以 **单库** 存储元数据、文本与向量为目标；其中 `embedding` 是 pgvector 的 `vector(1024)`（示例维度
> 1024，可按你在百炼请求里指定的 `dimensions` 一致）。百炼 v4 支持多个维度（默认 1024），文档中给出 `dimensions`
> 参数与可选维度范围。citeturn20search199turn20search203

#### 5.2.1 ingest_job（导入任务）

```sql
CREATE TABLE IF NOT EXISTS ingest_job
(
    id
    BIGSERIAL
    PRIMARY
    KEY,
    source_path
    TEXT
    NOT
    NULL,
    status
    VARCHAR
(
    32
) NOT NULL, -- PENDING/RUNNING/SUCCEEDED/FAILED
    message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now
(
),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now
(
)
    );

CREATE INDEX IF NOT EXISTS idx_ingest_job_status ON ingest_job(status);
```

#### 5.2.2 recording_transcript（转写结果）

```sql
CREATE TABLE IF NOT EXISTS recording_transcript
(
    id
    BIGSERIAL
    PRIMARY
    KEY,
    job_id
    BIGINT
    NOT
    NULL
    REFERENCES
    ingest_job
(
    id
) ON DELETE CASCADE,
    asr_model VARCHAR
(
    128
) NOT NULL, -- e.g. fun-asr-realtime
    language VARCHAR
(
    32
),
    transcript TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now
(
)
    );

CREATE INDEX IF NOT EXISTS idx_transcript_job_id ON recording_transcript(job_id);
```

#### 5.2.3 document_chunk（切块与向量）

```sql
CREATE TABLE IF NOT EXISTS document_chunk
(
    id
    BIGSERIAL
    PRIMARY
    KEY,
    job_id
    BIGINT
    NOT
    NULL
    REFERENCES
    ingest_job
(
    id
) ON DELETE CASCADE,
    chunk_index INT NOT NULL,
    chunk_text TEXT NOT NULL,
    -- 示例：使用 1024 维向量（与 text-embedding-v4 dimensions=1024 保持一致）
    embedding vector
(
    1024
),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now
(
)
    );

CREATE UNIQUE INDEX IF NOT EXISTS uk_chunk_job_idx ON document_chunk(job_id, chunk_index);
```

### 5.3 索引建议

#### 5.3.1 关键词检索（FTS）

```sql
ALTER TABLE document_chunk
    ADD COLUMN IF NOT EXISTS chunk_tsv tsvector
    GENERATED ALWAYS AS (to_tsvector('simple', chunk_text)) STORED;

CREATE INDEX IF NOT EXISTS idx_chunk_tsv ON document_chunk USING GIN (chunk_tsv);
```

> 说明：中文更好的分词效果可引入额外分词插件（如 zhparser），但本项目本地 MVP 可以先用 `simple`。

#### 5.3.2 语义检索（pgvector）

pgvector 支持多种距离度量与 ANN 索引（HNSW / IVFFlat）。citeturn20search194

- **余弦距离**（常用于文本 embedding）：

```sql
CREATE INDEX IF NOT EXISTS idx_chunk_embedding_hnsw
    ON document_chunk
    USING hnsw (embedding vector_cosine_ops);
```

- 如果你希望更快建索引、牺牲一些召回，可用 `ivfflat`（一般需要在有一定数据量后再建）。

---

## 6. Chunk 策略（建议实现）

### 6.1 切分原则

- **目标 chunk 大小**：建议 300~800 中文字符（或 200~400 tokens 的量级），以兼顾语义完整与检索粒度。
- **边界优先**：尽量按句号/问号/分号/换行等边界切。
- **重叠窗口**：相邻 chunk 之间保留 10%~20% 的 overlap（提升上下文召回）。

### 6.2 输出结构

每个 chunk 需要至少：

- `job_id`
- `chunk_index`
- `chunk_text`

（可选增强）

- `start_offset/end_offset`（原文字符位置）
- `speaker`（如做说话人分离）
- `timestamp_start/timestamp_end`（如果 ASR 输出时间戳）

---

## 7. 百炼 SDK 集成要点

## 7.1 Fun‑ASR（Java SDK）

### 7.1.1 模型与参数

文档给出 Fun‑ASR 模型名示例 `fun-asr-realtime`，并展示设置 `format`、`sampleRate` 以及可选参数（如 `language_hints`
）。citeturn20search181turn20search184

### 7.1.2 同步识别（适合本地 MP3 文件）

- `Recognition` 类支持同步调用：传入本地文件并一次性返回完整结果；适合录制好的音频。citeturn20search181

> 你的方案是 ingest API 传入 mp3 路径后转写并落库；因此 MVP 建议先用 **同步识别**，实现更简单。

### 7.1.3 流式识别（可选增强）

- 流式模式：循环调用 `sendAudioFrame` 发送二进制音频流，服务端通过回调实时返回结果；建议每次发送约 100ms、1KB~
  16KB。citeturn20search181

---

## 7.2 Embedding（text-embedding-v4）

### 7.2.1 OpenAI 兼容模式

百炼 embedding 支持 OpenAI 兼容：只需调整 `base_url`、`api_key`、`model`，并可用 HTTP 端点 `POST /embeddings`
。citeturn20search199turn20search203

- `base_url`：`https://dashscope.aliyuncs.com/compatible-mode/v1`citeturn20search199turn20search203
- `endpoint`：`POST https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings`citeturn20search199
- `model`：`text-embedding-v4`citeturn20search199turn20search203
- `dimensions`：v4 支持指定向量维度（如 1024）；需与 DB 里 `vector(1024)` 一致。citeturn20search199turn20search203

---

## 8. 自定义 API 设计（REST）

> 约定：`Content-Type: application/json`，统一返回结构：
>
> ```json
> { "requestId": "...", "success": true, "data": { ... }, "error": null }
> ```

### 8.1 Ingest API

#### 8.1.1 创建导入任务（同步处理或异步处理）

- **URL**：`POST /api/v1/ingest`
- **Request**：

```json
{
  "path": "C:/Users/you/recordings/meeting.mp3",
  "asr": {
    "model": "fun-asr-realtime",
    "format": "mp3",
    "sampleRate": 16000,
    "languageHints": [
      "zh",
      "en"
    ],
    "mode": "sync"
  },
  "chunk": {
    "maxChars": 800,
    "overlapChars": 120
  },
  "embedding": {
    "model": "text-embedding-v4",
    "dimensions": 1024
  }
}
```

- **Response（成功）**：

```json
{
  "requestId": "...",
  "success": true,
  "data": {
    "jobId": 123,
    "status": "SUCCEEDED",
    "transcriptId": 456,
    "chunks": 37
  },
  "error": null
}
```

- **Response（失败示例）**：

```json
{
  "requestId": "...",
  "success": false,
  "data": null,
  "error": {
    "code": "ASR_FAILED",
    "message": "Fun-ASR 调用失败：..."
  }
}
```

> 说明：Fun‑ASR Java SDK 同步调用适合处理本地录制音频，并能一次性返回识别结果。citeturn20search181

#### 8.1.2 查询任务状态

- **URL**：`GET /api/v1/ingest/{jobId}`
- **Response**：

```json
{
  "requestId": "...",
  "success": true,
  "data": {
    "jobId": 123,
    "path": "C:/Users/you/recordings/meeting.mp3",
    "status": "SUCCEEDED",
    "message": null,
    "createdAt": "2025-12-15T00:00:00+08:00",
    "updatedAt": "2025-12-15T00:01:12+08:00"
  },
  "error": null
}
```

---

### 8.2 Search API

#### 8.2.1 关键词检索

- **URL**：`POST /api/v1/search/keyword`
- **Request**：

```json
{
  "q": "推荐 发券 策略",
  "topK": 10,
  "jobId": null
}
```

- **Response**：

```json
{
  "requestId": "...",
  "success": true,
  "data": {
    "mode": "keyword",
    "hits": [
      {
        "chunkId": 1001,
        "jobId": 123,
        "chunkIndex": 12,
        "score": 0.87,
        "text": "..."
      }
    ]
  },
  "error": null
}
```

#### 8.2.2 语义检索（向量）

- **URL**：`POST /api/v1/search/semantic`
- **Request**：

```json
{
  "q": "如何优化发券触达与转化",
  "topK": 10,
  "jobId": null,
  "embedding": {
    "model": "text-embedding-v4",
    "dimensions": 1024
  }
}
```

- **处理流程**：
    1) 使用百炼 embedding 将 query 转向量（OpenAI 兼容 `/embeddings`）。citeturn20search199turn20search203
    2) 使用 pgvector 按余弦距离排序返回 TopK。pgvector 支持多种距离与索引（如 HNSW/IVFFlat）。citeturn20search194

- **Response**：

```json
{
  "requestId": "...",
  "success": true,
  "data": {
    "mode": "semantic",
    "hits": [
      {
        "chunkId": 1002,
        "jobId": 123,
        "chunkIndex": 5,
        "score": 0.91,
        "text": "..."
      }
    ]
  },
  "error": null
}
```

#### 8.2.3 混合检索（可选增强）

- **URL**：`POST /api/v1/search/hybrid`
- **策略建议**：
    - 先做 keyword 召回（TopN），再在 TopN 内做向量重排；或 keyword / semantic 各召回 TopK，再按归一化分数加权融合。

---

## 9. 关键实现建议（工程化）

### 9.1 任务与事务边界

- ingest 过程是长链路：建议将 job 状态记录到 DB（`PENDING/RUNNING/SUCCEEDED/FAILED`），并在关键节点更新。
- ASR 成功后先写 transcript，再 chunk + embedding；embedding 建议批量提交（每批 N 个 chunk）以减少网络开销。

### 9.2 幂等与去重

- 对 `source_path` 计算 hash（或文件修改时间+大小），避免重复导入。
- `document_chunk(job_id, chunk_index)` 建唯一约束，避免重复写入。

### 9.3 维度一致性

- embedding 请求 `dimensions` 必须与 DB 的 `vector(dim)` 一致，否则会导致写入失败或检索不可比。
- 百炼 `text-embedding-v4` 支持多种维度（默认 1024），并允许在请求中指定。citeturn20search199turn20search203

### 9.4 关闭资源

- Fun‑ASR Java SDK 示例在 finally 中关闭 WebSocket 连接。citeturn20search181
- 数据库连接池、HTTP 客户端需复用并在应用停止时释放。

---

## 10. 最小可用验收（MVP Checklist）

1. DB：PostgreSQL 已安装并启用 `CREATE EXTENSION vector;`citeturn20search194
2. 服务启动：Spring Boot 端口可访问。
3. ingest：`POST /api/v1/ingest` 指定一个 mp3 路径，最终状态为 `SUCCEEDED`。
4. search/keyword：对转写内容关键词可命中。
5. search/semantic：语义 query 可返回合理 TopK（分数有序）。

---

## 11. 附录：你会用到的关键官方片段（便于对照）

- Fun‑ASR Java SDK：同步识别与流式识别说明、参数示例、关闭 WebSocket、流式发送音频帧建议（100ms / 1KB~
  16KB）。citeturn20search181
- Embedding v4：OpenAI 兼容 `base_url` 与 `POST /embeddings`，Java 示例（OpenAI 兼容 SDK），以及 `dimensions`
  参数。citeturn20search199turn20search203
- pgvector：扩展启用、`vector(n)` 类型、距离度量与 HNSW/IVFFlat 索引能力。citeturn20search194

