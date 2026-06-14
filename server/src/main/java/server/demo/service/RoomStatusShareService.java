package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.RoomStatusShareRequest;
import server.demo.dto.RoomStatusSharePublicResponse;
import server.demo.dto.RoomStatusShareResponse;
import server.demo.dto.RoomStatusCalendarDTO;
import server.demo.dto.DailyRoomStatusDTO;
import server.demo.entity.Room;
import server.demo.entity.RoomStatusShare;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomStatusShareRepository;
import server.demo.util.StoreContextUtils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
        Long storeId = StoreContextUtils.requireStoreId();
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<RoomStatusShare> sharePage = shareRepository.findActiveSharesByStoreId(storeId, pageable);

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
        Long storeId = StoreContextUtils.requireStoreId();
        Long userId = StoreContextUtils.requireUserId();

        // 检查标题是否重复
        if (shareRepository.existsByStoreIdAndShareTitle(storeId, request.getShareTitle())) {
            throw new RuntimeException("分享标题已存在");
        }

        // 生成唯一的分享token
        String shareToken = generateUniqueToken();

        // 创建分享实体
        RoomStatusShare share = new RoomStatusShare();
        share.setStoreId(storeId);
        share.setUserId(userId);
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
            share.setAssociatedRoomIds(serializeAssociatedRoomIds(storeId, request.getAssociatedRooms()));
        }

        return shareRepository.save(share);
    }

    /**
     * 更新分享链接
     */
    @Transactional
    public RoomStatusShare updateShare(Long id, RoomStatusShareRequest request) {
        Long storeId = StoreContextUtils.requireStoreId();
        RoomStatusShare share = shareRepository.findByStoreIdAndId(storeId, id)
                .orElseThrow(() -> new RuntimeException("分享记录不存在"));

        // 检查标题是否重复（排除当前记录）
        if (shareRepository.existsByStoreIdAndShareTitleAndIdNot(storeId, request.getShareTitle(), id)) {
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
            share.setAssociatedRoomIds(serializeAssociatedRoomIds(storeId, request.getAssociatedRooms()));
        }

        return shareRepository.save(share);
    }

    /**
     * 删除分享链接
     */
    @Transactional
    public void deleteShare(Long id) {
        Long storeId = StoreContextUtils.requireStoreId();
        RoomStatusShare share = shareRepository.findByStoreIdAndId(storeId, id)
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
     * 根据token获取公开分享信息
     */
    public RoomStatusSharePublicResponse getPublicShareByToken(String shareToken) {
        RoomStatusShare share = getShareByToken(shareToken);
        return new RoomStatusSharePublicResponse(share);
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

                List<Room> rooms = share.getStoreId() != null
                        ? roomRepository.findByStoreIdAndIdIn(share.getStoreId(), roomIds)
                        : List.of();
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
        return sanitizeForPublicShare(share, data);
    }

    private String serializeAssociatedRoomIds(Long storeId, List<Long> associatedRooms) {
        LinkedHashSet<Long> normalizedRoomIds = new LinkedHashSet<>();
        for (Long roomId : associatedRooms) {
            if (roomId != null) {
                normalizedRoomIds.add(roomId);
            }
        }
        if (normalizedRoomIds.isEmpty()) {
            return "";
        }

        List<Room> rooms = roomRepository.findByStoreIdAndIdIn(storeId, normalizedRoomIds);
        Set<Long> foundRoomIds = rooms.stream()
                .map(Room::getId)
                .collect(Collectors.toSet());
        if (foundRoomIds.size() != normalizedRoomIds.size()) {
            throw new RuntimeException("部分关联房间不存在或无权限");
        }

        return normalizedRoomIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    /**
     * 公开分享只输出展示白名单，避免复用内部日历 DTO 泄露订单号、备注、金额等字段。
     */
    private RoomStatusCalendarDTO sanitizeForPublicShare(RoomStatusShare share, RoomStatusCalendarDTO data) {
        if (data == null) {
            return null;
        }

        List<RoomStatusCalendarDTO.CalendarRoomDataDTO> publicRooms = new ArrayList<>();
        if (data.getRooms() != null) {
            long publicRoomId = 1L;
            for (RoomStatusCalendarDTO.CalendarRoomDataDTO roomData : data.getRooms()) {
                if (roomData == null) {
                    continue;
                }
                publicRooms.add(sanitizeRoomForPublicShare(share, roomData, publicRoomId));
                publicRoomId++;
            }
        }

        return new RoomStatusCalendarDTO(data.getDateRange(), publicRooms);
    }

    private RoomStatusCalendarDTO.CalendarRoomDataDTO sanitizeRoomForPublicShare(
            RoomStatusShare share,
            RoomStatusCalendarDTO.CalendarRoomDataDTO roomData,
            Long publicRoomId) {

        List<DailyRoomStatusDTO> publicDailyStatus = new ArrayList<>();
        if (roomData.getDailyStatus() != null) {
            for (DailyRoomStatusDTO dailyStatus : roomData.getDailyStatus()) {
                if (dailyStatus == null) {
                    continue;
                }
                publicDailyStatus.add(sanitizeDailyStatusForPublicShare(share, dailyStatus));
            }
        }

        return new RoomStatusCalendarDTO.CalendarRoomDataDTO(
            publicRoomId,
            roomData.getRoomNumber(),
            roomData.getRoomType(),
            publicDailyStatus
        );
    }

    private DailyRoomStatusDTO sanitizeDailyStatusForPublicShare(RoomStatusShare share, DailyRoomStatusDTO dailyStatus) {
        return new DailyRoomStatusDTO(
                dailyStatus.getDate(),
                dailyStatus.getStatus(),
                sanitizeReservationForPublicShare(share, dailyStatus),
                dailyStatus.getClosed(),
                dailyStatus.getCloseType(),
                null
        );
    }

    private DailyRoomStatusDTO.ReservationInfoDTO sanitizeReservationForPublicShare(
            RoomStatusShare share,
            DailyRoomStatusDTO dailyStatus) {
        DailyRoomStatusDTO.ReservationInfoDTO reservationInfo = dailyStatus.getReservation();
        if (reservationInfo == null) {
            return null;
        }

        String guestName = reservationInfo.getGuestName();
        if (isBlurredView(share)) {
            guestName = blurGuestName(guestName);
        }

        DailyRoomStatusDTO.ReservationInfoDTO publicInfo = new DailyRoomStatusDTO.ReservationInfoDTO();
        publicInfo.setGuestName(guestName);
        publicInfo.setChannel(reservationInfo.getChannel());
        publicInfo.setCheckIn(reservationInfo.getCheckIn());
        publicInfo.setCheckOut(reservationInfo.getCheckOut());
        publicInfo.setStatus(reservationInfo.getStatus());
        return publicInfo;
    }

    private boolean isBlurredView(RoomStatusShare share) {
        return share != null && "blurred".equals(share.getViewType());
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
