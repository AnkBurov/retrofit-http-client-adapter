package io.ankburov.retrofit.httpclient.interceptor;

import static org.junit.Assert.assertEquals;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.ankburov.retrofit.httpclient.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class TestInterceptor implements Interceptor {
    
    public static final String REQUEST_HEADER = "request-header";
    public static final String REQUEST_HEADER_VALUE = "request-value";
    
    public static final String RESPONSE_HEADER = "response-header";
    public static final String RESPONSE_HEADER_VALUE = "response-value";
    
    public volatile boolean gotError = false;
    
    @Override
    public Request onRequest(@NotNull Request request) {
        return request.newBuilder()
                .header(REQUEST_HEADER, REQUEST_HEADER_VALUE)
                .build();
    }
    
    @Override
    public Response onResult(@NotNull Request request, @Nullable Response response, @Nullable Throwable throwable) {
        // verify also that a decorated above request is the request in argument
        assertEquals(REQUEST_HEADER_VALUE, request.header(REQUEST_HEADER));
        
        if (response != null) {
            response = response.newBuilder()
                    .header(RESPONSE_HEADER, RESPONSE_HEADER_VALUE)
                    .build();
        } else if (throwable != null) {
            gotError = true;
        }
        
        return response;
    }
}
