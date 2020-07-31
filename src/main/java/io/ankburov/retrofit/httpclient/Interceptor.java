package io.ankburov.retrofit.httpclient;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Wrapping logic around request execution
 * <p>
 * Intermediate variables can be stored in request tags
 */
public interface Interceptor {
    
    default Request onRequest(@NotNull Request request) {
        return request;
    }
    
    /**
     * @param response  not null for non-error result
     * @param throwable not null for error result
     * @return response, can be null
     */
    default Response onResult(@NotNull Request request, @Nullable Response response, @Nullable Throwable throwable) {
        return response;
    }
}
