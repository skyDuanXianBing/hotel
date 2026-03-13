package server.demo.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import server.demo.repository.StoreRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SuImageSyncServiceTest {

    @Test
    void toSuImageUrl_shouldConvertRelativeMediaPathToPublicUrl() {
        SuImageSyncService service = new SuImageSyncService(
                Mockito.mock(StoreRepository.class),
                Mockito.mock(SuApiClient.class),
                Mockito.mock(SuAccessTokenService.class),
                "https://3d6fae0f.r9.cpolar.top"
        );

        String result = service.toSuImageUrl("/media/1/store-desktop/demo.jpg");

        assertEquals("https://3d6fae0f.r9.cpolar.top/media/1/store-desktop/demo.jpg", result);
    }

    @Test
    void toSuImageUrl_shouldRejectLocalhostUrl() {
        SuImageSyncService service = new SuImageSyncService(
                Mockito.mock(StoreRepository.class),
                Mockito.mock(SuApiClient.class),
                Mockito.mock(SuAccessTokenService.class),
                "https://3d6fae0f.r9.cpolar.top"
        );

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.toSuImageUrl("http://localhost:8092/media/1/store-desktop/demo.jpg"));

        assertEquals(
                "图片地址不能使用本地回环地址，请使用可公网访问的后端域名: http://localhost:8092/media/1/store-desktop/demo.jpg",
                exception.getMessage()
        );
    }
}
