package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.RoomGroupWithMembersDTO;
import server.demo.entity.RoomGroup;
import server.demo.entity.RoomGroupMember;
import server.demo.repository.RoomGroupMemberRepository;
import server.demo.repository.RoomGroupRepository;
import server.demo.repository.RoomRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoomGroupServiceWithMembersTest {

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void getAllWithMembersForCurrentStore_shouldGroupMembersInTwoQueries() {
        StoreContextHolder.setContext(new StoreContext(1L, 26L, "OWNER"));

        RoomGroupRepository roomGroupRepository = mock(RoomGroupRepository.class);
        RoomGroupMemberRepository roomGroupMemberRepository = mock(RoomGroupMemberRepository.class);
        RoomRepository roomRepository = mock(RoomRepository.class);

        RoomGroup groupA = new RoomGroup();
        groupA.setId(1L);
        groupA.setName("A");
        RoomGroup groupB = new RoomGroup();
        groupB.setId(2L);
        groupB.setName("B");

        RoomGroupMember memberA1 = new RoomGroupMember(1L, 101L, 26L);
        RoomGroupMember memberA2 = new RoomGroupMember(1L, 102L, 26L);
        RoomGroupMember memberB1 = new RoomGroupMember(2L, 201L, 26L);

        when(roomGroupRepository.findByStoreId(26L)).thenReturn(List.of(groupA, groupB));
        when(roomGroupMemberRepository.findByStoreId(26L))
                .thenReturn(List.of(memberA1, memberA2, memberB1));

        RoomGroupService service = new RoomGroupService();
        ReflectionTestUtils.setField(service, "roomGroupRepository", roomGroupRepository);
        ReflectionTestUtils.setField(service, "roomGroupMemberRepository", roomGroupMemberRepository);
        ReflectionTestUtils.setField(service, "roomRepository", roomRepository);

        List<RoomGroupWithMembersDTO> result = service.getAllWithMembersForCurrentStore();

        assertEquals(2, result.size());
        assertEquals("A", result.get(0).getName());
        assertEquals(2, result.get(0).getMembers().size());
        assertEquals("B", result.get(1).getName());
        assertEquals(1, result.get(1).getMembers().size());
        assertEquals(201L, result.get(1).getMembers().get(0).getRoomId());
    }

    @Test
    void getAllWithMembersForCurrentStore_shouldReturnEmptyMembersWhenGroupHasNone() {
        StoreContextHolder.setContext(new StoreContext(1L, 26L, "OWNER"));

        RoomGroupRepository roomGroupRepository = mock(RoomGroupRepository.class);
        RoomGroupMemberRepository roomGroupMemberRepository = mock(RoomGroupMemberRepository.class);

        RoomGroup group = new RoomGroup();
        group.setId(3L);
        group.setName("Empty");
        when(roomGroupRepository.findByStoreId(26L)).thenReturn(List.of(group));
        when(roomGroupMemberRepository.findByStoreId(26L)).thenReturn(List.of());

        RoomGroupService service = new RoomGroupService();
        ReflectionTestUtils.setField(service, "roomGroupRepository", roomGroupRepository);
        ReflectionTestUtils.setField(service, "roomGroupMemberRepository", roomGroupMemberRepository);
        ReflectionTestUtils.setField(service, "roomRepository", mock(RoomRepository.class));

        List<RoomGroupWithMembersDTO> result = service.getAllWithMembersForCurrentStore();

        assertEquals(1, result.size());
        assertTrue(result.get(0).getMembers().isEmpty());
    }
}
