package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.entity.Room;
import server.demo.repository.RoomRepository;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
@CrossOrigin
@StoreScoped
public class RoomController extends BaseStoreController {

    @Autowired
    private RoomRepository roomRepository;

    @GetMapping
    public ApiResponse<List<Room>> getRooms(
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        try {
            Long storeId = currentStoreId();
            List<Room> rooms;

            if (roomTypeId != null) {
                rooms = roomRepository.findByStoreIdAndRoomTypeId(storeId, roomTypeId);
            } else {
                rooms = roomRepository.findByStoreIdWithRoomType(storeId);
            }

            return ApiResponse.success(rooms);
        } catch (Exception e) {
            return ApiResponse.error("获取房间列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<Room> getRoomById(@PathVariable Long id) {
        try {
            return roomRepository.findByStoreIdAndId(currentStoreId(), id)
                    .map(ApiResponse::success)
                    .orElse(ApiResponse.error("房间不存在"));
        } catch (Exception e) {
            return ApiResponse.error("获取房间信息失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/room-type")
    public ApiResponse<server.demo.entity.RoomType> getRoomTypeByRoomId(@PathVariable Long id) {
        try {
            return roomRepository.findByStoreIdAndIdWithRoomType(currentStoreId(), id)
                    .map(room -> ApiResponse.success("获取房型信息成功", room.getRoomType()))
                    .orElse(ApiResponse.error("房间不存在"));
        } catch (Exception e) {
            return ApiResponse.error("获取房型信息失败: " + e.getMessage());
        }
    }
}
