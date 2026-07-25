package server.demo.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IndependentSiteUrlFetchServiceTest {

    private static final String PUBLIC_IP = "93.184.216.34";

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void fetch_shouldExtractContentFromHtmlPage() throws Exception {
        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                <title>Seaside Resort &amp; Spa</title>
                <meta name="description" content="A quiet resort by the sea with warm hospitality.">
                </head>
                <body>
                <nav><p>Navigation boilerplate paragraph that must be skipped by the extractor.</p></nav>
                <h1>Seaside Resort &amp; Spa</h1>
                <h2>Rooms with ocean view</h2>
                <h2>Rooms with ocean view</h2>
                <h3>Dining on the terrace</h3>
                <p>Too short.</p>
                <p>Nestled between pine forests and a private cove, our resort offers quiet rooms and slow mornings for every guest.</p>
                <header><p>Header boilerplate paragraph that must also be skipped by the extractor.</p></header>
                <img src="/images/hero-view.jpg" width="1200" height="800">
                <img src="https://cdn.example.com/assets/logo.png">
                <img src="data:image/png;base64,AAAA">
                <img src="/assets/icons/close-icon.svg">
                <img src="/img/spacer-pixel.gif" width="1" height="1">
                <img src="/img/photo-small.jpg" width="50" height="90">
                <img srcset="/img/from-srcset.jpg 1x, /img/from-srcset-2x.jpg 2x">
                <ul>
                  <li>设施与服务</li>
                  <li>免费无线网络覆盖全馆</li>
                  <li>每日客房清洁与洗衣</li>
                </ul>
                <ul>
                  <li>Home</li>
                  <li>Contact</li>
                </ul>
                </body>
                </html>
                """;
        startServer(exchange -> {
            if (!"/page".equals(exchange.getRequestURI().getPath())) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }
            byte[] body = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        IndependentSiteUrlFetchService.ExtractedContent content =
                allowAllService().fetch(baseUrl() + "/page");

        assertEquals(baseUrl() + "/page", content.finalUrl());
        assertEquals("Seaside Resort & Spa", content.title());
        assertEquals("A quiet resort by the sea with warm hospitality.", content.metaDescription());
        assertEquals(
                List.of("Seaside Resort & Spa", "Rooms with ocean view", "Dining on the terrace"),
                content.headings()
        );
        assertEquals(
                List.of("Nestled between pine forests and a private cove, our resort offers "
                        + "quiet rooms and slow mornings for every guest."),
                content.paragraphs()
        );
        assertEquals(
                List.of(
                        baseUrl() + "/images/hero-view.jpg",
                        baseUrl() + "/img/from-srcset.jpg"
                ),
                content.imageUrls()
        );
        assertEquals(
                List.of("设施与服务", "免费无线网络覆盖全馆", "每日客房清洁与洗衣"),
                content.facilityHints()
        );
    }

    @Test
    void fetch_shouldFollowRedirectsAndReportFinalUrl() throws Exception {
        startServer(exchange -> {
            String path = exchange.getRequestURI().getPath();
            if ("/start".equals(path)) {
                redirect(exchange, "/middle");
                return;
            }
            if ("/middle".equals(path)) {
                redirect(exchange, "/final");
                return;
            }
            byte[] body = "<html><head><title>Final Page</title></head><body></body></html>"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        IndependentSiteUrlFetchService.ExtractedContent content =
                allowAllService().fetch(baseUrl() + "/start");

        assertEquals(baseUrl() + "/final", content.finalUrl());
        assertEquals("Final Page", content.title());
    }

    @Test
    void fetch_shouldFailAfterMoreThanThreeRedirects() throws Exception {
        AtomicInteger hits = new AtomicInteger();
        startServer(exchange -> {
            int hop = hits.incrementAndGet();
            redirect(exchange, "/hop-" + hop);
        });

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> allowAllService().fetch(baseUrl() + "/hop-0")
        );

        assertEquals(422, exception.getStatus().value());
        assertEquals("URL_FETCH_FAILED", exception.getCode());
    }

    @Test
    void fetch_shouldRejectRedirectToInternalAddress() throws Exception {
        startServer(exchange -> {
            redirect(exchange, "http://169.254.169.254/latest/meta-data");
        });
        IndependentSiteUrlFetchService service = new IndependentSiteUrlFetchService(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build(),
                host -> "169.254.169.254".equals(host)
                        ? new InetAddress[]{InetAddress.getByName("169.254.169.254")}
                        : new InetAddress[]{InetAddress.getByName(PUBLIC_IP)}
        );

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> service.fetch(baseUrl() + "/start")
        );

        assertEquals(400, exception.getStatus().value());
        assertEquals("URL_NOT_ALLOWED", exception.getCode());
    }

    @Test
    void fetch_shouldRejectInternalAndReservedTargets() throws Exception {
        assertRejected("127.0.0.1", "127.0.0.1");
        assertRejected("10.0.0.8", "10.0.0.8");
        assertRejected("172.16.5.4", "172.16.5.4");
        assertRejected("192.168.1.10", "192.168.1.10");
        assertRejected("169.254.169.254", "169.254.169.254");
        assertRejected("0.0.0.0", "0.0.0.0");
        assertRejected("224.0.0.1", "224.0.0.1");
        assertRejected("100.64.0.1", "100.64.0.1");
        assertRejected("192.0.2.1", "192.0.2.1");
        assertRejected("198.51.100.7", "198.51.100.7");
        assertRejected("203.0.113.5", "203.0.113.5");
        assertRejected("240.1.2.3", "240.1.2.3");
        assertRejected("::1", "::1");
        assertRejected("fe80::1", "fe80::1");
        assertRejected("fc00::1", "fc00::1");
        assertRejected("ff02::1", "ff02::1");
        // 任一解析结果命中内网即拒绝（公网 + 内网混合）
        assertRejected("mixed.example", PUBLIC_IP, "10.0.0.1");
    }

    @Test
    void fetch_shouldRejectLocalhostWithRealResolver() {
        IndependentSiteUrlFetchService service = new IndependentSiteUrlFetchService(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build(),
                InetAddress::getAllByName
        );

        assertNotAllowed(service, "http://localhost:1/");
        assertNotAllowed(service, "http://127.0.0.1:1/");
    }

    @Test
    void fetch_shouldRejectNonHtmlContentType() throws Exception {
        startServer(exchange -> {
            byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> allowAllService().fetch(baseUrl() + "/data")
        );

        assertEquals(422, exception.getStatus().value());
        assertEquals("URL_CONTENT_UNSUPPORTED", exception.getCode());
    }

    @Test
    void fetch_shouldFailOnNon2xxResponses() throws Exception {
        startServer(exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> allowAllService().fetch(baseUrl() + "/missing")
        );

        assertEquals(422, exception.getStatus().value());
        assertEquals("URL_FETCH_FAILED", exception.getCode());
    }

    @Test
    void fetch_shouldRejectUnsupportedSchemesAndMalformedUrls() throws Exception {
        IndependentSiteUrlFetchService service = allowAllService();

        assertNotAllowed(service, "ftp://example.com/file");
        assertNotAllowed(service, "file:///etc/passwd");
        assertNotAllowed(service, "not-a-url");
        assertNotAllowed(service, "  ");
    }

    @Test
    void fetch_shouldTruncateBodiesBeyondTwoMegabytes() throws Exception {
        String bigParagraph = "a".repeat(2 * 1024 * 1024);
        String html = "<html><head><title>Big Page</title></head><body><p>"
                + bigParagraph + "</p></body></html>";
        startServer(exchange -> {
            byte[] body = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        IndependentSiteUrlFetchService.ExtractedContent content =
                allowAllService().fetch(baseUrl() + "/big");

        assertEquals("Big Page", content.title());
        assertEquals(1, content.paragraphs().size());
        assertEquals(600, content.paragraphs().get(0).length());
    }

    private void assertRejected(String host, String... addresses) throws Exception {
        InetAddress[] resolved = new InetAddress[addresses.length];
        for (int i = 0; i < addresses.length; i++) {
            resolved[i] = InetAddress.getByName(addresses[i]);
        }
        IndependentSiteUrlFetchService service = new IndependentSiteUrlFetchService(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build(),
                ignored -> resolved
        );
        String url = host.contains(":") ? "http://[" + host + "]/" : "http://" + host + "/";
        assertNotAllowed(service, url);
    }

    private static void assertNotAllowed(IndependentSiteUrlFetchService service, String url) {
        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> service.fetch(url)
        );
        assertEquals(400, exception.getStatus().value());
        assertEquals("URL_NOT_ALLOWED", exception.getCode());
    }

    private static IndependentSiteUrlFetchService allowAllService() throws Exception {
        InetAddress publicAddress = InetAddress.getByName(PUBLIC_IP);
        return new IndependentSiteUrlFetchService(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build(),
                ignored -> new InetAddress[]{publicAddress}
        );
    }

    private void startServer(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", handler);
        server.start();
    }

    private static void redirect(com.sun.net.httpserver.HttpExchange exchange, String location)
            throws IOException {
        exchange.getResponseHeaders().add("Location", location);
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }
}
