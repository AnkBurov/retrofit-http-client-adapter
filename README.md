# Retrofit Java 11 Http Client adapter
[![Download](https://api.bintray.com/packages/ankburov/maven/retrofit-http-client-adapter/images/download.svg) ](https://bintray.com/ankburov/maven/retrofit-http-client-adapter/_latestVersion)
[![Build Status](https://travis-ci.org/AnkBurov/retrofit-http-client-adapter.svg?branch=master)](https://travis-ci.org/AnkBurov/retrofit-http-client-adapter)

An adapter between Retrofit and asynchronous non-blocking Java 11 Http Client. 

With Retrofit by default OkHttp client is used under the hood as an HTTP client library
which is a great library, but it has one major flaw - under the hood OkHttp uses blocking IO
even for async calls (Dispatcher creates a worker thread per each network call). 
It's acceptable for Android applications where a number of background threads usually is not
too big, but for Java web server applications a thread per each network call sometimes is
a major no-go.

The adapter solves the problem by bringing to Retrofit support of fully asynchronous 
non-blocking Java 11 Http Client. With the adapter, Http Client seamlessly integrates
into Retrofit architecture - Retrofit features like converter or call adapter factories 
(like Retrofit Project Reactor for example) tend to work out of the box.

## Download

### Gradle

```groovy
repositories {
    jcenter()
}

dependencies {
    compile "io.ankburov:retrofit-http-client-adapter:<version>"
}
```

### Usage

Declare a Retrofit interface:
```java
public interface GitHubService {
    
    // synchronous call
    @GET("users/{user}/repos")
    Call<List<Repo>> listRepos(@Path("user") String user);
    
    // asynchronous call
    CompletableFuture<List<Repo>> listReposAsync(@Path("user") String user);
}
``` 

Create an instance of Retrofit interface
```java
HttpClient httpClient = HttpClient.newHttpClient();

HttpClientRetrofitCallFactory callFactory = HttpClientRetrofitCallFactory.builder(httpClient)
        .build();

Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .callFactory(callFactory)
        .build();

GitHubService service = retrofit.create(GitHubService.class);
```

#### Interceptors

Java 11 HTTP Client currently lacks the support of interceptors concept - code that
wraps around an HTTP call. The library adds the feature for all (sync and asyc) requests.
To use it, implement `io.ankburov.retrofit.httpclient.Interceptor` interface and register
the instance to the call factory:

```java
public class CallMetricsInterceptor implements Interceptor {

    private final MeterRegistry meterRegistry;
    
    @Override
    public Request onRequest(@NotNull Request request) {
        return request.newBuilder()
                .tag(Timer.Sample.class, Timer.start(meterRegistry))
                .build();
    }
    
    @Override
    public Response onResult(@NotNull Request request, @Nullable Response response, @Nullable Throwable throwable) {
        request.tag(Timer.Sample.class)
                .stop(Timer.builder("call_duration")
                .description("Call duration")
                .tag("method", request.method())
                .tag("path", request.url().encodedPath())
                .publishPercentileHistogram(false)
                .publishPercentiles(0.8, 0.9, 0.95)
                .register(meterRegistry));
        return response;
    }
}
``` 

```java
callFactory.addInterceptor(new CallMetricsInterceptor())
```