package server.demo.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.dto.RoomGroupDTO;
import server.demo.dto.RoomGroupMemberDTO;
import server.demo.dto.RoomGroupWithMembersDTO;
import server.demo.entity.RoomGroup;
import server.demo.entity.RoomGroupMember;
import server.demo.service.RoomGroupService;

import java.util.List;

import server.demo.i18n.ApiMessages;
@RestController
@RequestMapping("/api/v1/room-groups")
@StoreScoped
public class RoomGroupController extends BaseStoreController {

    @Autowired
    private RoomGroupService roomGroupService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomGroup>>> getAll() {
        try {
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.1cd85afc1204"), roomGroupService.getAllForCurrentStore()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.fe70c4153364") + e.getMessage()));
        }
    }

    @GetMapping("/with-members")
    public ResponseEntity<ApiResponse<List<RoomGroupWithMembersDTO>>> getAllWithMembers() {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    ApiMessages.get("api.t.6419fbfbcbdd"),
                    roomGroupService.getAllWithMembersForCurrentStore()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.a316b356cc3a") + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomGroup>> getById(@PathVariable Long id) {
        try {
            RoomGroup group = roomGroupService.getById(id);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.e8633229ad44"), group));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.291e7122e3be") + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoomGroup>> create(@Valid @RequestBody RoomGroupDTO dto) {
        try {
            RoomGroup group = new RoomGroup();
            group.setName(dto.getName());
            group.setDescription(dto.getDescription());
            RoomGroup created = roomGroupService.create(group);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.f014b50e1f6f"), created));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.05b044055d4b") + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomGroup>> update(@PathVariable Long id,
                                                         @Valid @RequestBody RoomGroupDTO dto) {
        try {
            RoomGroup updates = new RoomGroup();
            updates.setName(dto.getName());
            updates.setDescription(dto.getDescription());
            RoomGroup updated = roomGroupService.update(id, updates);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.c5b4569cc2bd"), updated));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.3f60ceb2727f") + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            roomGroupService.delete(id);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.c739bdb82eaf"), null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.477e7fb15eb7") + e.getMessage()));
        }
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<ApiResponse<List<RoomGroupMember>>> getGroupMembers(@PathVariable Long id) {
        try {
            List<RoomGroupMember> members = roomGroupService.getGroupMembers(id);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.9cb66a69f003"), members));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.6b21e9e8fc8d") + e.getMessage()));
        }
    }

    @PostMapping("/{id}/members/{roomId}")
    public ResponseEntity<ApiResponse<RoomGroupMember>> addRoomToGroup(@PathVariable Long id,
                                                                       @PathVariable Long roomId) {
        try {
            RoomGroupMember member = roomGroupService.addRoomToGroup(id, roomId);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.aed74ce4fee8"), member));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.7839066362bd") + e.getMessage()));
        }
    }

    @PostMapping("/{id}/members/batch")
    public ResponseEntity<ApiResponse<Void>> addRoomsToGroup(@PathVariable Long id,
                                                             @Valid @RequestBody RoomGroupMemberDTO dto) {
        try {
            roomGroupService.addRoomsToGroup(id, dto.getRoomIds());
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.303c97ac57fb"), null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.e0ba273291c2") + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/members/{roomId}")
    public ResponseEntity<ApiResponse<Void>> removeRoomFromGroup(@PathVariable Long id,
                                                                 @PathVariable Long roomId) {
        try {
            roomGroupService.removeRoomFromGroup(id, roomId);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.5036f2dbfe85"), null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.9496894b9c4e") + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/members/batch")
    public ResponseEntity<ApiResponse<Void>> removeRoomsFromGroup(@PathVariable Long id,
                                                                  @Valid @RequestBody RoomGroupMemberDTO dto) {
        try {
            roomGroupService.removeRoomsFromGroup(id, dto.getRoomIds());
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.c46fd1c774b6"), null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.53cc100dff4e") + e.getMessage()));
        }
    }
}
