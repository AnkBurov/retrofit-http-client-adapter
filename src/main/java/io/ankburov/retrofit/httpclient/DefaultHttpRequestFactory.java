package io.ankburov.retrofit.httpclient;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpRequest;
import java.time.Duration;

import org.jetbrains.annotations.Nullable;

import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;

public class DefaultHttpRequestFactory implements HttpRequestFactory {
    
    private static final String CONTENT_TYPE = "Content-Type";
    
    @Override
    public HttpRequest build(Request request, @Nullable Duration timeout) throws IOException {
        
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(request.url().uri())
                .method(request.method(), getBody(request));
        
        request.headers().forEach(pair -> builder.header(pair.getFirst(), pair.getSecond()));
    
        if (request.body() != null && request.body().contentType() != null) {
            builder.setHeader(CONTENT_TYPE, request.body().contentType().toString());
        }
    
        if (timeout != null) {
            builder.timeout(timeout);
        }
        
        return builder.build();
    }
    
    protected HttpRequest.BodyPublisher getBody(Request request) throws IOException {
        RequestBody body = request.body();
        
        if (body == null) {
            return HttpRequest.BodyPublishers.noBody();
        }
        try (Buffer buffer = new Buffer()) {
            body.writeTo(buffer);
            try (InputStream bodyStream = buffer.inputStream()) {
                return HttpRequest.BodyPublishers.ofInputStream(() -> bodyStream);
            }
        }
    }
}
