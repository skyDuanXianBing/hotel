package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class IndependentSitePageSchemaValidator {

    public static final String SCHEMA_VERSION = "independent_site_page_v1";

    private static final Set<String> ROOT_FIELDS = Set.of("schemaVersion", "theme", "sections");
    private static final Set<String> THEME_FIELDS = Set.of(
            "primaryColor",
            "accentColor",
            "surfaceColor",
            "textColor",
            "typography",
            "cornerStyle"
    );
    private static final Set<String> SECTION_FIELDS = Set.of(
            "type",
            "title",
            "body",
            "items",
            "alignment",
            "id",
            "imageUrl",
            "images"
    );
    private static final Set<String> SECTION_TYPES = Set.of(
            "HERO",
            "ABOUT",
            "HIGHLIGHTS",
            "AMENITIES",
            "LOCATION",
            "HOUSE_RULES",
            "GALLERY",
            "BOOKING"
    );
    private static final Set<String> IMAGE_URL_TYPES = Set.of("HERO", "ABOUT");
    private static final Set<String> GALLERY_IMAGE_FIELDS = Set.of("url", "alt");
    private static final Pattern SECTION_ID = Pattern.compile("^[A-Za-z0-9-]{1,40}$");
    private static final Pattern IMAGE_URL = Pattern.compile("^(https?://|/)[^\\s<>{}\"']{1,1500}$");
    private static final Set<String> TYPOGRAPHIES = Set.of("MODERN", "CLASSIC", "FRIENDLY");
    private static final Set<String> CORNER_STYLES = Set.of("SOFT", "SQUARE", "PILL");
    private static final Set<String> ALIGNMENTS = Set.of("LEFT", "CENTER");
    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final Pattern HTML_TAG = Pattern.compile("(?s).*<[^>]+>.*");
    private static final Pattern MONEY_VALUE = Pattern.compile(
            "(?i).*(?:[$€£¥￥]|\\b\\d+(?:\\.\\d{1,2})?\\s*(?:USD|CNY|RMB|JPY|EUR|GBP)\\b).*"
    );
    private static final Pattern CSS_DECLARATION = Pattern.compile(
            "(?is).*(?:^|[;\\s])(?:background(?:-color)?|border(?:-radius)?|color|display|font"
                    + "(?:-family|-size)?|height|margin|padding|position|width)\\s*:.*"
    );
    private static final Pattern URL_OR_ROUTE = Pattern.compile(
            "(?is).*(?:\\b[a-z][a-z0-9+.-]{1,20}://|\\bwww\\.|"
                    + "\\b[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\."
                    + "(?:com|net|org|cn|jp|co|io|hotel|travel)(?:[/\\s]|$)|"
                    + "(?:^|\\s)/(?:[a-z0-9][a-z0-9._~-]*)(?:/[a-z0-9][a-z0-9._~-]*)*"
                    + "(?:\\?\\S*)?(?:\\s|$)).*"
    );
    private static final String[] FORBIDDEN_TEXT = {
            "http://",
            "https://",
            "www.",
            "javascript:",
            "data:",
            "<script",
            "<style",
            "onclick",
            "onerror",
            "onload",
            "function(",
            "=>",
            "href=",
            "src=",
            "style=",
            "class=",
            "classname=",
            "```",
            "window.",
            "document.",
            "/api/",
            "/stay/",
            "/checkout",
            "/payment",
            "payment",
            "checkout",
            "price",
            "currency",
            "支付",
            "价格",
            "金额",
            "路由"
    };

    private final ObjectMapper objectMapper;

    public IndependentSitePageSchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode validate(JsonNode input) {
        if (input == null || !input.isObject()) {
            throw new IllegalArgumentException("页面配置必须是 JSON 对象");
        }
        assertOnlyFields(input, ROOT_FIELDS, "页面配置");

        String schemaVersion = requiredText(input, "schemaVersion", 60);
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("仅支持 " + SCHEMA_VERSION);
        }

        JsonNode theme = input.get("theme");
        if (theme == null || !theme.isObject()) {
            throw new IllegalArgumentException("页面配置缺少 theme 对象");
        }
        JsonNode sections = input.get("sections");
        if (sections == null || !sections.isArray()) {
            throw new IllegalArgumentException("页面配置缺少 sections 数组");
        }
        if (sections.isEmpty() || sections.size() > 8) {
            throw new IllegalArgumentException("sections 数量必须为 1 到 8");
        }

        ObjectNode canonical = objectMapper.createObjectNode();
        canonical.put("schemaVersion", SCHEMA_VERSION);
        canonical.set("theme", validateTheme(theme));
        canonical.set("sections", validateSections(sections));
        return canonical;
    }

    public JsonNode defaultSchema() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("schemaVersion", SCHEMA_VERSION);
        ObjectNode theme = root.putObject("theme");
        theme.put("primaryColor", "#2563EB");
        theme.put("accentColor", "#F59E0B");
        theme.put("surfaceColor", "#FFFFFF");
        theme.put("textColor", "#111827");
        theme.put("typography", "MODERN");
        theme.put("cornerStyle", "SOFT");
        ObjectNode hero = root.putArray("sections").addObject();
        hero.put("type", "HERO");
        hero.put("title", "Welcome");
        hero.put("body", "Discover a comfortable stay and reserve directly.");
        hero.put("alignment", "CENTER");
        return validate(root);
    }

    private ObjectNode validateTheme(JsonNode theme) {
        assertOnlyFields(theme, THEME_FIELDS, "theme");
        ObjectNode canonical = objectMapper.createObjectNode();
        canonical.put("primaryColor", requiredColor(theme, "primaryColor"));
        canonical.put("accentColor", requiredColor(theme, "accentColor"));
        canonical.put("surfaceColor", requiredColor(theme, "surfaceColor"));
        canonical.put("textColor", requiredColor(theme, "textColor"));
        canonical.put("typography", requiredEnum(theme, "typography", TYPOGRAPHIES));
        canonical.put("cornerStyle", requiredEnum(theme, "cornerStyle", CORNER_STYLES));
        return canonical;
    }

    private ArrayNode validateSections(JsonNode sections) {
        ArrayNode canonical = objectMapper.createArrayNode();
        Set<String> seenTypes = new HashSet<>();
        for (JsonNode section : sections) {
            if (section == null || !section.isObject()) {
                throw new IllegalArgumentException("section 必须是 JSON 对象");
            }
            assertOnlyFields(section, SECTION_FIELDS, "section");
            String type = requiredEnum(section, "type", SECTION_TYPES);
            if (!seenTypes.add(type)) {
                throw new IllegalArgumentException("section 类型不可重复: " + type);
            }

            ObjectNode normalized = canonical.addObject();
            normalized.put("type", type);

            JsonNode id = section.get("id");
            if (id != null && !id.isNull()) {
                String idValue = text(id, "id", 40);
                if (!SECTION_ID.matcher(idValue).matches()) {
                    throw new IllegalArgumentException("id 仅允许 1-40 位字母、数字和中划线");
                }
                normalized.put("id", idValue);
            }

            normalized.put("title", safeContent(requiredText(section, "title", 120), "title"));

            JsonNode body = section.get("body");
            if (body != null && !body.isNull()) {
                normalized.put("body", safeContent(text(body, "body", 600), "body"));
            }

            JsonNode items = section.get("items");
            if (items != null && !items.isNull()) {
                if (!items.isArray() || items.size() > 12) {
                    throw new IllegalArgumentException("section.items 必须是最多 12 项的数组");
                }
                if (!Set.of("HIGHLIGHTS", "AMENITIES", "HOUSE_RULES").contains(type)) {
                    throw new IllegalArgumentException(type + " 不允许 items");
                }
                ArrayNode normalizedItems = normalized.putArray("items");
                for (JsonNode item : items) {
                    normalizedItems.add(safeContent(text(item, "items", 100), "items"));
                }
            }

            JsonNode imageUrl = section.get("imageUrl");
            if (imageUrl != null && !imageUrl.isNull()) {
                if (!IMAGE_URL_TYPES.contains(type)) {
                    throw new IllegalArgumentException(type + " 不允许 imageUrl");
                }
                normalized.put("imageUrl", safeImageUrl(imageUrl, "imageUrl"));
            }

            JsonNode images = section.get("images");
            if (images != null && !images.isNull()) {
                if (!"GALLERY".equals(type)) {
                    throw new IllegalArgumentException(type + " 不允许 images");
                }
                if (!images.isArray() || images.isEmpty() || images.size() > 12) {
                    throw new IllegalArgumentException("GALLERY.images 必须是 1 到 12 项的数组");
                }
                ArrayNode normalizedImages = normalized.putArray("images");
                for (JsonNode image : images) {
                    if (image == null || !image.isObject()) {
                        throw new IllegalArgumentException("GALLERY.images 项必须是 JSON 对象");
                    }
                    assertOnlyFields(image, GALLERY_IMAGE_FIELDS, "GALLERY.images 项");
                    ObjectNode normalizedImage = normalizedImages.addObject();
                    normalizedImage.put("url", safeImageUrl(requiredNode(image, "url"), "url"));
                    JsonNode alt = image.get("alt");
                    if (alt != null && !alt.isNull()) {
                        normalizedImage.put("alt", safeContent(text(alt, "alt", 100), "alt"));
                    }
                }
            }
            if ("GALLERY".equals(type) && !normalized.has("images")) {
                throw new IllegalArgumentException("GALLERY 缺少 images");
            }

            JsonNode alignment = section.get("alignment");
            normalized.put(
                    "alignment",
                    alignment == null || alignment.isNull()
                            ? "LEFT"
                            : enumText(alignment, "alignment", ALIGNMENTS)
            );
        }
        if (!seenTypes.contains("HERO")) {
            throw new IllegalArgumentException("页面配置必须包含 HERO section");
        }
        return canonical;
    }

    private static void assertOnlyFields(JsonNode node, Set<String> allowed, String location) {
        Iterator<String> fieldNames = node.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (!allowed.contains(fieldName)) {
                throw new IllegalArgumentException(location + " 包含不允许的字段: " + fieldName);
            }
        }
    }

    private static String requiredColor(JsonNode node, String field) {
        String value = requiredText(node, field, 7);
        if (!HEX_COLOR.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " 必须是六位十六进制颜色");
        }
        return value.toUpperCase(Locale.ROOT);
    }

    private static String requiredEnum(JsonNode node, String field, Set<String> values) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException("缺少字段: " + field);
        }
        return enumText(value, field, values);
    }

    private static String enumText(JsonNode value, String field, Set<String> values) {
        String normalized = text(value, field, 40).toUpperCase(Locale.ROOT);
        if (!values.contains(normalized)) {
            throw new IllegalArgumentException(field + " 的值不受支持");
        }
        return normalized;
    }

    private static String requiredText(JsonNode node, String field, int maxLength) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException("缺少字段: " + field);
        }
        return text(value, field, maxLength);
    }

    private static String text(JsonNode value, String field, int maxLength) {
        if (value == null || !value.isTextual()) {
            throw new IllegalArgumentException(field + " 必须是文本");
        }
        String normalized = value.asText().trim();
        if (normalized.isEmpty() || normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + " 为空或超过长度限制");
        }
        return normalized;
    }

    private static String safeImageUrl(JsonNode value, String field) {
        if (value == null || !value.isTextual()) {
            throw new IllegalArgumentException(field + " 必须是文本");
        }
        String normalized = value.asText().trim();
        if (normalized.isEmpty() || !IMAGE_URL.matcher(normalized).matches()) {
            throw new IllegalArgumentException(field + " 必须是 http(s) 或 / 开头的图片地址");
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.startsWith("javascript:") || lower.startsWith("data:")) {
            throw new IllegalArgumentException(field + " 包含不允许的协议");
        }
        return normalized;
    }

    private static JsonNode requiredNode(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException("缺少字段: " + field);
        }
        return value;
    }

    private static String safeContent(String value, String field) {
        String normalized = value.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (HTML_TAG.matcher(normalized).matches() || normalized.contains("{") || normalized.contains("}")) {
            throw new IllegalArgumentException(field + " 包含 HTML/CSS/代码");
        }
        if (MONEY_VALUE.matcher(normalized).matches()) {
            throw new IllegalArgumentException(field + " 不得包含价格或货币值");
        }
        if (CSS_DECLARATION.matcher(normalized).matches()
                || URL_OR_ROUTE.matcher(normalized).matches()) {
            throw new IllegalArgumentException(field + " 不得包含 CSS、URL 或路由");
        }
        for (String forbidden : FORBIDDEN_TEXT) {
            if (lower.contains(forbidden.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException(field + " 包含不允许的页面能力");
            }
        }
        return normalized;
    }
}
