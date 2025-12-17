package suy.sk8.coach.api;

import com.github.lianjiatech.retrofit.spring.boot.core.RetrofitClient;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import suy.sk8.coach.dto.AsrResponse;

@RetrofitClient(baseUrl = "${dashscope.base-url:}")
public interface AsrApi {

    @POST("v1/services/audio/asr/transcription")
    @Multipart
    Call<AsrResponse> transcribe(
            @Header("Authorization") String authorization,
            @Part("model") String model,
            @Part("audio_format") String format,
            @Part("sample_rate") Integer sampleRate,
            @Part MultipartBody.Part file);
}