package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import server.demo.repository.ReservationRepository;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OtaReservationInventoryLockTest {

    @Test
    void lockInventoryForUpsert_shouldUseSharedRoomTypeLockForUnassignedOtaStay()
            throws Exception {
        ReservationRepository reservationRepository = repository(
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByStoreIdAndOrderNumberWithRoomType",
                         "findByStoreIdAndExternalBookingKeyWithRoomType",
                         "findByStoreIdAndChannelOrderNumberWithRoomType" -> List.of();
                    case "findByStoreIdAndSuReservationIdAndRoomReservationId" ->
                            Optional.empty();
                    default -> objectMethodOrFail(proxy, method, args);
                }
        );
        RecordingInventoryLockService inventoryLockService =
                new RecordingInventoryLockService();
        OtaReservationSyncService service = new OtaReservationSyncService(
                null,
                null,
                null,
                null,
                null,
                reservationRepository,
                null,
                null,
                inventoryLockService,
                new NoopTransactionManager(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        JsonNode reservation = new ObjectMapper().readTree("""
                {
                  "id": "BOOKING-1",
                  "channel_booking_id": "BOOKING-1",
                  "affiliation": { "OTA_Code": "19" },
                  "rooms": [
                    {
                      "id": "65",
                      "roomreservation_id": "ROOM-1"
                    }
                  ]
                }
                """);

        Set<Long> locked = service.lockInventoryForUpsert(26L, List.of(reservation));

        assertEquals(Set.of(65L), locked);
        assertEquals(List.of(Set.of(65L)), inventoryLockService.calls);
    }

    private static final class RecordingInventoryLockService
            extends RoomTypeInventoryLockService {

        private final List<Set<Long>> calls = new ArrayList<>();

        private RecordingInventoryLockService() {
            super(null);
        }

        @Override
        public Set<Long> lockRoomTypes(Long storeId, Collection<Long> roomTypeIds) {
            assertEquals(26L, storeId);
            Set<Long> locked = Set.copyOf(roomTypeIds);
            calls.add(locked);
            return locked;
        }
    }

    @SuppressWarnings("unchecked")
    private static ReservationRepository repository(InvocationHandler handler) {
        return (ReservationRepository) Proxy.newProxyInstance(
                ReservationRepository.class.getClassLoader(),
                new Class<?>[]{ReservationRepository.class},
                handler
        );
    }

    private static Object objectMethodOrFail(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "toString" -> "ReservationRepositoryProxy";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> throw new AssertionError("Unexpected repository method: " + method);
        };
    }

    private static final class NoopTransactionManager implements PlatformTransactionManager {

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition)
                throws TransactionException {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) throws TransactionException {
        }

        @Override
        public void rollback(TransactionStatus status) throws TransactionException {
        }
    }
}
