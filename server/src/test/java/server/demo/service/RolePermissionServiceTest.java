package server.demo.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.demo.dto.PermissionDTO;
import server.demo.entity.Role;
import server.demo.entity.RolePermission;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.repository.RolePermissionRepository;
import server.demo.repository.RoleRepository;
import server.demo.repository.RoomTypeRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RolePermissionServiceTest {

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @InjectMocks
    private RolePermissionService rolePermissionService;

    @Test
    void updateRolePermissions_normalizesRoomTypeIdAndDedupes() {
        Role role = new Role();
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(rolePermissionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PermissionDTO viewRoomStatusAll = new PermissionDTO();
        viewRoomStatusAll.setModule(PermissionModule.ACCOMMODATION);
        viewRoomStatusAll.setAction(PermissionAction.VIEW_ROOM_STATUS);
        viewRoomStatusAll.setAllRoomTypes(true);

        PermissionDTO viewRoomStatusRoomType2 = new PermissionDTO();
        viewRoomStatusRoomType2.setModule(PermissionModule.ACCOMMODATION);
        viewRoomStatusRoomType2.setAction(PermissionAction.VIEW_ROOM_STATUS);
        viewRoomStatusRoomType2.setRoomTypeId(2L);

        PermissionDTO editRoomStatus = new PermissionDTO();
        editRoomStatus.setModule(PermissionModule.ACCOMMODATION);
        editRoomStatus.setAction(PermissionAction.EDIT_ROOM_STATUS);

        // Include duplicates and conflicting scope (ALL should win)
        List<PermissionDTO> request = List.of(
                viewRoomStatusRoomType2,
                viewRoomStatusRoomType2,
                viewRoomStatusAll,
                editRoomStatus,
                editRoomStatus
        );

        rolePermissionService.updateRolePermissions(1L, request);

        verify(rolePermissionRepository).deleteByRoleId(1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<RolePermission>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(rolePermissionRepository).saveAll(captor.capture());

        List<RolePermission> saved = StreamSupport.stream(captor.getValue().spliterator(), false).toList();
        assertThat(saved).hasSize(2);

        RolePermission savedViewRoomStatus = saved.stream()
                .filter(p -> p.getModule() == PermissionModule.ACCOMMODATION && p.getAction() == PermissionAction.VIEW_ROOM_STATUS)
                .findFirst()
                .orElseThrow();
        assertThat(savedViewRoomStatus.getAllRoomTypes()).isTrue();
        assertThat(savedViewRoomStatus.getRoomTypeId()).isEqualTo(0L);

        RolePermission savedEditRoomStatus = saved.stream()
                .filter(p -> p.getModule() == PermissionModule.ACCOMMODATION && p.getAction() == PermissionAction.EDIT_ROOM_STATUS)
                .findFirst()
                .orElseThrow();
        assertThat(savedEditRoomStatus.getAllRoomTypes()).isFalse();
        assertThat(savedEditRoomStatus.getRoomTypeId()).isEqualTo(0L);
    }
}
