package server.demo.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import server.demo.i18n.ApiMessages;
/**
 * 独立站"抠页面"URL 抓取与内容抽取服务。
 *
 * SSRF 防护：仅允许 http/https；每一跳（含重定向）都用 InetAddress.getAllByName 解析 host，
 * 任一解析结果为内网/环回/链路本地/组播/保留地址即拒绝；重定向手动跟随且不超过 3 跳；
 * 最终响应必须是 text/html；响应体读取上限 2MB（超出截断后继续解析）。
 */
@Component
public class IndependentSiteUrlFetchService {

    private static final Logger logger = LoggerFactory.getLogger(IndependentSiteUrlFetchService.class);

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_REDIRECTS = 3;
    private static final int MAX_BODY_BYTES = 2 * 1024 * 1024;
    private static final int MAX_LIST_ITEMS = 12;
    private static final int MAX_HEADING_LENGTH = 120;
    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_META_DESCRIPTION_LENGTH = 500;
    private static final int MIN_PARAGRAPH_LENGTH = 40;
    private static final int MAX_PARAGRAPH_LENGTH = 600;
    private static final int MAX_FACILITY_HINT_LENGTH = 100;
    private static final int MIN_IMAGE_DIMENSION = 100;

    private static final Set<String> BOILERPLATE_TAGS = Set.of(
            "nav",
            "header",
            "footer",
            "aside",
            "script",
            "style"
    );
    private static final Pattern FACILITY_KEYWORD = Pattern.compile("设施|配套|服务|amenit|facilit");
    private static final Pattern IMAGE_FILENAME_BLOCKLIST = Pattern.compile(
            "logo|icon|sprite|pixel|tracking"
    );
    private static final Pattern CHARSET_PARAM = Pattern.compile(
            "(?i)(?:^|;)\\s*charset\\s*=\\s*\"?([^\";\\s]+)"
    );

    private final HttpClient httpClient;
    private final AddressResolver addressResolver;

    @Autowired
    public IndependentSiteUrlFetchService() {
        this(defaultHttpClient(), InetAddress::getAllByName);
    }

    IndependentSiteUrlFetchService(HttpClient httpClient, AddressResolver addressResolver) {
        this.httpClient = httpClient;
        this.addressResolver = addressResolver;
    }

    /** 可注入的 DNS 解析器，测试用假解析结果覆盖各类地址形态，无需真实网络。 */
    @FunctionalInterface
    interface AddressResolver {
        InetAddress[] resolve(String host) throws UnknownHostException;
    }

    /** 抓取并抽取后的页面内容。所有文本已去 HTML 实体并压缩空白。 */
    public record ExtractedContent(
            String finalUrl,
            String title,
            String metaDescription,
            List<String> headings,
            List<String> paragraphs,
            List<String> imageUrls,
            List<String> facilityHints
    ) {
    }

