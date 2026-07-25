package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndependentSiteCanvasValidatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final IndependentSiteCanvasValidator validator = new IndependentSiteCanvasValidator(objectMapper);

    private static final String VALID_TREE = """
            {
              "schemaVersion":"independent_site_canvas_v1",
              "root":{
                "id":"root","type":"element","tag":"main","class":"min-h-screen bg-white text-slate-800",
                "children":[
                  {"id":"sec-hero","type":"element","tag":"section","class":"flex min-h-[60vh] flex-col items-center justify-center gap-6 px-6 text-center","children":[
                    {"id":"hero-title","type":"element","tag":"h1","class":"text-4xl font-bold tracking-wide md:text-6xl","children":[
                      {"id":"hero-title-t","type":"text","text":"山景温泉民宿"}
                    ]},
                    {"id":"hero-cta","type":"element","tag":"button","class":"rounded-full bg-slate-900 px-8 py-3 text-white","action":"scroll-to-booking","children":[
                      {"id":"hero-cta-t","type":"text","text":"立即预订"}
                    ]},
                    {"id":"hero-link","type":"element","tag":"a","attrs":{"href":"https://example.com/about","target":"_blank"},"children":[
                      {"id":"hero-link-t","type":"text","text":"了解更多"}
                    ]},
                    {"id":"hero-img","type":"element","tag":"img","class":"w-full rounded-2xl","attrs":{"src":"/media/1/room/a.jpg","alt":"山景客房"}}
                  ]},
                  {"id":"slot-rooms","type":"slot","slot":"room-list","props":{"layout":"grid"}}
                ]
              }
            }
            """;

    @Test
    void validate_shouldAcceptValidTreeAndNormalize() throws Exception {
        JsonNode canonical = validator.validate(objectMapper.readTree(VALID_TREE));

        assertEquals(IndependentSiteCanvasValidator.SCHEMA_VERSION, canonical.path("schemaVersion").asText());
        JsonNode root = canonical.path("root");
        assertEquals("main", root.path("tag").asText());
        JsonNode hero = root.path("children").get(0);
        assertEquals("section", hero.path("tag").asText());
        JsonNode link = hero.path("children").get(2);
        assertEquals("https://example.com/about", link.path("attrs").path("href").asText());
        assertEquals("_blank", link.path("attrs").path("target").asText());
        JsonNode slot = root.path("children").get(1);
        assertEquals("room-list", slot.path("slot").asText());
        assertEquals("grid", slot.path("props").path("layout").asText());
    }

    @Test
    void validate_shouldNormalizeSlotLayoutDefaultAndDropEmptyOptionals() throws Exception {
        JsonNode input = objectMapper.readTree("""
                {
                  "schemaVersion":"independent_site_canvas_v1",
                  "root":{"id":"root","type":"element","tag":"main","class":"  ","children":[
                    {"id":"slot-rooms","type":"slot","slot":"room-list"}
                  ]}
                }
                """);

        JsonNode canonical = validator.validate(input);

        assertFalse(canonical.path("root").has("class"));
        assertEquals("grid", canonical.path("root").path("children").get(0).path("props").path("layout").asText());
    }

    @Test
    void validate_shouldRejectWrongSchemaVersion() throws Exception {
        JsonNode input = objectMapper.readTree(VALID_TREE);
        ((ObjectNode) input).put("schemaVersion", "independent_site_page_v1");

        assertThrows(IllegalArgumentException.class, () -> validator.validate(input));
    }

    @Test
    void validate_shouldRejectUnknownTag() throws Exception {
        assertRejectedWith("\"tag\":\"script\"", "tag");
        assertRejectedWith("\"tag\":\"iframe\"", "tag");
    }

    @Test
    void validate_shouldRejectAttrsOnNonLinkTags() throws Exception {
        // attrs 出现在 div 上
        String tree = """
                {
                  "schemaVersion":"independent_site_canvas_v1",
                  "root":{"id":"root","type":"element","tag":"div","attrs":{"href":"https://example.com"}}
                }
                """;
        assertThrows(IllegalArgumentException.class, () -> validator.validate(objectMapper.readTree(tree)));
    }

    @Test
    void validate_shouldRejectUnknownAttrKeys() throws Exception {
        String tree = """
                {
                  "schemaVersion":"independent_site_canvas_v1",
                  "root":{"id":"root","type":"element","tag":"a","attrs":{"href":"https://example.com","onclick":"x"}}
                }
                """;
        assertThrows(IllegalArgumentException.class, () -> validator.validate(objectMapper.readTree(tree)));
    }

    @Test
    void validate_shouldRejectBadHrefValues() throws Exception {
        assertRejectedAttr("javascript:alert(1)");
        assertRejectedAttr("data:text/html;base64,AAAA");
        assertRejectedAttr("//cdn.example.com/x.js");
        assertRejectedAttr("example.com/no-scheme");
        assertRejectedAttr("https://exa mple.com/x");
    }

    @Test
    void validate_shouldAcceptHrefVariants() throws Exception {
        assertAcceptedAttr("https://example.com/x.jpg");
        assertAcceptedAttr("http://example.com/x.jpg");
        assertAcceptedAttr("/media/1/room/a.jpg");
        assertAcceptedAttr("#booking");
    }

    @Test
    void validate_shouldRejectAnchorForImgSrc() throws Exception {
        String tree = """
                {
                  "schemaVersion":"independent_site_canvas_v1",
                  "root":{"id":"root","type":"element","tag":"img","attrs":{"src":"#anchor"}}
                }
                """;
        assertThrows(IllegalArgumentException.class, () -> validator.validate(objectMapper.readTree(tree)));
    }

    @Test
    void validate_shouldRejectNonBlankTarget() throws Exception {
        String tree = """
                {
                  "schemaVersion":"independent_site_canvas_v1",
                  "root":{"id":"root","type":"element","tag":"a","attrs":{"href":"https://example.com","target":"_self"}}
                }
                """;
        assertThrows(IllegalArgumentException.class, () -> validator.validate(objectMapper.readTree(tree)));
    }

    @Test
    void validate_shouldRejectClassBlacklist() throws Exception {
        assertRejectedClass("bg-[url(https://example.com/x.png)]");
        assertRejectedClass("x-javascript-y");
        assertRejectedClass("width: expression(alert(1))");
        assertRejectedClass("font-bold; color: red");
        assertRejectedClass("!important");
        assertRejectedClass("@media");
        assertRejectedClass("a<b");
        assertRejectedClass("a{b");
        assertRejectedClass("a`b");
        // 反斜杠不能走 JSON 字符串拼接（\b 会被解析成退格符），用 ObjectNode 构造
        ObjectNode backslash = objectMapper.createObjectNode();
        backslash.put("schemaVersion", IndependentSiteCanvasValidator.SCHEMA_VERSION);
        ObjectNode backslashRoot = backslash.putObject("root");
        backslashRoot.put("id", "root");
        backslashRoot.put("type", "element");
        backslashRoot.put("tag", "div");
        backslashRoot.put("class", "a\\b");
        assertThrows(IllegalArgumentException.class, () -> validator.validate(backslash));
    }

    @Test
    void validate_shouldAcceptTailwindArbitraryValues() throws Exception {
        JsonNode tree = objectMapper.readTree("""
                {
                  "schemaVersion":"independent_site_canvas_v1",
                  "root":{"id":"root","type":"element","tag":"div",
                    "class":"bg-[#1a2b3c] mt-[3px] grid-cols-[1fr_2fr] bg-[var(--site-primary)] md:text-6xl"}
                }
                """);

        JsonNode canonical = validator.validate(tree);
        assertTrue(canonical.path("root").path("class").asText().contains("grid-cols-[1fr_2fr]"));
    }

    @Test
    void validate_shouldRejectPriceAndPaymentText() throws Exception {
        assertRejectedText("Only $100 per night");
        assertRejectedText("每晚 100 USD 起");
        assertRejectedText("Best price guaranteed");
        assertRejectedText("支持在线支付");
        assertRejectedText("查看价格");
    }

    @Test
    void validate_shouldRejectHtmlUrlAndRouteText() throws Exception {
        assertRejectedText("<b>bold</b>");
        assertRejectedText("visit example.com today");
        assertRejectedText("go to /stay/alpha");
        assertRejectedText("点击 https://example.com");
    }

    @Test
    void validate_shouldRejectTooLongText() throws Exception {
        assertRejectedText("x".repeat(501));
    }

    @Test
    void validate_shouldRejectIllegalAndDuplicateIds() throws Exception {
        // 大写不允许
        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(objectMapper.readTree("""
                        {"schemaVersion":"independent_site_canvas_v1",
                         "root":{"id":"Root","type":"element","tag":"main"}}
                        """))
        );
        // 重复 id
        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(objectMapper.readTree("""
                        {"schemaVersion":"independent_site_canvas_v1",
                         "root":{"id":"root","type":"element","tag":"main","children":[
                           {"id":"dup","type":"text","text":"a"},
                           {"id":"dup","type":"text","text":"b"}
                         ]}}
                        """))
        );
    }

    @Test
    void validate_shouldRejectSecondRoomListSlot() throws Exception {
        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(objectMapper.readTree("""
                        {"schemaVersion":"independent_site_canvas_v1",
                         "root":{"id":"root","type":"element","tag":"main","children":[
                           {"id":"s1","type":"slot","slot":"room-list"},
                           {"id":"s2","type":"slot","slot":"room-list"}
                         ]}}
                        """))
        );
    }

    @Test
    void validate_shouldAcceptBookingFlowSlotWithoutProps() throws Exception {
        JsonNode canonical = validator.validate(objectMapper.readTree("""
                {
                  "schemaVersion":"independent_site_canvas_v1",
                  "root":{"id":"root","type":"element","tag":"main","children":[
                    {"id":"slot-rooms","type":"slot","slot":"room-list"},
                    {"id":"slot-booking","type":"slot","slot":"booking-flow"}
                  ]}
                }
                """));

        JsonNode booking = canonical.path("root").path("children").get(1);
        assertEquals("booking-flow", booking.path("slot").asText());
        assertFalse(booking.has("props"));
    }

    @Test
    void validate_shouldRejectSecondBookingFlowSlot() throws Exception {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(objectMapper.readTree("""
                        {"schemaVersion":"independent_site_canvas_v1",
                         "root":{"id":"root","type":"element","tag":"main","children":[
                           {"id":"s1","type":"slot","slot":"booking-flow"},
                           {"id":"s2","type":"slot","slot":"booking-flow"}
                         ]}}
                        """))
        );
        assertTrue(exception.getMessage().contains("booking-flow"));
    }

    @Test
    void validate_shouldRejectPropsOnBookingFlow() throws Exception {
        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(objectMapper.readTree("""
                        {"schemaVersion":"independent_site_canvas_v1",
                         "root":{"id":"root","type":"element","tag":"main","children":[
                           {"id":"s1","type":"slot","slot":"booking-flow","props":{"layout":"grid"}}
                         ]}}
                        """))
        );
    }

    @Test
    void validate_shouldRejectTooDeepTree() {
        // 手工构造 15 层嵌套（限制 14）
        ObjectMapper mapper = objectMapper;
        ObjectNode root = mapper.createObjectNode();
        root.put("schemaVersion", IndependentSiteCanvasValidator.SCHEMA_VERSION);
        ObjectNode current = root.putObject("root");
        current.put("id", "n1");
        current.put("type", "element");
        current.put("tag", "div");
        for (int depth = 2; depth <= 15; depth++) {
            ObjectNode child = current.putArray("children").addObject();
            child.put("id", "n" + depth);
            child.put("type", "element");
            child.put("tag", "div");
            current = child;
        }

        assertThrows(IllegalArgumentException.class, () -> validator.validate(root));
    }

    @Test
    void validate_shouldRejectTooManyNodes() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("schemaVersion", IndependentSiteCanvasValidator.SCHEMA_VERSION);
        ObjectNode main = root.putObject("root");
        main.put("id", "root");
        main.put("type", "element");
        main.put("tag", "main");
        var children = main.putArray("children");
        for (int index = 0; index < 25; index++) {
            ObjectNode section = children.addObject();
            section.put("id", "sec-" + index);
            section.put("type", "element");
            section.put("tag", "section");
            var grandchildren = section.putArray("children");
            for (int child = 0; child < 13; child++) {
                ObjectNode text = grandchildren.addObject();
                text.put("id", "t-" + index + "-" + child);
                text.put("type", "text");
                text.put("text", "hello");
            }
        }

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(root)
        );
        assertTrue(exception.getMessage().contains("节点总数"));
    }

    @Test
    void validate_shouldRejectTooManyChildren() throws Exception {
        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(objectMapper.readTree("""
                        {"schemaVersion":"independent_site_canvas_v1",
                         "root":{"id":"root","type":"element","tag":"main","children":[
                           {"id":"t1","type":"text","text":"a"},{"id":"t2","type":"text","text":"a"},
                           {"id":"t3","type":"text","text":"a"},{"id":"t4","type":"text","text":"a"},
                           {"id":"t5","type":"text","text":"a"},{"id":"t6","type":"text","text":"a"},
                           {"id":"t7","type":"text","text":"a"},{"id":"t8","type":"text","text":"a"},
                           {"id":"t9","type":"text","text":"a"},{"id":"t10","type":"text","text":"a"},
                           {"id":"t11","type":"text","text":"a"},{"id":"t12","type":"text","text":"a"},
                           {"id":"t13","type":"text","text":"a"},{"id":"t14","type":"text","text":"a"},
                           {"id":"t15","type":"text","text":"a"},{"id":"t16","type":"text","text":"a"},
                           {"id":"t17","type":"text","text":"a"},{"id":"t18","type":"text","text":"a"},
                           {"id":"t19","type":"text","text":"a"},{"id":"t20","type":"text","text":"a"},
                           {"id":"t21","type":"text","text":"a"},{"id":"t22","type":"text","text":"a"},
                           {"id":"t23","type":"text","text":"a"},{"id":"t24","type":"text","text":"a"},
                           {"id":"t25","type":"text","text":"a"},{"id":"t26","type":"text","text":"a"}
                         ]}}
                        """))
        );
    }

    @Test
    void validate_shouldRejectImgWithChildrenAndMissingSrc() throws Exception {
        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(objectMapper.readTree("""
                        {"schemaVersion":"independent_site_canvas_v1",
                         "root":{"id":"root","type":"element","tag":"img","attrs":{"src":"/a.jpg"},
                           "children":[{"id":"t1","type":"text","text":"a"}]}}
                        """))
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(objectMapper.readTree("""
                        {"schemaVersion":"independent_site_canvas_v1",
                         "root":{"id":"root","type":"element","tag":"img","attrs":{"alt":"x"}}}
                        """))
        );
    }

    @Test
    void validate_shouldEnforceActionRules() throws Exception {
        // action 只允许 scroll-to-booking
        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(objectMapper.readTree("""
                        {"schemaVersion":"independent_site_canvas_v1",
                         "root":{"id":"root","type":"element","tag":"button","action":"open-booking"}}
                        """))
        );
        // action 不允许出现在 div
        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(objectMapper.readTree("""
                        {"schemaVersion":"independent_site_canvas_v1",
                         "root":{"id":"root","type":"element","tag":"div","action":"scroll-to-booking"}}
                        """))
        );
        // a 同时带 href 与 action 非法
        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(objectMapper.readTree("""
                        {"schemaVersion":"independent_site_canvas_v1",
                         "root":{"id":"root","type":"element","tag":"a","attrs":{"href":"https://example.com"},
                           "action":"scroll-to-booking"}}
                        """))
        );
    }

    @Test
    void defaultCanvasSchema_shouldBeSelfConsistentAndContainStoreName() {
        JsonNode schema = validator.defaultCanvasSchema("山景温泉民宿");

        assertEquals(IndependentSiteCanvasValidator.SCHEMA_VERSION, schema.path("schemaVersion").asText());
        assertEquals(
                "山景温泉民宿",
                schema.path("root").path("children").get(0)
                        .path("children").get(0)
                        .path("children").get(0)
                        .path("text").asText()
        );
        assertEquals(
                "room-list",
                schema.path("root").path("children").get(1).path("slot").asText()
        );
        // 骨架末尾追加 booking-flow 插槽（无 props）
        JsonNode bookingSlot = schema.path("root").path("children").get(2);
        assertEquals("slot-booking", bookingSlot.path("id").asText());
        assertEquals("booking-flow", bookingSlot.path("slot").asText());
        assertFalse(bookingSlot.has("props"));
        // 自洽：规范化输出再过一遍校验必须仍然通过且结果相等
        JsonNode again = validator.validate(schema);
        assertEquals(schema, again);
    }

    @Test
    void defaultCanvasSchema_shouldFallbackWhenStoreNameBlank() {
        JsonNode schema = validator.defaultCanvasSchema("  ");

        assertEquals(
                "Hotel",
                schema.path("root").path("children").get(0)
                        .path("children").get(0)
                        .path("children").get(0)
                        .path("text").asText()
        );
    }

    private void assertRejectedWith(String replacement, String fieldToReplace) throws Exception {
        String tree = VALID_TREE.replaceFirst(
                "\"tag\":\"h1\"",
                replacement
        );
        assertThrows(IllegalArgumentException.class, () -> validator.validate(objectMapper.readTree(tree)));
    }

    private void assertRejectedAttr(String href) throws Exception {
        String tree = """
                {
                  "schemaVersion":"independent_site_canvas_v1",
                  "root":{"id":"root","type":"element","tag":"a","attrs":{"href":"%s"}}
                }
                """.formatted(href);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(objectMapper.readTree(tree)));
    }

    private void assertAcceptedAttr(String href) throws Exception {
        String tree = """
                {
                  "schemaVersion":"independent_site_canvas_v1",
                  "root":{"id":"root","type":"element","tag":"a","attrs":{"href":"%s"}}
                }
                """.formatted(href);
        validator.validate(objectMapper.readTree(tree));
    }

    private void assertRejectedClass(String classValue) throws Exception {
        String tree = """
                {
                  "schemaVersion":"independent_site_canvas_v1",
                  "root":{"id":"root","type":"element","tag":"div","class":"%s"}
                }
                """.formatted(classValue);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(objectMapper.readTree(tree)));
    }

    private void assertRejectedText(String text) throws Exception {
        String tree = """
                {
                  "schemaVersion":"independent_site_canvas_v1",
                  "root":{"id":"root","type":"element","tag":"p","children":[
                    {"id":"t1","type":"text","text":"%s"}
                  ]}
                }
                """.formatted(text);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(objectMapper.readTree(tree)));
    }
}
