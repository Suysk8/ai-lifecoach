package suy.sk8.coach.service;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkService {
    
    public List<String> chunk(String text, int maxChars, int overlapChars) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }
        
        String[] sentences = text.split("[。！？\\n]+");
        StringBuilder currentChunk = new StringBuilder();
        
        for (String sentence : sentences) {
            if (sentence.trim().isEmpty()) continue;
            
            if (currentChunk.length() + sentence.length() > maxChars && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                
                // 重叠窗口
                int overlapStart = Math.max(0, currentChunk.length() - overlapChars);
                currentChunk = new StringBuilder(currentChunk.substring(overlapStart));
            }
            
            currentChunk.append(sentence).append("。");
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        return chunks;
    }
}
