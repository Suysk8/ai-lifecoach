package suy.sk8.coach.service;

import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.alibaba.dashscope.embeddings.TextEmbeddingResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EmbeddingService {

    @Value("${dashscope.api-key}")
    private String apiKey;

    public List<Double> embed(String text, String model, Integer dimensions) {
        try {
            TextEmbeddingParam param = TextEmbeddingParam.builder()
                    .model(model)
                    .texts(Arrays.asList(text))
                    .apiKey(apiKey)
                    .parameter("dimension", dimensions)
                    .build();

            TextEmbedding textEmbedding = new TextEmbedding();
            TextEmbeddingResult result = textEmbedding.call(param);

            return result.getOutput().getEmbeddings().get(0).getEmbedding();
        } catch (Exception e) {
            log.error("Embedding generation failed", e);
            throw new RuntimeException("Embedding failed: " + e.getMessage(), e);
        }
    }

    public String formatVector(List<Double> vector) {
        return "[" + vector.stream().map(String::valueOf).collect(Collectors.joining(",")) + "]";
    }
}
