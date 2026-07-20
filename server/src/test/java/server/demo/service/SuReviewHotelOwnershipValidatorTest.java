package server.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import server.demo.entity.Store;
import server.demo.repository.StoreRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class SuReviewHotelOwnershipValidatorTest {

    private StoreRepository storeRepository;
    private SuReviewHotelOwnershipValidator validator;

    @BeforeEach
    void setUp() {
        storeRepository = Mockito.mock(StoreRepository.class);
        validator = new SuReviewHotelOwnershipValidator(storeRepository);
    }

    @Test
    void returnsHotelIdOnlyWhenItUniquelyBelongsToCurrentStore() {
        Store store = store(10L, "HOTEL1");
        when(storeRepository.findById(10L)).thenReturn(Optional.of(store));
        when(storeRepository.findAllBySuHotelIdOrderByIdAsc("HOTEL1")).thenReturn(List.of(store));

        assertEquals("HOTEL1", validator.requireUniqueOwnership(10L));
    }

    @Test
    void duplicateHotelIdAcrossStoresFailsClosed() {
        Store current = store(10L, "HOTEL1");
        Store other = store(11L, "HOTEL1");
        when(storeRepository.findById(10L)).thenReturn(Optional.of(current));
        when(storeRepository.findAllBySuHotelIdOrderByIdAsc("HOTEL1"))
                .thenReturn(List.of(current, other));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> validator.requireUniqueOwnership(10L)
        );

        assertEquals(
                "Su hotel_id 未唯一归属当前门店，已拒绝评价渠道操作",
                error.getMessage()
        );
    }

    @Test
    void hotelOwnedByAnotherStoreFailsClosed() {
        Store current = store(10L, "HOTEL1");
        Store other = store(11L, "HOTEL1");
        when(storeRepository.findById(10L)).thenReturn(Optional.of(current));
        when(storeRepository.findAllBySuHotelIdOrderByIdAsc("HOTEL1")).thenReturn(List.of(other));

        assertThrows(
                IllegalStateException.class,
                () -> validator.requireUniqueOwnership(10L)
        );
    }

    private static Store store(Long id, String hotelId) {
        Store store = new Store();
        store.setId(id);
        store.setSuHotelId(hotelId);
        return store;
    }
}
