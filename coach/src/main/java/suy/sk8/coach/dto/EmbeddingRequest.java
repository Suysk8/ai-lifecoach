package suy.sk8.coach.dto;

import lombok.Data;

@Data
public class EmbeddingRequest {
    private String model;
    private String input;
    private Integer dimensions;
    
    public EmbeddingRequest(String model, String input, Integer dimensions) {
        this.model = model;
        this.input = input;
        this.dimensions = dimensions;
    }
}