package io.ankburov.retrofit.httpclient;

import static com.github.tomakehurst.wiremock.client.WireMock.aMultipart;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.binaryEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.ankburov.retrofit.httpclient.adapter.TestRetrofitAdapter;
import io.ankburov.retrofit.httpclient.interceptor.TestInterceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;

public class HttpClientRetrofitCallFactoryTest {
    
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
    public void testGet() throws IOException {
        TestRetrofitAdapter retrofitAdapter = createRetrofitAdapter();
        
        wireMock.stubFor(get("/rest/info/main?detailed=true")
                .willReturn(
                        aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\"answer\": 42}")
                ));
        
        Call<ResponseBody> call = retrofitAdapter.getInfo("main", true);
        
        assertFalse(call.isExecuted());
        Response<ResponseBody> response = call.execute();
        assertTrue(call.isExecuted());
        
        assertTrue(response.isSuccessful());
        assertEquals("application/json", response.headers().get("Content-Type"));
        
        try (ResponseBody body = response.body()) {
            assertNotNull(body);
            assertEquals("{\"answer\": 42}", body.string());
        }
    }
    
    @Test
    public void testPost() throws IOException {
        TestRetrofitAdapter retrofitAdapter = createRetrofitAdapter();
        
        wireMock.stubFor(post("/rest/post")
                .willReturn(
                        aResponse()
                ));
        
        Response<Void> response = retrofitAdapter.post()
                .execute();
        
        assertTrue(response.isSuccessful());
        
        wireMock.verify(postRequestedFor(urlEqualTo("/rest/post")));
    }
    
    @Test
    public void testPostWithBody() throws IOException {
        TestRetrofitAdapter retrofitAdapter = createRetrofitAdapter();
        
        wireMock.stubFor(post("/rest/post-with-body")
                .withHeader("Content-Type", equalTo("application/json"))
                .withHeader("custom", equalTo("value"))
                .withRequestBody(equalToJson("{\"a\": 1}"))
                .willReturn(
                        aResponse()
                ));
        
        RequestBody requestBody = RequestBody.create("{\"a\": 1}".getBytes(StandardCharsets.UTF_8),
                MediaType.parse("application/json"));
        
        Response<Void> response = retrofitAdapter.postWithBody(requestBody)
                .execute();
        
        assertTrue(response.isSuccessful());
    }
    
    @Test
    public void testPostMultipartChunked() throws IOException {
        TestRetrofitAdapter retrofitAdapter = createRetrofitAdapter();
        
        byte[] catBytes;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("cat.png")) {
            catBytes = IOUtils.toByteArray(inputStream);
        }
        
        wireMock.stubFor(post("/rest/post-multipart")
                .withMultipartRequestBody(
                        aMultipart()
                                .withBody(binaryEqualTo(catBytes)))
                .willReturn(
                        aResponse()
                                .withBody(catBytes)
                ));
        
        MultipartBody.Part formData = MultipartBody.Part.createFormData("file", "cat", RequestBody.create(catBytes));
        
        Response<ResponseBody> response = retrofitAdapter.postMultiPart(formData).execute();
        
        assertTrue(response.isSuccessful());
        assertEquals("chunked", response.headers().get("transfer-encoding"));
        assertNull(response.headers().get("Content-Length"));
        
        try (ResponseBody body = response.body()) {
            assertNotNull(body);
            assertArrayEquals(catBytes, body.bytes());
        }
    }
    
    @Test
    public void testPostMultipartContentLength() throws IOException {
        TestRetrofitAdapter retrofitAdapter = createRetrofitAdapter();
        
        byte[] catBytes;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("cat.png")) {
            catBytes = IOUtils.toByteArray(inputStream);
        }
        
        wireMock.stubFor(post("/rest/post-multipart")
                .withMultipartRequestBody(
                        aMultipart()
                                .withBody(binaryEqualTo(catBytes)))
                .willReturn(
                        aResponse()
                                .withBody(catBytes)
                                .withHeader("Content-Length", String.valueOf(catBytes.length))
                ));
        
        MultipartBody.Part formData = MultipartBody.Part.createFormData("file", "cat", RequestBody.create(catBytes));
        
        Response<ResponseBody> response = retrofitAdapter.postMultiPart(formData).execute();
        
        assertTrue(response.isSuccessful());
        assertEquals(String.valueOf(catBytes.length), response.headers().get("Content-Length"));
        assertNull(response.headers().get("transfer-encoding"));
        
        try (ResponseBody body = response.body()) {
            assertNotNull(body);
            assertArrayEquals(catBytes, body.bytes());
        }
    }
    
    @Test
    public void testGetNotFound() throws IOException {
        TestRetrofitAdapter retrofitAdapter = createRetrofitAdapter();
    
        Response<Void> response = retrofitAdapter.getNotFound().execute();
    
        assertEquals(404, response.code());
    }
    
    @Test(expected = ConnectException.class)
    public void testSyncError() throws IOException {
        TestRetrofitAdapter retrofitAdapter = createRetrofitAdapter();
        wireMock.stop();
        try {
    
            retrofitAdapter.getNotFound().execute();
        } finally {
            wireMock.start();
        }
    }
    
    @Test
    public void testClone() {
        TestRetrofitAdapter retrofitAdapter = createRetrofitAdapter();
    
        Call<Void> call = retrofitAdapter.post();
        assertNotEquals(call, call.clone());
    }
    
    @Test
    public void testSyncInterceptorOkPath() throws IOException {
        wireMock.stubFor(post("/rest/post")
                .withHeader(TestInterceptor.REQUEST_HEADER, equalTo(TestInterceptor.REQUEST_HEADER_VALUE))
                .willReturn(
                        aResponse()
                ));
        
        TestInterceptor interceptor = new TestInterceptor();
        TestRetrofitAdapter retrofitAdapter = createRetrofitAdapter(interceptor);
    
        Response<Void> response = retrofitAdapter.post().execute();
    
        assertEquals(TestInterceptor.RESPONSE_HEADER_VALUE, response.headers().get(TestInterceptor.RESPONSE_HEADER));
        assertFalse(interceptor.gotError);
    }
    
    @Test
    public void testSyncInterceptorErrorPath() throws IOException {
        TestInterceptor interceptor = new TestInterceptor();
        TestRetrofitAdapter retrofitAdapter = createRetrofitAdapter(interceptor);
    
        wireMock.stop();
        try {
            retrofitAdapter.getNotFound().execute();
        
        } catch (ConnectException e) {
            assertTrue(interceptor.gotError);
            return;
        } finally {
            wireMock.start();
        }
        Assert.fail();
    }
    
    @Test
    public void testPostHttp2() throws IOException {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
    
        HttpClientRetrofitCallFactory callFactory = HttpClientRetrofitCallFactory.builder(httpClient)
                .build();
    
        Retrofit retrofit = new Retrofit.Builder()
                .callFactory(callFactory)
                .baseUrl(wireMock.baseUrl())
                .build();
    
        TestRetrofitAdapter retrofitAdapter = retrofit.create(TestRetrofitAdapter.class);
    
        wireMock.stubFor(post("/rest/post")
                .willReturn(
                        aResponse()
                ));
    
        Response<Void> response = retrofitAdapter.post()
                .execute();
    
        assertTrue(response.isSuccessful());
    
        wireMock.verify(postRequestedFor(urlEqualTo("/rest/post")));
    }
    
    private TestRetrofitAdapter createRetrofitAdapter(Interceptor... interceptors) {
        HttpClient httpClient = HttpClient.newHttpClient();
    
        HttpClientRetrofitCallFactory.Builder callFactoryBuilder = HttpClientRetrofitCallFactory.builder(httpClient);
        Stream.of(interceptors).forEach(callFactoryBuilder::addInterceptor);
        okhttp3.Call.Factory callFactory = callFactoryBuilder.build();
        
        Retrofit retrofit = new Retrofit.Builder()
                .callFactory(callFactory)
                .baseUrl(wireMock.baseUrl())
                .build();
        
        return retrofit.create(TestRetrofitAdapter.class);
    }
}