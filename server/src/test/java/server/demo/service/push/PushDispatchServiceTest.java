package server.demo.service.push;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.demo.dto.NotificationBadgeSummaryDTO;
import server.demo.entity.NotificationSetting;
import server.demo.entity.PushDeviceToken;
import server.demo.entity.StoreUser;
import server.demo.entity.User;
import server.demo.enums.PushPlatform;
import server.demo.i18n.ApiMessageService;
import server.demo.repository.NotificationSettingRepository;
import server.demo.repository.PushDeviceTokenRepository;
import server.demo.repository.StoreUserRepository;
import server.demo.service.NotificationBadgeService;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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

    @Mock
    private ApiMessageService apiMessageService;

    @Mock
    private NotificationBadgeService notificationBadgeService;

    private PushDispatchService dispatchService;

    @BeforeEach
    void setUp() {
        dispatchService = new PushDispatchService(
                storeUserRepository, pushDeviceTokenRepository, notificationSettingRepository,
                apnsPushService, apiMessageService, notificationBadgeService);
        lenient().when(apnsPushService.send(anyString(), anyString(), anyString(), anyMap(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        ApnsPushService.ApnsSendResult.accepted("token")));
        lenient().when(notificationBadgeService.summaryFor(any(), any()))
                .thenReturn(new NotificationBadgeSummaryDTO(0, 0, 0));
        lenient().when(apiMessageService.resolve(any(Locale.class), anyString(), any(Object[].class)))
                .thenAnswer(invocation -> {
                    Locale locale = invocation.getArgument(0);
                    String key = invocation.getArgument(1);
                    return locale.toLanguageTag() + ":" + key;
                });
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
                .thenReturn(List.of(token(PushPlatform.IOS, "ios-token", 102L, 7L, "zh-CN")));

        dispatchService.dispatchToStoreUsers(7L, PushDispatchService.PushCategory.CHAT,
                PushDispatchService.PushText.keyed("title.key", "body.key"), Map.of());

        verify(pushDeviceTokenRepository).findByUserIdInAndEnabledTrue(List.of(102L));
        verify(apnsPushService).send(eq("ios-token"), eq("zh-CN:title.key"), eq("zh-CN:body.key"), anyMap(), anyInt());
    }

    @Test
    void dispatchToStoreUsers_shouldNotSendApnsToAndroidTokens() {
        when(storeUserRepository.findActiveUsersByStoreId(7L)).thenReturn(List.of(storeUser(101L)));
        when(notificationSettingRepository.findByUserId(101L)).thenReturn(Optional.empty());
        when(pushDeviceTokenRepository.findByUserIdInAndEnabledTrue(List.of(101L)))
                .thenReturn(List.of(token(PushPlatform.ANDROID, "android-token", 101L, 7L, "zh-CN")));

        dispatchService.dispatchToStoreUsers(7L, PushDispatchService.PushCategory.ORDER,
                PushDispatchService.PushText.keyed("title.key", "body.key"), Map.of());

        verify(apnsPushService, never()).send(anyString(), anyString(), anyString(), anyMap(), any());
    }

    @Test
    void dispatchToStoreUsers_shouldDisableTokenOnBadDeviceTokenRejection() {
        when(storeUserRepository.findActiveUsersByStoreId(7L)).thenReturn(List.of(storeUser(101L)));
        when(notificationSettingRepository.findByUserId(101L)).thenReturn(Optional.empty());
        PushDeviceToken token = token(PushPlatform.IOS, "dead-token", 101L, 7L, "zh-CN");
        token.setId(9L);
        when(pushDeviceTokenRepository.findByUserIdInAndEnabledTrue(List.of(101L)))
                .thenReturn(List.of(token));
        when(apnsPushService.send(eq("dead-token"), anyString(), anyString(), anyMap(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        ApnsPushService.ApnsSendResult.rejected("dead-token", "BadDeviceToken")));
        when(pushDeviceTokenRepository.findById(9L)).thenReturn(Optional.of(token));

        dispatchService.dispatchToStoreUsers(7L, PushDispatchService.PushCategory.ORDER,
                PushDispatchService.PushText.keyed("title.key", "body.key"), Map.of());

        verify(pushDeviceTokenRepository).save(org.mockito.ArgumentMatchers.argThat(
                (PushDeviceToken t) -> Boolean.FALSE.equals(t.getEnabled())));
    }

    @Test
    void dispatchToUsers_shouldRenderTextPerDeviceLocale() {
        when(notificationSettingRepository.findByUserId(101L)).thenReturn(Optional.empty());
        when(pushDeviceTokenRepository.findByUserIdInAndEnabledTrue(List.of(101L)))
                .thenReturn(List.of(
                        token(PushPlatform.IOS, "ios-zh", 101L, 7L, "zh-CN"),
                        token(PushPlatform.IOS, "ios-ja", 101L, 7L, "ja")
                ));

        dispatchService.dispatchToUsers(List.of(101L), PushDispatchService.PushCategory.ORDER,
                PushDispatchService.PushText.keyed("title.key", "body.key", "arg1"), Map.of());

        verify(apnsPushService).send(eq("ios-zh"), eq("zh-CN:title.key"), eq("zh-CN:body.key"), anyMap(), anyInt());
        verify(apnsPushService).send(eq("ios-ja"), eq("ja:title.key"), eq("ja:body.key"), anyMap(), anyInt());
    }

    @Test
    void dispatchToUsers_shouldPreferLiteralsOverKeys() {
        when(notificationSettingRepository.findByUserId(101L)).thenReturn(Optional.empty());
        when(pushDeviceTokenRepository.findByUserIdInAndEnabledTrue(List.of(101L)))
                .thenReturn(List.of(token(PushPlatform.IOS, "ios-zh", 101L, 7L, "zh-CN")));

        dispatchService.dispatchToUsers(List.of(101L), PushDispatchService.PushCategory.CHAT,
                new PushDispatchService.PushText(null, "客人A", null, "你好", null), Map.of());

        verify(apnsPushService).send(eq("ios-zh"), eq("客人A"), eq("你好"), anyMap(), anyInt());
    }

    @Test
    void dispatchToUsers_shouldPassBadgeTotalFromBadgeService() {
        when(notificationSettingRepository.findByUserId(101L)).thenReturn(Optional.empty());
        when(pushDeviceTokenRepository.findByUserIdInAndEnabledTrue(List.of(101L)))
                .thenReturn(List.of(token(PushPlatform.IOS, "ios-zh", 101L, 7L, "zh-CN")));
        when(notificationBadgeService.summaryFor(7L, 101L))
                .thenReturn(new NotificationBadgeSummaryDTO(2, 3, 5));

        dispatchService.dispatchToUsers(List.of(101L), PushDispatchService.PushCategory.TASK,
                PushDispatchService.PushText.keyed("title.key", "body.key"), Map.of());

        verify(apnsPushService).send(eq("ios-zh"), anyString(), anyString(), anyMap(), eq(5));
    }

    @Test
    void dispatchToUsers_shouldFallBackToDefaultLocaleWhenTokenLocaleMissing() {
        when(notificationSettingRepository.findByUserId(101L)).thenReturn(Optional.empty());
        when(pushDeviceTokenRepository.findByUserIdInAndEnabledTrue(List.of(101L)))
                .thenReturn(List.of(token(PushPlatform.IOS, "ios-null", 101L, 7L, null)));

        dispatchService.dispatchToUsers(List.of(101L), PushDispatchService.PushCategory.CHAT,
                PushDispatchService.PushText.keyed("title.key", "body.key"), Map.of());

        verify(apnsPushService).send(eq("ios-null"), eq("zh-CN:title.key"), eq("zh-CN:body.key"), anyMap(), anyInt());
    }

    private StoreUser storeUser(Long userId) {
        User user = new User();
        user.setId(userId);
        StoreUser storeUser = new StoreUser();
        storeUser.setUser(user);
        storeUser.setIsActive(true);
        return storeUser;
    }

    private PushDeviceToken token(PushPlatform platform, String deviceToken, Long userId, Long storeId, String locale) {
        PushDeviceToken token = new PushDeviceToken();
        token.setPlatform(platform);
        token.setDeviceToken(deviceToken);
        token.setUserId(userId);
        token.setStoreId(storeId);
        token.setLocale(locale);
        token.setEnabled(true);
        return token;
    }
}
