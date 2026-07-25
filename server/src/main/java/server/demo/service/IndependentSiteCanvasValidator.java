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

/**
 * 独立站 Canvas 页面（自由节点树，schemaVersion=independent_site_canvas_v1）白名单校验器。
 * 与 BLOCKS 校验器相互独立：规则只能更严不能更松，前端 normalize 镜像同一契约。
 * AI 输出节点树 JSON，渲染侧递归渲染、无 v-html，XSS 面归零。
 */
@Service
public class IndependentSiteCanvasValidator {

    public static final String SCHEMA_VERSION = "independent_site_canvas_v1";

    private static final Set<String> ROOT_FIELDS = Set.of("schemaVersion", "root");
    private static final Set<String> ELEMENT_FIELDS = Set.of(
            "id",
            "type",
            "tag",
            "class",
            "attrs",
            "action",
            "children"
    );
    private static final Set<String> TEXT_FIELDS = Set.of("id", "type", "text");
    private static final Set<String> SLOT_FIELDS = Set.of("id", "type", "slot", "props");
    private static final Set<String> SLOT_PROPS_FIELDS = Set.of("layout");
    private static final Set<String> TAGS = Set.of(
            "div",
            "section",
            "header",
            "footer",
            "main",
            "nav",
            "h1",
            "h2",
            "h3",
            "h4",
            "h5",
            "h6",
            "p",
            "span",
            "a",
            "img",
            "ul",
            "ol",
            "li",
            "button",
            "figure",
            "figcaption",
            "hr",
            "strong",
            "em",
            "small",
            "blockquote"
    );
    private static final Set<String> A_ATTRS = Set.of("href", "target");
    private static final Set<String> IMG_ATTRS = Set.of("src", "alt");
    private static final Set<String> CHILDLESS_TAGS = Set.of("img", "hr");
    private static final Set<String> ACTION_TAGS = Set.of("button", "a");
    private static final String ONLY_ACTION = "scroll-to-booking";
    private static final String SLOT_ROOM_LIST = "room-list";
    private static final String SLOT_BOOKING_FLOW = "booking-flow";
    private static final Set<String> SLOTS = Set.of(SLOT_ROOM_LIST, SLOT_BOOKING_FLOW);
    private static final Set<String> SLOT_LAYOUTS = Set.of("grid", "list");

    private static final int MAX_CLASS_LENGTH = 1500;
    private static final int MAX_HREF_LENGTH = 1500;
    private static final int MAX_ALT_LENGTH = 200;
    private static final int MAX_TEXT_LENGTH = 500;
    private static final int MAX_CHILDREN = 25;
    private static final int MAX_NODES = 300;
    private static final int MAX_DEPTH = 14;

    private static final Pattern NODE_ID = Pattern.compile("^[a-z0-9][a-z0-9-]{1,39}$");
    private static final Pattern URL_FORBIDDEN_CHARS = Pattern.compile(".*[\\s<>{}\"'].*");
    /** class 黑名单：大小写不敏感，命中即非法（arbitrary value 如 bg-[#1a2b3c] 允许）。 */
    private static final String[] CLASS_FORBIDDEN = {
            "url(",
            "javascript",
            "expression(",
            "<",
            ">",
            "{",
            "}",
            "`",
            "\\",
            "!",
            "@",
            ";"
    };

    // 以下文本规则与 IndependentSitePageSchemaValidator.safeContent 同清单（价格/货币/URL/代码禁令）。
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

    public IndependentSiteCanvasValidator(ObjectMapper objectMapper) {
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
        JsonNode root = input.get("root");
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("页面配置缺少 root 节点");
        }

