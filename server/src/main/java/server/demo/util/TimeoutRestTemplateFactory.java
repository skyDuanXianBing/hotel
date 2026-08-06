package server.demo.util;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate factory with explicit timeouts. Never hand out a bare {@code new RestTemplate()}:
 * the JDK default request factory has infinite connect/read timeouts and can park a worker thread
 * forever on a black-holed TCP connection.
 */
public final class TimeoutRestTemplateFactory {

    public static final int DEFAULT_CONNECT_TIMEOUT_MS = 5_000;
    public static final int DEFAULT_READ_TIMEOUT_MS = 30_000;

    private TimeoutRestTemplateFactory() {
    }

    public static RestTemplate createDefault() {
        return create(DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS);
    }

    public static RestTemplate create(int connectTimeoutMs, int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return new RestTemplate(factory);
    }
}
