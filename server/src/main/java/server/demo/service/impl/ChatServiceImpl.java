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
            You are THE HOST HUB hotel assistant.
            Follow these rules strictly:
            1) Reply in the same language as the user's latest message. Do not default to Chinese.
            2) If the user mixes multiple languages, follow the dominant language of the latest message.
               If still unclear, follow the language used in the final sentence.
            3) Keep replies professional, polite, and concise.
            4) Focus on hotel topics: stay services, check-in/check-out, facilities, booking, billing.
            5) Do not invent facts. If uncertain, say so and suggest contacting human staff/front desk.
            6) For emergency/safety/medical topics, advise contacting front desk or local emergency services immediately.
            """;

    @Override
    public ChatMessageResponse processMessage(ChatMessageRequest request) {
        long startTime = System.currentTimeMillis();
        String sessionId = request.getSessionId() != null ? request.getSessionId() : generateSessionId();

        try {
            logger.info("Processing chat message: sessionId={}", sessionId);

            String fullPrompt = buildFullPrompt(request.getMessage());
            String aiReply = chatLanguageModel.generate(fullPrompt);

            long processingTime = System.currentTimeMillis() - startTime;
            ChatMessageResponse response = ChatMessageResponse.success(aiReply, sessionId);
            response.setProcessingTime(processingTime);
            return response;
        } catch (Exception e) {
            logger.error("Failed to process chat message: sessionId={}, error={}", sessionId, e.getMessage(), e);
            long processingTime = System.currentTimeMillis() - startTime;
            String fallbackMessage = buildLocalizedFallbackMessage(request.getMessage());
            ChatMessageResponse errorResponse = ChatMessageResponse.error(
                    fallbackMessage,
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

        String welcomeMessage = "Hello! I am THE HOST HUB assistant. " +
                "I can help with check-in/check-out, facilities, booking, and billing questions.";

        return ChatMessageResponse.success(welcomeMessage, sessionId);
    }

    @Override
    public boolean isServiceAvailable() {
        try {
            String testResponse = chatLanguageModel.generate("ping");
            return testResponse != null && !testResponse.trim().isEmpty();
        } catch (Exception e) {
            logger.warn("AI chat service unavailable: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getServiceInfo() {
        return "LangChain4j AI chat service (OpenAI primary, DashScope fallback)";
    }

    private static String buildLocalizedFallbackMessage(String userMessage) {
        String language = detectLanguageCode(userMessage);
        return switch (language) {
            case "zh" -> "抱歉，我暂时无法处理这个请求。请稍后再试，或联系人工客服。";
            case "ja" -> "申し訳ありません。現在このリクエストを処理できません。しばらくしてから再試行するか、スタッフにお問い合わせください。";
            case "ko" -> "죄송합니다. 지금은 요청을 처리할 수 없습니다. 잠시 후 다시 시도하시거나 직원에게 문의해 주세요.";
            case "ar" -> "عذرًا، لا يمكنني معالجة هذا الطلب الآن. يُرجى المحاولة لاحقًا أو التواصل مع موظفي الفندق.";
            case "ru" -> "Извините, сейчас я не могу обработать этот запрос. Попробуйте позже или свяжитесь с персоналом.";
            case "th" -> "ขออภัย ขณะนี้ไม่สามารถประมวลผลคำขอนี้ได้ กรุณาลองใหม่ภายหลังหรือติดต่อพนักงาน";
            default -> "Sorry, I cannot process this request right now. Please try again later or contact staff.";
        };
    }

    private static String detectLanguageCode(String text) {
        if (text == null || text.isBlank()) {
            return "en";
        }

        if (text.matches(".*[\\u3040-\\u30FF].*")) {
            return "ja";
        }
        if (text.matches(".*[\\uAC00-\\uD7AF].*")) {
            return "ko";
        }
        if (text.matches(".*[\\u0600-\\u06FF].*")) {
            return "ar";
        }
        if (text.matches(".*[\\u0400-\\u04FF].*")) {
            return "ru";
        }
        if (text.matches(".*[\\u0E00-\\u0E7F].*")) {
            return "th";
        }
        if (text.matches(".*[\\u4E00-\\u9FFF].*")) {
            return "zh";
        }
        return "en";
    }

    private String buildFullPrompt(String userMessage) {
        return SYSTEM_PROMPT + "\n\n" +
                "User message:\n" + userMessage + "\n\n" +
                "Output constraint: keep the same language and script as the user message unless the user asks for translation.";
    }

    private String generateSessionId() {
        return "chat_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    public void setChatLanguageModel(ChatLanguageModel chatLanguageModel) {
        this.chatLanguageModel = chatLanguageModel;
    }
}
