package io.ankburov.retrofit.httpclient.adapter;

import okhttp3.ResponseBody;
import reactor.core.publisher.Mono;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ReactorRetrofitAdapter {
    
    @GET("rest/info/{product}")
    Mono<ResponseBody> getInfo(@Path("product") String product, @Query("detailed") boolean detailed);
}
