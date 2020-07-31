package io.ankburov.retrofit.httpclient;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import org.jetbrains.annotations.NotNull;

import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import okio.Okio;

public class DefaultRetrofitResponseFactory implements RetrofitResponseFactory {
    
    private static final String EMPTY = "";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_LENGTH = "Content-Length";
    private static final int UNKNOWN_LENGTH = -1;
    
    @Override
    public Response build(Request retrofitRequest, HttpResponse<InputStream> response) {
        Response.Builder responseBuilder = new Response.Builder()
                .request(retrofitRequest)
                .protocol(response.version() == HttpClient.Version.HTTP_1_1 ? Protocol.HTTP_1_1 : Protocol.HTTP_2)
                .message(EMPTY)
                .code(response.statusCode())
                .body(new ResponseBody() {
                    @Override
                    public MediaType contentType() {
                        return response.headers().firstValue(CONTENT_TYPE)
                                .map(MediaType::parse)
                                .orElse(null);
                    }
                
                    @Override
                    public long contentLength() {
                        long length = response.headers()
                                .firstValueAsLong(CONTENT_LENGTH)
                                .orElseGet(() -> {
                                    try {
                                        return response.body().available();
                                    } catch (IOException e) {
                                        return UNKNOWN_LENGTH;
                                    }
                                });
    
                        return length != 0 ? length : UNKNOWN_LENGTH;
                    }
                
                    @Override
                    @NotNull
                    public BufferedSource source() {
                        return Okio.buffer(Okio.source(response.body()));
                    }
                });
    
        response.headers().map()
                .forEach((key, values) -> values.forEach(value -> responseBuilder.header(key, value)));
    
        return responseBuilder.build();
    }
}
