# 音频知识库 Demo

基于 Spring Boot 3.x + Java 21 的本地音频知识库系统。

## 功能

- **Ingest API**: 将 MP3 音频文件转写为文本，切块并生成向量存储
- **Search API**: 支持关键词检索和语义检索

## 前置要求

1. **Java 21**
2. **PostgreSQL** (需安装 pgvector 扩展)
3. **百炼 API Key** (设置环境变量 `DASHSCOPE_API_KEY`)

## 数据库初始化

```bash
# 连接到 PostgreSQL
psql -U postgres

# 创建数据库
CREATE DATABASE audio_kb;

# 连接到数据库
\c audio_kb

# 执行初始化脚本
\i src/main/resources/schema.sql
```

## 运行

```bash
# 设置环境变量
export DASHSCOPE_API_KEY=your_api_key_here

# Windows
set DASHSCOPE_API_KEY=your_api_key_here

# 运行
./gradlew bootRun
```

## API 使用

### 1. 导入音频

```bash
curl -X POST http://localhost:8080/api/v1/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "path": "C:/path/to/your/audio.mp3"
  }'
```

### 2. 查询任务状态

```bash
curl http://localhost:8080/api/v1/ingest/{jobId}
```

### 3. 关键词检索

```bash
curl -X POST http://localhost:8080/api/v1/search/keyword \
  -H "Content-Type: application/json" \
  -d '{
    "q": "关键词",
    "topK": 10
  }'
```

### 4. 语义检索

```bash
curl -X POST http://localhost:8080/api/v1/search/semantic \
  -H "Content-Type: application/json" \
  -d '{
    "q": "你的问题",
    "topK": 10
  }'
```

## 项目结构

```
src/main/java/suy/sk8/coach/
├── controller/      # REST API 控制器
├── service/         # 业务逻辑层
├── repository/      # 数据访问层
├── entity/          # JPA 实体
├── dto/             # 数据传输对象
└── CoachApplication.java
```

## 技术栈

- Spring Boot 4.0.0
- Java 21
- PostgreSQL + pgvector
- 百炼 Fun-ASR (语音识别)
- 百炼 text-embedding-v4 (文本向量化)
