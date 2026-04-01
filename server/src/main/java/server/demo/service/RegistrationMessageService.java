package server.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.SuMessagingSendRequest;
import server.demo.dto.registration.RegistrationMessageLogDTO;
import server.demo.dto.registration.RegistrationSendMessageRequest;
import server.demo.entity.RegistrationForm;
import server.demo.entity.RegistrationMessageLog;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.entity.Store;
import server.demo.entity.SuMessageThread;
import server.demo.enums.RegistrationMessageType;
import server.demo.enums.RegistrationSendStatus;
import server.demo.repository.RegistrationFormRepository;
import server.demo.repository.RegistrationMessageLogRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.StoreRepository;
import server.demo.repository.SuMessageThreadRepository;
import server.demo.util.AutoMessageTemplateRenderer;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.nio.charset.StandardCharsets;

@Service
public class RegistrationMessageService {

    private final RegistrationFormRepository formRepository;
    private final ReservationRepository reservationRepository;
    private final StoreRepository storeRepository;
    private final SuMessageThreadRepository threadRepository;
    private final SuMessagingService suMessagingService;
    private final RegistrationMessageLogRepository messageLogRepository;
    private final RegistrationLinkService registrationLinkService;
    private final String serverBaseUrl;

    public RegistrationMessageService(
            RegistrationFormRepository formRepository,
            ReservationRepository reservationRepository,
            StoreRepository storeRepository,
            SuMessageThreadRepository threadRepository,
            SuMessagingService suMessagingService,
            RegistrationMessageLogRepository messageLogRepository,
            RegistrationLinkService registrationLinkService,
            @Value("${server.base-url}") String serverBaseUrl
    ) {
        this.formRepository = formRepository;
        this.reservationRepository = reservationRepository;
        this.storeRepository = storeRepository;
        this.threadRepository = threadRepository;
        this.suMessagingService = suMessagingService;
        this.messageLogRepository = messageLogRepository;
        this.registrationLinkService = registrationLinkService;
        this.serverBaseUrl = serverBaseUrl;
    }

    @Transactional
    public RegistrationMessageLogDTO sendMessage(Long storeId, Long operatorUserId, Long formId, RegistrationSendMessageRequest req) {
        if (storeId == null) {
            throw new RuntimeException("缺少 storeId");
        }
        if (req == null || req.getType() == null) {
            throw new RuntimeException("请选择消息类型");
        }
        String template = req.getContent() != null ? req.getContent().trim() : null;
        if (template == null || template.isBlank()) {
            throw new RuntimeException("消息内容不能为空");
        }

        RegistrationForm form = formRepository.findById(formId)
                .orElseThrow(() -> new RuntimeException("登记表不存在"));
        if (!storeId.equals(form.getStoreId())) {
            throw new RuntimeException("无权限");
        }

        Reservation reservation = reservationRepository.findById(form.getReservation().getId())
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        Store store = storeRepository.findById(storeId).orElse(null);

        Map<String, String> vars = buildVariables(store, reservation);
        String rendered = AutoMessageTemplateRenderer.render(template, vars);
        if (rendered == null || rendered.isBlank()) {
            throw new RuntimeException("渲染后消息为空");
        }

        Integer suChannelId = toSuChannelId(reservation);
        if (suChannelId == null) {
            return saveLog(form, req.getType(), "SU", null, rendered, RegistrationSendStatus.FAILED, "不支持的渠道(仅 Booking/Airbnb)");
        }

        SuMessageThread thread = resolveThreadForReservation(storeId, suChannelId, reservation);
        if (thread == null) {
            return saveLog(form, req.getType(), "SU", null, rendered, RegistrationSendStatus.WAITING_THREAD,
                    "未找到会话(thread)，请等待 Su messaging webhook 入库后重试");
        }

        if (thread.getListingId() == null || thread.getListingId().isBlank()) {
            return saveLog(form, req.getType(), "SU", thread.getThreadKey(), rendered, RegistrationSendStatus.WAITING_LISTINGID,
                    "会话缺少 listingid，暂不可发送；等待 webhook 补齐后重试");
        }

        try {
            SuMessagingSendRequest send = new SuMessagingSendRequest();
            send.setContent(rendered.trim());
            send.setSenderName(req.getSenderName() != null && !req.getSenderName().isBlank() ? req.getSenderName().trim() : "前台");
            suMessagingService.sendMessage(storeId, thread.getId(), send);
            return saveLog(form, req.getType(), "SU", thread.getThreadKey(), rendered, RegistrationSendStatus.SENT, null);
        } catch (Exception e) {
            String err = e.getMessage() != null ? e.getMessage() : "发送失败";
            return saveLog(form, req.getType(), "SU", thread.getThreadKey(), rendered, RegistrationSendStatus.FAILED, err);
        }
    }

    private RegistrationMessageLogDTO saveLog(
            RegistrationForm form,
            RegistrationMessageType type,
            String channel,
            String toIdentifier,
            String content,
            RegistrationSendStatus status,
            String error
    ) {
        RegistrationMessageLog log = new RegistrationMessageLog();
        log.setForm(form);
        log.setType(type);
        log.setChannel(channel);
        log.setToIdentifier(toIdentifier);
        log.setContent(trimToMax(content, 4000));
        log.setSendStatus(status);
        log.setErrorMessage(trimToMax(error, 2000));
        log = messageLogRepository.save(log);

        RegistrationMessageLogDTO dto = new RegistrationMessageLogDTO();
        dto.setId(log.getId());
        dto.setType(log.getType());
        dto.setChannel(log.getChannel());
        dto.setToIdentifier(log.getToIdentifier());
        dto.setContent(log.getContent());
        dto.setSendStatus(log.getSendStatus());
        dto.setErrorMessage(log.getErrorMessage());
        dto.setCreatedAt(log.getCreatedAt());
        return dto;
    }

