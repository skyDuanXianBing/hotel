package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.DailyRoomStatusDTO;
import server.demo.dto.RoomStatusCalendarDTO;
import server.demo.dto.RoomStatusStatisticsDTO;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.enums.ReservationStatus;
import server.demo.enums.RoomStatus;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomRepository;
import server.demo.util.StoreContextUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoomStatusService {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private Long currentStoreId() {
        return StoreContextUtils.requireStoreId();
    }

    public RoomStatusCalendarDTO getRoomStatusCalendar(LocalDate startDate, LocalDate endDate) {
        return getRoomStatusCalendarForStore(currentStoreId(), startDate, endDate);
    }

    public RoomStatusCalendarDTO getRoomStatusCalendarForStore(Long storeId, LocalDate startDate, LocalDate endDate) {
        List<Room> rooms = roomRepository.findByStoreIdWithRoomType(storeId);

        RoomStatusCalendarDTO.DateRangeDTO dateRange = new RoomStatusCalendarDTO.DateRangeDTO(startDate, endDate);
        List<RoomStatusCalendarDTO.CalendarRoomDataDTO> roomDataList = new ArrayList<>();

        for (Room room : rooms) {
            List<DailyRoomStatusDTO> dailyStatusList = new ArrayList<>();
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                RoomStatus status = determineRoomStatus(room, currentDate, storeId);
                DailyRoomStatusDTO.ReservationInfoDTO reservationInfo = getReservationInfo(room, currentDate, storeId);
                dailyStatusList.add(new DailyRoomStatusDTO(currentDate, status, reservationInfo));
                currentDate = currentDate.plusDays(1);
            }

            String roomTypeName = room.getRoomType() != null ? room.getRoomType().getName() : "未知房型";
            RoomStatusCalendarDTO.CalendarRoomDataDTO roomData = new RoomStatusCalendarDTO.CalendarRoomDataDTO(
                    room.getId(),
                    room.getRoomNumber(),
                    roomTypeName,
                    dailyStatusList
            );
            roomDataList.add(roomData);
        }

        return new RoomStatusCalendarDTO(dateRange, roomDataList);
    }

    public void updateRoomStatus(Long roomId, LocalDate date, RoomStatus newStatus, String reason) {
        Long storeId = currentStoreId();
        Room room = roomRepository.findByStoreIdAndId(storeId, roomId)
                .orElseThrow(() -> new RuntimeException("房间不存在或无权限"));

        Optional<Reservation> existingReservation =
                reservationRepository.findByStoreIdAndRoomIdAndDate(storeId, room.getId(), date);

        if (existingReservation.isPresent() &&
                (newStatus == RoomStatus.AVAILABLE || newStatus == RoomStatus.MAINTENANCE)) {
            throw new RuntimeException("该日期已有预订，无法修改为该状态");
        }

        if (isDateToday(date)) {
            room.setStatus(newStatus);
            roomRepository.save(room);
        }

        // TODO: 后续可在此记录房态变更日志
    }

    public RoomStatusStatisticsDTO getRoomStatusStatistics(LocalDate date) {
        return getRoomStatusStatisticsForStore(currentStoreId(), date);
    }

    public RoomStatusStatisticsDTO getRoomStatusStatisticsForStore(Long storeId, LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<Room> storeRooms = roomRepository.findByStoreId(storeId);
        List<Long> roomIds = storeRooms.stream().map(Room::getId).collect(Collectors.toList());

        long todayArrivals = reservationRepository.countTodayArrivalsByStoreId(storeId, targetDate);
        long todayDepartures = reservationRepository.countByStoreIdAndCheckOutDate(storeId, targetDate);
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.plusDays(1).atStartOfDay();
        long todayNewOrders = reservationRepository.countTodayNewOrdersByStoreId(storeId, startOfDay, endOfDay);
        long availableRooms = roomRepository.countAvailableRoomsForDateByStore(storeId, targetDate);
        long unassignedOrders = reservationRepository.countByStoreIdAndRoomIsNull(storeId);
        long pendingOrders = reservationRepository.countPendingOrdersByStoreId(storeId);

        return new RoomStatusStatisticsDTO(
                targetDate,
                todayArrivals,
                todayDepartures,
                todayNewOrders,
                availableRooms,
                unassignedOrders,
                pendingOrders
        );
    }

    private RoomStatus determineRoomStatus(Room room, LocalDate date, Long storeId) {
        Optional<Reservation> reservation = reservationRepository.findByStoreIdAndRoomIdAndDate(storeId, room.getId(), date);
        if (reservation.isPresent()) {
            ReservationStatus reservationStatus = reservation.get().getStatus();
            if (reservationStatus == ReservationStatus.CONFIRMED) {
                return RoomStatus.RESERVED;
            }
            if (reservationStatus == ReservationStatus.REQUESTED) {
                return RoomStatus.RESERVED;
            }
            if (reservationStatus == ReservationStatus.CHECKED_IN) {
                return RoomStatus.OCCUPIED;
            }
        }
        if (isDateToday(date)) {
            return room.getStatus();
        }
        return RoomStatus.AVAILABLE;
    }

    private DailyRoomStatusDTO.ReservationInfoDTO getReservationInfo(Room room, LocalDate date, Long storeId) {
        Optional<Reservation> reservation = reservationRepository.findByStoreIdAndRoomIdAndDate(storeId, room.getId(), date);
        if (reservation.isEmpty()) {
            return null;
        }
        Reservation r = reservation.get();
        String channelName = r.getChannel() != null ? r.getChannel().getName() : "未知渠道";
        return new DailyRoomStatusDTO.ReservationInfoDTO(
                r.getId(),
                r.getGuestName(),
                channelName,
                r.getCheckInDate(),
                r.getCheckOutDate(),
                r.getOrderNumber()
        );
    }

    private boolean isDateToday(LocalDate date) {
        return LocalDate.now().equals(date);
    }
}
