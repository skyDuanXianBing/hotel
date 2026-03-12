package server.demo.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.entity.StoreUser;
import server.demo.exception.SuPropertyDeleteFailedException;
import server.demo.repository.StoreUserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StoreServiceDeleteStoreWithSuTest {

    @Test
    void deleteStoreWithSuRemoveProperty_shouldNotSoftDelete_whenSuReturns953() {
        StoreUserRepository storeUserRepository = Mockito.mock(StoreUserRepository.class);
        SuPropertyService suPropertyService = Mockito.mock(SuPropertyService.class);

        StoreService service = new StoreService();
        ReflectionTestUtils.setField(service, "storeUserRepository", storeUserRepository);
        ReflectionTestUtils.setField(service, "suPropertyService", suPropertyService);

        StoreUser operator = new StoreUser();
        operator.setRole("owner");

        when(storeUserRepository.findByStoreIdAndUserId(7L, 1L)).thenReturn(Optional.of(operator));
        when(suPropertyService.removeStoreProperty(7L, false)).thenReturn(
                new SuPropertyService.RemoveResult(
                        true,
                        false,
                        "KC",
                        "953",
                        "This Property Have Mapping With Channels"
                )
        );

        SuPropertyDeleteFailedException ex = assertThrows(
                SuPropertyDeleteFailedException.class,
                () -> service.deleteStoreWithSuRemoveProperty(7L, 1L)
        );
        assertEquals("953", ex.getErrorCode());
        verify(storeUserRepository, never()).saveAll(Mockito.anyList());
    }

    @Test
    void deleteStoreWithSuRemoveProperty_shouldSoftDelete_whenSuDeleteSuccess() {
        StoreUserRepository storeUserRepository = Mockito.mock(StoreUserRepository.class);
        SuPropertyService suPropertyService = Mockito.mock(SuPropertyService.class);

        StoreService service = new StoreService();
        ReflectionTestUtils.setField(service, "storeUserRepository", storeUserRepository);
        ReflectionTestUtils.setField(service, "suPropertyService", suPropertyService);

        StoreUser operator = new StoreUser();
        operator.setRole("owner");
        operator.setIsActive(true);

        StoreUser member = new StoreUser();
        member.setRole("member");
        member.setIsActive(true);

        when(storeUserRepository.findByStoreIdAndUserId(7L, 1L)).thenReturn(Optional.of(operator));
        when(suPropertyService.removeStoreProperty(7L, false)).thenReturn(
                new SuPropertyService.RemoveResult(true, true, "KC", null, "ok")
        );
        when(storeUserRepository.findByStoreId(7L)).thenReturn(List.of(operator, member));

        service.deleteStoreWithSuRemoveProperty(7L, 1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StoreUser>> captor = (ArgumentCaptor<List<StoreUser>>) (ArgumentCaptor<?>)
                ArgumentCaptor.forClass(List.class);
        verify(storeUserRepository).saveAll(captor.capture());

        List<StoreUser> saved = captor.getValue();
        assertEquals(2, saved.size());
        assertFalse(saved.get(0).getIsActive());
        assertFalse(saved.get(1).getIsActive());
    }
}

