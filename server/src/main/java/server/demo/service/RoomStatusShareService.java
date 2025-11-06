package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.RoomStatusShareRequest;
import server.demo.dto.RoomStatusShareResponse;
import server.demo.dto.RoomStatusCalendarDTO;
import server.demo.dto.DailyRoomStatusDTO;
import server.demo.entity.Room;
import server.demo.entity.RoomStatusShare;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomStatusShareRepository;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
public class RoomStatusShareService {

    @Autowired
    private RoomStatusShareRepository shareRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Value("${app.frontend.url:http://localhost:8091}")
    private String frontendUrl;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom random = new SecureRandom();

    /**
     * 获取分享列表
     */
    public RoomStatusShareResponse getShares(int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<RoomStatusShare> sharePage = shareRepository.findActiveShares(pageable);

        List<RoomStatusShareResponse.RoomStatusShareDto> shareDtos = sharePage.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return new RoomStatusShareResponse(
                shareDtos,
                (int) sharePage.getTotalElements(),
                page,
                pageSize
        );
    }

    /**
     * 创建分享链接
     */
    @Transactional
    public RoomStatusShare createShare(RoomStatusShareRequest request) {
        // 检查标题是否重复
        if (shareRepository.existsByShareTitle(request.getShareTitle())) {
            throw new RuntimeException("分享标题已存在");
        }

        // 生成唯一的分享token
        String shareToken = generateUniqueToken();

        // 创建分享实体
        RoomStatusShare share = new RoomStatusShare();
        share.setShareTitle(request.getShareTitle());
        share.setShareToken(shareToken);
        share.setShareLink(generateShareLink(shareToken));
        share.setViewRoomStatus(request.getViewRoomStatus());
        share.setQueryMethod(request.getQueryMethod());
        share.setViewType(request.getViewType());
        share.setQueryMode(request.getQueryMode());

        // 转换列表为字符串存储
        if (request.getFilterItems() != null) {
            share.setFilterItems(String.join(",", request.getFilterItems()));
        }
        if (request.getOrderItems() != null) {
            share.setOrderItems(String.join(",", request.getOrderItems()));
        }
        if (request.getAssociatedRooms() != null) {
            share.setAssociatedRoomIds(request.getAssociatedRooms().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));
        }

