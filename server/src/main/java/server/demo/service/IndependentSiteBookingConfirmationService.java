package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;
import server.demo.entity.Reservation;
import server.demo.entity.Store;
import server.demo.repository.StoreRepository;
import server.demo.util.GuestMessageLanguageUtil;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 独立站下单确认邮件：支付成功后发送给客人。
 * 语言判定复用 {@link GuestMessageLanguageUtil}：日本客人发日文，其他统一发英文。
 * 客人未填写邮箱时跳过；单个预订组发送失败不影响其他组。
 */
@Service
public class IndependentSiteBookingConfirmationService {

    private static final Logger logger = LoggerFactory.getLogger(IndependentSiteBookingConfirmationService.class);

    private final StoreRepository storeRepository;
    private final EmailService emailService;
    private final RegistrationLinkService registrationLinkService;
    private final String frontendBaseUrl;

    public IndependentSiteBookingConfirmationService(
            StoreRepository storeRepository,
            EmailService emailService,
            RegistrationLinkService registrationLinkService,
            @Value("${app.frontend.url}") String frontendBaseUrl
    ) {
        this.storeRepository = storeRepository;
        this.emailService = emailService;
        this.registrationLinkService = registrationLinkService;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    /**
     * 为一笔独立站订单（可能含多个房间预订）发送确认邮件。
     * 按 客人邮箱+组订单号 分组，避免多房间订单重复发送。
     */
    public void sendBookingConfirmations(List<Reservation> reservations) {
        if (reservations == null || reservations.isEmpty()) {
            return;
        }

        Map<String, List<Reservation>> groups = new LinkedHashMap<>();
        for (Reservation reservation : reservations) {
            if (reservation == null || reservation.getGuestEmail() == null || reservation.getGuestEmail().isBlank()) {
                continue;
            }
            String groupKey = reservation.getGuestEmail().trim().toLowerCase()
                    + "|" + nullToEmpty(reservation.getGroupOrderNo());
            groups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(reservation);
        }

        for (List<Reservation> group : groups.values()) {
            try {
                sendForGroup(group);
            } catch (Exception e) {
                Reservation first = group.get(0);
                logger.warn(
                        "Independent-site booking confirmation email failed. storeId={}, reservationId={}, email={}, err={}",
                        first.getStoreId(),
                        first.getId(),
                        first.getGuestEmail(),
                        e.getMessage(),
                        e
                );
            }
        }
    }

    private void sendForGroup(List<Reservation> group) throws Exception {
        Reservation first = group.get(0);
        Long storeId = first.getStoreId();
        Store store = storeId != null ? storeRepository.findById(storeId).orElse(null) : null;
        String storeName = store != null ? nullToEmpty(store.getName()) : "";

        boolean japanese = GuestMessageLanguageUtil.isJapaneseGuest(first);
        String bookingNo = resolveBookingNumber(first);
        String registrationLink = buildRegistrationLink(first);
        BigDecimal total = BigDecimal.ZERO;
        for (Reservation reservation : group) {
            if (reservation.getTotalAmount() != null) {
                total = total.add(reservation.getTotalAmount());
            }
        }
        String totalText = total.stripTrailingZeros().toPlainString();
        String currency = nullToEmpty(first.getCurrencyCode());
        int rooms = group.size();
        String checkIn = formatDate(first.getCheckInDate());
        String checkOut = formatDate(first.getCheckOutDate());

        String subject = japanese
                ? "【" + storeName + "】ご予約確認（予約番号: " + bookingNo + "）"
                : "[" + storeName + "] Booking confirmation (No. " + bookingNo + ")";
        String body = japanese
                ? buildJapaneseBody(first, storeName, store, bookingNo, checkIn, checkOut, rooms, currency, totalText, registrationLink)
                : buildEnglishBody(first, storeName, store, bookingNo, checkIn, checkOut, rooms, currency, totalText, registrationLink);

        emailService.sendEmail(null, first.getGuestEmail().trim(), subject, body, null);
        logger.info(
                "Independent-site booking confirmation email sent. storeId={}, reservationId={}, email={}, lang={}",
                storeId,
                first.getId(),
                first.getGuestEmail(),
                japanese ? "ja" : "en"
        );
    }

    private String buildJapaneseBody(
            Reservation first,
            String storeName,
            Store store,
            String bookingNo,
            String checkIn,
            String checkOut,
            int rooms,
            String currency,
            String totalText,
            String registrationLink
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append(nullToEmpty(first.getGuestName())).append(" 様\n\n");
        sb.append("このたびは ").append(storeName).append(" をご予約いただき、誠にありがとうございます。\n");
        sb.append("以下の内容でご予約を承りましたので、ご確認ください。\n\n");
        sb.append("予約番号: ").append(bookingNo).append('\n');
        sb.append("チェックイン: ").append(checkIn).append('\n');
        sb.append("チェックアウト: ").append(checkOut).append('\n');
        sb.append("客室数: ").append(rooms).append('\n');
        sb.append("合計金額: ").append(currency).append(' ').append(totalText).append("\n\n");
        if (registrationLink != null && !registrationLink.isBlank()) {
            sb.append("チェックイン登録はこちらからお願いいたします:\n").append(registrationLink).append("\n\n");
        }
        sb.append("ご不明な点がございましたら、施設までお問い合わせください。\n\n");
        sb.append(storeName).append('\n');
        appendStoreContact(sb, store);
        return sb.toString();
    }

    private String buildEnglishBody(
            Reservation first,
            String storeName,
            Store store,
            String bookingNo,
            String checkIn,
            String checkOut,
            int rooms,
            String currency,
            String totalText,
            String registrationLink
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Dear ").append(nullToEmpty(first.getGuestName())).append(",\n\n");
        sb.append("Thank you for booking with ").append(storeName).append(". Your reservation is confirmed.\n\n");
        sb.append("Booking number: ").append(bookingNo).append('\n');
        sb.append("Check-in: ").append(checkIn).append('\n');
        sb.append("Check-out: ").append(checkOut).append('\n');
        sb.append("Rooms: ").append(rooms).append('\n');
        sb.append("Total amount: ").append(currency).append(' ').append(totalText).append("\n\n");
        if (registrationLink != null && !registrationLink.isBlank()) {
            sb.append("Please complete your check-in registration here:\n").append(registrationLink).append("\n\n");
        }
        sb.append("If you have any questions, please contact the property.\n\n");
        sb.append(storeName).append('\n');
        appendStoreContact(sb, store);
        return sb.toString();
    }

    private void appendStoreContact(StringBuilder sb, Store store) {
        if (store == null) {
            return;
        }
        String phone = nullToEmpty(store.getPhone());
        String email = nullToEmpty(store.getEmail());
        String address = nullToEmpty(store.getAddress());
        if (!phone.isBlank()) {
            sb.append(phone).append('\n');
        }
        if (!email.isBlank()) {
            sb.append(email).append('\n');
        }
        if (!address.isBlank()) {
            sb.append(address).append('\n');
        }
    }

    private String resolveBookingNumber(Reservation reservation) {
        if (reservation.getChannelOrderNumber() != null && !reservation.getChannelOrderNumber().isBlank()) {
            return reservation.getChannelOrderNumber().trim();
        }
        if (reservation.getGroupOrderNo() != null && !reservation.getGroupOrderNo().isBlank()) {
            return reservation.getGroupOrderNo().trim();
        }
        return nullToEmpty(reservation.getOrderNumber());
    }

    /**
     * 与 SuBusinessAutoMessageService 相同的入住登记链接（/rb/{bookingKey}?t=token）。
     */
    private String buildRegistrationLink(Reservation reservation) {
        if (reservation == null || reservation.getStoreId() == null) {
            return "";
        }
        String bookingKey = resolveRegistrationBookingKey(reservation);
        if (bookingKey == null || bookingKey.isBlank()) {
            return "";
        }
        String token = registrationLinkService.generateToken(reservation.getStoreId(), bookingKey);
        String base = frontendBaseUrl != null ? frontendBaseUrl.trim() : "";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String encodedKey = UriUtils.encodePathSegment(bookingKey, StandardCharsets.UTF_8);
        return base + "/rb/" + encodedKey + "?t=" + token;
    }

    private String resolveRegistrationBookingKey(Reservation reservation) {
        if (reservation.getChannelOrderNumber() != null && !reservation.getChannelOrderNumber().isBlank()) {
            return reservation.getChannelOrderNumber().trim();
        }
        if (reservation.getExternalBookingKey() != null && !reservation.getExternalBookingKey().isBlank()) {
            return reservation.getExternalBookingKey().trim();
        }
        return reservation.getOrderNumber() != null ? reservation.getOrderNumber().trim() : null;
    }

    private static String formatDate(LocalDate date) {
        return date != null ? date.toString() : "";
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
