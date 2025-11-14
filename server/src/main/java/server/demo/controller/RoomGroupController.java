package server.demo.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.dto.RoomGroupDTO;
import server.demo.dto.RoomGroupMemberDTO;
import server.demo.entity.RoomGroup;
import server.demo.entity.RoomGroupMember;
import server.demo.service.RoomGroupService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/room-groups")
@StoreScoped
public class RoomGroupController extends BaseStoreController {

    @Autowired
    private RoomGroupService roomGroupService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomGroup>>> getAll() {
        try {
            return ResponseEntity.ok(ApiResponse.success("获取分组列表成功", roomGroupService.getAllForCurrentStore()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取分组列表失败: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomGroup>> getById(@PathVariable Long id) {
        try {
            RoomGroup group = roomGroupService.getById(id);
            return ResponseEntity.ok(ApiResponse.success("获取分组成功", group));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取分组失败: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoomGroup>> create(@Valid @RequestBody RoomGroupDTO dto) {
        try {
            RoomGroup group = new RoomGroup();
            group.setName(dto.getName());
            group.setDescription(dto.getDescription());
            RoomGroup created = roomGroupService.create(group);
            return ResponseEntity.ok(ApiResponse.success("创建分组成功", created));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("创建分组失败: " + e.getMessage()));
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
            return ResponseEntity.ok(ApiResponse.success("更新分组成功", updated));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("更新分组失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            roomGroupService.delete(id);
            return ResponseEntity.ok(ApiResponse.success("删除分组成功", null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("删除分组失败: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<ApiResponse<List<RoomGroupMember>>> getGroupMembers(@PathVariable Long id) {
        try {
            List<RoomGroupMember> members = roomGroupService.getGroupMembers(id);
            return ResponseEntity.ok(ApiResponse.success("获取分组房间成功", members));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取分组房间失败: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/members/{roomId}")
    public ResponseEntity<ApiResponse<RoomGroupMember>> addRoomToGroup(@PathVariable Long id,
                                                                       @PathVariable Long roomId) {
        try {
            RoomGroupMember member = roomGroupService.addRoomToGroup(id, roomId);
            return ResponseEntity.ok(ApiResponse.success("添加房间成功", member));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("添加房间失败: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/members/batch")
    public ResponseEntity<ApiResponse<Void>> addRoomsToGroup(@PathVariable Long id,
                                                             @Valid @RequestBody RoomGroupMemberDTO dto) {
        try {
            roomGroupService.addRoomsToGroup(id, dto.getRoomIds());
            return ResponseEntity.ok(ApiResponse.success("批量添加房间成功", null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("批量添加房间失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/members/{roomId}")
    public ResponseEntity<ApiResponse<Void>> removeRoomFromGroup(@PathVariable Long id,
                                                                 @PathVariable Long roomId) {
        try {
            roomGroupService.removeRoomFromGroup(id, roomId);
            return ResponseEntity.ok(ApiResponse.success("移除房间成功", null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("移除房间失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/members/batch")
    public ResponseEntity<ApiResponse<Void>> removeRoomsFromGroup(@PathVariable Long id,
                                                                  @Valid @RequestBody RoomGroupMemberDTO dto) {
        try {
            roomGroupService.removeRoomsFromGroup(id, dto.getRoomIds());
            return ResponseEntity.ok(ApiResponse.success("批量移除房间成功", null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("批量移除房间失败: " + e.getMessage()));
        }
    }
}
