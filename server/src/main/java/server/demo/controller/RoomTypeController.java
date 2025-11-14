package server.demo.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.dto.CreateRoomTypeRequest;
import server.demo.dto.RoomTypeWithRoomsDTO;
import server.demo.entity.RoomType;
import server.demo.service.RoomTypeService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/room-types")
@CrossOrigin
@StoreScoped
public class RoomTypeController extends BaseStoreController {

    @Autowired
    private RoomTypeService roomTypeService;

    @GetMapping
    public ApiResponse<List<RoomType>> getAllRoomTypes() {
        try {
            return ApiResponse.success(roomTypeService.getAllRoomTypes());
        } catch (Exception e) {
            return ApiResponse.error("获取房型列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/with-rooms")
    public ApiResponse<List<RoomTypeWithRoomsDTO>> getAllRoomTypesWithRooms() {
        try {
            return ApiResponse.success(roomTypeService.getAllRoomTypesWithRooms());
        } catch (Exception e) {
            return ApiResponse.error("获取房型及房间列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<RoomType> getRoomTypeById(@PathVariable Long id) {
        try {
            return roomTypeService.getRoomTypeById(id)
                    .map(ApiResponse::success)
                    .orElse(ApiResponse.error("房型不存在"));
        } catch (Exception e) {
            return ApiResponse.error("获取房型信息失败: " + e.getMessage());
        }
    }

    @PostMapping
    public ApiResponse<RoomType> createRoomType(@Valid @RequestBody CreateRoomTypeRequest request) {
        try {
            RoomType roomType = new RoomType(request.getName(), request.getCode(),
                    request.getTotalRooms(), request.getDescription());

            roomType.setDefaultPrice(request.getDefaultPrice());
            roomType.setWeekdayPrice(request.getWeekdayPrice());
            roomType.setWeekendPrice(request.getWeekendPrice());
            roomType.setMondayPrice(request.getMondayPrice());
            roomType.setTuesdayPrice(request.getTuesdayPrice());
            roomType.setWednesdayPrice(request.getWednesdayPrice());
            roomType.setThursdayPrice(request.getThursdayPrice());
            roomType.setFridayPrice(request.getFridayPrice());
            roomType.setSaturdayPrice(request.getSaturdayPrice());
            roomType.setSundayPrice(request.getSundayPrice());

            RoomType createdRoomType = roomTypeService.createRoomTypeWithRooms(roomType, request.getRoomNumbers());
            return ApiResponse.success("房型创建成功", createdRoomType);
        } catch (Exception e) {
            return ApiResponse.error("房型创建失败: " + e.getMessage());
        }
    }

    @PostMapping("/simple")
    public ApiResponse<RoomType> createSimpleRoomType(@Valid @RequestBody RoomType roomType) {
        try {
            return ApiResponse.success("房型创建成功", roomTypeService.createRoomType(roomType));
        } catch (Exception e) {
            return ApiResponse.error("房型创建失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<RoomType> updateRoomType(@PathVariable Long id, @Valid @RequestBody CreateRoomTypeRequest request) {
        try {
            RoomType roomType = new RoomType();
            roomType.setName(request.getName());
            roomType.setCode(request.getCode());
            roomType.setTotalRooms(request.getTotalRooms());
            roomType.setDescription(request.getDescription());
            roomType.setDefaultPrice(request.getDefaultPrice());
            roomType.setWeekdayPrice(request.getWeekdayPrice());
            roomType.setWeekendPrice(request.getWeekendPrice());
            roomType.setMondayPrice(request.getMondayPrice());
            roomType.setTuesdayPrice(request.getTuesdayPrice());
            roomType.setWednesdayPrice(request.getWednesdayPrice());
            roomType.setThursdayPrice(request.getThursdayPrice());
            roomType.setFridayPrice(request.getFridayPrice());
            roomType.setSaturdayPrice(request.getSaturdayPrice());
            roomType.setSundayPrice(request.getSundayPrice());

            RoomType updatedRoomType = roomTypeService.updateRoomTypeWithRooms(id, roomType, request.getRoomNumbers());
            return ApiResponse.success("房型更新成功", updatedRoomType);
        } catch (Exception e) {
            return ApiResponse.error("房型更新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteRoomType(@PathVariable Long id) {
        try {
            roomTypeService.deleteRoomType(id);
            return ApiResponse.success("房型删除成功");
        } catch (DataIntegrityViolationException e) {
            return ApiResponse.error("该房型下的房间还有预订记录，无法删除。请先处理或取消相关订单后再删除。");
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("房型删除失败,请稍后重试");
        }
    }
}
