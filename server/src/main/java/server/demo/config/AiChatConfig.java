package server.demo.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import server.demo.service.impl.MockChatLanguageModel;

/**
 * AI聊天服务配置类
 * 配置聊天模型的接入参数
 * 
 * @author AI Assistant
 */
@Configuration
public class AiChatConfig {

    /**
     * DashScope API密钥，从环境变量读取
     */
    @Value("${dashscope.api-key:#{environment.DASH_SCOPE_API_KEY}}")
    private String dashScopeApiKey;

    /**
     * 使用的模型名称，默认为qwen-turbo
     */
    @Value("${dashscope.model-name:qwen-turbo}")
    private String modelName;

    /**
     * 最大令牌数
     */
    @Value("${dashscope.max-tokens:1000}")
    private Integer maxTokens;

    /**
     * 温度参数，控制生成文本的随机性
     */
    @Value("${dashscope.temperature:0.7}")
    private Double temperature;

    /**
     * 创建聊天模型Bean
     * 使用真实的千问模型实现
     * 
     * @return ChatLanguageModel 聊天语言模型实例
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        // 检查API密钥是否配置
        if (dashScopeApiKey != null && !dashScopeApiKey.trim().isEmpty() && 
            !dashScopeApiKey.startsWith("#{")) {
            return createDashScopeChatModel();
        }
        
        // 如果没有配置API密钥，使用Mock实现
        System.out.println("警告：未配置DashScope API密钥，使用Mock实现。请设置DASH_SCOPE_API_KEY环境变量或在application.properties中配置dashscope.api-key");
        return new MockChatLanguageModel();
    }

    /**
     * 创建DashScope聊天模型
     * 
     * @return ChatLanguageModel 真实的聊天模型
     */
    private ChatLanguageModel createDashScopeChatModel() {
        return QwenChatModel.builder()
                .apiKey(dashScopeApiKey)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .temperature(temperature.floatValue())
                .build();
    }
}