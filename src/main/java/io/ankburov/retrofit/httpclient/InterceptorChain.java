package io.ankburov.retrofit.httpclient;

import java.util.List;

import okhttp3.Request;
import okhttp3.Response;

public interface InterceptorChain {
    
    void setInterceptors(List<Interceptor> interceptors);
    
    Request processOnRequest(Request request);
    
    Response processOnResult(Request request, Response response, Throwable throwable);
}