        return shareRepository.save(share);
    }

    /**
     * 更新分享链接
     */
    @Transactional
    public RoomStatusShare updateShare(Long id, RoomStatusShareRequest request) {
        RoomStatusShare share = shareRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("分享记录不存在"));

        // 检查标题是否重复（排除当前记录）
        if (shareRepository.existsByShareTitleAndIdNot(request.getShareTitle(), id)) {
            throw new RuntimeException("分享标题已存在");
        }

        // 更新字段
        share.setShareTitle(request.getShareTitle());
        share.setViewRoomStatus(request.getViewRoomStatus());
        share.setQueryMethod(request.getQueryMethod());
        share.setViewType(request.getViewType());
        share.setQueryMode(request.getQueryMode());

        // 转换列表为字符串存储
        if (request.getFilterItems() != null) {
            share.setFilterItems(String.join(",", request.getFilterItems()));
        }
        if (request.getOrderItems() != null) {
            share.setOrderItems(String.join(",", request.getOrderItems()));
        }
        if (request.getAssociatedRooms() != null) {
            share.setAssociatedRoomIds(request.getAssociatedRooms().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));
        }

        return shareRepository.save(share);
    }

    /**
     * 删除分享链接
     */
    @Transactional
    public void deleteShare(Long id) {
        RoomStatusShare share = shareRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("分享记录不存在"));
        
        share.setIsActive(false);
        shareRepository.save(share);
    }

    /**
     * 根据token获取分享信息
     */
    public RoomStatusShare getShareByToken(String shareToken) {
        return shareRepository.findByShareToken(shareToken)
                .filter(RoomStatusShare::getIsActive)
                .orElseThrow(() -> new RuntimeException("分享链接不存在或已失效"));
    }

    /**
     * 生成唯一的分享token
     */
    private String generateUniqueToken() {
        String token;
        do {
            token = generateRandomString(32);
        } while (shareRepository.existsByShareToken(token));
        return token;
    }

    /**
     * 生成随机字符串
     */
    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    /**
     * 生成分享链接
     */
    private String generateShareLink(String shareToken) {
        return frontendUrl + "/share/" + shareToken;
    }

    /**
     * 转换为DTO
     */
    private RoomStatusShareResponse.RoomStatusShareDto convertToDto(RoomStatusShare share) {
        // 获取关联房间信息
        String roomNumbers = "";
        int roomCount = 0;

        if (share.getAssociatedRoomIds() != null && !share.getAssociatedRoomIds().isEmpty()) {
            try {
                List<Long> roomIds = Arrays.stream(share.getAssociatedRoomIds().split(","))
                        .map(Long::valueOf)
                        .collect(Collectors.toList());

                List<Room> rooms = roomRepository.findAllById(roomIds);
                roomNumbers = rooms.stream()
                        .map(Room::getRoomNumber)
                        .collect(Collectors.joining(", "));
                roomCount = rooms.size();
            } catch (Exception e) {
                // 处理数据格式错误
                roomNumbers = "数据错误";
                roomCount = 0;
            }
        }

        return new RoomStatusShareResponse.RoomStatusShareDto(share, roomNumbers, roomCount);
    }

    /**
     * 根据分享配置过滤房态数据
     */
    public RoomStatusCalendarDTO filterRoomStatusByShare(RoomStatusShare share, RoomStatusCalendarDTO originalData) {
        // 如果没有关联房间配置，返回所有数据
        if (share.getAssociatedRoomIds() == null || share.getAssociatedRoomIds().isEmpty()) {
            return filterByViewType(share, originalData);
        }

        try {
            // 解析关联房间ID列表
            Set<Long> allowedRoomIds = Arrays.stream(share.getAssociatedRoomIds().split(","))
                    .map(String::trim)
                    .map(Long::valueOf)
                    .collect(Collectors.toSet());

            // 过滤房间数据
            List<RoomStatusCalendarDTO.CalendarRoomDataDTO> filteredRooms = originalData.getRooms().stream()
                    .filter(roomData -> allowedRoomIds.contains(roomData.getRoomId()))
                    .collect(Collectors.toList());

            // 创建新的过滤后的数据对象
            RoomStatusCalendarDTO filteredData = new RoomStatusCalendarDTO(
                    originalData.getDateRange(),
                    filteredRooms
            );

            // 根据查看类型进一步过滤
            return filterByViewType(share, filteredData);
        } catch (Exception e) {
            // 如果解析失败，返回原始数据
            System.err.println("解析关联房间ID失败: " + e.getMessage());
            return filterByViewType(share, originalData);
        }
    }

    /**
     * 根据查看类型过滤敏感信息
     */
    private RoomStatusCalendarDTO filterByViewType(RoomStatusShare share, RoomStatusCalendarDTO data) {
        // 如果是模糊查看模式，需要隐藏敏感信息
        if ("blurred".equals(share.getViewType())) {
            List<RoomStatusCalendarDTO.CalendarRoomDataDTO> blurredRooms = data.getRooms().stream()
                    .map(this::blurSensitiveInfo)
                    .collect(Collectors.toList());

            return new RoomStatusCalendarDTO(data.getDateRange(), blurredRooms);
        }
        
        return data;
    }

    /**
     * 模糊化敏感信息
     */
    private RoomStatusCalendarDTO.CalendarRoomDataDTO blurSensitiveInfo(
            RoomStatusCalendarDTO.CalendarRoomDataDTO roomData) {
        
        List<DailyRoomStatusDTO> blurredDailyStatus = roomData.getDailyStatus().stream()
                .map(dailyStatus -> {
                    DailyRoomStatusDTO.ReservationInfoDTO reservationInfo = dailyStatus.getReservation();
                    
                    // 如果有预订信息且房间状态为occupied或reserved，则模糊化客人姓名
                    if (reservationInfo != null && 
                        (dailyStatus.getStatus().name().equals("OCCUPIED") || 
                         dailyStatus.getStatus().name().equals("RESERVED"))) {
                        
                        String blurredGuestName = blurGuestName(reservationInfo.getGuestName());
                        
                        DailyRoomStatusDTO.ReservationInfoDTO blurredInfo = 
                            new DailyRoomStatusDTO.ReservationInfoDTO(
                                reservationInfo.getId(),
                                blurredGuestName,
                                reservationInfo.getChannel(),
                                reservationInfo.getCheckIn(),
                                reservationInfo.getCheckOut(),
                                reservationInfo.getOrderNumber()
                            );
                        
                        return new DailyRoomStatusDTO(
                            dailyStatus.getDate(),
                            dailyStatus.getStatus(),
                            blurredInfo
                        );
                    }
                    
                    return dailyStatus;
                })
                .collect(Collectors.toList());
        
        return new RoomStatusCalendarDTO.CalendarRoomDataDTO(
            roomData.getRoomId(),
            roomData.getRoomNumber(),
            roomData.getRoomType(),
            blurredDailyStatus
        );
    }

    /**
     * 模糊化客人姓名
     */
    private String blurGuestName(String guestName) {
        if (guestName == null || guestName.length() <= 1) {
            return "***";
        }
        
        return guestName.charAt(0) + "**";
    }
}