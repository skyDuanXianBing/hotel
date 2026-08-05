package server.demo.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.demo.entity.Channel;
import server.demo.entity.Store;
import server.demo.repository.ChannelRepository;
import server.demo.repository.StoreRepository;
import server.demo.service.ChannelBootstrapService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 三渠道存量迁移 Runner 聚焦测试：幂等补齐 + AGODA 脏名修复。
 */
@ExtendWith(MockitoExtension.class)
class OtaThreeChannelDataMigrationTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private ChannelBootstrapService channelBootstrapService;

    @Test
    void run_shouldBackfillThreeOtaChannelsForEveryStore() {
        OtaThreeChannelDataMigration migration = createMigration();
        Store store1 = store(1L);
        Store store2 = store(2L);
        when(storeRepository.findAll()).thenReturn(List.of(store1, store2));
        when(channelRepository.findAll()).thenReturn(List.of());

        migration.run();

        verify(channelBootstrapService).ensureDefaultChannelsForStore(1L);
        verify(channelBootstrapService).ensureDefaultChannelsForStore(2L);
    }

    @Test
    void run_shouldContinueOtherStoresWhenOneBackfillFails() {
        OtaThreeChannelDataMigration migration = createMigration();
        Store store1 = store(1L);
        Store store2 = store(2L);
        when(storeRepository.findAll()).thenReturn(List.of(store1, store2));
        when(channelBootstrapService.ensureDefaultChannelsForStore(1L))
                .thenThrow(new RuntimeException("unique constraint"));
        when(channelRepository.findAll()).thenReturn(List.of());

        migration.run();

        verify(channelBootstrapService).ensureDefaultChannelsForStore(2L);
    }

    @Test
    void run_shouldRenameLegacyAvatarAgodaChannelToAgoda() {
        OtaThreeChannelDataMigration migration = createMigration();
        when(storeRepository.findAll()).thenReturn(List.of());
        Channel dirtyAgoda = channel("AGODA", "阿凡达");
        Channel booking = channel("BOOKING", "Booking.com");
        Channel fixedAgoda = channel("AGODA", "Agoda");
        when(channelRepository.findAll()).thenReturn(List.of(dirtyAgoda, booking, fixedAgoda));

        migration.run();

        ArgumentCaptor<List<Channel>> captor = ArgumentCaptor.forClass(List.class);
        verify(channelRepository).saveAll(captor.capture());
        List<Channel> saved = captor.getValue();
        assertEquals(1, saved.size());
        assertSame(dirtyAgoda, saved.get(0));
        assertEquals("Agoda", dirtyAgoda.getName());
        assertEquals("Booking.com", booking.getName());
    }

    @Test
    void run_shouldBeIdempotentWhenNoDirtyChannelExists() {
        OtaThreeChannelDataMigration migration = createMigration();
        when(storeRepository.findAll()).thenReturn(List.of());
        when(channelRepository.findAll()).thenReturn(List.of(channel("AGODA", "Agoda")));

        migration.run();

        verify(channelRepository, never()).saveAll(any());
    }

    private OtaThreeChannelDataMigration createMigration() {
        return new OtaThreeChannelDataMigration(storeRepository, channelRepository, channelBootstrapService);
    }

    private static Store store(Long id) {
        Store store = new Store();
        store.setId(id);
        return store;
    }

    private static Channel channel(String code, String name) {
        Channel channel = new Channel();
        channel.setCode(code);
        channel.setName(name);
        return channel;
    }
}
