package suy.sk8.coach.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.UUID;

@Data
@AllArgsConstructor
public class ApiResponse<T> {
    private String requestId;
    private Boolean success;
    private T data;
    private ErrorInfo error;
    
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(UUID.randomUUID().toString(), true, data, null);
    }
    
    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(UUID.randomUUID().toString(), false, null, new ErrorInfo(code, message));
    }
    
    @Data
    @AllArgsConstructor
    public static class ErrorInfo {
        private String code;
        private String message;
    }
}
