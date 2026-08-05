package server.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import server.demo.entity.AutoMessage;
import server.demo.entity.Channel;
import server.demo.repository.AutoMessageRepository;
import server.demo.repository.AutoMessageSendLogRepository;
import server.demo.repository.ChannelRepository;
import server.demo.repository.StoreRepository;
import server.demo.repository.SuMessageRepository;
import server.demo.repository.SuMessageThreadRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * SuAutoReplyService 渠道解析聚焦测试：非 A 即 B 三元改为目录查询，未识别渠道显式跳过。
 */
class SuAutoReplyServiceChannelResolutionTest {

    private static final Long STORE_ID = 10L;

    private final SuMessageThreadRepository threadRepository = Mockito.mock(SuMessageThreadRepository.class);
    private final SuMessageRepository messageRepository = Mockito.mock(SuMessageRepository.class);
    private final AutoMessageRepository autoMessageRepository = Mockito.mock(AutoMessageRepository.class);
    private final AutoMessageSendLogRepository sendLogRepository = Mockito.mock(AutoMessageSendLogRepository.class);
    private final ChannelRepository channelRepository = Mockito.mock(ChannelRepository.class);
    private final StoreRepository storeRepository = Mockito.mock(StoreRepository.class);
    private final SuApiClient suApiClient = Mockito.mock(SuApiClient.class);
    private final SuAccessTokenService suAccessTokenService = Mockito.mock(SuAccessTokenService.class);
    private final SuMessagingRealtimeGateway realtimeGateway = Mockito.mock(SuMessagingRealtimeGateway.class);

    private SuAutoReplyService createService() {
        return new SuAutoReplyService(
                threadRepository,
                messageRepository,
                autoMessageRepository,
                sendLogRepository,
                channelRepository,
                storeRepository,
                suApiClient,
                suAccessTokenService,
                realtimeGateway,
                new ObjectMapper()
        );
    }

    @Test
    void findMatchingTemplate_shouldResolveBookingAsBefore() {
        SuAutoReplyService service = createService();
        Channel channel = channel(33L, "BOOKING");
        when(channelRepository.findByStoreIdAndCode(STORE_ID, "BOOKING")).thenReturn(Optional.of(channel));
        when(autoMessageRepository.findByStoreIdAndEnabledTrue(STORE_ID))
                .thenReturn(List.of(templateMatching(33L)));

        Optional<AutoMessage> result = service.findMatchingTemplate(STORE_ID, 19);

        assertTrue(result.isPresent());
        verify(channelRepository).findByStoreIdAndCode(STORE_ID, "BOOKING");
    }

    @Test
    void findMatchingTemplate_shouldResolveNewChannelsByCatalog() {
        SuAutoReplyService service = createService();
        Channel channel = channel(55L, "TRIP");
        when(channelRepository.findByStoreIdAndCode(STORE_ID, "TRIP")).thenReturn(Optional.of(channel));
        when(autoMessageRepository.findByStoreIdAndEnabledTrue(STORE_ID))
                .thenReturn(List.of(templateMatching(55L)));

        Optional<AutoMessage> result = service.findMatchingTemplate(STORE_ID, 339);

        assertTrue(result.isPresent());
        verify(channelRepository).findByStoreIdAndCode(STORE_ID, "TRIP");
    }

    @Test
    void findMatchingTemplate_shouldResolveExpediaInsteadOfMislabelingAirbnb() {
        SuAutoReplyService service = createService();
        Channel channel = channel(66L, "EXPEDIA");
        when(channelRepository.findByStoreIdAndCode(STORE_ID, "EXPEDIA")).thenReturn(Optional.of(channel));
        when(autoMessageRepository.findByStoreIdAndEnabledTrue(STORE_ID))
                .thenReturn(List.of(templateMatching(66L)));

        Optional<AutoMessage> result = service.findMatchingTemplate(STORE_ID, 9);

        // 修复前：非 19 一律错标 AIRBNB；修复后：按目录解析为 EXPEDIA
        assertTrue(result.isPresent());
        verify(channelRepository).findByStoreIdAndCode(STORE_ID, "EXPEDIA");
    }

    @Test
    void findMatchingTemplate_shouldSkipUnknownSuChannelIdWithoutMislabeling() {
        SuAutoReplyService service = createService();

        Optional<AutoMessage> result = service.findMatchingTemplate(STORE_ID, 999);

        assertTrue(result.isEmpty());
        verifyNoInteractions(channelRepository, autoMessageRepository);
    }

    private static Channel channel(Long id, String code) {
        Channel channel = new Channel();
        channel.setId(id);
        channel.setStoreId(STORE_ID);
        channel.setCode(code);
        return channel;
    }

    private static AutoMessage templateMatching(Long channelId) {
        AutoMessage template = new AutoMessage();
        template.setId(88L);
        template.setStoreId(STORE_ID);
        template.setAction(SuAutoReplyService.ACTION_GUEST_MESSAGE);
        template.setChannels("[" + channelId + "]");
        template.setRoomSelectionType("ALL_LOCAL");
        template.setMessage("Welcome {{guest_name}}");
        return template;
    }
}
