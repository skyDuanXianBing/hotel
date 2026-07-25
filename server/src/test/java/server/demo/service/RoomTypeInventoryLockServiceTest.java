package server.demo.service;

import org.junit.jupiter.api.Test;
import server.demo.entity.RoomType;
import server.demo.repository.RoomTypeRepository;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoomTypeInventoryLockServiceTest {

    @Test
    void lockRoomTypes_shouldDeduplicateAndAcquireRowsInAscendingIdOrder() {
        List<Long> lockOrder = new ArrayList<>();
        RoomTypeRepository repository = repository((proxy, method, args) -> {
            if ("findByStoreIdAndIdForUpdate".equals(method.getName())) {
                assertEquals(7L, args[0]);
                Long roomTypeId = (Long) args[1];
                lockOrder.add(roomTypeId);
                RoomType roomType = new RoomType();
                roomType.setId(roomTypeId);
                roomType.setStoreId(7L);
                return Optional.of(roomType);
            }
            return objectMethodOrFail(proxy, method, args);
        });

        Set<Long> locked = new RoomTypeInventoryLockService(repository)
                .lockRoomTypes(7L, List.of(9L, 3L, 5L, 3L));

        assertEquals(List.of(3L, 5L, 9L), lockOrder);
        assertEquals(Set.of(3L, 5L, 9L), locked);
    }

    @Test
    void lockRoomTypes_shouldFailClosedWhenRoomTypeIsOutsideStore() {
        RoomTypeRepository repository = repository((proxy, method, args) -> {
            if ("findByStoreIdAndIdForUpdate".equals(method.getName())) {
                return Optional.empty();
            }
            return objectMethodOrFail(proxy, method, args);
        });

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new RoomTypeInventoryLockService(repository)
                        .lockRoomTypes(7L, List.of(11L))
        );

        assertEquals(
                "房型不存在或不属于当前门店，roomTypeId=11",
                exception.getMessage()
        );
    }

    @SuppressWarnings("unchecked")
    private static RoomTypeRepository repository(InvocationHandler handler) {
        return (RoomTypeRepository) Proxy.newProxyInstance(
                RoomTypeRepository.class.getClassLoader(),
                new Class<?>[]{RoomTypeRepository.class},
                handler
        );
    }

    private static Object objectMethodOrFail(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "toString" -> "RoomTypeRepositoryProxy";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> throw new AssertionError("Unexpected repository method: " + method);
        };
    }
}
