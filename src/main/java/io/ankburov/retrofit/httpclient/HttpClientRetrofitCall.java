package io.ankburov.retrofit.httpclient;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import okio.Timeout;

/**
 * Adapter between Retrofit and http client
 */
public class HttpClientRetrofitCall implements Call {
    
    @NotNull
    private final HttpClient httpClient;
    
    @NotNull
    private final Request retrofitRequest;
    
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
    
    private volatile boolean isExecuted = false;
    private volatile CompletableFuture<HttpResponse<InputStream>> asyncCall;
    
    public HttpClientRetrofitCall(@NotNull HttpClient httpClient, @NotNull Request retrofitRequest, @NotNull HttpRequestFactory requestFactory, @NotNull RetrofitResponseFactory responseFactory, @NotNull Executor asyncCallbackExecutor, InterceptorChain interceptorChain, @Nullable Duration timeout) {
        this.httpClient = httpClient;
        this.retrofitRequest = retrofitRequest;
        this.requestFactory = requestFactory;
        this.responseFactory = responseFactory;
        this.asyncCallbackExecutor = asyncCallbackExecutor;
        this.interceptorChain = interceptorChain;
        this.timeout = timeout;
    }
    
    @NotNull
    @Override
    public Request request() {
        return retrofitRequest;
    }
    
    @NotNull
    @Override
    public Response execute() throws IOException {
        Request decoratedRequest = interceptorChain.processOnRequest(request());
        
        HttpRequest httpRequest = requestFactory.build(decoratedRequest, timeout);
        
        HttpResponse<InputStream> httpResponse;
        try {
            try {
                httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            } catch (Throwable t) {
                interceptorChain.processOnResult(decoratedRequest, null, t);
                throw t;
            }
        } catch (InterruptedException e) {
            throw new UnderlyingClientException(e);
        }
        isExecuted = true;
    
        Response response = responseFactory.build(decoratedRequest, httpResponse);
        return interceptorChain.processOnResult(decoratedRequest, response, null);
    }
    
    @Override
    public void enqueue(@NotNull Callback callback) {
        Request decoratedRequest = interceptorChain.processOnRequest(request());
        try {
            HttpRequest httpRequest = requestFactory.build(decoratedRequest, timeout);
            
            this.asyncCall = httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofInputStream())
                    .whenCompleteAsync((httpResponse, executionThrowable) -> {
                        try {
                            if (httpResponse != null) {
                                Response response = responseFactory.build(decoratedRequest, httpResponse);
                                
                                Response decoratedResponse = interceptorChain.processOnResult(decoratedRequest, response, null);
                                
                                callback.onResponse(this, decoratedResponse);
                            } else {
                                executionThrowable = executionThrowable instanceof CompletionException ?
                                        executionThrowable.getCause() : executionThrowable;
                                
                                interceptorChain.processOnResult(decoratedRequest, null, executionThrowable);
    
                                if (executionThrowable instanceof IOException) {
                                    callback.onFailure(this, (IOException) executionThrowable);
                                } else {
                                    callback.onFailure(this, new UnderlyingClientException(executionThrowable));
                                }
                            }
                        } catch (Throwable t) {
                            callback.onFailure(this, new HttpFactoryException(t));
                        }
                    }, asyncCallbackExecutor);
        } catch (IOException e) {
            interceptorChain.processOnResult(decoratedRequest, null, e);
            callback.onFailure(this, new HttpFactoryException(e));
        }
        isExecuted = true;
    }
    
    @Override
    public void cancel() {
        if (asyncCall != null) {
            asyncCall.cancel(true);
        }
    }
    
    @Override
    public boolean isExecuted() {
        return isExecuted;
    }
    
    @Override
    public boolean isCanceled() {
        return asyncCall != null && asyncCall.isCancelled();
    }
    
    @NotNull
    @Override
    public Timeout timeout() {
        return Timeout.NONE;
    }
    
    @NotNull
    @Override
    public Call clone() { //NOSONAR
        HttpClientRetrofitCall call = new HttpClientRetrofitCall(httpClient, request(), requestFactory, responseFactory, asyncCallbackExecutor, interceptorChain, timeout);
        call.isExecuted = isExecuted;
        call.asyncCall = asyncCall;
        return call;
    }
}
