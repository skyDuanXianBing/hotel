package server.demo.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class LangChain4jMessageKnowledgeEmbeddingProvider implements MessageKnowledgeEmbeddingProvider {
    private static final String PROVIDER_OPENAI = "openai";
    private static final String PROVIDER_DASHSCOPE = "dashscope";
    private static final String PROVIDER_AUTO = "auto";
    private static final String PROVIDER_DISABLED = "disabled";
    private static final String DEFAULT_OPENAI_MODEL = "text-embedding-3-small";
    private static final String DEFAULT_DASHSCOPE_MODEL = "text-embedding-v2";

    @Value("${messaging.knowledge.embedding.enabled:false}")
    private boolean enabled;

    @Value("${messaging.knowledge.embedding.provider:auto}")
    private String provider;

    @Value("${messaging.knowledge.embedding.model:}")
    private String modelName;

    @Value("${messaging.knowledge.embedding.dimensions:0}")
    private int configuredDimensions;

    @Value("${openai.api-key:}")
    private String openAiApiKey;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String openAiBaseUrl;

    @Value("${dashscope.api-key:}")
    private String dashScopeApiKey;

    @Value("${messaging.knowledge.embedding.dashscope.base-url:}")
    private String dashScopeBaseUrl;

    @Override
    public boolean isEnabled() {
        String normalizedProvider = providerName();
        if (!enabled) {
            return false;
        }
        if (PROVIDER_OPENAI.equals(normalizedProvider)) {
            return hasText(openAiApiKey);
        }
        if (PROVIDER_DASHSCOPE.equals(normalizedProvider)) {
            return hasText(dashScopeApiKey);
        }
        return false;
    }

    @Override
    public String providerName() {
        String normalizedProvider = configuredProviderName();
        if (PROVIDER_AUTO.equals(normalizedProvider)) {
            return autoProviderName();
        }
        return normalizedProvider;
    }

    @Override
    public MessageKnowledgeEmbeddingResponse embed(String input) {
        if (!isEnabled()) {
            throw new IllegalStateException("Message knowledge embedding provider is disabled");
        }
        if (!hasText(input)) {
            throw new IllegalArgumentException("Embedding input must not be blank");
        }

        EmbeddingModel embeddingModel = createEmbeddingModel();
        Response<Embedding> response = embeddingModel.embed(input);
        Embedding embedding = response.content();
        if (embedding == null || embedding.vector() == null || embedding.vector().length == 0) {
            throw new IllegalStateException("Embedding provider returned an empty vector");
        }
        return new MessageKnowledgeEmbeddingResponse(
                toList(embedding.vector()),
                providerName(),
                resolveModelName(),
                embedding.dimension()
        );
    }

    private EmbeddingModel createEmbeddingModel() {
        String normalizedProvider = providerName();
        if (PROVIDER_OPENAI.equals(normalizedProvider)) {
            OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder builder = OpenAiEmbeddingModel.builder()
                    .apiKey(openAiApiKey)
                    .modelName(resolveModelName());
            if (hasText(openAiBaseUrl)) {
                builder.baseUrl(openAiBaseUrl);
            }
            if (configuredDimensions > 0) {
                builder.dimensions(configuredDimensions);
            }
            return builder.build();
        }
        if (PROVIDER_DASHSCOPE.equals(normalizedProvider)) {
            QwenEmbeddingModel.QwenEmbeddingModelBuilder builder = QwenEmbeddingModel.builder()
                    .apiKey(dashScopeApiKey)
                    .modelName(resolveModelName());
            if (hasText(dashScopeBaseUrl)) {
                builder.baseUrl(dashScopeBaseUrl);
            }
            return builder.build();
        }
        throw new IllegalStateException("Unsupported embedding provider: " + provider);
    }

    private String resolveModelName() {
        if (hasText(modelName)) {
            return modelName.trim();
        }
        if (PROVIDER_DASHSCOPE.equals(providerName())) {
            return DEFAULT_DASHSCOPE_MODEL;
        }
        return DEFAULT_OPENAI_MODEL;
    }

    private String configuredProviderName() {
        if (!hasText(provider)) {
            return PROVIDER_AUTO;
        }
        return provider.trim().toLowerCase(Locale.ROOT);
    }

    private String autoProviderName() {
        if (hasText(openAiApiKey)) {
            return PROVIDER_OPENAI;
        }
        if (hasText(dashScopeApiKey)) {
            return PROVIDER_DASHSCOPE;
        }
        return PROVIDER_DISABLED;
    }

    private static List<Float> toList(float[] vector) {
        List<Float> values = new ArrayList<>();
        for (float value : vector) {
            values.add(value);
        }
        return values;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
