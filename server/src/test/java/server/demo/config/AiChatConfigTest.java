package server.demo.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.service.impl.MockChatLanguageModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiChatConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AiChatConfig.class)
            .withPropertyValues(
                    "openai.api-key=",
                    "dashscope.api-key="
            );

    @Test
    void chatMaxTokens_shouldDefaultTo4096WhenTokenPropertiesAreMissing() {
        contextRunner.run(context -> {
            AiChatConfig config = context.getBean(AiChatConfig.class);
            ChatLanguageModel chatLanguageModel = context.getBean(ChatLanguageModel.class);

            assertEquals(4096, ReflectionTestUtils.getField(config, "openAiMaxTokens"));
            assertEquals(4096, ReflectionTestUtils.getField(config, "dashScopeMaxTokens"));
            assertTrue(chatLanguageModel instanceof MockChatLanguageModel);
        });
    }

    @Test
    void chatMaxTokens_shouldUseConfiguredOverrides() {
        contextRunner
                .withPropertyValues(
                        "openai.max-tokens=2048",
                        "dashscope.max-tokens=3072"
                )
                .run(context -> {
                    AiChatConfig config = context.getBean(AiChatConfig.class);

                    assertEquals(2048, ReflectionTestUtils.getField(config, "openAiMaxTokens"));
                    assertEquals(3072, ReflectionTestUtils.getField(config, "dashScopeMaxTokens"));
                });
    }
}
