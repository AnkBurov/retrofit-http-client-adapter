package io.ankburov.retrofit.httpclient;

import java.util.List;

import okhttp3.Request;
import okhttp3.Response;

public class DefaultInterceptorChain implements InterceptorChain {
    
    private List<Interceptor> interceptors;
    
    @Override
    public void setInterceptors(List<Interceptor> interceptors) {
        this.interceptors = List.copyOf(interceptors);
    }
    
    @Override
    public Request processOnRequest(Request request) {
        for (Interceptor interceptor : interceptors) {
            request = interceptor.onRequest(request);
        }
        return request;
    }
    
    @Override
    public Response processOnResult(Request request, Response response, Throwable throwable) {
        for (int i = interceptors.size() - 1; i >= 0; i--) {
            response = interceptors.get(i).onResult(request, response, throwable);
        }
        return response;
    }
}