        ObjectNode canonical = objectMapper.createObjectNode();
        canonical.put("schemaVersion", SCHEMA_VERSION);
        canonical.set("root", validateNode(root, 1, new TreeState()));
        return canonical;
    }

    /**
     * 新建站点 HOME / 新自定义页的默认 Canvas 草稿骨架。
     * 产出会再过一遍 validate()，保证骨架自洽。
     */
    public JsonNode defaultCanvasSchema(String storeName) {
        String name = storeName == null || storeName.isBlank() ? "Hotel" : storeName.trim();
        if (name.length() > 120) {
            name = name.substring(0, 120);
        }

        ObjectNode root = objectMapper.createObjectNode();
        root.put("schemaVersion", SCHEMA_VERSION);
        ObjectNode main = root.putObject("root");
        main.put("id", "root");
        main.put("type", "element");
        main.put("tag", "main");
        main.put("class", "min-h-screen bg-white text-slate-800");
        ArrayNode mainChildren = main.putArray("children");

        ObjectNode hero = mainChildren.addObject();
        hero.put("id", "sec-hero");
        hero.put("type", "element");
        hero.put("tag", "section");
        hero.put(
                "class",
                "flex min-h-[60vh] flex-col items-center justify-center gap-6 px-6 text-center"
        );
        ArrayNode heroChildren = hero.putArray("children");

        ObjectNode title = heroChildren.addObject();
        title.put("id", "hero-title");
        title.put("type", "element");
        title.put("tag", "h1");
        title.put("class", "text-4xl font-bold tracking-wide md:text-6xl");
        ArrayNode titleChildren = title.putArray("children");
        textNode(titleChildren, "hero-title-t", name);

        ObjectNode sub = heroChildren.addObject();
        sub.put("id", "hero-sub");
        sub.put("type", "element");
        sub.put("tag", "p");
        sub.put("class", "max-w-xl text-lg text-slate-500");
        ArrayNode subChildren = sub.putArray("children");
        textNode(subChildren, "hero-sub-t", "Welcome. Book direct for the best stay.");

        ObjectNode cta = heroChildren.addObject();
        cta.put("id", "hero-cta");
        cta.put("type", "element");
        cta.put("tag", "button");
        cta.put(
                "class",
                "rounded-full bg-slate-900 px-8 py-3 text-white transition hover:bg-slate-700"
        );
        cta.put("action", ONLY_ACTION);
        ArrayNode ctaChildren = cta.putArray("children");
        textNode(ctaChildren, "hero-cta-t", "立即预订");

        ObjectNode slot = mainChildren.addObject();
        slot.put("id", "slot-rooms");
        slot.put("type", "slot");
        slot.put("slot", SLOT_ROOM_LIST);
        ObjectNode props = slot.putObject("props");
        props.put("layout", "grid");

        ObjectNode bookingSlot = mainChildren.addObject();
        bookingSlot.put("id", "slot-booking");
        bookingSlot.put("type", "slot");
        bookingSlot.put("slot", SLOT_BOOKING_FLOW);

        return validate(root);
    }

    private void textNode(ArrayNode parent, String id, String text) {
        ObjectNode node = parent.addObject();
        node.put("id", id);
        node.put("type", "text");
        node.put("text", text);
    }

    private ObjectNode validateNode(JsonNode node, int depth, TreeState state) {
        if (depth > MAX_DEPTH) {
            throw new IllegalArgumentException("节点树深度不可超过 " + MAX_DEPTH);
        }
        state.nodeCount++;
        if (state.nodeCount > MAX_NODES) {
            throw new IllegalArgumentException("节点总数不可超过 " + MAX_NODES);
        }
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException("节点必须是 JSON 对象");
        }
        String type = requiredText(node, "type", 20);
        return switch (type) {
            case "element" -> validateElement(node, depth, state);
            case "text" -> validateText(node, state);
            case "slot" -> validateSlot(node, state);
            default -> throw new IllegalArgumentException("节点类型不受支持: " + type);
        };
    }

    private ObjectNode validateElement(JsonNode node, int depth, TreeState state) {
        assertOnlyFields(node, ELEMENT_FIELDS, "element 节点");
        ObjectNode canonical = objectMapper.createObjectNode();
        canonical.put("id", nodeId(node, state));
        canonical.put("type", "element");

        String tag = requiredText(node, "tag", 30).toLowerCase(Locale.ROOT);
        if (!TAGS.contains(tag)) {
            throw new IllegalArgumentException("tag 不在白名单内: " + tag);
        }
        canonical.put("tag", tag);

        JsonNode classValue = node.get("class");
        if (classValue != null && !classValue.isNull()) {
            String normalized = safeClass(classValue);
            if (normalized != null) {
                canonical.put("class", normalized);
            }
        }

        JsonNode attrs = node.get("attrs");
        if (attrs != null && !attrs.isNull()) {
            ObjectNode normalizedAttrs = validateAttrs(tag, attrs);
            if (normalizedAttrs != null && !normalizedAttrs.isEmpty()) {
                canonical.set("attrs", normalizedAttrs);
            }
        }

        JsonNode action = node.get("action");
        if (action != null && !action.isNull()) {
            String value = text(action, "action", 40);
            if (!ACTION_TAGS.contains(tag)) {
                throw new IllegalArgumentException(tag + " 不允许 action");
            }
            if (!ONLY_ACTION.equals(value)) {
                throw new IllegalArgumentException("action 的值不受支持");
            }
            if ("a".equals(tag)
                    && canonical.has("attrs")
                    && canonical.path("attrs").has("href")) {
                throw new IllegalArgumentException("a 节点的 href 与 action 互斥");
            }
            canonical.put("action", ONLY_ACTION);
        }

        JsonNode children = node.get("children");
        if (children != null && !children.isNull()) {
            if (CHILDLESS_TAGS.contains(tag)) {
                throw new IllegalArgumentException(tag + " 不允许 children");
            }
            if (!children.isArray() || children.size() > MAX_CHILDREN) {
                throw new IllegalArgumentException("children 必须是最多 " + MAX_CHILDREN + " 项的数组");
            }
            if (!children.isEmpty()) {
                ArrayNode normalizedChildren = canonical.putArray("children");
                for (JsonNode child : children) {
                    normalizedChildren.add(validateNode(child, depth + 1, state));
                }
            }
        }
        return canonical;
    }

    private ObjectNode validateText(JsonNode node, TreeState state) {
        assertOnlyFields(node, TEXT_FIELDS, "text 节点");
        ObjectNode canonical = objectMapper.createObjectNode();
        canonical.put("id", nodeId(node, state));
        canonical.put("type", "text");
        canonical.put("text", safeContent(requiredText(node, "text", MAX_TEXT_LENGTH), "text"));
        return canonical;
    }

    private ObjectNode validateSlot(JsonNode node, TreeState state) {
        assertOnlyFields(node, SLOT_FIELDS, "slot 节点");
        ObjectNode canonical = objectMapper.createObjectNode();
        canonical.put("id", nodeId(node, state));
        canonical.put("type", "slot");
        String slot = requiredText(node, "slot", 30);
        if (!SLOTS.contains(slot)) {
            throw new IllegalArgumentException("slot 的值不受支持");
        }
        canonical.put("slot", slot);
        if (!state.seenSlots.add(slot)) {
            throw new IllegalArgumentException("每页 " + slot + " 插槽至多 1 个");
        }

        JsonNode props = node.get("props");
        if (SLOT_BOOKING_FLOW.equals(slot)) {
            // booking-flow 无 props：出现即非法（与未知字段同一拒绝策略）
            if (props != null && !props.isNull()) {
                throw new IllegalArgumentException("booking-flow 插槽不支持 props");
            }
            return canonical;
        }

        String layout = "grid";
        if (props != null && !props.isNull()) {
            if (!props.isObject()) {
                throw new IllegalArgumentException("slot.props 必须是 JSON 对象");
            }
            assertOnlyFields(props, SLOT_PROPS_FIELDS, "slot.props");
            JsonNode layoutValue = props.get("layout");
            if (layoutValue != null && !layoutValue.isNull()) {
                layout = text(layoutValue, "layout", 10).toLowerCase(Locale.ROOT);
                if (!SLOT_LAYOUTS.contains(layout)) {
                    throw new IllegalArgumentException("layout 的值不受支持");
                }
            }
        }
        ObjectNode canonicalProps = canonical.putObject("props");
        canonicalProps.put("layout", layout);
        return canonical;
    }

    private ObjectNode validateAttrs(String tag, JsonNode attrs) {
        if (!"a".equals(tag) && !"img".equals(tag)) {
            throw new IllegalArgumentException(tag + " 不允许 attrs");
        }
        if (!attrs.isObject()) {
            throw new IllegalArgumentException("attrs 必须是 JSON 对象");
        }
        ObjectNode canonical = objectMapper.createObjectNode();
        if ("a".equals(tag)) {
            assertOnlyFields(attrs, A_ATTRS, "a.attrs");
            JsonNode href = attrs.get("href");
            if (href != null && !href.isNull()) {
                canonical.put("href", safeLink(href, "href", true));
            }
            JsonNode target = attrs.get("target");
            if (target != null && !target.isNull()) {
                if (!"_blank".equals(text(target, "target", 20))) {
                    throw new IllegalArgumentException("target 仅允许 _blank");
                }
                canonical.put("target", "_blank");
            }
        } else {
            assertOnlyFields(attrs, IMG_ATTRS, "img.attrs");
            JsonNode src = attrs.get("src");
            if (src == null || src.isNull()) {
                throw new IllegalArgumentException("img 缺少 src");
            }
            canonical.put("src", safeLink(src, "src", false));
            JsonNode alt = attrs.get("alt");
            if (alt != null && !alt.isNull()) {
                String normalized = safeContent(text(alt, "alt", MAX_ALT_LENGTH), "alt");
                if (!normalized.isEmpty()) {
                    canonical.put("alt", normalized);
                }
            }
        }
        return canonical;
    }

    /** href/src 规则：http(s) 绝对地址、/ 开头相对地址（禁 //）、# 锚点（src 禁用）。 */
    private static String safeLink(JsonNode value, String field, boolean allowAnchor) {
        if (!value.isTextual()) {
            throw new IllegalArgumentException(field + " 必须是文本");
        }
        String normalized = value.asText().trim();
        if (normalized.isEmpty() || normalized.length() > MAX_HREF_LENGTH) {
            throw new IllegalArgumentException(field + " 为空或超过长度限制");
        }
        if (URL_FORBIDDEN_CHARS.matcher(normalized).matches()) {
            throw new IllegalArgumentException(field + " 包含不允许的字符");
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return normalized;
        }
        if (lower.startsWith("javascript:") || lower.startsWith("data:") || normalized.startsWith("//")) {
            throw new IllegalArgumentException(field + " 包含不允许的协议");
        }
        if (normalized.startsWith("/")) {
            return normalized;
        }
        if (allowAnchor && normalized.startsWith("#")) {
            return normalized;
        }
        throw new IllegalArgumentException(field + " 必须是 http(s)、/ 开头相对地址或 # 锚点");
    }

    private static String safeClass(JsonNode value) {
        if (!value.isTextual()) {
            throw new IllegalArgumentException("class 必须是文本");
        }
        String normalized = value.asText().trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > MAX_CLASS_LENGTH) {
            throw new IllegalArgumentException("class 超过长度限制");
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        for (String forbidden : CLASS_FORBIDDEN) {
            if (lower.contains(forbidden.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("class 包含不允许的内容");
            }
        }
        return normalized;
    }

    private String nodeId(JsonNode node, TreeState state) {
        String id = requiredText(node, "id", 40);
        if (!NODE_ID.matcher(id).matches()) {
            throw new IllegalArgumentException("id 必须匹配 ^[a-z0-9][a-z0-9-]{1,39}$");
        }
        if (!state.ids.add(id)) {
            throw new IllegalArgumentException("id 在全树必须唯一: " + id);
        }
        return id;
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

    /** 遍历状态：节点计数、id 去重、每种插槽全树至多 1 个。 */
    private static final class TreeState {
        private int nodeCount;
        private final Set<String> ids = new HashSet<>();
        private final Set<String> seenSlots = new HashSet<>();
    }
}
