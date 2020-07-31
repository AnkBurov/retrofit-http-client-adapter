package io.ankburov.retrofit.httpclient;

import java.io.IOException;

public class UnderlyingClientException extends IOException {
    
    public UnderlyingClientException(Throwable cause) {
        super(cause);
    }
}
