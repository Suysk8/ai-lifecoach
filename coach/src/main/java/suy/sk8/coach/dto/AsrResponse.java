package suy.sk8.coach.dto;

import lombok.Data;

@Data
public class AsrResponse {
    private Output output;
    
    @Data
    public static class Output {
        private String text;
    }
}