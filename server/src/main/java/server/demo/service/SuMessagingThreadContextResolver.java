package server.demo.service;

import org.springframework.stereotype.Service;
import server.demo.dto.SuMessagingAiReplyDraftRequest;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.entity.SuMessageThread;
import server.demo.repository.ReservationRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class SuMessagingThreadContextResolver {
    static final String WARNING_REQUEST_RESERVATION_NOT_FOUND = "REQUEST_RESERVATION_NOT_FOUND";
    static final String WARNING_MULTIPLE_RESERVATIONS_MATCHED = "MULTIPLE_RESERVATIONS_MATCHED";
    static final String WARNING_ROOM_CONTEXT_UNRESOLVED = "ROOM_CONTEXT_UNRESOLVED";

    private final ReservationRepository reservationRepository;
    private final ReservationBookingKeyResolver reservationBookingKeyResolver;

    public SuMessagingThreadContextResolver(
            ReservationRepository reservationRepository,
            ReservationBookingKeyResolver reservationBookingKeyResolver
    ) {
        this.reservationRepository = reservationRepository;
        this.reservationBookingKeyResolver = reservationBookingKeyResolver;
    }

    public SuMessagingThreadContext resolve(
            Long storeId,
            SuMessageThread thread,
            SuMessagingAiReplyDraftRequest request
    ) {
        SuMessagingThreadContext context = new SuMessagingThreadContext();
        applyThreadFields(context, thread);

        boolean appliedReservation = applyRequestReservation(storeId, context, request);
        if (!appliedReservation) {
            List<Reservation> reservations = findThreadReservations(storeId, thread);
            if (reservations.size() == 1) {
                applyReservation(context, reservations.get(0));
                context.setMatchStatus("EXACT_ONE");
            } else if (reservations.size() > 1) {
                context.setMatchStatus("MULTI");
                context.addWarning(WARNING_MULTIPLE_RESERVATIONS_MATCHED);
                applyCommonReservationFields(context, reservations);
            } else {
                context.setMatchStatus("NONE");
                context.addWarning(WARNING_ROOM_CONTEXT_UNRESOLVED);
            }
        }

        applyRequestFallback(context, request);
        if (context.getRoomId() == null && context.getRoomTypeId() == null) {
            context.addWarning(WARNING_ROOM_CONTEXT_UNRESOLVED);
        }
        return context;
    }

    public SuMessagingThreadContext resolveForIndex(Long storeId, SuMessageThread thread) {
        return resolve(storeId, thread, null);
    }

    private boolean applyRequestReservation(
            Long storeId,
            SuMessagingThreadContext context,
            SuMessagingAiReplyDraftRequest request
    ) {
        if (storeId == null || request == null || request.getReservationId() == null) {
            return false;
        }

        Optional<Reservation> reservation =
                reservationRepository.findByStoreIdAndIdWithRoomType(storeId, request.getReservationId());
        if (reservation.isPresent()) {
            applyReservation(context, reservation.get());
            context.setMatchStatus("REQUEST_RESERVATION");
            return true;
        }

        context.addWarning(WARNING_REQUEST_RESERVATION_NOT_FOUND);
        return false;
    }

    private List<Reservation> findThreadReservations(Long storeId, SuMessageThread thread) {
        List<Reservation> reservations = new ArrayList<>();
        if (storeId == null || thread == null) {
            return reservations;
        }

        List<String> candidates = reservationBookingKeyResolver.buildThreadLookupCandidates(thread);
        for (String candidate : candidates) {
            List<Reservation> matches =
                    reservationBookingKeyResolver.findReservationsByBookingKeyWithRoomType(storeId, candidate);
            addDistinctReservations(reservations, matches);
            if (!reservations.isEmpty()) {
                break;
            }
        }
        return reservations;
    }

    private void applyThreadFields(SuMessagingThreadContext context, SuMessageThread thread) {
        if (thread == null) {
            return;
        }

        context.setChannelId(thread.getChannelId());
        context.setChannelName(resolveChannelName(thread.getChannelId()));
        context.setBookingKey(firstNonBlank(thread.getBookingId(), thread.getThreadKey(), thread.getThreadId()));
        context.setGuestName(blankToNull(thread.getGuestName()));
    }

    private void applyRequestFallback(
            SuMessagingThreadContext context,
            SuMessagingAiReplyDraftRequest request
    ) {
        if (request == null) {
            return;
        }

        if (context.getChannelId() == null) {
            Integer channelId = parseChannel(request.getChannel());
            context.setChannelId(channelId);
            context.setChannelName(resolveChannelName(channelId));
        }
        if (context.getBookingKey() == null) {
            context.setBookingKey(firstNonBlank(request.getBookingId(), request.getExternalThreadId()));
        }
        if (context.getGuestName() == null) {
            context.setGuestName(blankToNull(request.getGuestName()));
        }
        if (context.getRoomId() == null) {
            context.setRoomId(request.getRoomId());
        }
        if (context.getRoomNumber() == null) {
            context.setRoomNumber(blankToNull(request.getRoomNumber()));
        }
        if (context.getRoomTypeId() == null) {
            context.setRoomTypeId(request.getRoomTypeId());
        }
        if (context.getRoomTypeName() == null) {
            context.setRoomTypeName(blankToNull(request.getRoomTypeName()));
        }
    }

    private void applyReservation(SuMessagingThreadContext context, Reservation reservation) {
        if (reservation == null) {
            return;
        }

        context.setReservationId(reservation.getId());
        context.setBookingKey(firstNonBlank(
                reservationBookingKeyResolver.resolvePrimaryBookingKey(reservation),
                context.getBookingKey()
        ));
        context.setGuestName(firstNonBlank(reservation.getGuestName(), context.getGuestName()));
        context.setCheckInDate(reservation.getCheckInDate());
        context.setCheckOutDate(reservation.getCheckOutDate());

        Room room = reservation.getRoom();
        if (room != null) {
            context.setRoomId(room.getId());
            context.setRoomNumber(blankToNull(room.getRoomNumber()));
            applyRoomType(context, room.getRoomType());
            return;
        }

        context.setRoomNumber(blankToNull(reservation.getOtaRoomNumber()));
        if (reservation.getOtaRoomTypeId() != null) {
            context.setRoomTypeId(reservation.getOtaRoomTypeId());
        }
    }

    private void applyCommonReservationFields(
            SuMessagingThreadContext context,
            List<Reservation> reservations
    ) {
        Long commonRoomId = null;
        String commonRoomNumber = null;
        Long commonRoomTypeId = null;
        String commonRoomTypeName = null;
        boolean roomInitialized = false;
        boolean roomTypeInitialized = false;

        for (Reservation reservation : reservations) {
            Room room = reservation.getRoom();
            Long roomId = room == null ? null : room.getId();
            String roomNumber = room == null ? null : blankToNull(room.getRoomNumber());
            Long roomTypeId = resolveRoomTypeId(reservation);
            String roomTypeName = resolveRoomTypeName(reservation);

            if (!roomInitialized) {
                commonRoomId = roomId;
                commonRoomNumber = roomNumber;
                roomInitialized = true;
            } else if (!Objects.equals(commonRoomId, roomId)) {
                commonRoomId = null;
                commonRoomNumber = null;
            }

            if (!roomTypeInitialized) {
                commonRoomTypeId = roomTypeId;
                commonRoomTypeName = roomTypeName;
                roomTypeInitialized = true;
            } else if (!Objects.equals(commonRoomTypeId, roomTypeId)) {
                commonRoomTypeId = null;
                commonRoomTypeName = null;
            }
        }

        context.setRoomId(commonRoomId);
        context.setRoomNumber(commonRoomNumber);
        context.setRoomTypeId(commonRoomTypeId);
        context.setRoomTypeName(commonRoomTypeName);
    }

    private static void applyRoomType(SuMessagingThreadContext context, RoomType roomType) {
        if (roomType == null) {
            return;
        }
        context.setRoomTypeId(roomType.getId());
        context.setRoomTypeName(blankToNull(roomType.getName()));
    }

    private static Long resolveRoomTypeId(Reservation reservation) {
        if (reservation == null) {
            return null;
        }
        Room room = reservation.getRoom();
        if (room != null && room.getRoomType() != null) {
            return room.getRoomType().getId();
        }
        return reservation.getOtaRoomTypeId();
    }

    private static String resolveRoomTypeName(Reservation reservation) {
        if (reservation == null || reservation.getRoom() == null) {
            return null;
        }
        RoomType roomType = reservation.getRoom().getRoomType();
        return roomType == null ? null : blankToNull(roomType.getName());
    }

    private static void addDistinctReservations(List<Reservation> target, List<Reservation> candidates) {
        if (candidates == null) {
            return;
        }
        for (Reservation candidate : candidates) {
            if (candidate == null || containsReservation(target, candidate)) {
                continue;
            }
            target.add(candidate);
        }
    }

    private static boolean containsReservation(List<Reservation> reservations, Reservation candidate) {
        for (Reservation reservation : reservations) {
            if (reservation != null
                    && candidate.getId() != null
                    && candidate.getId().equals(reservation.getId())) {
                return true;
            }
        }
        return false;
    }

    private static Integer parseChannel(String channel) {
        String normalized = blankToNull(channel);
        if (normalized == null) {
            return null;
        }
        if ("AIRBNB".equalsIgnoreCase(normalized)) {
            return SuMessagingService.CHANNEL_AIRBNB;
        }
        if ("BOOKING".equalsIgnoreCase(normalized) || "BOOKING.COM".equalsIgnoreCase(normalized)) {
            return SuMessagingService.CHANNEL_BOOKING;
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String resolveChannelName(Integer channelId) {
        if (channelId == null) {
            return null;
        }
        if (channelId == SuMessagingService.CHANNEL_AIRBNB) {
            return "Airbnb";
        }
        if (channelId == SuMessagingService.CHANNEL_BOOKING) {
            return "Booking.com";
        }
        return "CHANNEL_" + channelId;
    }

    private static String firstNonBlank(String first, String second) {
        String normalizedFirst = blankToNull(first);
        return normalizedFirst != null ? normalizedFirst : blankToNull(second);
    }

    private static String firstNonBlank(String first, String second, String third) {
        String value = firstNonBlank(first, second);
        return value != null ? value : blankToNull(third);
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
