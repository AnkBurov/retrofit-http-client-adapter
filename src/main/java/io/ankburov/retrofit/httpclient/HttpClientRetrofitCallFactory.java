package io.ankburov.retrofit.httpclient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import okhttp3.Call;
import okhttp3.Request;

public class HttpClientRetrofitCallFactory implements Call.Factory {
    
    @NotNull
    private final HttpClient httpClient;
    
    @NotNull
    private final HttpRequestFactory requestFactory;
    
    @NotNull
    private final RetrofitResponseFactory responseFactory;
    
    @NotNull
    private final Executor asyncCallbackExecutor;
    
    @NotNull
    private final InterceptorChain interceptorChain;
    
    @Nullable
    private final Duration timeout;
    
    private HttpClientRetrofitCallFactory(@NotNull HttpClient httpClient, @NotNull HttpRequestFactory requestFactory, @NotNull RetrofitResponseFactory responseFactory, @NotNull Executor asyncCallbackExecutor, InterceptorChain interceptorChain, @Nullable Duration timeout) {
        this.httpClient = httpClient;
        this.requestFactory = requestFactory;
        this.responseFactory = responseFactory;
        this.asyncCallbackExecutor = asyncCallbackExecutor;
        this.interceptorChain = interceptorChain;
        this.timeout = timeout;
    }
    
    @NotNull
    @Override
    public Call newCall(@NotNull Request request) {
        return new HttpClientRetrofitCall(httpClient, request, requestFactory, responseFactory, asyncCallbackExecutor, interceptorChain, timeout);
    }
    
    public static Builder builder(HttpClient httpClient) {
        return new Builder(httpClient);
    }
    
    public static class Builder {
        
        @NotNull
        private final HttpClient httpClient;
        
        @NotNull
        private HttpRequestFactory requestFactory = new DefaultHttpRequestFactory();
        
        @NotNull
        private RetrofitResponseFactory responseFactory = new DefaultRetrofitResponseFactory();
        
        @NotNull
        private Executor asyncCallbackExecutor = ForkJoinPool.commonPool();
    
        @NotNull
        private InterceptorChain interceptorChain = new DefaultInterceptorChain();
    
        @NotNull
        private List<Interceptor> interceptors = new ArrayList<>();
        
        @Nullable
        private Duration timeout;
        
        public Builder(@NotNull HttpClient httpClient) {
            this.httpClient = httpClient;
        }
        
        /**
         * Specify a factory converting Retrofit requests to Http Client requests
         */
        public Builder withRequestFactory(@NotNull HttpRequestFactory requestFactory) {
            this.requestFactory = requestFactory;
            return this;
        }
        
        /**
         * Specify a factory converting Http Client response to Retrofit response
         */
        public Builder withResponseFactory(@NotNull RetrofitResponseFactory responseFactory) {
            this.responseFactory = responseFactory;
            return this;
        }
        
        /**
         * Specify an executor which will execute asynchronous callbacks
         * <p>
         * ForkJoin common pool is the default executor, do not block threads if the default one is used
         */
        public Builder withAsyncCallbackExecutor(@NotNull Executor executor) {
            this.asyncCallbackExecutor = executor;
            return this;
        }
    
        /**
         * If not set, DefaultRetrofitResponseFactory is used
         */
        public Builder withInterceptorChain(@NotNull InterceptorChain interceptorChain) {
            this.interceptorChain = interceptorChain;
            return this;
        }
    
        public Builder addInterceptor(@NotNull Interceptor interceptor) {
            this.interceptors.add(interceptor);
            return this;
        }
        
        /**
         * Specify timeout for requests
         * <p>
         * By default there is no timeout
         */
        public Builder withTimeout(@Nullable Duration duration) {
            this.timeout = duration;
            return this;
        }
        
        public HttpClientRetrofitCallFactory build() {
            interceptorChain.setInterceptors(interceptors);
            
            return new HttpClientRetrofitCallFactory(httpClient, requestFactory, responseFactory, asyncCallbackExecutor, interceptorChain, timeout);
        }
    }
}
