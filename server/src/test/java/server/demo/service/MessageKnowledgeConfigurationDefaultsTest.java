package server.demo.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageKnowledgeConfigurationDefaultsTest {

    @Test
    void knowledgeDefaults_shouldStayDisabledWithoutVisibleSwitch() throws IOException {
        PropertySourcesPropertyResolver resolver = newResolver(Map.of());

        assertFalse(booleanProperty(resolver, "messaging.knowledge.enabled"));
        assertFalse(booleanProperty(resolver, "messaging.knowledge.thread-extractor.enabled"));
        assertFalse(booleanProperty(resolver, "messaging.knowledge.thread-scheduler.enabled"));
        assertFalse(booleanProperty(resolver, "messaging.knowledge.embedding.enabled"));
        assertFalse(booleanProperty(resolver, "messaging.knowledge.embedding.backfill.enabled"));
        assertFalse(booleanProperty(resolver, "messaging.knowledge.semantic-retrieval.enabled"));
        assertFalse(booleanProperty(resolver, "messaging.knowledge.topic-classifier.enabled"));
        assertFalse(booleanProperty(resolver, "messaging.knowledge.semantic-rerank.enabled"));
        assertFalse(booleanProperty(resolver, "messaging.knowledge.ingest-dedup.enabled"));
        assertMissing(resolver, "messaging.knowledge.refined-scanner.enabled");
        assertMissing(resolver, "messaging.knowledge.indexer.enabled");
        assertMissing(resolver, "messaging.knowledge.legacy-fallback.enabled");
        assertMissing(resolver, "messaging.knowledge.legacy-rebuild.enabled");
    }

    @Test
    void knowledgeDefaults_shouldEnableBoundedCoreServicesWithSingleVisibleSwitch() throws IOException {
        PropertySourcesPropertyResolver resolver = newResolver(Map.of(
                "MESSAGING_KNOWLEDGE_ENABLED", "true"
        ));

        assertTrue(booleanProperty(resolver, "messaging.knowledge.enabled"));
        assertTrue(booleanProperty(resolver, "messaging.knowledge.thread-extractor.enabled"));
        assertTrue(booleanProperty(resolver, "messaging.knowledge.thread-scheduler.enabled"));
        assertTrue(booleanProperty(resolver, "messaging.knowledge.embedding.enabled"));
        assertTrue(booleanProperty(resolver, "messaging.knowledge.embedding.backfill.enabled"));
        assertTrue(booleanProperty(resolver, "messaging.knowledge.semantic-retrieval.enabled"));
        assertTrue(booleanProperty(resolver, "messaging.knowledge.topic-classifier.enabled"));

        assertFalse(booleanProperty(resolver, "messaging.knowledge.semantic-rerank.enabled"));
        assertFalse(booleanProperty(resolver, "messaging.knowledge.ingest-dedup.enabled"));
        assertMissing(resolver, "messaging.knowledge.refined-scanner.enabled");
        assertMissing(resolver, "messaging.knowledge.indexer.enabled");
        assertMissing(resolver, "messaging.knowledge.legacy-fallback.enabled");
        assertMissing(resolver, "messaging.knowledge.legacy-rebuild.enabled");

        assertEquals(30, intProperty(resolver, "messaging.knowledge.thread-dirty-marker.delay-minutes"));
        assertEquals(50, intProperty(resolver, "messaging.knowledge.thread-extractor.max-topics-per-store"));
        assertEquals(60000, intProperty(resolver, "messaging.knowledge.thread-scheduler.initial-delay-ms"));
        assertEquals(60000, intProperty(resolver, "messaging.knowledge.thread-scheduler.fixed-delay-ms"));
        assertEquals(3, intProperty(resolver, "messaging.knowledge.thread-scheduler.claim-size"));
        assertEquals(3, intProperty(resolver, "messaging.knowledge.thread-scheduler.max-concurrency"));
        assertEquals(600000, intProperty(resolver, "messaging.knowledge.thread-scheduler.lease-ms"));
        assertEquals(900000, intProperty(resolver, "messaging.knowledge.thread-scheduler.failure-backoff-ms"));

        assertEquals("auto", stringProperty(resolver, "messaging.knowledge.embedding.provider"));
        assertEquals("", stringProperty(resolver, "messaging.knowledge.embedding.model"));
        assertEquals(0, intProperty(resolver, "messaging.knowledge.embedding.dimensions"));
        assertEquals(1, intProperty(resolver, "messaging.knowledge.embedding.backfill.batch-size"));
        assertEquals(1, intProperty(resolver, "messaging.knowledge.embedding.backfill.stores-per-run"));
        assertEquals(1, intProperty(resolver, "messaging.knowledge.embedding.backfill.max-calls-per-run"));
        assertEquals(300000, intProperty(resolver, "messaging.knowledge.embedding.backfill.lease-ms"));
        assertEquals(900000, intProperty(resolver, "messaging.knowledge.embedding.backfill.failure-backoff-ms"));

        assertEquals(12, intProperty(resolver, "messaging.knowledge.semantic-retrieval.candidate-cap"));
        assertEquals(8, intProperty(resolver, "messaging.knowledge.semantic-retrieval.top-k"));
        assertEquals(0.45, doubleProperty(resolver, "messaging.knowledge.semantic-retrieval.min-score"));

        assertEquals(5, intProperty(resolver, "messaging.knowledge.ingest-dedup.candidate-cap"));
        assertEquals(0.82, doubleProperty(resolver, "messaging.knowledge.ingest-dedup.min-confidence"));
        assertEquals(320, intProperty(resolver, "messaging.knowledge.ingest-dedup.max-text-length"));
        assertEquals("gpt-4o-mini", stringProperty(resolver, "messaging.knowledge.ingest-dedup.openai.model-name"));
        assertEquals("qwen-turbo", stringProperty(resolver, "messaging.knowledge.ingest-dedup.dashscope.model-name"));
        assertEquals(512, intProperty(resolver, "messaging.knowledge.ingest-dedup.max-tokens"));
        assertEquals(0.0, doubleProperty(resolver, "messaging.knowledge.ingest-dedup.temperature"));

        assertEquals(0.8, doubleProperty(resolver, "messaging.knowledge.topic-classifier.min-confidence"));
        assertEquals(50, intProperty(resolver, "messaging.knowledge.topic-classifier.max-topics-per-store"));
        assertEquals(1, intProperty(resolver, "messaging.knowledge.topic-classifier.max-model-calls-per-run"));
        assertFalse(booleanProperty(resolver, "messaging.knowledge.topic-classifier.auto-create-enabled"));
    }

    @Test
    void knowledgeDefaults_shouldPreserveLegacySwitchOverride() throws IOException {
        PropertySourcesPropertyResolver resolver = newResolver(Map.of(
                "MESSAGING_KNOWLEDGE_ENABLED", "true",
                "MESSAGING_KNOWLEDGE_THREAD_EXTRACTOR_ENABLED", "false",
                "MESSAGING_KNOWLEDGE_TOPIC_CLASSIFIER_ENABLED", "false"
        ));

        assertTrue(booleanProperty(resolver, "messaging.knowledge.enabled"));
        assertFalse(booleanProperty(resolver, "messaging.knowledge.thread-extractor.enabled"));
        assertFalse(booleanProperty(resolver, "messaging.knowledge.thread-scheduler.enabled"));
        assertFalse(booleanProperty(resolver, "messaging.knowledge.topic-classifier.enabled"));
        assertTrue(booleanProperty(resolver, "messaging.knowledge.embedding.enabled"));
    }

    @Test
    void knowledgeDefaults_shouldAllowLegacySwitchWithoutVisibleSwitch() throws IOException {
        PropertySourcesPropertyResolver resolver = newResolver(Map.of(
                "MESSAGING_KNOWLEDGE_EMBEDDING_BACKFILL_ENABLED", "true"
        ));

        assertFalse(booleanProperty(resolver, "messaging.knowledge.enabled"));
        assertTrue(booleanProperty(resolver, "messaging.knowledge.embedding.backfill.enabled"));
        assertFalse(booleanProperty(resolver, "messaging.knowledge.embedding.enabled"));
    }

    @Test
    void embeddingProviderAuto_shouldUseExistingOpenAiKeyWhenEnabled() {
        LangChain4jMessageKnowledgeEmbeddingProvider provider =
                new LangChain4jMessageKnowledgeEmbeddingProvider();
        ReflectionTestUtils.setField(provider, "enabled", true);
        ReflectionTestUtils.setField(provider, "provider", "auto");
        ReflectionTestUtils.setField(provider, "openAiApiKey", "test-key");
        ReflectionTestUtils.setField(provider, "dashScopeApiKey", "");

        assertEquals("openai", provider.providerName());
        assertTrue(provider.isEnabled());
    }

    @Test
    void embeddingProviderAuto_shouldUseExistingDashScopeKeyWhenOpenAiKeyIsMissing() {
        LangChain4jMessageKnowledgeEmbeddingProvider provider =
                new LangChain4jMessageKnowledgeEmbeddingProvider();
        ReflectionTestUtils.setField(provider, "enabled", true);
        ReflectionTestUtils.setField(provider, "provider", "auto");
        ReflectionTestUtils.setField(provider, "openAiApiKey", "");
        ReflectionTestUtils.setField(provider, "dashScopeApiKey", "test-key");

        assertEquals("dashscope", provider.providerName());
        assertTrue(provider.isEnabled());
    }

    @Test
    void embeddingProviderAuto_shouldStayDisabledWithoutAnExistingAiKey() {
        LangChain4jMessageKnowledgeEmbeddingProvider provider =
                new LangChain4jMessageKnowledgeEmbeddingProvider();
        ReflectionTestUtils.setField(provider, "enabled", true);
        ReflectionTestUtils.setField(provider, "provider", "auto");
        ReflectionTestUtils.setField(provider, "openAiApiKey", "");
        ReflectionTestUtils.setField(provider, "dashScopeApiKey", "");

        assertEquals("disabled", provider.providerName());
        assertFalse(provider.isEnabled());
    }

    private static PropertySourcesPropertyResolver newResolver(Map<String, Object> env) throws IOException {
        Properties properties = PropertiesLoaderUtils.loadProperties(
                new FileSystemResource("src/main/resources/application.properties")
        );
        MutablePropertySources propertySources = new MutablePropertySources();
        propertySources.addFirst(new MapPropertySource("test-env", env));
        propertySources.addLast(new PropertiesPropertySource("application", properties));
        return new PropertySourcesPropertyResolver(propertySources);
    }

    private static boolean booleanProperty(
            PropertySourcesPropertyResolver resolver,
            String key
    ) {
        return Boolean.parseBoolean(stringProperty(resolver, key));
    }

    private static int intProperty(
            PropertySourcesPropertyResolver resolver,
            String key
    ) {
        return Integer.parseInt(stringProperty(resolver, key));
    }

    private static double doubleProperty(
            PropertySourcesPropertyResolver resolver,
            String key
    ) {
        return Double.parseDouble(stringProperty(resolver, key));
    }

    private static String stringProperty(
            PropertySourcesPropertyResolver resolver,
            String key
    ) {
        String value = resolver.getProperty(key);
        assertNotNull(value, "Missing property: " + key);
        return value;
    }

    private static void assertMissing(PropertySourcesPropertyResolver resolver, String key) {
        assertNull(resolver.getProperty(key), "Unexpected property: " + key);
    }
}
