package suy.sk8.coach.api;

import com.github.lianjiatech.retrofit.spring.boot.core.RetrofitClient;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.*;
import suy.sk8.coach.dto.AsrResponse;

@RetrofitClient(baseUrl = "${dashscope.asr.base-url:https://dashscope.aliyuncs.com}")
public interface AsrApi {
    
    @POST("/api/v1/services/aigc/asr/transcription")
    @Multipart
    Call<AsrResponse> transcribe(
        @Header("Authorization") String authorization,
        @Part("model") String model,
        @Part("audio_format") String format,
        @Part("sample_rate") Integer sampleRate,
        @Part MultipartBody.Part file
    );
}