    private SuMessageThread resolveThreadForReservation(Long storeId, Integer suChannelId, Reservation reservation) {
        String bookingId = reservation.getChannelOrderNumber();
        if (bookingId == null || bookingId.isBlank()) {
            bookingId = reservation.getOrderNumber();
        }
        if (bookingId == null || bookingId.isBlank()) {
            return null;
        }
        return threadRepository.findFirstByStoreIdAndChannelIdAndBookingIdOrderByLastActivityDesc(storeId, suChannelId, bookingId.trim())
                .orElse(null);
    }

    private Integer toSuChannelId(Reservation reservation) {
        if (reservation == null || reservation.getChannel() == null || reservation.getChannel().getCode() == null) {
            return null;
        }
        String normalized = reservation.getChannel().getCode().trim().toUpperCase();
        if ("BOOKING".equals(normalized) || "BOOKING.COM".equals(normalized)) {
            return SuMessagingService.CHANNEL_BOOKING;
        }
        if ("AIRBNB".equals(normalized)) {
            return SuMessagingService.CHANNEL_AIRBNB;
        }
        return null;
    }

    private Map<String, String> buildVariables(Store store, Reservation reservation) {
        Map<String, String> vars = new HashMap<>();
        vars.put("property_name", store != null ? nullToEmpty(store.getName()) : "");
        vars.put("property_address", store != null ? nullToEmpty(store.getAddress()) : "");
        vars.put("property_city", store != null ? nullToEmpty(store.getCity()) : "");
        vars.put("property_phone", store != null ? nullToEmpty(store.getPhone()) : "");
        vars.put("property_email", store != null ? nullToEmpty(store.getEmail()) : "");

        vars.put("guest_name", reservation != null ? nullToEmpty(reservation.getGuestName()) : "");
        vars.put("guest_phone", reservation != null ? nullToEmpty(reservation.getGuestPhone()) : "");

        vars.put("checkin_date", formatDate(reservation != null ? reservation.getCheckInDate() : null));
        vars.put("checkout_date", formatDate(reservation != null ? reservation.getCheckOutDate() : null));

        vars.put("room_type_name", resolveRoomTypeName(reservation));
        vars.put("room_type_address", resolveRoomTypeAddress(reservation));
        vars.put("room_number", resolveRoomNumber(reservation));
        vars.put("confirmation_code", reservation != null ? nullToEmpty(reservation.getChannelOrderNumber()) : "");

        vars.put("order_number", reservation != null ? nullToEmpty(reservation.getOrderNumber()) : "");
        vars.put("registration_link", buildRegistrationLink(reservation));

        vars.put("number_of_nights", resolveNights(reservation));
        return vars;
    }

    private String buildRegistrationLink(Reservation reservation) {
        if (reservation == null || reservation.getStoreId() == null) {
            return "";
        }

        String bookingKey = reservation.getChannelOrderNumber();
        if (bookingKey == null || bookingKey.isBlank()) {
            bookingKey = reservation.getOrderNumber();
        }
        if (bookingKey == null || bookingKey.isBlank()) {
            return "";
        }

        String token = registrationLinkService.generateToken(reservation.getStoreId(), bookingKey);
        String base = serverBaseUrl != null ? serverBaseUrl.trim() : "";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String encodedKey = UriUtils.encodePathSegment(bookingKey, StandardCharsets.UTF_8);
        return base + "/rb/" + encodedKey + "?t=" + token;
    }

    private static String resolveRoomTypeName(Reservation reservation) {
        if (reservation == null) {
            return "";
        }
        Room room = reservation.getRoom();
        if (room != null && room.getRoomType() != null) {
            RoomType rt = room.getRoomType();
            return nullToEmpty(rt.getName());
        }
        return "";
    }

    private static String resolveRoomTypeAddress(Reservation reservation) {
        if (reservation == null) {
            return "";
        }
        Room room = reservation.getRoom();
        if (room != null && room.getRoomType() != null) {
            RoomType rt = room.getRoomType();
            return nullToEmpty(rt.getRoomTypeAddress());
        }
        return "";
    }

    private static String resolveRoomNumber(Reservation reservation) {
        if (reservation == null) {
            return "";
        }
        Room room = reservation.getRoom();
        if (room != null) {
            return nullToEmpty(room.getRoomNumber());
        }
        return nullToEmpty(reservation.getOtaRoomNumber());
    }

    private static String resolveNights(Reservation reservation) {
        if (reservation == null) {
            return "";
        }
        LocalDate in = reservation.getCheckInDate();
        LocalDate out = reservation.getCheckOutDate();
        if (in == null || out == null) {
            return "";
        }
        long nights = ChronoUnit.DAYS.between(in, out);
        return String.valueOf(Math.max(0, nights));
    }

    private static String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.toString();
    }

    private static String nullToEmpty(String v) {
        return v == null ? "" : v;
    }

    private static String trimToMax(String v, int max) {
        if (v == null) {
            return null;
        }
        String s = v.trim();
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }
}
