package server.demo.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import server.demo.dto.ChatMessageRequest;
import server.demo.dto.ChatMessageResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatGptTranslationServiceTest {

    @Test
    void translate_shouldRetryUntilThirdAttemptAndExtractJsonTranslation() {
        ChatService chatService = mock(ChatService.class);
        ChatGptTranslationService service = new ChatGptTranslationService(chatService);
        RegistrationTargetLanguage targetLanguage =
                RegistrationTargetLanguage.resolved("ja", "Japanese", "Japan");

        when(chatService.processMessage(any(ChatMessageRequest.class)))
                .thenReturn(
                        ChatMessageResponse.error("temporary outage", null),
                        ChatMessageResponse.error("temporary outage", null),
                        ChatMessageResponse.success("```json\n{\"translation\":\"こんにちは\"}\n```", null)
                );

        AiTranslationResult result = service.translate("Hello guest", targetLanguage);

        assertTrue(result.isTranslated());
        assertEquals("こんにちは", result.getTranslatedText());
        assertEquals("ja", result.getTargetLanguage().getCode());
        assertEquals("Japanese", result.getTargetLanguage().getName());

        ArgumentCaptor<ChatMessageRequest> requestCaptor =
                ArgumentCaptor.forClass(ChatMessageRequest.class);
        verify(chatService, times(3)).processMessage(requestCaptor.capture());
        List<ChatMessageRequest> requests = requestCaptor.getAllValues();
        for (ChatMessageRequest request : requests) {
            assertEquals("TRANSLATION", request.getTaskType());
            assertTrue(request.getMessage().contains("Japanese"));
            assertTrue(request.getMessage().contains("<<<TEXT>>>\nHello guest\n<<<END>>>"));
            assertTrue(request.getMessage().contains("{\"translation\":\"...\"}"));
        }
    }

    @Test
    void translate_shouldFallbackToOriginalAfterThreeFailedAttempts() {
        ChatService chatService = mock(ChatService.class);
        ChatGptTranslationService service = new ChatGptTranslationService(chatService);
        RegistrationTargetLanguage targetLanguage =
                RegistrationTargetLanguage.resolved("ko", "Korean", "Korea");

        when(chatService.processMessage(any(ChatMessageRequest.class)))
                .thenReturn(ChatMessageResponse.error("model unavailable", null));

        AiTranslationResult result = service.translate("Please upload passport", targetLanguage);

        assertFalse(result.isTranslated());
        assertEquals("Please upload passport", result.getTranslatedText());
        assertEquals("ko", result.getTargetLanguage().getCode());
        assertEquals("model unavailable", result.getErrorMessage());
        verify(chatService, times(3)).processMessage(any(ChatMessageRequest.class));
    }

    @Test
    void translate_shouldFallbackWithoutCallingChatServiceWhenSourceIsBlank() {
        ChatService chatService = mock(ChatService.class);
        ChatGptTranslationService service = new ChatGptTranslationService(chatService);

        AiTranslationResult result = service.translate("   ", null);

        assertFalse(result.isTranslated());
        assertEquals("   ", result.getTranslatedText());
        assertEquals("en", result.getTargetLanguage().getCode());
        assertEquals("EMPTY_SOURCE_TEXT", result.getErrorMessage());
        verify(chatService, never()).processMessage(any(ChatMessageRequest.class));
    }
}
