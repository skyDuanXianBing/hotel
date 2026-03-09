package server.demo.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import server.demo.service.impl.MockChatLanguageModel;

@Configuration
public class AiChatConfig {

    @Value("${openai.api-key:#{environment.OPENAI_API_KEY}}")
    private String openAiApiKey;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String openAiBaseUrl;

    @Value("${openai.model-name:gpt-4o-mini}")
    private String openAiModelName;

    @Value("${openai.max-tokens:1000}")
    private Integer openAiMaxTokens;

    @Value("${openai.temperature:0.7}")
    private Double openAiTemperature;

    @Value("${dashscope.api-key:#{environment.DASH_SCOPE_API_KEY}}")
    private String dashScopeApiKey;

    @Value("${dashscope.model-name:qwen-turbo}")
    private String dashScopeModelName;

    @Value("${dashscope.max-tokens:1000}")
    private Integer dashScopeMaxTokens;

    @Value("${dashscope.temperature:0.7}")
    private Double dashScopeTemperature;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        if (hasText(openAiApiKey)) {
            return createOpenAiChatModel();
        }

        if (hasText(dashScopeApiKey)) {
            return createDashScopeChatModel();
        }

        System.out.println("警告：未配置 OpenAI 或 DashScope API Key，使用 Mock AI 模型。请设置 OPENAI_API_KEY（推荐）或 DASH_SCOPE_API_KEY。");
        return new MockChatLanguageModel();
    }

    private ChatLanguageModel createOpenAiChatModel() {
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(openAiApiKey)
                .modelName(openAiModelName)
                .maxTokens(openAiMaxTokens)
                .temperature(openAiTemperature);

        if (hasText(openAiBaseUrl)) {
            builder.baseUrl(openAiBaseUrl);
        }

        return builder.build();
    }

    private ChatLanguageModel createDashScopeChatModel() {
        return QwenChatModel.builder()
                .apiKey(dashScopeApiKey)
                .modelName(dashScopeModelName)
                .maxTokens(dashScopeMaxTokens)
                .temperature(dashScopeTemperature.floatValue())
                .build();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty() && !value.startsWith("#{");
    }
}
