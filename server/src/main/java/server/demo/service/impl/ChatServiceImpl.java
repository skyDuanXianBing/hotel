package server.demo.service.impl;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import server.demo.dto.ChatMessageRequest;
import server.demo.dto.ChatMessageResponse;
import server.demo.service.ChatService;

import java.util.UUID;

@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatServiceImpl.class);

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    private static final String SYSTEM_PROMPT = """
        你是 THE HOST HUB 的酒店智能客服助手。
        请遵循以下规则：
        1. 语气专业、礼貌、简洁，优先中文回复。
        2. 回答聚焦住宿服务、入住安排、设施说明、订单与账单相关问题。
        3. 无法确认的信息不要编造，明确说明并建议联系人工客服。
        4. 涉及紧急安全/医疗问题时，优先建议立即联系前台或当地紧急服务。
        """;

    @Override
    public ChatMessageResponse processMessage(ChatMessageRequest request) {
        long startTime = System.currentTimeMillis();
        String sessionId = request.getSessionId() != null ? request.getSessionId() : generateSessionId();

        try {
            logger.info("处理聊天消息: sessionId={}", sessionId);

            String fullPrompt = buildFullPrompt(request.getMessage());
            String aiReply = chatLanguageModel.generate(fullPrompt);

            long processingTime = System.currentTimeMillis() - startTime;
            ChatMessageResponse response = ChatMessageResponse.success(aiReply, sessionId);
            response.setProcessingTime(processingTime);
            return response;
        } catch (Exception e) {
            logger.error("处理聊天消息失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
            long processingTime = System.currentTimeMillis() - startTime;
            ChatMessageResponse errorResponse = ChatMessageResponse.error(
                    "抱歉，我暂时无法处理该请求，请稍后重试或联系人工客服。",
                    sessionId
            );
            errorResponse.setProcessingTime(processingTime);
            return errorResponse;
        }
    }

    @Override
    public ChatMessageResponse generateWelcomeMessage(String sessionId) {
        if (sessionId == null) {
            sessionId = generateSessionId();
        }

        String welcomeMessage = "您好，欢迎使用 THE HOST HUB 智能客服。" +
                "您可以咨询入住、设施、账单和订单相关问题，我会尽快为您处理。";

        return ChatMessageResponse.success(welcomeMessage, sessionId);
    }

    @Override
    public boolean isServiceAvailable() {
        try {
            String testResponse = chatLanguageModel.generate("你好");
            return testResponse != null && !testResponse.trim().isEmpty();
        } catch (Exception e) {
            logger.warn("AI 聊天服务不可用: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getServiceInfo() {
        return "LangChain4j AI聊天服务（默认 OpenAI GPT，兼容 DashScope 回退） - 乐迪酒店智能客服";
    }

    private String buildFullPrompt(String userMessage) {
        return SYSTEM_PROMPT + "\n\n用户问题: " + userMessage + "\n\n请基于规则给出回复。";
    }

    private String generateSessionId() {
        return "chat_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    public void setChatLanguageModel(ChatLanguageModel chatLanguageModel) {
        this.chatLanguageModel = chatLanguageModel;
    }
}
