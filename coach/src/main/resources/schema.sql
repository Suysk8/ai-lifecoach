-- 启用 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 导入任务表
CREATE TABLE IF NOT EXISTS ingest_job (
    id BIGSERIAL PRIMARY KEY,
    source_path TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ingest_job_status ON ingest_job(status);

-- 转写结果表
CREATE TABLE IF NOT EXISTS recording_transcript (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES ingest_job(id) ON DELETE CASCADE,
    asr_model VARCHAR(128) NOT NULL,
    language VARCHAR(32),
    transcript TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_transcript_job_id ON recording_transcript(job_id);

-- 文档切块表
CREATE TABLE IF NOT EXISTS document_chunk (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES ingest_job(id) ON DELETE CASCADE,
    chunk_index INT NOT NULL,
    chunk_text TEXT NOT NULL,
    embedding vector(1024),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_chunk_job_idx ON document_chunk(job_id, chunk_index);

-- 全文检索索引
ALTER TABLE document_chunk ADD COLUMN IF NOT EXISTS chunk_tsv tsvector 
    GENERATED ALWAYS AS (to_tsvector('simple', chunk_text)) STORED;

CREATE INDEX IF NOT EXISTS idx_chunk_tsv ON document_chunk USING GIN (chunk_tsv);

-- 向量检索索引
CREATE INDEX IF NOT EXISTS idx_chunk_embedding_hnsw 
    ON document_chunk USING hnsw (embedding vector_cosine_ops);
