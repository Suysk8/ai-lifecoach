package suy.sk8.coach.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "recording_transcript")
public class RecordingTranscript {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "asr_model", nullable = false, length = 128)
    private String asrModel;

    @Column(length = 32)
    private String language;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String transcript;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
