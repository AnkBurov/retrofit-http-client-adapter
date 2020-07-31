package io.ankburov.retrofit.httpclient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import io.ankburov.retrofit.httpclient.adapter.TestRetrofitAdapter;
import io.ankburov.retrofit.httpclient.interceptor.TestInterceptor;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class HttpClientRetrofitCallFactoryAsyncTest {
    
    private static final int CONCURRENT_REQUESTS_NUMBER = 1000;
    private static final int WIREMOCK_RESPONSE_DELAY = 1000; // milliseconds
    
    private static WireMockServer wireMock = new WireMockServer(new WireMockConfiguration()
            .containerThreads(CONCURRENT_REQUESTS_NUMBER)
            .dynamicPort());
    
    @BeforeClass
    public static void setUp() {
        wireMock.start();
    }
    
    @AfterClass
    public static void tearDown() {
        wireMock.stop();
    }
    
    /**
     * Fire many requests through an intentionally set single-threaded HTTP client and verify that all requests
     * were completed fast enough. Blocking (under the hood) clients like OkHttp will fail such test
     */
    @Test
    public void testNonBlocking() {
        TestRetrofitAdapter retrofitAdapter = createRetrofitAdapterSingleThreadedClient();
        
        wireMock.stubFor(get("/rest/info/main?detailed=true")
                .willReturn(
                        aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\"answer\": 42}")
                                .withFixedDelay(WIREMOCK_RESPONSE_DELAY)
                ));
        
        var futures = IntStream.range(0, CONCURRENT_REQUESTS_NUMBER)
                .mapToObj(i -> retrofitAdapter.getInfoAsync("main", true))
                .collect(Collectors.toList());
        
        long start = System.currentTimeMillis();
        
        futures.stream()
                .map(CompletableFuture::join)
                .forEach(responseBody -> {
                    try (responseBody) {
                        assertNotNull(responseBody);
                        assertEquals("{\"answer\": 42}", getString(responseBody));
                    }
                });
        
        long diff = System.currentTimeMillis() - start;
        System.out.println(String.format("Firing %s concurrent requests via single-threaded HTTP client took %s ms to complete",
                CONCURRENT_REQUESTS_NUMBER, diff));
        
        double percent = 0.5; // if test fails consider increasing this percent just a bit
        String errorMessage = "Operation was not fast enough. Perhaps request execution was blocking or sequential?";
        assertThat(errorMessage, (double) diff, lessThan(CONCURRENT_REQUESTS_NUMBER * WIREMOCK_RESPONSE_DELAY / 100 * percent));
    }
    
    @Test
    public void testCancel() throws InterruptedException {
        TestRetrofitAdapter retrofitAdapter = createRetrofitAdapterSingleThreadedClient();
        
        wireMock.stubFor(get("/rest/info/main?detailed=true")
                .willReturn(
                        aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\"answer\": 42}")
                                .withFixedDelay(WIREMOCK_RESPONSE_DELAY)
                ));
        
        Call<ResponseBody> call = retrofitAdapter.getInfo("main", true);
        
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Assert.fail();
            }
            
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Assert.fail();
            }
        });
        
        TimeUnit.MILLISECONDS.sleep(WIREMOCK_RESPONSE_DELAY / 2);
        
        call.cancel();
        assertTrue(call.isCanceled());
    }
    
    @Test(expected = HttpTimeoutException.class)
    public void testTimeout() throws IOException {
        TestRetrofitAdapter retrofitAdapter = createRetrofitAdapterSingleThreadedClient(Duration.ofMillis(WIREMOCK_RESPONSE_DELAY / 2));
        
        wireMock.stubFor(get("/rest/info/main?detailed=true")
                .willReturn(
                        aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\"answer\": 42}")
                                .withFixedDelay(WIREMOCK_RESPONSE_DELAY)
                ));
        
        retrofitAdapter.getInfo("main", true).execute();
    }
    
    @Test
    public void testAsyncTimeout() {
        TestRetrofitAdapter retrofitAdapter = createRetrofitAdapterSingleThreadedClient(Duration.ofMillis(WIREMOCK_RESPONSE_DELAY / 2));
        
        wireMock.stubFor(get("/rest/info/main?detailed=true")
                .willReturn(
                        aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\"answer\": 42}")
                                .withFixedDelay(WIREMOCK_RESPONSE_DELAY)
                ));
        
        try {
            retrofitAdapter.getInfoAsync("main", true)
                    .join();
        } catch (CompletionException e) {
            assertTrue(e.getCause() instanceof HttpTimeoutException);
            return;
        }
        Assert.fail();
    }
    
    @Test
    public void testAsyncInterceptorOkPath() {
        wireMock.stubFor(post("/rest/post")
                .withHeader(TestInterceptor.REQUEST_HEADER, equalTo(TestInterceptor.REQUEST_HEADER_VALUE))
                .willReturn(
                        aResponse()
                ));
        
        TestInterceptor interceptor = new TestInterceptor();
        
        TestRetrofitAdapter retrofitAdapter = createRetrofitAdapterSingleThreadedClient(interceptor);
        
        Response<Void> response = retrofitAdapter.postAsync().join();
        
        assertEquals(TestInterceptor.RESPONSE_HEADER_VALUE, response.headers().get(TestInterceptor.RESPONSE_HEADER));
        assertFalse(interceptor.gotError);
    }
    
    @Test
    public void testAsyncInterceptorErrorPath() {
        TestInterceptor interceptor = new TestInterceptor();
        TestRetrofitAdapter retrofitAdapter = createRetrofitAdapterSingleThreadedClient(interceptor);
        
        wireMock.stop();
        try {
            retrofitAdapter.postAsync().join();
            
        } catch (CompletionException e) {
            assertTrue(e.getCause() instanceof ConnectException);
            assertTrue(interceptor.gotError);
            return;
        } finally {
            wireMock.start();
        }
        Assert.fail();
    }
    
    private TestRetrofitAdapter createRetrofitAdapterSingleThreadedClient(Interceptor... interceptors) {
        return createRetrofitAdapterSingleThreadedClient(null, interceptors);
    }
    
    private TestRetrofitAdapter createRetrofitAdapterSingleThreadedClient(Duration timeout, Interceptor... interceptors) {
        HttpClient httpClient = HttpClient.newBuilder()
                .executor(Executors.newSingleThreadExecutor())
                .build();
        
        HttpClientRetrofitCallFactory.Builder callFactoryBuilder = HttpClientRetrofitCallFactory
                .builder(httpClient)
                .withAsyncCallbackExecutor(Executors.newSingleThreadExecutor())
                .withTimeout(timeout);
        Stream.of(interceptors).forEach(callFactoryBuilder::addInterceptor);
        
        okhttp3.Call.Factory callFactory = callFactoryBuilder
                .build();
        
        Retrofit retrofit = new Retrofit.Builder()
                .callFactory(callFactory)
                .baseUrl(wireMock.baseUrl())
                .build();
        
        return retrofit.create(TestRetrofitAdapter.class);
    }
    
    @NotNull
    private String getString(ResponseBody responseBody) {
        try {
            return responseBody.string();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
