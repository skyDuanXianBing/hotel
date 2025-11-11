package server.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;
import server.demo.dto.ApiResponse;
import server.demo.dto.CreateRoomTypeRequest;
import server.demo.dto.RoomTypeWithRoomsDTO;
import server.demo.entity.RoomType;
import server.demo.service.RoomTypeService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/room-types")
@CrossOrigin
public class RoomTypeController {

    @Autowired
    private RoomTypeService roomTypeService;

    @GetMapping
    public ApiResponse<List<RoomType>> getAllRoomTypes(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            List<RoomType> roomTypes = roomTypeService.getAllRoomTypes(userId);
            return ApiResponse.success(roomTypes);
        } catch (Exception e) {
            return ApiResponse.error("获取房型列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/with-rooms")
    public ApiResponse<List<RoomTypeWithRoomsDTO>> getAllRoomTypesWithRooms(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            List<RoomTypeWithRoomsDTO> roomTypes = roomTypeService.getAllRoomTypesWithRooms(userId);
            return ApiResponse.success(roomTypes);
        } catch (Exception e) {
            return ApiResponse.error("获取房型及房间列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<RoomType> getRoomTypeById(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            return roomTypeService.getRoomTypeById(userId, id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error("房型不存在"));
        } catch (Exception e) {
            return ApiResponse.error("获取房型信息失败: " + e.getMessage());
        }
    }

    @PostMapping
    public ApiResponse<RoomType> createRoomType(@Valid @RequestBody CreateRoomTypeRequest request, HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            RoomType roomType = new RoomType(request.getName(), request.getCode(),
                request.getTotalRooms(), request.getDescription());

            // 设置价格字段
            roomType.setDefaultPrice(request.getDefaultPrice());
            roomType.setWeekdayPrice(request.getWeekdayPrice());
            roomType.setWeekendPrice(request.getWeekendPrice());

            // 设置周价格字段
            roomType.setMondayPrice(request.getMondayPrice());
            roomType.setTuesdayPrice(request.getTuesdayPrice());
            roomType.setWednesdayPrice(request.getWednesdayPrice());
            roomType.setThursdayPrice(request.getThursdayPrice());
            roomType.setFridayPrice(request.getFridayPrice());
            roomType.setSaturdayPrice(request.getSaturdayPrice());
            roomType.setSundayPrice(request.getSundayPrice());

            RoomType createdRoomType = roomTypeService.createRoomTypeWithRooms(userId, roomType, request.getRoomNumbers());
            return ApiResponse.success("房型创建成功", createdRoomType);
        } catch (Exception e) {
            return ApiResponse.error("房型创建失败: " + e.getMessage());
        }
    }

    @PostMapping("/simple")
    public ApiResponse<RoomType> createSimpleRoomType(@Valid @RequestBody RoomType roomType, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            RoomType createdRoomType = roomTypeService.createRoomType(userId, roomType);
            return ApiResponse.success("房型创建成功", createdRoomType);
        } catch (Exception e) {
            return ApiResponse.error("房型创建失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<RoomType> updateRoomType(@PathVariable Long id, @Valid @RequestBody CreateRoomTypeRequest request, HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");

            RoomType roomType = new RoomType();
            roomType.setName(request.getName());
            roomType.setCode(request.getCode());
            roomType.setTotalRooms(request.getTotalRooms());
            roomType.setDescription(request.getDescription());

            // 设置价格字段
            roomType.setDefaultPrice(request.getDefaultPrice());
            roomType.setWeekdayPrice(request.getWeekdayPrice());
            roomType.setWeekendPrice(request.getWeekendPrice());

            // 设置周价格字段
            roomType.setMondayPrice(request.getMondayPrice());
            roomType.setTuesdayPrice(request.getTuesdayPrice());
            roomType.setWednesdayPrice(request.getWednesdayPrice());
            roomType.setThursdayPrice(request.getThursdayPrice());
            roomType.setFridayPrice(request.getFridayPrice());
            roomType.setSaturdayPrice(request.getSaturdayPrice());
            roomType.setSundayPrice(request.getSundayPrice());

            RoomType updatedRoomType = roomTypeService.updateRoomTypeWithRooms(userId, id, roomType, request.getRoomNumbers());
            return ApiResponse.success("房型更新成功", updatedRoomType);
        } catch (Exception e) {
            return ApiResponse.error("房型更新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteRoomType(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            roomTypeService.deleteRoomType(userId, id);
            return ApiResponse.success("房型删除成功");
        } catch (DataIntegrityViolationException e) {
            // 外键约束错误,返回友好提示
            return ApiResponse.error("该房型下的房间还有预订记录,无法删除。请先处理或取消相关订单后再删除。");
        } catch (RuntimeException e) {
            // 业务逻辑异常,直接返回错误信息
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            // 其他未知异常
            return ApiResponse.error("房型删除失败,请稍后重试");
        }
    }
}