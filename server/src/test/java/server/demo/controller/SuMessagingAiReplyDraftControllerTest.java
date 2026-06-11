package server.demo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import server.demo.annotation.StoreScoped;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.ApiResponse;
import server.demo.dto.SuMessagingAiReplyDraftRequest;
import server.demo.dto.SuMessagingAiReplyDraftResponse;
import server.demo.service.MessageKnowledgeSearchService;
import server.demo.service.SuMessagingAiReplyDraftService;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuMessagingAiReplyDraftControllerTest {

    @Test
    void generateReplyDraft_shouldExposeStoreScopedEndpointAndReturnDraftContract() throws Exception {
        SuMessagingAiReplyDraftService service = mock(SuMessagingAiReplyDraftService.class);
        SuMessagingAiReplyDraftController controller = new SuMessagingAiReplyDraftController(service);
        SuMessagingAiReplyDraftRequest request = new SuMessagingAiReplyDraftRequest();
        SuMessagingAiReplyDraftResponse draftResponse = new SuMessagingAiReplyDraftResponse(
                "Sure, late checkout is possible.",
                MessageKnowledgeSearchService.STATUS_MATCHED,
                List.of(),
                1,
                42L
        );

        when(service.generateDraft(26L, 77L, request)).thenReturn(draftResponse);

        StoreContextHolder.setContext(new StoreContext(1L, 26L, "OWNER"));
        try {
            ResponseEntity<ApiResponse<SuMessagingAiReplyDraftResponse>> response =
                    controller.generateReplyDraft(77L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isSuccess());
            assertEquals("Sure, late checkout is possible.", response.getBody().getData().getDraftReply());
            assertEquals(MessageKnowledgeSearchService.STATUS_MATCHED, response.getBody().getData().getRetrievalStatus());
            verify(service).generateDraft(26L, 77L, request);
        } finally {
            StoreContextHolder.clear();
        }

        Method method = SuMessagingAiReplyDraftController.class.getMethod(
                "generateReplyDraft",
                Long.class,
                SuMessagingAiReplyDraftRequest.class
        );
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        StoreScoped storeScoped = method.getAnnotation(StoreScoped.class);
        assertNotNull(mapping);
        assertNotNull(storeScoped);
        assertEquals("/threads/{threadId}/ai-reply-draft", mapping.value()[0]);
    }
}
