package server.demo.service;

import org.junit.jupiter.api.Test;
import server.demo.dto.SuMessagingTranslationRequest;
import server.demo.dto.SuMessagingTranslationResponse;
import server.demo.entity.SuMessage;
import server.demo.entity.SuMessageThread;
import server.demo.entity.SuMessageTranslation;
import server.demo.repository.SuMessageRepository;
import server.demo.repository.SuMessageThreadRepository;
import server.demo.repository.SuMessageTranslationRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuMessagingTranslationServiceConcurrencyTest {

    @Test
    void getOrCreateTranslation_concurrentSameKey_translatesAndSavesOnlyOnce() throws Exception {
        SuMessageThreadRepository threadRepository = mock(SuMessageThreadRepository.class);
        SuMessageRepository messageRepository = mock(SuMessageRepository.class);
        SuMessageTranslationRepository translationRepository = mock(SuMessageTranslationRepository.class);
        AiTranslationService aiTranslationService = mock(AiTranslationService.class);

        SuMessagingTranslationService service = new SuMessagingTranslationService(
                threadRepository,
                messageRepository,
                translationRepository,
                aiTranslationService
        );

        Long storeId = 26L;
        SuMessageThread thread = new SuMessageThread();
        thread.setId(100L);
        SuMessage message = new SuMessage();
        message.setId(34177L);
        message.setContent("Hello guest");

        when(threadRepository.findByStoreIdAndId(storeId, 100L)).thenReturn(Optional.of(thread));
        when(messageRepository.findByStoreIdAndThreadIdAndId(storeId, 100L, 34177L))
                .thenReturn(Optional.of(message));

        AtomicReference<SuMessageTranslation> savedRow = new AtomicReference<>();
        when(translationRepository
                .findFirstByStoreIdAndMessage_IdAndTargetLanguageAndSourceContentHashAndTranslationStatus(
                        any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> Optional.ofNullable(savedRow.get()));
        when(translationRepository.save(any(SuMessageTranslation.class)))
                .thenAnswer(invocation -> {
                    SuMessageTranslation entity = invocation.getArgument(0);
                    savedRow.compareAndSet(null, entity);
                    return entity;
                });
        when(aiTranslationService.translate(any(), any())).thenAnswer(invocation -> {
            // Widen the race window so all threads cache-miss before the winner finishes.
            Thread.sleep(150);
            RegistrationTargetLanguage language = invocation.getArgument(1);
            return AiTranslationResult.translated("你好客人", language);
        });

        SuMessagingTranslationRequest request = new SuMessagingTranslationRequest();
        request.setTargetLanguage("zh-CN");

        int threadCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<SuMessagingTranslationResponse>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    return service.getOrCreateTranslation(storeId, 100L, 34177L, request);
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            for (Future<SuMessagingTranslationResponse> future : futures) {
                SuMessagingTranslationResponse response = future.get(10, TimeUnit.SECONDS);
                assertNotNull(response);
                assertEquals("你好客人", response.getTranslatedContent());
            }
        } finally {
            executor.shutdownNow();
        }

        verify(aiTranslationService, times(1)).translate(any(), any());
        verify(translationRepository, times(1)).save(any(SuMessageTranslation.class));
    }
}
