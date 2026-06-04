package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import server.demo.dto.ChatMessageRequest;
import server.demo.dto.ChatMessageResponse;

@Service
public class ChatGptTranslationService implements AiTranslationService {
    private static final Logger logger = LoggerFactory.getLogger(ChatGptTranslationService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int MAX_TRANSLATION_ATTEMPTS = 3;
    private static final String TRANSLATION_TASK_TYPE = "TRANSLATION";
    private static final String SUCCESS_STATUS = "success";

    private final ChatService chatService;

    public ChatGptTranslationService(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public AiTranslationResult translate(String sourceText, RegistrationTargetLanguage targetLanguage) {
        if (sourceText == null || sourceText.isBlank()) {
            return AiTranslationResult.fallback(sourceText, normalizeTargetLanguage(targetLanguage), "EMPTY_SOURCE_TEXT");
        }

        RegistrationTargetLanguage safeTargetLanguage = normalizeTargetLanguage(targetLanguage);
        String lastError = null;
        for (int attempt = 1; attempt <= MAX_TRANSLATION_ATTEMPTS; attempt++) {
            try {
                String translatedText = translateOnce(sourceText, safeTargetLanguage);
                return AiTranslationResult.translated(translatedText, safeTargetLanguage);
            } catch (Exception ex) {
                lastError = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                logger.warn(
                        "Registration message translation attempt failed: attempt={}, targetLanguage={}, error={}",
                        attempt,
                        safeTargetLanguage.getCode(),
                        lastError
                );
            }
        }

        String errorMessage = lastError == null ? "TRANSLATION_FAILED" : lastError;
        return AiTranslationResult.fallback(sourceText, safeTargetLanguage, errorMessage);
    }

    private String translateOnce(String sourceText, RegistrationTargetLanguage targetLanguage) {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setTaskType(TRANSLATION_TASK_TYPE);
        request.setMessage(buildPrompt(sourceText, targetLanguage));

        ChatMessageResponse response = chatService.processMessage(request);
        if (response == null) {
            throw new RuntimeException("TRANSLATION_EMPTY_RESPONSE");
        }
        if (!SUCCESS_STATUS.equalsIgnoreCase(nullToEmpty(response.getStatus()))) {
            String error = response.getErrorMessage();
            throw new RuntimeException(error == null || error.isBlank() ? "TRANSLATION_ERROR_RESPONSE" : error);
        }

        String translated = extractTranslation(response.getReply());
        if (translated == null || translated.isBlank()) {
            throw new RuntimeException("TRANSLATION_BLANK_RESULT");
        }
        return translated.trim();
    }

    private static String buildPrompt(String sourceText, RegistrationTargetLanguage targetLanguage) {
        return "Translate the text between <<<TEXT>>> and <<<END>>> into "
                + targetLanguage.getName()
                + ". Preserve URLs, order numbers, dates, times, room numbers, names, currency values, and line breaks. "
                + "Return only valid JSON in the form {\"translation\":\"...\"}.\n"
                + "<<<TEXT>>>\n"
                + sourceText
                + "\n<<<END>>>";
    }

    private static String extractTranslation(String reply) {
        if (reply == null || reply.isBlank()) {
            throw new RuntimeException("TRANSLATION_REPLY_EMPTY");
        }

        String json = extractJsonObject(reply.trim());
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            JsonNode translationNode = root.get("translation");
            if (translationNode == null || translationNode.isNull()) {
                throw new RuntimeException("TRANSLATION_FIELD_MISSING");
            }
            return translationNode.asText();
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? "TRANSLATION_JSON_PARSE_FAILED" : ex.getMessage();
            throw new RuntimeException(message, ex);
        }
    }

    private static String extractJsonObject(String value) {
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new RuntimeException("TRANSLATION_JSON_NOT_FOUND");
        }
        return value.substring(start, end + 1);
    }

    private static RegistrationTargetLanguage normalizeTargetLanguage(RegistrationTargetLanguage targetLanguage) {
        if (targetLanguage == null) {
            return RegistrationTargetLanguage.defaultEnglish("MISSING_TARGET_LANGUAGE");
        }
        return targetLanguage;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
