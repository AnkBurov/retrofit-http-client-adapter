package io.ankburov.retrofit.httpclient;

import java.io.InputStream;
import java.net.http.HttpResponse;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Convert Http Client response to Retrofit response
 */
public interface RetrofitResponseFactory {
    
    Response build(Request retrofitRequest, HttpResponse<InputStream> response);
}
