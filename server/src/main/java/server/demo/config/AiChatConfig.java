package server.demo.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import server.demo.service.impl.MockChatLanguageModel;

@Configuration
public class AiChatConfig {
    private static final int DEFAULT_CHAT_MAX_TOKENS = 4096;
    private static final int DEFAULT_DEDUP_MAX_TOKENS = 512;

    @Value("${openai.api-key:#{environment.OPENAI_API_KEY}}")
    private String openAiApiKey;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String openAiBaseUrl;

    @Value("${openai.model-name:gpt-4o-mini}")
    private String openAiModelName;

    @Value("${openai.max-tokens:" + DEFAULT_CHAT_MAX_TOKENS + "}")
    private Integer openAiMaxTokens;

    @Value("${openai.temperature:0.7}")
    private Double openAiTemperature;

    @Value("${dashscope.api-key:#{environment.DASH_SCOPE_API_KEY}}")
    private String dashScopeApiKey;

    @Value("${dashscope.model-name:qwen-turbo}")
    private String dashScopeModelName;

    @Value("${dashscope.max-tokens:" + DEFAULT_CHAT_MAX_TOKENS + "}")
    private Integer dashScopeMaxTokens;

    @Value("${dashscope.temperature:0.7}")
    private Double dashScopeTemperature;

    @Value("${messaging.knowledge.ingest-dedup.openai.model-name:gpt-4o-mini}")
    private String ingestDedupOpenAiModelName;

    @Value("${messaging.knowledge.ingest-dedup.dashscope.model-name:qwen-turbo}")
    private String ingestDedupDashScopeModelName;

    @Value("${messaging.knowledge.ingest-dedup.max-tokens:" + DEFAULT_DEDUP_MAX_TOKENS + "}")
    private Integer ingestDedupMaxTokens;

    @Value("${messaging.knowledge.ingest-dedup.temperature:0.0}")
    private Double ingestDedupTemperature;

    @Bean
    @Primary
    public ChatLanguageModel chatLanguageModel() {
        if (hasText(openAiApiKey)) {
            return createOpenAiChatModel(openAiModelName, openAiMaxTokens, openAiTemperature);
        }

        if (hasText(dashScopeApiKey)) {
            return createDashScopeChatModel(dashScopeModelName, dashScopeMaxTokens, dashScopeTemperature);
        }

        System.out.println("警告：未配置 OpenAI 或 DashScope API Key，使用 Mock AI 模型。请设置 OPENAI_API_KEY（推荐）或 DASH_SCOPE_API_KEY。");
        return new MockChatLanguageModel();
    }

    @Bean(name = "messageKnowledgeDedupChatLanguageModel")
    public ChatLanguageModel messageKnowledgeDedupChatLanguageModel() {
        if (hasText(openAiApiKey)) {
            return createOpenAiChatModel(
                    ingestDedupOpenAiModelName,
                    ingestDedupMaxTokens,
                    ingestDedupTemperature
            );
        }

        if (hasText(dashScopeApiKey)) {
            return createDashScopeChatModel(
                    ingestDedupDashScopeModelName,
                    ingestDedupMaxTokens,
                    ingestDedupTemperature
            );
        }

        return new MockChatLanguageModel();
    }

    private ChatLanguageModel createOpenAiChatModel(
            String modelName,
            Integer maxTokens,
            Double temperature
    ) {
        String effectiveModelName = hasText(modelName) ? modelName : openAiModelName;
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(openAiApiKey)
                .modelName(effectiveModelName)
                .temperature(temperature == null ? openAiTemperature : temperature);

        // GPT-5 系列在 chat/completions 下不支持 legacy 参数 max_tokens。
        // 这里对 GPT-5 跳过 maxTokens，避免 400 unsupported_parameter。
        if (supportsLegacyMaxTokens(effectiveModelName) && maxTokens != null) {
            builder.maxTokens(maxTokens);
        }

        if (hasText(openAiBaseUrl)) {
            builder.baseUrl(openAiBaseUrl);
        }

        return builder.build();
    }

    private ChatLanguageModel createDashScopeChatModel(
            String modelName,
            Integer maxTokens,
            Double temperature
    ) {
        return QwenChatModel.builder()
                .apiKey(dashScopeApiKey)
                .modelName(hasText(modelName) ? modelName : dashScopeModelName)
                .maxTokens(maxTokens == null ? dashScopeMaxTokens : maxTokens)
                .temperature((temperature == null ? dashScopeTemperature : temperature).floatValue())
                .build();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty() && !value.startsWith("#{");
    }

    private static boolean supportsLegacyMaxTokens(String modelName) {
        if (!hasText(modelName)) {
            return true;
        }
        return !modelName.trim().toLowerCase().startsWith("gpt-5");
    }
}