    public ExtractedContent fetch(String rawUrl) {
        URI current = parseUri(rawUrl);
        for (int redirectCount = 0; ; ) {
            assertUrlAllowed(current);
            HttpResponse<InputStream> response = send(current);
            int status = response.statusCode();
            if (isRedirect(status)) {
                final URI next;
                try {
                    if (++redirectCount > MAX_REDIRECTS) {
                        logger.warn(
                                "Independent-site URL import exceeded {} redirects: {}",
                                MAX_REDIRECTS,
                                rawUrl
                        );
                        throw fetchFailed();
                    }
                    next = resolveRedirect(current, response);
                } finally {
                    closeQuietly(response.body());
                }
                current = next;
                continue;
            }
            if (status < 200 || status >= 300) {
                closeQuietly(response.body());
                logger.warn("Independent-site URL import got HTTP {} for {}", status, current);
                throw fetchFailed();
            }
            String contentType = response.headers().firstValue("Content-Type").orElse("");
            if (!contentType.toLowerCase(Locale.ROOT).startsWith("text/html")) {
                closeQuietly(response.body());
                throw new IndependentSiteServiceException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "URL_CONTENT_UNSUPPORTED",
                        ApiMessages.get("api.t.dea7a0586d06")
                );
            }
            String html = readBody(response.body(), resolveCharset(contentType), current);
            return extract(current, html);
        }
    }

    private HttpResponse<InputStream> send(URI uri) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", "Mozilla/5.0 (compatible; HotelPMS-SiteImporter/1.0)")
                .header("Accept", "text/html,application/xhtml+xml")
                .GET()
                .build();
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException | IllegalArgumentException e) {
            logger.warn("Independent-site URL import failed for {}: {}", uri, e.getMessage());
            throw fetchFailed();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw fetchFailed();
        }
    }

    // ------------------------------------------------------------------
    // SSRF 防护
    // ------------------------------------------------------------------

    private void assertUrlAllowed(URI uri) {
        String scheme = uri.getScheme();
        if (scheme == null
                || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            throw urlNotAllowed();
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw urlNotAllowed();
        }
        final InetAddress[] addresses;
        try {
            addresses = addressResolver.resolve(host);
        } catch (UnknownHostException e) {
            logger.warn("Independent-site URL import cannot resolve host {}: {}", host, e.getMessage());
            throw fetchFailed();
        }
        if (addresses == null || addresses.length == 0) {
            throw fetchFailed();
        }
        for (InetAddress address : addresses) {
            if (isDisallowedAddress(address)) {
                logger.warn(
                        "Independent-site URL import rejected host {} resolving to {}",
                        host,
                        address.getHostAddress()
                );
                throw urlNotAllowed();
            }
        }
    }

    /**
     * 内网/环回/链路本地/组播/保留地址判定。JDK 自带判断覆盖 127/8、::1、10/8、
     * 172.16/12、192.168/16、169.254/16、fe80::/10、224/4、ff00::/8 与未指定地址，
     * 这里补充 JDK 未覆盖的保留段（0/8、100.64/10、192.0.0/24、文档段、
     * 198.18/15、240/4、fc00::/7、2001:db8::/32）。
     */
    static boolean isDisallowedAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isSiteLocalAddress()
                || address.isLinkLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int b0 = bytes[0] & 0xff;
            int b1 = bytes[1] & 0xff;
            int b2 = bytes[2] & 0xff;
            if (b0 == 0) {
                return true;
            }
            if (b0 == 100 && (b1 & 0xc0) == 0x40) {
                return true;
            }
            if (b0 == 192 && b1 == 0 && (b2 == 0 || b2 == 2)) {
                return true;
            }
            if (b0 == 198 && (b1 == 18 || b1 == 19 || (b1 == 51 && b2 == 100))) {
                return true;
            }
            if (b0 == 203 && b1 == 0 && b2 == 113) {
                return true;
            }
            return b0 >= 240;
        }
        int b0 = bytes[0] & 0xff;
        int b1 = bytes[1] & 0xff;
        int b2 = bytes[2] & 0xff;
        int b3 = bytes[3] & 0xff;
        if ((b0 & 0xfe) == 0xfc) {
            return true;
        }
        return b0 == 0x20 && b1 == 0x01 && b2 == 0x0d && b3 == 0xb8;
    }

    private static IndependentSiteServiceException urlNotAllowed() {
        return new IndependentSiteServiceException(
                HttpStatus.BAD_REQUEST,
                "URL_NOT_ALLOWED",
                ApiMessages.get("api.t.79480bc20251")
        );
    }

    private static IndependentSiteServiceException fetchFailed() {
        return new IndependentSiteServiceException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "URL_FETCH_FAILED",
                ApiMessages.get("api.t.200c8a9a8250")
        );
    }

    // ------------------------------------------------------------------
    // 重定向与响应体
    // ------------------------------------------------------------------

    private static boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }

    private URI resolveRedirect(URI current, HttpResponse<InputStream> response) {
        Optional<String> location = response.headers().firstValue("Location");
        if (location.isEmpty() || location.get().isBlank()) {
            throw fetchFailed();
        }
        final URI next;
        try {
            next = current.resolve(location.get().trim());
        } catch (IllegalArgumentException e) {
            throw fetchFailed();
        }
        return stripFragment(next);
    }

    private static URI parseUri(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw urlNotAllowed();
        }
        final URI uri;
        try {
            uri = URI.create(rawUrl.trim());
        } catch (IllegalArgumentException e) {
            throw urlNotAllowed();
        }
        return stripFragment(uri);
    }

    private static URI stripFragment(URI uri) {
        if (uri.getRawFragment() == null) {
            return uri;
        }
        String text = uri.toString();
        int fragmentStart = text.indexOf('#');
        try {
            return URI.create(fragmentStart >= 0 ? text.substring(0, fragmentStart) : text);
        } catch (IllegalArgumentException e) {
            throw urlNotAllowed();
        }
    }

    private static String readBody(InputStream body, Charset charset, URI uri) {
        try (InputStream in = body) {
            java.io.ByteArrayOutputStream collected = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int total = 0;
            boolean truncated = false;
            int read;
            while ((read = in.read(buffer)) >= 0) {
                if (total + read > MAX_BODY_BYTES) {
                    collected.write(buffer, 0, MAX_BODY_BYTES - total);
                    truncated = true;
                    break;
                }
                collected.write(buffer, 0, read);
                total += read;
            }
            if (truncated) {
                logger.warn(
                        "Independent-site URL import truncated response beyond {} bytes: {}",
                        MAX_BODY_BYTES,
                        uri
                );
            }
            return collected.toString(charset);
        } catch (IOException e) {
            logger.warn("Independent-site URL import failed reading {}: {}", uri, e.getMessage());
            throw fetchFailed();
        }
    }

    private static Charset resolveCharset(String contentType) {
        Matcher matcher = CHARSET_PARAM.matcher(contentType);
        if (matcher.find()) {
            try {
                return Charset.forName(matcher.group(1));
            } catch (Exception ignored) {
                // 未知字符集按 UTF-8 处理
            }
        }
        return StandardCharsets.UTF_8;
    }

    private static void closeQuietly(InputStream body) {
        try {
            body.close();
        } catch (Exception ignored) {
            // 重定向/错误响应体关闭失败不影响主流程
        }
    }

    // ------------------------------------------------------------------
    // jsoup 内容抽取
    // ------------------------------------------------------------------

    private static ExtractedContent extract(URI finalUri, String html) {
        String finalUrl = finalUri.toString();
        Document document = Jsoup.parse(html, finalUrl);

        String title = truncate(normalizeText(document.title()), MAX_TITLE_LENGTH);
        Element metaDescription = document.selectFirst("meta[name=description]");
        String description = metaDescription == null
                ? ""
                : truncate(normalizeText(metaDescription.attr("content")), MAX_META_DESCRIPTION_LENGTH);

        List<String> headings = new ArrayList<>();
        Set<String> seenHeadings = new LinkedHashSet<>();
        for (Element heading : document.select("h1, h2, h3")) {
            if (headings.size() >= MAX_LIST_ITEMS) {
                break;
            }
            String text = truncate(normalizeText(heading.text()), MAX_HEADING_LENGTH);
            if (!text.isEmpty() && seenHeadings.add(text)) {
                headings.add(text);
            }
        }

        List<String> paragraphs = new ArrayList<>();
        Set<String> seenParagraphs = new LinkedHashSet<>();
        for (Element paragraph : document.select("p")) {
            if (paragraphs.size() >= MAX_LIST_ITEMS) {
                break;
            }
            if (hasBoilerplateAncestor(paragraph)) {
                continue;
            }
            String text = normalizeText(paragraph.text());
            if (text.length() < MIN_PARAGRAPH_LENGTH) {
                continue;
            }
            text = truncate(text, MAX_PARAGRAPH_LENGTH);
            if (seenParagraphs.add(text)) {
                paragraphs.add(text);
            }
        }

        List<String> imageUrls = new ArrayList<>();
        Set<String> seenImages = new LinkedHashSet<>();
        for (Element image : document.select("img")) {
            if (imageUrls.size() >= MAX_LIST_ITEMS) {
                break;
            }
            String absolute = extractImageUrl(image, finalUri);
            if (absolute.isEmpty() || isBlockedImage(absolute) || isTinyImage(image)) {
                continue;
            }
            if (seenImages.add(absolute)) {
                imageUrls.add(absolute);
            }
        }

        List<String> facilityHints = new ArrayList<>();
        Set<String> seenHints = new LinkedHashSet<>();
        for (Element list : document.select("ul")) {
            if (facilityHints.size() >= MAX_LIST_ITEMS) {
                break;
            }
            if (!FACILITY_KEYWORD.matcher(list.text().toLowerCase(Locale.ROOT)).find()) {
                continue;
            }
            for (Element item : list.select("li")) {
                if (facilityHints.size() >= MAX_LIST_ITEMS) {
                    break;
                }
                String text = truncate(normalizeText(item.text()), MAX_FACILITY_HINT_LENGTH);
                if (!text.isEmpty() && seenHints.add(text)) {
                    facilityHints.add(text);
                }
            }
        }

        return new ExtractedContent(
                finalUrl,
                title,
                description,
                List.copyOf(headings),
                List.copyOf(paragraphs),
                List.copyOf(imageUrls),
                List.copyOf(facilityHints)
        );
    }

    private static boolean hasBoilerplateAncestor(Element element) {
        for (Element parent = element.parent(); parent != null; parent = parent.parent()) {
            if (BOILERPLATE_TAGS.contains(parent.normalName())) {
                return true;
            }
        }
        return false;
    }

    private static String extractImageUrl(Element image, URI baseUri) {
        String candidate = image.attr("src").trim();
        if (candidate.isEmpty()) {
            String srcset = image.attr("srcset").trim();
            if (!srcset.isEmpty()) {
                candidate = srcset.split(",")[0].trim().split("\\s+")[0];
            }
        }
        if (candidate.isEmpty() || candidate.toLowerCase(Locale.ROOT).startsWith("data:")) {
            return "";
        }
        final URI resolved;
        try {
            resolved = baseUri.resolve(candidate);
        } catch (IllegalArgumentException e) {
            return "";
        }
        if (resolved == null || resolved.getScheme() == null) {
            return "";
        }
        String scheme = resolved.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            return "";
        }
        return resolved.toString();
    }

    private static boolean isBlockedImage(String absoluteUrl) {
        String lower = absoluteUrl.toLowerCase(Locale.ROOT);
        int queryStart = lower.indexOf('?');
        String path = queryStart >= 0 ? lower.substring(0, queryStart) : lower;
        String filename = path.substring(path.lastIndexOf('/') + 1);
        return filename.endsWith(".svg") || IMAGE_FILENAME_BLOCKLIST.matcher(filename).find();
    }

    private static boolean isTinyImage(Element image) {
        Integer width = parseDimension(image.attr("width"));
        Integer height = parseDimension(image.attr("height"));
        return (width != null && width < MIN_IMAGE_DIMENSION)
                || (height != null && height < MIN_IMAGE_DIMENSION);
    }

    private static Integer parseDimension(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (!normalized.matches("\\d+")) {
            return null;
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    // ------------------------------------------------------------------
    // 代理桥接（照 IndependentSitePageSchemaOpenAiClient 模式）
    // ------------------------------------------------------------------

    /**
     * 构建默认 HttpClient。Java 不识别 shell 的 https_proxy 等环境变量，这里显式桥接：
     * 优先 https_proxy/HTTPS_PROXY（HTTP 代理），其次 all_proxy/ALL_PROXY（SOCKS5），未设置则直连。
     * 仅作用于独立站 URL 导入抓取，不影响 JVM 内其他出站请求。
     */
    static HttpClient defaultHttpClient() {
        HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT);
        Proxy proxy = proxyFromEnv();
        if (proxy != null) {
            builder.proxy(ProxySelector.of((InetSocketAddress) proxy.address()));
        }
        return builder.build();
    }

    static Proxy proxyFromEnv() {
        String httpProxy = firstNonBlank(System.getenv("https_proxy"), System.getenv("HTTPS_PROXY"));
        if (httpProxy != null) {
            return toProxy(httpProxy, Proxy.Type.HTTP);
        }
        String socksProxy = firstNonBlank(System.getenv("all_proxy"), System.getenv("ALL_PROXY"));
        if (socksProxy != null) {
            return toProxy(socksProxy, Proxy.Type.SOCKS);
        }
        return null;
    }

    private static Proxy toProxy(String value, Proxy.Type type) {
        try {
            String normalized = value.trim();
            if (!normalized.contains("://")) {
                normalized = (type == Proxy.Type.SOCKS ? "socks5://" : "http://") + normalized;
            }
            URI uri = URI.create(normalized);
            String host = uri.getHost();
            int port = uri.getPort();
            if (host == null || port <= 0) {
                return null;
            }
            return new Proxy(type, new InetSocketAddress(host, port));
        } catch (Exception e) {
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
