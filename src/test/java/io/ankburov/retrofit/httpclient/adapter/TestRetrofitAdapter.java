package io.ankburov.retrofit.httpclient.adapter;

import java.util.concurrent.CompletableFuture;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TestRetrofitAdapter {
    
    @GET("rest/info/{product}")
    Call<ResponseBody> getInfo(@Path("product") String product, @Query("detailed") boolean detailed);
    
    @POST("rest/post")
    Call<Void> post();
    
    @POST("rest/post")
    CompletableFuture<Response<Void>> postAsync();
    
    @Headers({"custom: value"})
    @POST("rest/post-with-body")
    Call<Void> postWithBody(@Body RequestBody body);
    
    @Multipart
    @POST("rest/post-multipart")
    Call<ResponseBody> postMultiPart(@Part MultipartBody.Part file);
    
    @GET("rest/not-found}")
    Call<Void> getNotFound();
    
    @GET("rest/info/{product}")
    CompletableFuture<ResponseBody> getInfoAsync(@Path("product") String product, @Query("detailed") boolean detailed);
}
