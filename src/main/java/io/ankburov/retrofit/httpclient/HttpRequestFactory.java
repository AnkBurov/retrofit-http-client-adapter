package io.ankburov.retrofit.httpclient;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.time.Duration;

import org.jetbrains.annotations.Nullable;

import okhttp3.Request;

/**
 * Convert Retrofit request to Http Client request
 */
public interface HttpRequestFactory {
    
    HttpRequest build(Request request, @Nullable Duration timeout) throws IOException;
}
