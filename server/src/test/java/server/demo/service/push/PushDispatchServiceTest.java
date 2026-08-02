package server.demo.service.push;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.demo.entity.NotificationSetting;
import server.demo.entity.PushDeviceToken;
import server.demo.entity.StoreUser;
import server.demo.entity.User;
import server.demo.enums.PushPlatform;
import server.demo.repository.NotificationSettingRepository;
import server.demo.repository.PushDeviceTokenRepository;
import server.demo.repository.StoreUserRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushDispatchServiceTest {

    @Mock
    private StoreUserRepository storeUserRepository;

    @Mock
    private PushDeviceTokenRepository pushDeviceTokenRepository;

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @Mock
    private ApnsPushService apnsPushService;

    private PushDispatchService dispatchService;

    @BeforeEach
    void setUp() {
        dispatchService = new PushDispatchService(
                storeUserRepository, pushDeviceTokenRepository, notificationSettingRepository, apnsPushService);
        lenient().when(apnsPushService.send(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(CompletableFuture.completedFuture(
                        ApnsPushService.ApnsSendResult.accepted("token")));
    }

    @Test
    void dispatchToStoreUsers_shouldSkipUsersWhoDisabledChatPopup() {
        when(storeUserRepository.findActiveUsersByStoreId(7L)).thenReturn(List.of(
                storeUser(101L),
                storeUser(102L)
        ));
        NotificationSetting off = new NotificationSetting();
        off.setChatPopup(false);
        when(notificationSettingRepository.findByUserId(101L)).thenReturn(Optional.of(off));
        when(notificationSettingRepository.findByUserId(102L)).thenReturn(Optional.empty());
        when(pushDeviceTokenRepository.findByUserIdInAndEnabledTrue(List.of(102L)))
                .thenReturn(List.of(token(PushPlatform.IOS, "ios-token")));

        dispatchService.dispatchToStoreUsers(7L, PushDispatchService.PushCategory.CHAT, "t", "b", Map.of());

        verify(pushDeviceTokenRepository).findByUserIdInAndEnabledTrue(List.of(102L));
        verify(apnsPushService).send(eq("ios-token"), eq("t"), eq("b"), anyMap());
    }

    @Test
    void dispatchToStoreUsers_shouldNotSendApnsToAndroidTokens() {
        when(storeUserRepository.findActiveUsersByStoreId(7L)).thenReturn(List.of(storeUser(101L)));
        when(notificationSettingRepository.findByUserId(101L)).thenReturn(Optional.empty());
        when(pushDeviceTokenRepository.findByUserIdInAndEnabledTrue(List.of(101L)))
                .thenReturn(List.of(token(PushPlatform.ANDROID, "android-token")));

        dispatchService.dispatchToStoreUsers(7L, PushDispatchService.PushCategory.ORDER, "t", "b", Map.of());

        verify(apnsPushService, never()).send(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void dispatchToStoreUsers_shouldDisableTokenOnBadDeviceTokenRejection() {
        when(storeUserRepository.findActiveUsersByStoreId(7L)).thenReturn(List.of(storeUser(101L)));
        when(notificationSettingRepository.findByUserId(101L)).thenReturn(Optional.empty());
        PushDeviceToken token = token(PushPlatform.IOS, "dead-token");
        token.setId(9L);
        when(pushDeviceTokenRepository.findByUserIdInAndEnabledTrue(List.of(101L)))
                .thenReturn(List.of(token));
        when(apnsPushService.send(eq("dead-token"), anyString(), anyString(), anyMap()))
                .thenReturn(CompletableFuture.completedFuture(
                        ApnsPushService.ApnsSendResult.rejected("dead-token", "BadDeviceToken")));
        when(pushDeviceTokenRepository.findById(9L)).thenReturn(Optional.of(token));

        dispatchService.dispatchToStoreUsers(7L, PushDispatchService.PushCategory.ORDER, "t", "b", Map.of());

        verify(pushDeviceTokenRepository).save(org.mockito.ArgumentMatchers.argThat(
                (PushDeviceToken t) -> Boolean.FALSE.equals(t.getEnabled())));
    }

    private StoreUser storeUser(Long userId) {
        User user = new User();
        user.setId(userId);
        StoreUser storeUser = new StoreUser();
        storeUser.setUser(user);
        storeUser.setIsActive(true);
        return storeUser;
    }

    private PushDeviceToken token(PushPlatform platform, String deviceToken) {
        PushDeviceToken token = new PushDeviceToken();
        token.setPlatform(platform);
        token.setDeviceToken(deviceToken);
        token.setEnabled(true);
        return token;
    }
}
