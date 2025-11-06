package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import server.demo.dto.ApiResponse;
import server.demo.entity.Room;
import server.demo.repository.RoomRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/rooms")
@CrossOrigin
public class RoomController {

    @Autowired
    private RoomRepository roomRepository;

    @GetMapping
    public ApiResponse<List<Room>> getRooms(
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        try {
            List<Room> rooms;
            
            if (roomTypeId != null) {
                rooms = roomRepository.findByRoomTypeId(roomTypeId);
            } else {
                rooms = roomRepository.findAllWithRoomType();
            }
            
            return ApiResponse.success(rooms);
        } catch (Exception e) {
            return ApiResponse.error("获取房间列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<Room> getRoomById(@PathVariable Long id) {
        try {
            Optional<Room> room = roomRepository.findById(id);
            return room.map(ApiResponse::success)
                      .orElse(ApiResponse.error("房间不存在"));
        } catch (Exception e) {
            return ApiResponse.error("获取房间信息失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/room-type")
    public ApiResponse<server.demo.entity.RoomType> getRoomTypeByRoomId(@PathVariable Long id) {
        try {
            Optional<Room> room = roomRepository.findByIdWithRoomType(id);
            if (room.isPresent()) {
                return ApiResponse.success("获取房型信息成功", room.get().getRoomType());
            } else {
                return ApiResponse.error("房间不存在");
            }
        } catch (Exception e) {
            return ApiResponse.error("获取房型信息失败: " + e.getMessage());
        }
    }
}