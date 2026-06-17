package server.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.entity.MessageKnowledgeEvidence;
import server.demo.entity.MessageKnowledgeItem;
import server.demo.entity.SuMessage;
import server.demo.entity.SuMessageThread;
import server.demo.enums.SuMessagingSenderType;
import server.demo.repository.MessageKnowledgeEvidenceRepository;
import server.demo.repository.MessageKnowledgeItemRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MessageKnowledgeRefinementServiceTest {

    @Test
    void refineSourcePair_shouldCreateScopedItemAndEvidenceForReusableRoomTypeFact() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEvidenceRepository evidenceRepository =
                Mockito.mock(MessageKnowledgeEvidenceRepository.class);
        SuMessagingThreadContextResolver contextResolver =
                Mockito.mock(SuMessagingThreadContextResolver.class);
        MessageKnowledgeRefinementService service = newService(
                itemRepository,
                evidenceRepository,
                contextResolver
        );

        SuMessageThread thread = newThread(77L, 26L);
        SuMessage guestMessage = newMessage(
                101L,
                thread,
                SuMessagingSenderType.GUEST,
                "What is the wifi password?",
                "SENT"
        );
        SuMessage staffMessage = newMessage(
                102L,
                thread,
                SuMessagingSenderType.STAFF,
                "The wifi password is sakura2026.",
                "SENT"
        );
        when(evidenceRepository.findByStoreIdAndSourceGuestMessageIdAndSourceStaffMessageId(26L, 101L, 102L))
                .thenReturn(Optional.empty());
        when(contextResolver.resolveForIndex(26L, thread)).thenReturn(roomTypeContext());
        when(itemRepository.findByStoreIdAndScopeKeyAndTopicHash(eq(26L), eq("ROOM_TYPE:3"), anyString()))
                .thenReturn(Optional.empty());
        when(itemRepository.save(any(MessageKnowledgeItem.class))).thenAnswer(invocation -> {
            MessageKnowledgeItem item = invocation.getArgument(0);
            if (item.getId() == null) {
                item.setId(501L);
            }
            return item;
        });
        when(evidenceRepository.save(any(MessageKnowledgeEvidence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean refined = service.refineSourcePair(26L, thread, guestMessage, staffMessage);

        assertTrue(refined);
        ArgumentCaptor<MessageKnowledgeItem> itemCaptor = ArgumentCaptor.forClass(MessageKnowledgeItem.class);
        ArgumentCaptor<MessageKnowledgeEvidence> evidenceCaptor =
                ArgumentCaptor.forClass(MessageKnowledgeEvidence.class);
        verify(itemRepository, Mockito.times(2)).save(itemCaptor.capture());
        verify(evidenceRepository).save(evidenceCaptor.capture());

        MessageKnowledgeItem savedItem = itemCaptor.getAllValues().get(1);
        assertEquals(26L, savedItem.getStoreId());
        assertEquals(SuMessagingThreadContext.SCOPE_ROOM_TYPE, savedItem.getScopeType());
        assertEquals(3L, savedItem.getScopeId());
        assertEquals("ROOM_TYPE:3", savedItem.getScopeKey());
        assertEquals("wifi", savedItem.getTopic());
        assertEquals("The wifi password is sakura2026.", savedItem.getAnswer());
        assertEquals(MessageKnowledgeItem.STATUS_ACTIVE, savedItem.getStatus());
        assertEquals(MessageKnowledgeItem.REDACTION_STATUS_REDACTED, savedItem.getPiiRedactionStatus());
        assertEquals(MessageKnowledgeItem.EMBEDDING_STATUS_PENDING, savedItem.getEmbeddingStatus());
        assertTrue(savedItem.getSemanticText().contains("Topic: wifi"));
        assertTrue(savedItem.getSemanticText().contains("Scope: ROOM_TYPE Deluxe"));
        assertTrue(savedItem.getSemanticText().contains("Canonical fact: The wifi password is sakura2026."));
        assertTrue(savedItem.getSearchIntentsJson().contains("\"topic\":\"wifi\""));
        assertNull(savedItem.getEmbeddingVector());
        assertEquals(1, savedItem.getEvidenceCount());
        assertEquals(BigDecimal.valueOf(0.7200).setScale(4), savedItem.getConfidence());
        assertNotNull(savedItem.getRefinedAt());

        MessageKnowledgeEvidence savedEvidence = evidenceCaptor.getValue();
        assertEquals(26L, savedEvidence.getStoreId());
        assertEquals(501L, savedEvidence.getItem().getId());
        assertEquals(101L, savedEvidence.getSourceGuestMessageId());
        assertEquals(102L, savedEvidence.getSourceStaffMessageId());
        assertEquals(MessageKnowledgeEvidence.STATUS_ACTIVE, savedEvidence.getStatus());
        assertEquals("en", savedEvidence.getLanguage());
        assertEquals("Deluxe", savedEvidence.getRoomTypeName());
    }

    @Test
    void refineSourcePair_shouldFallbackToStoreScopeWhenRoomContextIsUnresolved() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEvidenceRepository evidenceRepository =
                Mockito.mock(MessageKnowledgeEvidenceRepository.class);
        SuMessagingThreadContextResolver contextResolver =
                Mockito.mock(SuMessagingThreadContextResolver.class);
        MessageKnowledgeRefinementService service = newService(
                itemRepository,
                evidenceRepository,
                contextResolver
        );

        SuMessageThread thread = newThread(77L, 26L);
        SuMessage guestMessage = newMessage(
                111L,
                thread,
                SuMessagingSenderType.GUEST,
                "What is the wifi password?",
                "SENT"
        );
        SuMessage staffMessage = newMessage(
                112L,
                thread,
                SuMessagingSenderType.STAFF,
                "The wifi password is sakura2026.",
                "SENT"
        );
        when(evidenceRepository.findByStoreIdAndSourceGuestMessageIdAndSourceStaffMessageId(26L, 111L, 112L))
                .thenReturn(Optional.empty());
        when(contextResolver.resolveForIndex(26L, thread)).thenReturn(storeFallbackContext());
        when(itemRepository.findByStoreIdAndScopeKeyAndTopicHash(eq(26L), eq("STORE"), anyString()))
                .thenReturn(Optional.empty());
        when(itemRepository.save(any(MessageKnowledgeItem.class))).thenAnswer(invocation -> {
            MessageKnowledgeItem item = invocation.getArgument(0);
            if (item.getId() == null) {
                item.setId(701L);
            }
            return item;
        });
        when(evidenceRepository.save(any(MessageKnowledgeEvidence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean refined = service.refineSourcePair(26L, thread, guestMessage, staffMessage);

        assertTrue(refined);
        ArgumentCaptor<MessageKnowledgeItem> itemCaptor = ArgumentCaptor.forClass(MessageKnowledgeItem.class);
        ArgumentCaptor<MessageKnowledgeEvidence> evidenceCaptor =
                ArgumentCaptor.forClass(MessageKnowledgeEvidence.class);
        verify(itemRepository, Mockito.times(2)).save(itemCaptor.capture());
        verify(evidenceRepository).save(evidenceCaptor.capture());

        MessageKnowledgeItem savedItem = itemCaptor.getAllValues().get(1);
        assertEquals(26L, savedItem.getStoreId());
        assertEquals(SuMessagingThreadContext.SCOPE_STORE, savedItem.getScopeType());
        assertNull(savedItem.getScopeId());
        assertEquals("STORE", savedItem.getScopeKey());
        assertNull(savedItem.getRoomId());
        assertNull(savedItem.getRoomTypeId());
        assertEquals("wifi", savedItem.getTopic());
        assertEquals(MessageKnowledgeItem.STATUS_ACTIVE, savedItem.getStatus());
        assertEquals(MessageKnowledgeItem.EMBEDDING_STATUS_PENDING, savedItem.getEmbeddingStatus());
        assertTrue(savedItem.getSemanticText().contains("Scope: STORE"));
        assertEquals(BigDecimal.valueOf(0.6200).setScale(4), savedItem.getConfidence());

        MessageKnowledgeEvidence savedEvidence = evidenceCaptor.getValue();
        assertEquals(26L, savedEvidence.getStoreId());
        assertEquals(SuMessagingThreadContext.SCOPE_STORE, savedEvidence.getScopeType());
        assertEquals("STORE", savedEvidence.getScopeKey());
        assertNull(savedEvidence.getReservationId());
        assertEquals("SIM-BOOKING-77", savedEvidence.getBookingKey());
        assertEquals(MessageKnowledgeEvidence.STATUS_ACTIVE, savedEvidence.getStatus());
    }

    @Test
    void refineSourcePair_shouldSkipExistingSourcePairWithoutResolvingContext() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEvidenceRepository evidenceRepository =
                Mockito.mock(MessageKnowledgeEvidenceRepository.class);
        SuMessagingThreadContextResolver contextResolver =
                Mockito.mock(SuMessagingThreadContextResolver.class);
        MessageKnowledgeRefinementService service = newService(
                itemRepository,
                evidenceRepository,
                contextResolver
        );
        SuMessageThread thread = newThread(77L, 26L);
        SuMessage guestMessage = newMessage(101L, thread, SuMessagingSenderType.GUEST, "What is wifi?", "SENT");
        SuMessage staffMessage = newMessage(102L, thread, SuMessagingSenderType.STAFF, "Wifi is sakura2026.", "SENT");

        when(evidenceRepository.findByStoreIdAndSourceGuestMessageIdAndSourceStaffMessageId(26L, 101L, 102L))
                .thenReturn(Optional.of(new MessageKnowledgeEvidence()));

        boolean refined = service.refineSourcePair(26L, thread, guestMessage, staffMessage);

        assertFalse(refined);
        verifyNoInteractions(contextResolver);
        verify(itemRepository, never()).save(any());
        verify(evidenceRepository, never()).save(any());
    }

    @Test
    void refineSourcePair_shouldRejectMismatchedStoreBeforeWritingEvidence() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEvidenceRepository evidenceRepository =
                Mockito.mock(MessageKnowledgeEvidenceRepository.class);
        SuMessagingThreadContextResolver contextResolver =
                Mockito.mock(SuMessagingThreadContextResolver.class);
        MessageKnowledgeRefinementService service = newService(
                itemRepository,
                evidenceRepository,
                contextResolver
        );
        SuMessageThread thread = newThread(77L, 26L);
        SuMessage guestMessage = newMessage(101L, thread, SuMessagingSenderType.GUEST, "What is wifi?", "SENT");
        guestMessage.setStoreId(99L);
        SuMessage staffMessage = newMessage(102L, thread, SuMessagingSenderType.STAFF, "Wifi is sakura2026.", "SENT");

        boolean refined = service.refineSourcePair(26L, thread, guestMessage, staffMessage);

        assertFalse(refined);
        verifyNoInteractions(itemRepository, evidenceRepository, contextResolver);
    }

    @Test
    void refineSourcePair_shouldRejectFailedAndSendingStaffReplies() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEvidenceRepository evidenceRepository =
                Mockito.mock(MessageKnowledgeEvidenceRepository.class);
        SuMessagingThreadContextResolver contextResolver =
                Mockito.mock(SuMessagingThreadContextResolver.class);
        MessageKnowledgeRefinementService service = newService(
                itemRepository,
                evidenceRepository,
                contextResolver
        );
        SuMessageThread thread = newThread(77L, 26L);
        SuMessage guestMessage = newMessage(101L, thread, SuMessagingSenderType.GUEST, "What is wifi?", "SENT");

        String[] deliveryStatuses = {"FAILED", "SENDING"};
        for (String deliveryStatus : deliveryStatuses) {
            SuMessage staffMessage = newMessage(
                    102L,
                    thread,
                    SuMessagingSenderType.STAFF,
                    "Wifi is sakura2026.",
                    deliveryStatus
            );

            boolean refined = service.refineSourcePair(26L, thread, guestMessage, staffMessage);

            assertFalse(refined);
        }

        verifyNoInteractions(itemRepository, evidenceRepository, contextResolver);
    }

    @Test
    void refineSourcePair_shouldFilterRedactedPiiAndPrivateBookingContent() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEvidenceRepository evidenceRepository =
                Mockito.mock(MessageKnowledgeEvidenceRepository.class);
        SuMessagingThreadContextResolver contextResolver =
                Mockito.mock(SuMessagingThreadContextResolver.class);
        MessageKnowledgeRefinementService service = newService(
                itemRepository,
                evidenceRepository,
                contextResolver
        );
        SuMessageThread thread = newThread(77L, 26L);
        SuMessage phoneQuestion = newMessage(
                101L,
                thread,
                SuMessagingSenderType.GUEST,
                "My phone is 13800138000. What is the wifi password?",
                "SENT"
        );
        SuMessage phoneAnswer = newMessage(
                102L,
                thread,
                SuMessagingSenderType.STAFF,
                "The wifi password is sakura2026.",
                "SENT"
        );
        SuMessage bookingQuestion = newMessage(
                103L,
                thread,
                SuMessagingSenderType.GUEST,
                "For booking number ABC123, can I check in early?",
                "SENT"
        );
        SuMessage bookingAnswer = newMessage(
                104L,
                thread,
                SuMessagingSenderType.STAFF,
                "Please check in after 15:00.",
                "SENT"
        );

        when(evidenceRepository.findByStoreIdAndSourceGuestMessageIdAndSourceStaffMessageId(26L, 101L, 102L))
                .thenReturn(Optional.empty());
        when(evidenceRepository.findByStoreIdAndSourceGuestMessageIdAndSourceStaffMessageId(26L, 103L, 104L))
                .thenReturn(Optional.empty());
        when(contextResolver.resolveForIndex(26L, thread)).thenReturn(roomTypeContext());

        boolean phoneRefined = service.refineSourcePair(26L, thread, phoneQuestion, phoneAnswer);
        boolean bookingRefined = service.refineSourcePair(26L, thread, bookingQuestion, bookingAnswer);

        assertFalse(phoneRefined);
        assertFalse(bookingRefined);
        verify(itemRepository, never()).save(any());
        verify(evidenceRepository, never()).save(any());
    }

    @Test
    void refineSourcePair_shouldKeepRejectingPrivateBookingContentOnStoreFallback() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEvidenceRepository evidenceRepository =
                Mockito.mock(MessageKnowledgeEvidenceRepository.class);
        SuMessagingThreadContextResolver contextResolver =
                Mockito.mock(SuMessagingThreadContextResolver.class);
        MessageKnowledgeRefinementService service = newService(
                itemRepository,
                evidenceRepository,
                contextResolver
        );
        SuMessageThread thread = newThread(77L, 26L);
        SuMessage guestMessage = newMessage(
                121L,
                thread,
                SuMessagingSenderType.GUEST,
                "For booking number ABC123, can I check in early?",
                "SENT"
        );
        SuMessage staffMessage = newMessage(
                122L,
                thread,
                SuMessagingSenderType.STAFF,
                "Please check in after 15:00.",
                "SENT"
        );

        when(evidenceRepository.findByStoreIdAndSourceGuestMessageIdAndSourceStaffMessageId(26L, 121L, 122L))
                .thenReturn(Optional.empty());
        when(contextResolver.resolveForIndex(26L, thread)).thenReturn(storeFallbackContext());

        boolean refined = service.refineSourcePair(26L, thread, guestMessage, staffMessage);

        assertFalse(refined);
        verify(itemRepository, never()).save(any());
        verify(evidenceRepository, never()).save(any());
    }

    @Test
    void refineSourcePair_shouldUseClassifierExistingTopicWhenProvided() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEvidenceRepository evidenceRepository =
                Mockito.mock(MessageKnowledgeEvidenceRepository.class);
        SuMessagingThreadContextResolver contextResolver =
                Mockito.mock(SuMessagingThreadContextResolver.class);
        MessageKnowledgeTopicClassifierService classifierService =
                Mockito.mock(MessageKnowledgeTopicClassifierService.class);
        MessageKnowledgeRefinementService service = newService(
                itemRepository,
                evidenceRepository,
                contextResolver
        );
        service.setTopicClassifierService(classifierService);

        SuMessageThread thread = newThread(77L, 26L);
        SuMessage guestMessage = newMessage(
                131L,
                thread,
                SuMessagingSenderType.GUEST,
                "Do you provide extra towels?",
                "SENT"
        );
        SuMessage staffMessage = newMessage(
                132L,
                thread,
                SuMessagingSenderType.STAFF,
                "Extra towels are in the hallway cabinet.",
                "SENT"
        );
        when(evidenceRepository.findByStoreIdAndSourceGuestMessageIdAndSourceStaffMessageId(26L, 131L, 132L))
                .thenReturn(Optional.empty());
        when(contextResolver.resolveForIndex(26L, thread)).thenReturn(roomTypeContext());
        when(classifierService.classify(eq(26L), anyString(), anyString()))
                .thenReturn(MessageKnowledgeTopicClassifierService.TopicClassification.useExisting(
                        "amenity",
                        BigDecimal.valueOf(0.9100).setScale(4),
                        "classifier matched amenity"
                ));
        when(itemRepository.findByStoreIdAndScopeKeyAndTopicHash(eq(26L), eq("ROOM_TYPE:3"), anyString()))
                .thenReturn(Optional.empty());
        when(itemRepository.save(any(MessageKnowledgeItem.class))).thenAnswer(invocation -> {
            MessageKnowledgeItem item = invocation.getArgument(0);
            if (item.getId() == null) {
                item.setId(801L);
            }
            return item;
        });
        when(evidenceRepository.save(any(MessageKnowledgeEvidence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean refined = service.refineSourcePair(26L, thread, guestMessage, staffMessage);

        assertTrue(refined);
        ArgumentCaptor<MessageKnowledgeItem> itemCaptor = ArgumentCaptor.forClass(MessageKnowledgeItem.class);
        verify(itemRepository, Mockito.times(2)).save(itemCaptor.capture());
        MessageKnowledgeItem savedItem = itemCaptor.getAllValues().get(1);
        assertEquals("amenity", savedItem.getTopic());
        assertEquals(BigDecimal.valueOf(0.9100).setScale(4), savedItem.getConfidence());
        assertTrue(savedItem.getSearchIntentsJson().contains("\"topic\":\"amenity\""));
    }

    @Test
    void refineSourcePair_shouldSkipReusableKnowledgeWhenClassifierRequestsReview() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEvidenceRepository evidenceRepository =
                Mockito.mock(MessageKnowledgeEvidenceRepository.class);
        SuMessagingThreadContextResolver contextResolver =
                Mockito.mock(SuMessagingThreadContextResolver.class);
        MessageKnowledgeTopicClassifierService classifierService =
                Mockito.mock(MessageKnowledgeTopicClassifierService.class);
        MessageKnowledgeRefinementService service = newService(
                itemRepository,
                evidenceRepository,
                contextResolver
        );
        service.setTopicClassifierService(classifierService);

        SuMessageThread thread = newThread(77L, 26L);
        SuMessage guestMessage = newMessage(
                141L,
                thread,
                SuMessagingSenderType.GUEST,
                "What are the pool hours?",
                "SENT"
        );
        SuMessage staffMessage = newMessage(
                142L,
                thread,
                SuMessagingSenderType.STAFF,
                "The pool is open from 09:00 to 20:00.",
                "SENT"
        );
        when(evidenceRepository.findByStoreIdAndSourceGuestMessageIdAndSourceStaffMessageId(26L, 141L, 142L))
                .thenReturn(Optional.empty());
        when(contextResolver.resolveForIndex(26L, thread)).thenReturn(roomTypeContext());
        when(classifierService.classify(eq(26L), anyString(), anyString()))
                .thenReturn(MessageKnowledgeTopicClassifierService.TopicClassification.needsReview(
                        MessageKnowledgeTopicClassifierService.TOPIC_GENERAL_CANDIDATE,
                        BigDecimal.valueOf(0.9500).setScale(4),
                        "create new requires review"
                ));

        boolean refined = service.refineSourcePair(26L, thread, guestMessage, staffMessage);

        assertFalse(refined);
        verify(itemRepository, never()).save(any());
        verify(evidenceRepository, never()).save(any());
    }

    @Test
    void refineSourcePair_shouldUseFallbackTopicWhenClassifierFallsBack() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEvidenceRepository evidenceRepository =
                Mockito.mock(MessageKnowledgeEvidenceRepository.class);
        SuMessagingThreadContextResolver contextResolver =
                Mockito.mock(SuMessagingThreadContextResolver.class);
        MessageKnowledgeTopicClassifierService classifierService =
                Mockito.mock(MessageKnowledgeTopicClassifierService.class);
        MessageKnowledgeRefinementService service = newService(
                itemRepository,
                evidenceRepository,
                contextResolver
        );
        service.setTopicClassifierService(classifierService);

        SuMessageThread thread = newThread(77L, 26L);
        SuMessage guestMessage = newMessage(151L, thread, SuMessagingSenderType.GUEST, "What is the wifi?", "SENT");
        SuMessage staffMessage = newMessage(152L, thread, SuMessagingSenderType.STAFF, "WiFi password is sakura.", "SENT");

        when(evidenceRepository.findByStoreIdAndSourceGuestMessageIdAndSourceStaffMessageId(26L, 151L, 152L))
                .thenReturn(Optional.empty());
        when(contextResolver.resolveForIndex(26L, thread)).thenReturn(roomTypeContext());
        when(classifierService.classify(eq(26L), anyString(), anyString()))
                .thenReturn(MessageKnowledgeTopicClassifierService.TopicClassification.fallback("disabled"));
        when(itemRepository.findByStoreIdAndScopeKeyAndTopicHash(eq(26L), eq("ROOM_TYPE:3"), anyString()))
                .thenReturn(Optional.empty());
        when(itemRepository.save(any(MessageKnowledgeItem.class))).thenAnswer(invocation -> {
            MessageKnowledgeItem item = invocation.getArgument(0);
            if (item.getId() == null) {
                item.setId(811L);
            }
            return item;
        });
        when(evidenceRepository.save(any(MessageKnowledgeEvidence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean refined = service.refineSourcePair(26L, thread, guestMessage, staffMessage);

        assertTrue(refined);
        ArgumentCaptor<MessageKnowledgeItem> itemCaptor = ArgumentCaptor.forClass(MessageKnowledgeItem.class);
        verify(itemRepository, Mockito.times(2)).save(itemCaptor.capture());
        assertEquals("wifi", itemCaptor.getAllValues().get(1).getTopic());
    }

    @Test
    void refineSourcePair_shouldSkipTemporaryCodeBeforeTopicClassifier() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEvidenceRepository evidenceRepository =
                Mockito.mock(MessageKnowledgeEvidenceRepository.class);
        SuMessagingThreadContextResolver contextResolver =
                Mockito.mock(SuMessagingThreadContextResolver.class);
        MessageKnowledgeTopicClassifierService classifierService =
                Mockito.mock(MessageKnowledgeTopicClassifierService.class);
        MessageKnowledgeRefinementService service = newService(
                itemRepository,
                evidenceRepository,
                contextResolver
        );
        service.setTopicClassifierService(classifierService);

        SuMessageThread thread = newThread(77L, 26L);
        SuMessage guestMessage = newMessage(
                161L,
                thread,
                SuMessagingSenderType.GUEST,
                "What is my temporary code?",
                "SENT"
        );
        SuMessage staffMessage = newMessage(
                162L,
                thread,
                SuMessagingSenderType.STAFF,
                "Your temporary code is 123456.",
                "SENT"
        );
        when(evidenceRepository.findByStoreIdAndSourceGuestMessageIdAndSourceStaffMessageId(26L, 161L, 162L))
                .thenReturn(Optional.empty());
        when(contextResolver.resolveForIndex(26L, thread)).thenReturn(roomTypeContext());

        boolean refined = service.refineSourcePair(26L, thread, guestMessage, staffMessage);

        assertFalse(refined);
        verifyNoInteractions(classifierService);
        verify(itemRepository, never()).save(any());
        verify(evidenceRepository, never()).save(any());
    }

    @Test
    void refineSourcePair_shouldOverwriteExistingCanonicalAnswerWhenLatestWifiAnswerChanges() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEvidenceRepository evidenceRepository =
                Mockito.mock(MessageKnowledgeEvidenceRepository.class);
        SuMessagingThreadContextResolver contextResolver =
                Mockito.mock(SuMessagingThreadContextResolver.class);
        MessageKnowledgeRefinementService service = newService(
                itemRepository,
                evidenceRepository,
                contextResolver
        );

        SuMessageThread thread = newThread(77L, 26L);
        SuMessage guestMessage = newMessage(101L, thread, SuMessagingSenderType.GUEST, "What is the wifi password?", "SENT");
        SuMessage staffMessage = newMessage(
                102L,
                thread,
                SuMessagingSenderType.STAFF,
                "The wifi password is momiji2026.",
                "SENT"
        );
        MessageKnowledgeItem existingItem = newExistingWifiItem();
        existingItem.setLastSeenAt(LocalDateTime.of(2026, 6, 15, 10, 30));

        when(evidenceRepository.findByStoreIdAndSourceGuestMessageIdAndSourceStaffMessageId(26L, 101L, 102L))
                .thenReturn(Optional.empty());
        when(contextResolver.resolveForIndex(26L, thread)).thenReturn(roomTypeContext());
        when(itemRepository.findByStoreIdAndScopeKeyAndTopicHash(eq(26L), eq("ROOM_TYPE:3"), anyString()))
                .thenReturn(Optional.of(existingItem));
        when(itemRepository.save(any(MessageKnowledgeItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(evidenceRepository.save(any(MessageKnowledgeEvidence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean refined = service.refineSourcePair(26L, thread, guestMessage, staffMessage);

        assertTrue(refined);
        assertEquals(MessageKnowledgeItem.STATUS_ACTIVE, existingItem.getStatus());
        assertEquals("The wifi password is momiji2026.", existingItem.getAnswer());
        assertEquals("the wifi password is momiji2026", existingItem.getNormalizedAnswer());
        assertEquals(staffMessage.getSentAt(), existingItem.getLastSeenAt());
        assertEquals(3, existingItem.getEvidenceCount());

        ArgumentCaptor<MessageKnowledgeEvidence> evidenceCaptor =
                ArgumentCaptor.forClass(MessageKnowledgeEvidence.class);
        verify(evidenceRepository).save(evidenceCaptor.capture());
        assertEquals(MessageKnowledgeEvidence.STATUS_ACTIVE, evidenceCaptor.getValue().getStatus());
        assertEquals("The wifi password is momiji2026.", evidenceCaptor.getValue().getAnswer());
    }

    @Test
    void refineSourcePair_shouldNotOverwriteCanonicalFieldsWhenOlderReplyFinishesLater() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEvidenceRepository evidenceRepository =
                Mockito.mock(MessageKnowledgeEvidenceRepository.class);
        SuMessagingThreadContextResolver contextResolver =
                Mockito.mock(SuMessagingThreadContextResolver.class);
        MessageKnowledgeRefinementService service = newService(
                itemRepository,
                evidenceRepository,
                contextResolver
        );

        LocalDateTime olderSourceSeenAt = LocalDateTime.of(2026, 6, 15, 10, 0);
        LocalDateTime newerCanonicalSeenAt = LocalDateTime.of(2026, 6, 15, 11, 0);
        LocalDateTime originalRefinedAt = LocalDateTime.of(2026, 6, 15, 11, 5);
        SuMessageThread thread = newThread(77L, 26L);
        SuMessage guestMessage = newMessage(
                301L,
                thread,
                SuMessagingSenderType.GUEST,
                "What is the wifi password?",
                "SENT"
        );
        SuMessage staffMessage = newMessage(
                302L,
                thread,
                SuMessagingSenderType.STAFF,
                "The wifi password is sakura2026.",
                "SENT"
        );
        staffMessage.setSentAt(olderSourceSeenAt);
        MessageKnowledgeItem existingItem = newExistingWifiItemWithAnswer(
                "The wifi password is momiji2026.",
                "the wifi password is momiji2026"
        );
        existingItem.setEvidenceCount(1);
        existingItem.setLastSeenAt(newerCanonicalSeenAt);
        existingItem.setRefinedAt(originalRefinedAt);
        prepareReadyEmbedding(existingItem);

        when(evidenceRepository.findByStoreIdAndSourceGuestMessageIdAndSourceStaffMessageId(26L, 301L, 302L))
                .thenReturn(Optional.empty());
        when(contextResolver.resolveForIndex(26L, thread)).thenReturn(roomTypeContext());
        when(itemRepository.findByStoreIdAndScopeKeyAndTopicHash(eq(26L), eq("ROOM_TYPE:3"), anyString()))
                .thenReturn(Optional.of(existingItem));
        when(itemRepository.save(any(MessageKnowledgeItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(evidenceRepository.save(any(MessageKnowledgeEvidence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean refined = service.refineSourcePair(26L, thread, guestMessage, staffMessage);

        assertTrue(refined);
        assertEquals(MessageKnowledgeItem.STATUS_ACTIVE, existingItem.getStatus());
        assertEquals("The wifi password is momiji2026.", existingItem.getAnswer());
        assertEquals("the wifi password is momiji2026", existingItem.getNormalizedAnswer());
        assertEquals(newerCanonicalSeenAt, existingItem.getLastSeenAt());
        assertEquals(originalRefinedAt, existingItem.getRefinedAt());
        assertEquals(2, existingItem.getEvidenceCount());
        assertEquals(MessageKnowledgeItem.EMBEDDING_STATUS_READY, existingItem.getEmbeddingStatus());
        assertEquals("[0.1,0.2]", existingItem.getEmbeddingVector());
        assertTrue(existingItem.getSemanticText().contains("momiji2026"));
        assertFalse(existingItem.getSemanticText().contains("sakura2026"));
        assertTrue(existingItem.getSearchIntentsJson().contains("momiji2026"));
        assertFalse(existingItem.getSearchIntentsJson().contains("sakura2026"));

        ArgumentCaptor<MessageKnowledgeEvidence> evidenceCaptor =
                ArgumentCaptor.forClass(MessageKnowledgeEvidence.class);
        verify(evidenceRepository).save(evidenceCaptor.capture());
        assertEquals(MessageKnowledgeEvidence.STATUS_ACTIVE, evidenceCaptor.getValue().getStatus());
        assertEquals("The wifi password is sakura2026.", evidenceCaptor.getValue().getAnswer());
        verify(itemRepository, Mockito.times(1)).save(existingItem);
    }

    @Test
    void refineSourcePair_shouldCreateSeparateWifiItemForDifferentRoomTypeScope() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEvidenceRepository evidenceRepository =
                Mockito.mock(MessageKnowledgeEvidenceRepository.class);
        SuMessagingThreadContextResolver contextResolver =
                Mockito.mock(SuMessagingThreadContextResolver.class);
        MessageKnowledgeRefinementService service = newService(
                itemRepository,
                evidenceRepository,
                contextResolver
        );

        SuMessageThread thread = newThread(88L, 26L);
        SuMessage guestMessage = newMessage(131L, thread, SuMessagingSenderType.GUEST, "What is the wifi password?", "SENT");
        SuMessage staffMessage = newMessage(
                132L,
                thread,
                SuMessagingSenderType.STAFF,
                "The wifi password is momiji2026.",
                "SENT"
        );

        when(evidenceRepository.findByStoreIdAndSourceGuestMessageIdAndSourceStaffMessageId(26L, 131L, 132L))
                .thenReturn(Optional.empty());
        when(contextResolver.resolveForIndex(26L, thread)).thenReturn(roomTypeContext(4L, "Suite"));
        when(itemRepository.findByStoreIdAndScopeKeyAndTopicHash(eq(26L), eq("ROOM_TYPE:4"), anyString()))
                .thenReturn(Optional.empty());
        when(itemRepository.save(any(MessageKnowledgeItem.class))).thenAnswer(invocation -> {
            MessageKnowledgeItem item = invocation.getArgument(0);
            if (item.getId() == null) {
                item.setId(801L);
            }
            return item;
        });
        when(evidenceRepository.save(any(MessageKnowledgeEvidence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean refined = service.refineSourcePair(26L, thread, guestMessage, staffMessage);

        assertTrue(refined);
        ArgumentCaptor<MessageKnowledgeItem> itemCaptor = ArgumentCaptor.forClass(MessageKnowledgeItem.class);
        verify(itemRepository, Mockito.times(2)).save(itemCaptor.capture());
        MessageKnowledgeItem savedItem = itemCaptor.getAllValues().get(1);
        assertEquals(4L, savedItem.getScopeId());
        assertEquals("ROOM_TYPE:4", savedItem.getScopeKey());
        assertEquals("Suite", savedItem.getRoomTypeName());
        assertEquals("The wifi password is momiji2026.", savedItem.getAnswer());
        assertEquals(MessageKnowledgeItem.STATUS_ACTIVE, savedItem.getStatus());
        verify(itemRepository).findByStoreIdAndScopeKeyAndTopicHash(eq(26L), eq("ROOM_TYPE:4"), anyString());
    }

    @Test
    void refineSourcePair_shouldMarkReadyEmbeddingStaleWhenCanonicalTextChanges() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEvidenceRepository evidenceRepository =
                Mockito.mock(MessageKnowledgeEvidenceRepository.class);
        SuMessagingThreadContextResolver contextResolver =
                Mockito.mock(SuMessagingThreadContextResolver.class);
        MessageKnowledgeRefinementService service = newService(
                itemRepository,
                evidenceRepository,
                contextResolver
        );

        SuMessageThread thread = newThread(77L, 26L);
        SuMessage guestMessage = newMessage(601L, thread, SuMessagingSenderType.GUEST, "What is the wifi password?", "SENT");
        SuMessage staffMessage = newMessage(
                602L,
                thread,
                SuMessagingSenderType.STAFF,
                "The wifi password is momiji2026.",
                "SENT"
        );
        MessageKnowledgeItem existingItem = newExistingWifiItem();
        existingItem.setEmbeddingStatus(MessageKnowledgeItem.EMBEDDING_STATUS_READY);
        existingItem.setEmbeddingVector("[0.1,0.2]");
        existingItem.setEmbeddingProvider("mock");
        existingItem.setEmbeddingModel("mock-model");
        existingItem.setEmbeddingDimensions(2);
        existingItem.setEmbeddingInputHash("old-input-hash");
        existingItem.setSemanticText("Topic: wifi\nCanonical fact: The wifi password is sakura2026.");
        existingItem.setSearchIntentsJson("{\"topic\":\"wifi\",\"queries\":[\"wifi\"]}");

        when(evidenceRepository.findByStoreIdAndSourceGuestMessageIdAndSourceStaffMessageId(26L, 601L, 602L))
                .thenReturn(Optional.empty());
        when(contextResolver.resolveForIndex(26L, thread)).thenReturn(roomTypeContext());
        when(itemRepository.findByStoreIdAndScopeKeyAndTopicHash(eq(26L), eq("ROOM_TYPE:3"), anyString()))
                .thenReturn(Optional.of(existingItem));
        when(itemRepository.save(any(MessageKnowledgeItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(evidenceRepository.save(any(MessageKnowledgeEvidence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean refined = service.refineSourcePair(26L, thread, guestMessage, staffMessage);

        assertTrue(refined);
        assertEquals("The wifi password is momiji2026.", existingItem.getAnswer());
        assertEquals(MessageKnowledgeItem.EMBEDDING_STATUS_STALE, existingItem.getEmbeddingStatus());
        assertEquals("[0.1,0.2]", existingItem.getEmbeddingVector());
        assertEquals("old-input-hash", existingItem.getEmbeddingInputHash());
        assertNull(existingItem.getEmbeddingError());
        assertNull(existingItem.getEmbeddingLeaseOwner());
        assertNull(existingItem.getEmbeddingLeaseUntil());
    }

    @Test
    void refineSourcePair_shouldMergeMultilingualWifiEvidenceWhenPasswordMatches() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEvidenceRepository evidenceRepository =
                Mockito.mock(MessageKnowledgeEvidenceRepository.class);
        SuMessagingThreadContextResolver contextResolver =
                Mockito.mock(SuMessagingThreadContextResolver.class);
        MessageKnowledgeRefinementService service = newService(
                itemRepository,
                evidenceRepository,
                contextResolver
        );
        SuMessageThread thread = newThread(77L, 26L);
        SuMessage guestMessage = newMessage(201L, thread, SuMessagingSenderType.GUEST, "WiFi密码是多少？", "SENT");
        SuMessage staffMessage = newMessage(202L, thread, SuMessagingSenderType.STAFF, "WiFi密码是 sakura2026。", "SENT");
        MessageKnowledgeItem existingItem = newExistingWifiItem();
        existingItem.setStatus(MessageKnowledgeItem.STATUS_CONFLICT);
        prepareReadyEmbedding(existingItem);

        when(evidenceRepository.findByStoreIdAndSourceGuestMessageIdAndSourceStaffMessageId(26L, 201L, 202L))
                .thenReturn(Optional.empty());
        when(contextResolver.resolveForIndex(26L, thread)).thenReturn(roomTypeContext());
        when(itemRepository.findByStoreIdAndScopeKeyAndTopicHash(eq(26L), eq("ROOM_TYPE:3"), anyString()))
                .thenReturn(Optional.of(existingItem));
        when(itemRepository.save(any(MessageKnowledgeItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(evidenceRepository.save(any(MessageKnowledgeEvidence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean refined = service.refineSourcePair(26L, thread, guestMessage, staffMessage);

        assertTrue(refined);
        assertEquals(MessageKnowledgeItem.STATUS_ACTIVE, existingItem.getStatus());
        assertEquals("The wifi password is sakura2026.", existingItem.getAnswer());
        assertEquals("en", existingItem.getLanguage());
        assertEquals(MessageKnowledgeItem.EMBEDDING_STATUS_READY, existingItem.getEmbeddingStatus());
        assertEquals("[0.1,0.2]", existingItem.getEmbeddingVector());
        assertEquals(3, existingItem.getEvidenceCount());

        ArgumentCaptor<MessageKnowledgeEvidence> evidenceCaptor =
                ArgumentCaptor.forClass(MessageKnowledgeEvidence.class);
        verify(evidenceRepository).save(evidenceCaptor.capture());
        assertEquals(MessageKnowledgeEvidence.STATUS_ACTIVE, evidenceCaptor.getValue().getStatus());
        assertEquals("zh", evidenceCaptor.getValue().getLanguage());
    }

    @Test
    void extractWifiFacts_shouldStopEnglishSsidBeforePasswordClause() {
        Object facts = ReflectionTestUtils.invokeMethod(
                MessageKnowledgeRefinementService.class,
                "extractWifiFacts",
                "SSID is HotelLocalQA and password is sakura"
        );

        String ssid = ReflectionTestUtils.invokeMethod(facts, "ssid");
        String password = ReflectionTestUtils.invokeMethod(facts, "password");

        assertEquals("hotellocalqa", ssid);
        assertEquals("sakura", password);
    }

    @Test
    void refineSourcePair_shouldKeepMultilingualWifiWithSameSsidAndPasswordActive() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEvidenceRepository evidenceRepository =
                Mockito.mock(MessageKnowledgeEvidenceRepository.class);
        SuMessagingThreadContextResolver contextResolver =
                Mockito.mock(SuMessagingThreadContextResolver.class);
        MessageKnowledgeRefinementService service = newService(
                itemRepository,
                evidenceRepository,
                contextResolver
        );
        SuMessageThread thread = newThread(77L, 26L);
        MessageKnowledgeItem existingItem = newExistingWifiItemWithAnswer(
                "The SSID is HotelLocalQA and password is sakura.",
                "the ssid is hotellocalqa and password is sakura"
        );
        List<String[]> pairs = List.of(
                new String[]{"What is the WiFi?", "SSID is HotelLocalQA and password is sakura."},
                new String[]{"WiFi密码是多少？", "无线名称是 HotelLocalQA，密码是 sakura。"},
                new String[]{"WiFiは何ですか？", "ネットワーク名は HotelLocalQA、パスワードは sakuraです。"},
                new String[]{"WiFi密碼是多少？", "網路名稱是 HotelLocalQA，密碼是 sakura。"}
        );

        when(evidenceRepository.findByStoreIdAndSourceGuestMessageIdAndSourceStaffMessageId(
                eq(26L),
                anyLong(),
                anyLong()
        )).thenReturn(Optional.empty());
        when(contextResolver.resolveForIndex(26L, thread)).thenReturn(roomTypeContext());
        when(itemRepository.findByStoreIdAndScopeKeyAndTopicHash(eq(26L), eq("ROOM_TYPE:3"), anyString()))
                .thenReturn(Optional.of(existingItem));
        when(itemRepository.save(any(MessageKnowledgeItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(evidenceRepository.save(any(MessageKnowledgeEvidence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        long messageId = 401L;
        for (String[] pair : pairs) {
            SuMessage guestMessage = newMessage(messageId, thread, SuMessagingSenderType.GUEST, pair[0], "SENT");
            SuMessage staffMessage = newMessage(messageId + 1L, thread, SuMessagingSenderType.STAFF, pair[1], "SENT");

            boolean refined = service.refineSourcePair(26L, thread, guestMessage, staffMessage);

            assertTrue(refined);
            assertEquals(MessageKnowledgeItem.STATUS_ACTIVE, existingItem.getStatus());
            messageId = messageId + 2L;
        }

        assertEquals(MessageKnowledgeItem.STATUS_ACTIVE, existingItem.getStatus());
        assertEquals(6, existingItem.getEvidenceCount());

        ArgumentCaptor<MessageKnowledgeEvidence> evidenceCaptor =
                ArgumentCaptor.forClass(MessageKnowledgeEvidence.class);
        verify(evidenceRepository, Mockito.times(4)).save(evidenceCaptor.capture());
        for (MessageKnowledgeEvidence evidence : evidenceCaptor.getAllValues()) {
            assertEquals(MessageKnowledgeEvidence.STATUS_ACTIVE, evidence.getStatus());
        }
    }

    @Test
    void refineSourcePair_shouldOverwriteWifiCanonicalAnswerWhenPasswordDiffers() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEvidenceRepository evidenceRepository =
                Mockito.mock(MessageKnowledgeEvidenceRepository.class);
        SuMessagingThreadContextResolver contextResolver =
                Mockito.mock(SuMessagingThreadContextResolver.class);
        MessageKnowledgeRefinementService service = newService(
                itemRepository,
                evidenceRepository,
                contextResolver
        );
        SuMessageThread thread = newThread(77L, 26L);
        SuMessage guestMessage = newMessage(203L, thread, SuMessagingSenderType.GUEST, "WiFi密码是多少？", "SENT");
        SuMessage staffMessage = newMessage(204L, thread, SuMessagingSenderType.STAFF, "WiFi密码是 momiji2026。", "SENT");
        MessageKnowledgeItem existingItem = newExistingWifiItem();

        when(evidenceRepository.findByStoreIdAndSourceGuestMessageIdAndSourceStaffMessageId(26L, 203L, 204L))
                .thenReturn(Optional.empty());
        when(contextResolver.resolveForIndex(26L, thread)).thenReturn(roomTypeContext());
        when(itemRepository.findByStoreIdAndScopeKeyAndTopicHash(eq(26L), eq("ROOM_TYPE:3"), anyString()))
                .thenReturn(Optional.of(existingItem));
        when(itemRepository.save(any(MessageKnowledgeItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(evidenceRepository.save(any(MessageKnowledgeEvidence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean refined = service.refineSourcePair(26L, thread, guestMessage, staffMessage);

        assertTrue(refined);
        assertEquals(MessageKnowledgeItem.STATUS_ACTIVE, existingItem.getStatus());
        assertEquals("WiFi密码是 momiji2026。", existingItem.getAnswer());
        assertEquals("zh", existingItem.getLanguage());

        ArgumentCaptor<MessageKnowledgeEvidence> evidenceCaptor =
                ArgumentCaptor.forClass(MessageKnowledgeEvidence.class);
        verify(evidenceRepository).save(evidenceCaptor.capture());
        assertEquals(MessageKnowledgeEvidence.STATUS_ACTIVE, evidenceCaptor.getValue().getStatus());
    }

    @Test
    void refineSourcePair_shouldOverwriteWifiCanonicalAnswerWhenSameSsidHasDifferentPassword() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEvidenceRepository evidenceRepository =
                Mockito.mock(MessageKnowledgeEvidenceRepository.class);
        SuMessagingThreadContextResolver contextResolver =
                Mockito.mock(SuMessagingThreadContextResolver.class);
        MessageKnowledgeRefinementService service = newService(
                itemRepository,
                evidenceRepository,
                contextResolver
        );
        SuMessageThread thread = newThread(77L, 26L);
        SuMessage guestMessage = newMessage(501L, thread, SuMessagingSenderType.GUEST, "What is the WiFi?", "SENT");
        SuMessage staffMessage = newMessage(
                502L,
                thread,
                SuMessagingSenderType.STAFF,
                "SSID is HotelLocalQA and password is momiji.",
                "SENT"
        );
        MessageKnowledgeItem existingItem = newExistingWifiItemWithAnswer(
                "SSID is HotelLocalQA and password is sakura.",
                "ssid is hotellocalqa and password is sakura"
        );

        when(evidenceRepository.findByStoreIdAndSourceGuestMessageIdAndSourceStaffMessageId(26L, 501L, 502L))
                .thenReturn(Optional.empty());
        when(contextResolver.resolveForIndex(26L, thread)).thenReturn(roomTypeContext());
        when(itemRepository.findByStoreIdAndScopeKeyAndTopicHash(eq(26L), eq("ROOM_TYPE:3"), anyString()))
                .thenReturn(Optional.of(existingItem));
        when(itemRepository.save(any(MessageKnowledgeItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(evidenceRepository.save(any(MessageKnowledgeEvidence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean refined = service.refineSourcePair(26L, thread, guestMessage, staffMessage);

        assertTrue(refined);
        assertEquals(MessageKnowledgeItem.STATUS_ACTIVE, existingItem.getStatus());
        assertEquals("SSID is HotelLocalQA and password is momiji.", existingItem.getAnswer());

        ArgumentCaptor<MessageKnowledgeEvidence> evidenceCaptor =
                ArgumentCaptor.forClass(MessageKnowledgeEvidence.class);
        verify(evidenceRepository).save(evidenceCaptor.capture());
        assertEquals(MessageKnowledgeEvidence.STATUS_ACTIVE, evidenceCaptor.getValue().getStatus());
    }

    @Test
    void refineSourcePair_shouldClassifyBreakfastTermsAcrossLanguages() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEvidenceRepository evidenceRepository =
                Mockito.mock(MessageKnowledgeEvidenceRepository.class);
        SuMessagingThreadContextResolver contextResolver =
                Mockito.mock(SuMessagingThreadContextResolver.class);
        MessageKnowledgeRefinementService service = newService(
                itemRepository,
                evidenceRepository,
                contextResolver
        );
        SuMessageThread thread = newThread(77L, 26L);
        List<String[]> pairs = List.of(
                new String[]{"What time is breakfast?", "Breakfast is served from 07:00 to 10:00."},
                new String[]{"早餐几点开始？", "早餐时间是 07:00-10:00。"},
                new String[]{"朝食は何時ですか？", "朝食は07:00から10:00までです。"},
                new String[]{"早餐幾點開始？", "早餐時間是 07:00-10:00。"}
        );
        when(evidenceRepository.findByStoreIdAndSourceGuestMessageIdAndSourceStaffMessageId(
                eq(26L),
                anyLong(),
                anyLong()
        )).thenReturn(Optional.empty());
        when(contextResolver.resolveForIndex(26L, thread)).thenReturn(roomTypeContext());
        when(itemRepository.findByStoreIdAndScopeKeyAndTopicHash(eq(26L), eq("ROOM_TYPE:3"), anyString()))
                .thenReturn(Optional.empty());
        when(itemRepository.save(any(MessageKnowledgeItem.class))).thenAnswer(invocation -> {
            MessageKnowledgeItem item = invocation.getArgument(0);
            if (item.getId() == null) {
                item.setId(900L + item.getEvidenceCount());
            }
            return item;
        });
        when(evidenceRepository.save(any(MessageKnowledgeEvidence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        long messageId = 300L;
        for (String[] pair : pairs) {
            SuMessage guestMessage = newMessage(messageId, thread, SuMessagingSenderType.GUEST, pair[0], "SENT");
            SuMessage staffMessage = newMessage(messageId + 1L, thread, SuMessagingSenderType.STAFF, pair[1], "SENT");

            boolean refined = service.refineSourcePair(26L, thread, guestMessage, staffMessage);

            assertTrue(refined);
            messageId = messageId + 2L;
        }

        ArgumentCaptor<MessageKnowledgeItem> itemCaptor = ArgumentCaptor.forClass(MessageKnowledgeItem.class);
        verify(itemRepository, Mockito.times(8)).save(itemCaptor.capture());
        for (MessageKnowledgeItem item : itemCaptor.getAllValues()) {
            assertEquals("breakfast", item.getTopic());
            assertEquals(MessageKnowledgeItem.STATUS_ACTIVE, item.getStatus());
        }
    }

    @Test
    void refineSourcePair_shouldClassifyPetPolicyAndShuttleTopicsAcrossLanguages() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEvidenceRepository evidenceRepository =
                Mockito.mock(MessageKnowledgeEvidenceRepository.class);
        SuMessagingThreadContextResolver contextResolver =
                Mockito.mock(SuMessagingThreadContextResolver.class);
        MessageKnowledgeRefinementService service = newService(
                itemRepository,
                evidenceRepository,
                contextResolver
        );
        SuMessageThread thread = newThread(77L, 26L);
        List<String[]> pairs = List.of(
                new String[]{"pet_policy", "Do you allow pets?", "Pets are not allowed in the rooms."},
                new String[]{"pet_policy", "¿Aceptan mascotas?", "No aceptamos mascotas en la propiedad."},
                new String[]{"pet_policy", "Les animaux sont-ils acceptés?", "Les animaux ne sont pas autorisés."},
                new String[]{"shuttle", "Do you have an airport shuttle?", "The airport shuttle departs from the lobby."},
                new String[]{"shuttle", "Navette aéroport?", "La navette aéroport est disponible sur réservation."},
                new String[]{"shuttle", "공항 셔틀이 있나요?", "공항 셔틀은 프런트에서 예약할 수 있습니다."}
        );
        when(evidenceRepository.findByStoreIdAndSourceGuestMessageIdAndSourceStaffMessageId(
                eq(26L),
                anyLong(),
                anyLong()
        )).thenReturn(Optional.empty());
        when(contextResolver.resolveForIndex(26L, thread)).thenReturn(roomTypeContext());
        when(itemRepository.findByStoreIdAndScopeKeyAndTopicHash(eq(26L), eq("ROOM_TYPE:3"), anyString()))
                .thenReturn(Optional.empty());
        final long[] nextItemId = {1000L};
        when(itemRepository.save(any(MessageKnowledgeItem.class))).thenAnswer(invocation -> {
            MessageKnowledgeItem item = invocation.getArgument(0);
            if (item.getId() == null) {
                item.setId(nextItemId[0]);
                nextItemId[0] = nextItemId[0] + 1L;
            }
            return item;
        });
        when(evidenceRepository.save(any(MessageKnowledgeEvidence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        long messageId = 700L;
        for (String[] pair : pairs) {
            SuMessage guestMessage = newMessage(messageId, thread, SuMessagingSenderType.GUEST, pair[1], "SENT");
            SuMessage staffMessage = newMessage(messageId + 1L, thread, SuMessagingSenderType.STAFF, pair[2], "SENT");

            boolean refined = service.refineSourcePair(26L, thread, guestMessage, staffMessage);

            assertTrue(refined);
            messageId = messageId + 2L;
        }

        ArgumentCaptor<MessageKnowledgeItem> itemCaptor = ArgumentCaptor.forClass(MessageKnowledgeItem.class);
        verify(itemRepository, Mockito.times(pairs.size() * 2)).save(itemCaptor.capture());
        List<MessageKnowledgeItem> savedItems = itemCaptor.getAllValues();
        for (int index = 0; index < pairs.size(); index++) {
            String expectedTopic = pairs.get(index)[0];
            MessageKnowledgeItem item = savedItems.get(index * 2 + 1);
            assertEquals(expectedTopic, item.getTopic());
            assertEquals(MessageKnowledgeItem.STATUS_ACTIVE, item.getStatus());
            assertTrue(item.getSemanticText().contains("Topic: " + expectedTopic));
            assertTrue(item.getSearchIntentsJson().contains("\"topic\":\"" + expectedTopic + "\""));
        }
    }

    @Test
    void classifyTopic_shouldPreferShuttleOverLocationAndKeepPlainLocation() {
        String petTopic = ReflectionTestUtils.invokeMethod(
                MessageKnowledgeRefinementService.class,
                "classifyTopic",
                "do you allow pets"
        );
        String shuttleTopic = ReflectionTestUtils.invokeMethod(
                MessageKnowledgeRefinementService.class,
                "classifyTopic",
                "is there an airport shuttle near the station"
        );
        String locationTopic = ReflectionTestUtils.invokeMethod(
                MessageKnowledgeRefinementService.class,
                "classifyTopic",
                "what is your address near the nearest station"
        );

        assertEquals("pet_policy", petTopic);
        assertEquals("shuttle", shuttleTopic);
        assertEquals("location", locationTopic);
    }

    @Test
    void getMaxModelCalls_shouldDefaultToZeroAndClampNegativeValues() {
        MessageKnowledgeRefinementService service = newService(
                Mockito.mock(MessageKnowledgeItemRepository.class),
                Mockito.mock(MessageKnowledgeEvidenceRepository.class),
                Mockito.mock(SuMessagingThreadContextResolver.class)
        );

        assertEquals(0, service.getMaxModelCalls());

        org.springframework.test.util.ReflectionTestUtils.setField(service, "maxModelCalls", -10);

        assertEquals(0, service.getMaxModelCalls());
    }

    private static MessageKnowledgeRefinementService newService(
            MessageKnowledgeItemRepository itemRepository,
            MessageKnowledgeEvidenceRepository evidenceRepository,
            SuMessagingThreadContextResolver contextResolver
    ) {
        return new MessageKnowledgeRefinementService(
                itemRepository,
                evidenceRepository,
                contextResolver,
                new SuMessagingAiRedactor(),
                new SuMessagingAiTextService(),
                new MessageKnowledgeEmbeddingTextService(
                        new ObjectMapper(),
                        new SuMessagingAiTextService()
                )
        );
    }

    private static SuMessagingThreadContext roomTypeContext() {
        return roomTypeContext(3L, "Deluxe");
    }

    private static SuMessagingThreadContext roomTypeContext(Long roomTypeId, String roomTypeName) {
        SuMessagingThreadContext context = new SuMessagingThreadContext();
        context.setRoomTypeId(roomTypeId);
        context.setRoomTypeName(roomTypeName);
        context.setChannelId(SuMessagingService.CHANNEL_AIRBNB);
        return context;
    }

    private static SuMessagingThreadContext storeFallbackContext() {
        SuMessagingThreadContext context = new SuMessagingThreadContext();
        context.setChannelId(SuMessagingService.CHANNEL_AIRBNB);
        context.setChannelName("Airbnb");
        context.setBookingKey("SIM-BOOKING-77");
        context.setMatchStatus("NONE");
        return context;
    }

    private static MessageKnowledgeItem newExistingWifiItem() {
        MessageKnowledgeItem item = new MessageKnowledgeItem();
        item.setId(501L);
        item.setStoreId(26L);
        item.setScopeType(SuMessagingThreadContext.SCOPE_ROOM_TYPE);
        item.setScopeId(3L);
        item.setScopeKey("ROOM_TYPE:3");
        item.setThreadId(77L);
        item.setRoomTypeId(3L);
        item.setRoomTypeName("Deluxe");
        item.setTopic("wifi");
        item.setTopicHash("topic-hash");
        item.setQuestion("What is the wifi password?");
        item.setAnswer("The wifi password is sakura2026.");
        item.setNormalizedQuestion("what is the wifi password");
        item.setNormalizedAnswer("the wifi password is sakura2026");
        item.setNormalizedAnswerHash("answer-hash");
        item.setLanguage("en");
        item.setStatus(MessageKnowledgeItem.STATUS_ACTIVE);
        item.setEvidenceCount(2);
        item.setConfidence(BigDecimal.valueOf(0.72));
        return item;
    }

    private static MessageKnowledgeItem newExistingWifiItemWithAnswer(String answer, String normalizedAnswer) {
        MessageKnowledgeItem item = newExistingWifiItem();
        item.setAnswer(answer);
        item.setNormalizedAnswer(normalizedAnswer);
        return item;
    }

    private static void prepareReadyEmbedding(MessageKnowledgeItem item) {
        MessageKnowledgeEmbeddingTextService embeddingTextService = new MessageKnowledgeEmbeddingTextService(
                new ObjectMapper(),
                new SuMessagingAiTextService()
        );
        embeddingTextService.refreshSemanticFields(item, false);
        item.setEmbeddingStatus(MessageKnowledgeItem.EMBEDDING_STATUS_READY);
        item.setEmbeddingVector("[0.1,0.2]");
        item.setEmbeddingProvider("mock");
        item.setEmbeddingModel("mock-model");
        item.setEmbeddingDimensions(2);
        item.setEmbeddingInputHash("old-input-hash");
    }

    private static SuMessageThread newThread(Long id, Long storeId) {
        SuMessageThread thread = new SuMessageThread();
        thread.setId(id);
        thread.setStoreId(storeId);
        thread.setChannelId(SuMessagingService.CHANNEL_AIRBNB);
        thread.setBookingId("B" + id);
        return thread;
    }

    private static SuMessage newMessage(
            Long id,
            SuMessageThread thread,
            SuMessagingSenderType senderType,
            String content,
            String deliveryStatus
    ) {
        SuMessage message = new SuMessage();
        message.setId(id);
        message.setStoreId(thread.getStoreId());
        message.setThread(thread);
        message.setSenderType(senderType);
        message.setContent(content);
        message.setDeliveryStatus(deliveryStatus);
        message.setSentAt(LocalDateTime.of(2026, 6, 15, 10, 0).plusMinutes(id));
        return message;
    }
}
