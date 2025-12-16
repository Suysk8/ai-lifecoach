package suy.sk8.coach.api;

import com.github.lianjiatech.retrofit.spring.boot.core.RetrofitClient;
import retrofit2.Call;
import retrofit2.http.*;
import suy.sk8.coach.dto.EmbeddingRequest;
import suy.sk8.coach.dto.EmbeddingResponse;

@RetrofitClient(baseUrl = "${dashscope.embedding.base-url}")
public interface DashScopeApi {
    
    @POST("/embeddings")
    @Headers("Content-Type: application/json")
    Call<EmbeddingResponse> createEmbedding(
        @Header("Authorization") String authorization,
        @Body EmbeddingRequest request
    );
}