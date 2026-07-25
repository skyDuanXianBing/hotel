package server.demo.service;

/**
 * 独立站页面 schema 生成专用 AI 客户端。
 * 与全局共享的 ChatLanguageModel 解耦，便于独立指定模型与推理强度。
 */
public interface IndependentSitePageSchemaAiClient {

    /**
     * 是否已完成配置（API key 就绪）。未配置时生成服务必须 fail-closed。
     */
    boolean isConfigured();

    /**
     * 发送单条用户消息并返回模型的文本输出。
     * 网络或上游错误统一映射为 IndependentSiteServiceException。
     */
    String complete(String userMessage);
}
