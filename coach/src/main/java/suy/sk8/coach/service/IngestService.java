package suy.sk8.coach.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import suy.sk8.coach.dto.IngestRequest;
import suy.sk8.coach.dto.IngestResponse;
import suy.sk8.coach.entity.DocumentChunk;
import suy.sk8.coach.entity.IngestJob;
import suy.sk8.coach.entity.RecordingTranscript;
import suy.sk8.coach.repository.DocumentChunkRepository;
import suy.sk8.coach.repository.IngestJobRepository;
import suy.sk8.coach.repository.RecordingTranscriptRepository;
import java.io.File;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestService {

    private final IngestJobRepository jobRepository;
    private final RecordingTranscriptRepository transcriptRepository;
    private final DocumentChunkRepository chunkRepository;
    private final AsrService asrService;
    private final ChunkService chunkService;
    private final EmbeddingService embeddingService;

    @Value("${recording.base-dir}")
    private String baseDir;

    @Transactional
    public IngestResponse ingest(IngestRequest request) {
        // 1. 拼接完整路径
        String fullPath = baseDir + File.separator + request.getPath();

        // 2. 创建任务
        IngestJob job = new IngestJob();
        job.setSourcePath(fullPath);
        job.setStatus("PENDING");

        try {
            // 3. 校验文件
            File file = new File(fullPath);
            if (!file.exists() || !file.isFile()) {
                throw new RuntimeException("File not found: " + fullPath);
            }

            job.setStatus("RUNNING");
            job.setUpdatedAt(OffsetDateTime.now());
            jobRepository.save(job);

            // 4. ASR 转写
            log.info("Starting ASR for job {}", job.getId());
            String transcript = asrService.transcribe(
                    fullPath,
                    request.getAsr().getModel(),
                    request.getAsr().getFormat(),
                    request.getAsr().getSampleRate());

            log.info("识别结果：{}", transcript);

            // 5. 保存转写结果
            RecordingTranscript recordingTranscript = new RecordingTranscript();
            recordingTranscript.setJobId(job.getId());
            recordingTranscript.setAsrModel(request.getAsr().getModel());
            recordingTranscript.setTranscript(transcript);
            recordingTranscript = transcriptRepository.save(recordingTranscript);

            // 6. 切块
            log.info("Chunking transcript for job {}", job.getId());
            List<String> chunks = chunkService.chunk(
                    transcript,
                    request.getChunk().getMaxChars(),
                    request.getChunk().getOverlapChars());

            // 7. 生成向量并保存
            log.info("Generating embeddings for {} chunks", chunks.size());
            for (int i = 0; i < chunks.size(); i++) {
                String chunkText = chunks.get(i);
                List<Double> embedding = embeddingService.embed(
                        chunkText,
                        request.getEmbedding().getModel(),
                        request.getEmbedding().getDimensions());

                DocumentChunk chunk = new DocumentChunk();
                chunk.setJobId(job.getId());
                chunk.setChunkIndex(i);
                chunk.setChunkText(chunkText);
                chunk.setEmbedding(embeddingService.formatVector(embedding));
                chunkRepository.save(chunk);
            }

            // 8. 更新任务状态
            job.setStatus("SUCCEEDED");
            job.setUpdatedAt(OffsetDateTime.now());
            jobRepository.save(job);

            return new IngestResponse(job.getId(), job.getStatus(), recordingTranscript.getId(), chunks.size());

        } catch (Exception e) {
            log.error("Ingest failed for job {}", job.getId(), e);
            job.setStatus("FAILED");
            job.setMessage(e.getMessage());
            job.setUpdatedAt(OffsetDateTime.now());
            jobRepository.save(job);
            throw new RuntimeException("Ingest failed: " + e.getMessage(), e);
        }
    }

    public IngestJob getJob(Long jobId) {
        return jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
    }

    @Transactional
    public IngestResponse ingestText(suy.sk8.coach.dto.TextIngestRequest request) {
        IngestJob job = new IngestJob();
        job.setSourcePath("text-input");
        job.setStatus("PENDING");
        job = jobRepository.save(job);

        try {
            job.setStatus("RUNNING");
            job.setUpdatedAt(OffsetDateTime.now());
            jobRepository.save(job);

            List<String> chunks = chunkService.chunk(
                    request.getText(),
                    request.getChunk().getMaxChars(),
                    request.getChunk().getOverlapChars());

            log.info("Generating embeddings for {} chunks", chunks.size());
            for (int i = 0; i < chunks.size(); i++) {
                String chunkText = chunks.get(i);
                List<Double> embedding = embeddingService.embed(
                        chunkText,
                        request.getEmbedding().getModel(),
                        request.getEmbedding().getDimensions());

                DocumentChunk chunk = new DocumentChunk();
                chunk.setJobId(job.getId());
                chunk.setChunkIndex(i);
                chunk.setChunkText(chunkText);
                chunk.setEmbedding(embeddingService.formatVector(embedding));
                chunkRepository.save(chunk);
            }

            job.setStatus("SUCCEEDED");
            job.setUpdatedAt(OffsetDateTime.now());
            jobRepository.save(job);

            return new IngestResponse(job.getId(), job.getStatus(), null, chunks.size());
        } catch (Exception e) {
            log.error("Text ingest failed for job {}", job.getId(), e);
            job.setStatus("FAILED");
            job.setMessage(e.getMessage());
            job.setUpdatedAt(OffsetDateTime.now());
            jobRepository.save(job);
            throw new RuntimeException("Text ingest failed: " + e.getMessage(), e);
        }
    }
}
