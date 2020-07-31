package io.ankburov.retrofit.httpclient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.concurrent.Executors;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.jakewharton.retrofit2.adapter.reactor.ReactorCallAdapterFactory;

import io.ankburov.retrofit.httpclient.adapter.ReactorRetrofitAdapter;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;

public class HttpClientRetrofitCallFactoryReactorTest {
    
    private static WireMockServer wireMock = new WireMockServer(0);
    
    @BeforeClass
    public static void setUp() {
        wireMock.start();
    }
    
    @AfterClass
    public static void tearDown() {
        wireMock.stop();
    }
    
    @Test
    public void testReactor() throws IOException {
        wireMock.stubFor(get("/rest/info/main?detailed=true")
                .willReturn(
                        aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\"answer\": 42}")
                ));
    
        ReactorRetrofitAdapter retrofitAdapter = createRetrofitAdapterWithReactor();
    
        long mainThreadId = Thread.currentThread().getId();
        ResponseBody responseBody = retrofitAdapter.getInfo("main", true)
                .doOnNext(r -> assertNotEquals(mainThreadId, Thread.currentThread().getId()))
                .block();
    
        try (responseBody) {
            assertNotNull(responseBody);
            assertEquals("{\"answer\": 42}", responseBody.string());
        }
    }
    
    private ReactorRetrofitAdapter createRetrofitAdapterWithReactor() {
        HttpClient httpClient = HttpClient.newBuilder()
                .executor(Executors.newSingleThreadExecutor())
                .build();
    
        HttpClientRetrofitCallFactory.Builder callFactoryBuilder = HttpClientRetrofitCallFactory
                .builder(httpClient);
        
        okhttp3.Call.Factory callFactory = callFactoryBuilder
                .build();
        
        Retrofit retrofit = new Retrofit.Builder()
                .callFactory(callFactory)
                .baseUrl(wireMock.baseUrl())
                .addCallAdapterFactory(ReactorCallAdapterFactory.createAsync())
                .build();
        
        return retrofit.create(ReactorRetrofitAdapter.class);
    }
}
