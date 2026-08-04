package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.registration.*;
import server.demo.entity.*;
import server.demo.enums.RegistrationFormStatus;
import server.demo.enums.RegistrationMessageType;
import server.demo.enums.RegistrationReviewAction;
import server.demo.enums.ReservationStatus;
import server.demo.exception.RegistrationReviewConflictException;
import server.demo.repository.*;
import server.demo.util.StoreContextUtils;
import server.demo.util.StoreTimeZoneUtil;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import server.demo.i18n.ApiMessages;
@Service
public class RegistrationAdminService {
    private static final Logger logger = LoggerFactory.getLogger(RegistrationAdminService.class);
    private static final String ROOM_NUMBER_FILTER_SENTINEL = "__REGISTRATION_ADMIN_EMPTY_ROOM_NUMBER_FILTER__";
    private static final String REVIEW_CANCELLED_RESERVATION_MESSAGE_KEY = "api.t.7c4e979dd124";
    private static final String REVIEW_SUBMITTED_ONLY_MESSAGE_KEY = "api.t.3746916d6064";
    private static final String REVIEW_ALREADY_REVIEWED_MESSAGE_KEY = "api.t.dea5b1096466";
    private static final String REVIEW_STATE_CHANGED_MESSAGE_KEY = "api.t.39f12d8f52ea";

    @Autowired
    private RegistrationFormRepository registrationFormRepository;

    @Autowired
    private RegistrationGuestRepository registrationGuestRepository;

    @Autowired
    private RegistrationReviewLogRepository registrationReviewLogRepository;

    @Autowired
    private RegistrationMessageLogRepository registrationMessageLogRepository;

    @Autowired
    private RegistrationAttachmentRepository registrationAttachmentRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private RegistrationMessageService registrationMessageService;

    @Autowired
    private RegistrationReviewSettingsService registrationReviewSettingsService;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private Clock clock;

    @Autowired(required = false)
    private SuMessagingRealtimeGateway realtimeGateway;

    @Transactional(readOnly = true)
    public List<AdminRegistrationListItemDTO> list(
            RegistrationFormStatus status,
            Long channelId,
            ReservationStatus reservationStatus,
            List<String> roomNumbers,
            Long roomGroupId,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            LocalDate checkInStartDate,
            LocalDate checkInEndDate,
            LocalDate checkOutStartDate,
            LocalDate checkOutEndDate
    ) {
        Long storeId = StoreContextUtils.requireStoreId();
        List<String> normalizedRoomNumbers = normalizeRoomNumbers(roomNumbers);
        boolean roomNumberFilterEnabled = normalizedRoomNumbers != null;
        List<String> queryRoomNumbers = roomNumberFilterEnabled
                ? normalizedRoomNumbers
                : List.of(ROOM_NUMBER_FILTER_SENTINEL);
        LocalDate effectiveCheckInStartDate = resolveRangeStart(
                checkInDate,
                checkInStartDate,
                checkInEndDate
        );
        LocalDate effectiveCheckInEndDate = resolveRangeEnd(
                checkInDate,
                checkInStartDate,
                checkInEndDate
        );
        LocalDate effectiveCheckOutStartDate = resolveRangeStart(
                checkOutDate,
                checkOutStartDate,
                checkOutEndDate
        );
        LocalDate effectiveCheckOutEndDate = resolveRangeEnd(
                checkOutDate,
                checkOutStartDate,
                checkOutEndDate
        );
        List<RegistrationForm> forms = registrationFormRepository.searchForAdminList(
                storeId,
                status,
                channelId,
                reservationStatus,
                reservationStatus == ReservationStatus.CANCELLED,
                roomNumberFilterEnabled,
                queryRoomNumbers,
                roomGroupId,
                effectiveCheckInStartDate,
                effectiveCheckInEndDate,
                effectiveCheckOutStartDate,
                effectiveCheckOutEndDate
        );

        return forms.stream().map(this::toListItem).toList();
    }

    @Transactional(readOnly = true)
    public Page<AdminRegistrationListItemDTO> listPage(
            RegistrationFormStatus status,
            Long channelId,
            ReservationStatus reservationStatus,
            List<String> roomNumbers,
            Long roomGroupId,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            LocalDate checkInStartDate,
            LocalDate checkInEndDate,
            LocalDate checkOutStartDate,
            LocalDate checkOutEndDate,
            int page,
            int size
    ) {
        Long storeId = StoreContextUtils.requireStoreId();
        List<String> normalizedRoomNumbers = normalizeRoomNumbers(roomNumbers);
        boolean roomNumberFilterEnabled = normalizedRoomNumbers != null;
        List<String> queryRoomNumbers = roomNumberFilterEnabled
                ? normalizedRoomNumbers
                : List.of(ROOM_NUMBER_FILTER_SENTINEL);
        LocalDate effectiveCheckInStartDate = resolveRangeStart(
                checkInDate,
                checkInStartDate,
                checkInEndDate
        );
        LocalDate effectiveCheckInEndDate = resolveRangeEnd(
                checkInDate,
                checkInStartDate,
                checkInEndDate
        );
        LocalDate effectiveCheckOutStartDate = resolveRangeStart(
                checkOutDate,
                checkOutStartDate,
                checkOutEndDate
        );
        LocalDate effectiveCheckOutEndDate = resolveRangeEnd(
                checkOutDate,
                checkOutStartDate,
                checkOutEndDate
        );

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<RegistrationForm> formPage = registrationFormRepository.searchForAdminListPage(
                storeId,
                status,
                channelId,
                reservationStatus,
                reservationStatus == ReservationStatus.CANCELLED,
                roomNumberFilterEnabled,
                queryRoomNumbers,
                roomGroupId,
                effectiveCheckInStartDate,
                effectiveCheckInEndDate,
                effectiveCheckOutStartDate,
                effectiveCheckOutEndDate,
                PageRequest.of(safePage, safeSize)
        );

        return formPage.map(this::toListItem);
    }

    @Transactional(readOnly = true)
    public List<AdminRegistrationListItemDTO> listRecentApprovedForHome(LocalDateTime approvedSince) {
        if (approvedSince == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.31424aee660d"));
        }
        Long storeId = StoreContextUtils.requireStoreId();
        return registrationFormRepository.findRecentApprovedForHome(storeId, approvedSince).stream()
                .map(this::toListItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminRegistrationListItemDTO> listHomeSlice(
            LocalDateTime completedSince,
            RegistrationFormStatus status,
            Integer cursorPriority,
            LocalDateTime cursorDueAt,
            Long cursorId,
            int size
    ) {
        Long storeId = StoreContextUtils.requireStoreId();
        boolean hasCursor = cursorId != null;
        return registrationFormRepository.findHomeSlice(
                        storeId,
                        completedSince,
                        status,
                        hasCursor,
                        cursorPriority == null ? 0 : cursorPriority,
                        cursorDueAt == null ? completedSince : cursorDueAt,
                        cursorId == null ? 0L : cursorId,
                        PageRequest.of(0, Math.max(1, Math.min(size, 101)))
                ).stream()
                .map(this::toListItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countHome(RegistrationFormStatus status, LocalDateTime completedSince) {
        return registrationFormRepository.countHomeByStatus(
                StoreContextUtils.requireStoreId(), status, completedSince);
    }

    private AdminRegistrationListItemDTO toListItem(RegistrationForm form) {
        Reservation reservation = form.getReservation();
        AdminRegistrationListItemDTO dto = new AdminRegistrationListItemDTO();
        dto.setFormId(form.getId());
        dto.setOrderNumber(form.getOrderNumber());
        dto.setStatus(form.getStatus());
        dto.setSubmittedAt(form.getSubmittedAt());
        dto.setApprovedAt(form.getApprovedAt());
        dto.setUpdatedAt(form.getUpdatedAt());
        if (reservation != null) {
            dto.setGuestName(reservation.getGuestName());
            dto.setCheckInDate(reservation.getCheckInDate());
            dto.setCheckOutDate(reservation.getCheckOutDate());
            dto.setReservationStatus(reservation.getStatus());
            dto.setChannelOrderNumber(reservation.getChannelOrderNumber());
            dto.setChannelName(reservation.getChannel() != null ? reservation.getChannel().getName() : null);
        }
        return dto;
    }

    @Transactional(readOnly = true)
    public AdminRegistrationDetailDTO detail(Long formId) {
        Long storeId = StoreContextUtils.requireStoreId();
        RegistrationForm form = registrationFormRepository.findById(formId)
                .orElseThrow(() -> new RuntimeException(ApiMessages.get("api.t.5ea3eb2ea267")));
        if (!storeId.equals(form.getStoreId())) {
            throw new RuntimeException(ApiMessages.get("api.permission.denied"));
        }

        Reservation reservation = reservationRepository.findById(form.getReservation().getId())
                .orElseThrow(() -> new RuntimeException(ApiMessages.get("api.t.b8768a4b0d04")));

        AdminRegistrationDetailDTO dto = new AdminRegistrationDetailDTO();
        dto.setFormId(form.getId());
        dto.setReservationId(reservation.getId());
        dto.setOrderNumber(form.getOrderNumber());
        dto.setChannelOrderNumber(reservation.getChannelOrderNumber());
        dto.setChannelName(reservation.getChannel() != null ? reservation.getChannel().getName() : null);
        dto.setStatus(form.getStatus());
        dto.setReservationStatus(reservation.getStatus());
        dto.setSubmittedAt(form.getSubmittedAt());
        dto.setApprovedAt(form.getApprovedAt());
        dto.setRejectedAt(form.getRejectedAt());
        dto.setReviewNote(form.getReviewNote());
        dto.setAutoFinalizeDate(resolveAutoFinalizeDate(storeId, form, reservation));

        dto.setGuestName(reservation.getGuestName());
        dto.setCheckInDate(reservation.getCheckInDate());
        dto.setCheckOutDate(reservation.getCheckOutDate());
        dto.setRoomTypeName(resolveRoomTypeName(reservation));
        dto.setRoomNumber(resolveRoomNumber(reservation));
        dto.setAdults(reservation.getAdults());
        dto.setChildren(reservation.getChildren());

        List<RegistrationGuest> guests = registrationGuestRepository.findByFormIdOrderBySortOrderAsc(form.getId());
        List<PublicRegistrationGuestDTO> guestDTOs = new ArrayList<>();
        for (RegistrationGuest g : guests) {
            PublicRegistrationGuestDTO gd = new PublicRegistrationGuestDTO();
            gd.setId(g.getId());
            gd.setSortOrder(g.getSortOrder());
            gd.setLastName(g.getLastName());
            gd.setFirstName(g.getFirstName());
            gd.setLastNameKana(g.getLastNameKana());
            gd.setFirstNameKana(g.getFirstNameKana());
            gd.setGender(g.getGender());
            gd.setBirthday(g.getBirthday());
            gd.setNationality(g.getNationality());
            gd.setResidenceType(g.getResidenceType());
            gd.setAddress(g.getAddress());
            gd.setPhone(g.getPhone());
            gd.setEmail(g.getEmail());
            gd.setPassportNumber(g.getPassportNumber());
            gd.setPriorStay(g.getPriorStay());
            gd.setNextDestination(g.getNextDestination());
            guestDTOs.add(gd);
        }
        dto.setGuests(guestDTOs);

        List<RegistrationAttachment> atts = registrationAttachmentRepository.findByFormId(form.getId());
        List<PublicRegistrationAttachmentDTO> attDTOs = new ArrayList<>();
        if (atts != null) {
            for (RegistrationAttachment a : atts) {
                PublicRegistrationAttachmentDTO ad = new PublicRegistrationAttachmentDTO();
                ad.setId(a.getId());
                ad.setType(a.getType());
                ad.setOriginalName(a.getOriginalName());
                if (a.getGuest() != null) {
                    ad.setGuestId(a.getGuest().getId());
                }
                attDTOs.add(ad);
            }
        }
        dto.setAttachments(attDTOs);

        List<RegistrationReviewLog> reviewLogs = registrationReviewLogRepository.findByFormIdOrderByCreatedAtDesc(form.getId());
        List<RegistrationReviewLogDTO> reviewDTOs = new ArrayList<>();
        for (RegistrationReviewLog log : reviewLogs) {
            RegistrationReviewLogDTO rd = new RegistrationReviewLogDTO();
            rd.setId(log.getId());
            rd.setAction(log.getAction());
            rd.setOperatorUserId(log.getOperatorUserId());
            rd.setOperatorName(log.getOperatorName());
            rd.setNote(log.getNote());
            rd.setCreatedAt(log.getCreatedAt());
            reviewDTOs.add(rd);
        }
        dto.setReviewLogs(reviewDTOs);

        List<RegistrationMessageLog> msgLogs = registrationMessageLogRepository.findByFormIdOrderByCreatedAtDesc(form.getId());
        List<RegistrationMessageLogDTO> msgDTOs = new ArrayList<>();
        for (RegistrationMessageLog log : msgLogs) {
            RegistrationMessageLogDTO md = new RegistrationMessageLogDTO();
            md.setId(log.getId());
            md.setType(log.getType());
            md.setChannel(log.getChannel());
            md.setToIdentifier(log.getToIdentifier());
            md.setContent(log.getContent());
            md.setSendStatus(log.getSendStatus());
            md.setErrorMessage(log.getErrorMessage());
            md.setCreatedAt(log.getCreatedAt());
            msgDTOs.add(md);
        }
        dto.setMessageLogs(msgDTOs);

        return dto;
    }

    private static String resolveRoomTypeName(Reservation reservation) {
        if (reservation == null || reservation.getRoom() == null || reservation.getRoom().getRoomType() == null) {
            return "";
        }
        String roomTypeName = reservation.getRoom().getRoomType().getName();
        return roomTypeName == null ? "" : roomTypeName;
    }

    private static String resolveRoomNumber(Reservation reservation) {
        if (reservation == null) {
            return "";
        }
        if (reservation.getRoom() != null && reservation.getRoom().getRoomNumber() != null) {
            return reservation.getRoom().getRoomNumber();
        }
        return reservation.getOtaRoomNumber() == null ? "" : reservation.getOtaRoomNumber();
    }

    private static List<String> normalizeRoomNumbers(List<String> roomNumbers) {
        if (roomNumbers == null) {
            return null;
        }

        List<String> normalized = new ArrayList<>();
        for (String roomNumber : roomNumbers) {
            if (roomNumber == null) {
                continue;
            }
            String trimmed = roomNumber.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!normalized.contains(trimmed)) {
                normalized.add(trimmed);
            }
        }

        if (normalized.isEmpty()) {
            return null;
        }
        return normalized;
    }

    private static LocalDate resolveRangeStart(
            LocalDate exactDate,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate != null || endDate != null) {
            return startDate;
        }
        return exactDate;
    }

    private static LocalDate resolveRangeEnd(
            LocalDate exactDate,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate != null || endDate != null) {
            return endDate;
        }
        return exactDate;
    }

    private LocalDate resolveAutoFinalizeDate(Long storeId, RegistrationForm form, Reservation reservation) {
        if (form == null || reservation == null || reservation.getCheckInDate() == null) {
            return null;
        }
        if (form.getStatus() != RegistrationFormStatus.SUBMITTED
                && form.getStatus() != RegistrationFormStatus.REVIEWED) {
            return null;
        }
        int leadDays = registrationReviewSettingsService.getEffective(storeId).effectiveLeadDays();
        return reservation.getCheckInDate().minusDays(leadDays);
    }

    /**
     * 入住日是否已进入终审窗口（门店本地今天 >= 入住日 - leadDays）。
     * 无入住日时保持旧行为：通过即最终通过。
     */
    private boolean isWithinFinalizeWindow(Long storeId, Reservation reservation) {
        if (reservation == null || reservation.getCheckInDate() == null) {
            return true;
        }
        int leadDays = registrationReviewSettingsService.getEffective(storeId).effectiveLeadDays();
        LocalDate finalizeDate = reservation.getCheckInDate().minusDays(leadDays);
        return !resolveStoreToday(storeId).isBefore(finalizeDate);
    }

    private LocalDate resolveStoreToday(Long storeId) {
        Store store = storeRepository != null ? storeRepository.findById(storeId).orElse(null) : null;
        ZoneId zone = StoreTimeZoneUtil.resolveZoneId(store);
        return LocalDate.ofInstant(clock.instant(), zone);
    }

    @Transactional
    public AdminRegistrationReviewResponse approve(Long formId, AdminRegistrationReviewRequest req) {
        Long storeId = StoreContextUtils.requireStoreId();
        Long userId = StoreContextUtils.requireUserId();

        RegistrationForm form = registrationFormRepository.findById(formId)
                .orElseThrow(() -> new RuntimeException(ApiMessages.get("api.t.5ea3eb2ea267")));
        if (!storeId.equals(form.getStoreId())) {
            throw new RuntimeException(ApiMessages.get("api.permission.denied"));
        }

        Reservation reservation = requireReservation(form);
        validateReviewAllowed(form, reservation);

        String note = req != null ? req.getNote() : null;
        RegistrationMessageType messageType;
        RegistrationFormStatus resultStatus;
        if (form.getStatus() == RegistrationFormStatus.REVIEWED) {
            // 已初审通过：人工再次点通过 = 立即终审
            int updated = registrationFormRepository.approveSubmitted(storeId, formId, note, LocalDateTime.now());
            if (updated != 1) {
                throw new RegistrationReviewConflictException(ApiMessages.get(REVIEW_STATE_CHANGED_MESSAGE_KEY));
            }
            messageType = RegistrationMessageType.APPROVED_INFO;
            resultStatus = RegistrationFormStatus.APPROVED;
        } else if (isWithinFinalizeWindow(storeId, reservation)) {
            // 入住日在终审窗口内：通过即最终通过
            int updated = registrationFormRepository.approveSubmitted(storeId, formId, note, LocalDateTime.now());
            if (updated != 1) {
                throw new RegistrationReviewConflictException(ApiMessages.get(REVIEW_STATE_CHANGED_MESSAGE_KEY));
            }
            messageType = RegistrationMessageType.APPROVED_INFO;
            resultStatus = RegistrationFormStatus.APPROVED;
        } else {
            // 入住日在终审窗口外：初审通过，等待系统到期自动终审
            int updated = registrationFormRepository.markReviewed(storeId, formId, note);
            if (updated != 1) {
                throw new RegistrationReviewConflictException(ApiMessages.get(REVIEW_STATE_CHANGED_MESSAGE_KEY));
            }
            messageType = RegistrationMessageType.REVIEWED_INFO;
            resultStatus = RegistrationFormStatus.REVIEWED;
        }

        RegistrationReviewLog log = new RegistrationReviewLog();
        log.setForm(form);
        log.setAction(RegistrationReviewAction.APPROVE);
        log.setOperatorUserId(userId);
        log.setNote(note);
        registrationReviewLogRepository.save(log);

        publishWorkbenchInvalidationAfterCommit(storeId);

        AdminRegistrationReviewResponse response = sendReviewMessageIfPresent(
                storeId,
                userId,
                formId,
                req,
                messageType
        );
        response.setFormStatus(resultStatus.name());
        return response;
    }

    @Transactional
    public AdminRegistrationReviewResponse reject(Long formId, AdminRegistrationReviewRequest req) {
        Long storeId = StoreContextUtils.requireStoreId();
        Long userId = StoreContextUtils.requireUserId();

        RegistrationForm form = registrationFormRepository.findById(formId)
                .orElseThrow(() -> new RuntimeException(ApiMessages.get("api.t.5ea3eb2ea267")));
        if (!storeId.equals(form.getStoreId())) {
            throw new RuntimeException(ApiMessages.get("api.permission.denied"));
        }

        Reservation reservation = requireReservation(form);
        validateReviewAllowed(form, reservation);

        String note = req != null ? req.getNote() : null;
        int updated = registrationFormRepository.rejectSubmitted(storeId, formId, note, LocalDateTime.now());
        if (updated != 1) {
            throw new RegistrationReviewConflictException(ApiMessages.get(REVIEW_STATE_CHANGED_MESSAGE_KEY));
        }

        RegistrationReviewLog log = new RegistrationReviewLog();
        log.setForm(form);
        log.setAction(RegistrationReviewAction.REJECT);
        log.setOperatorUserId(userId);
        log.setNote(note);
        registrationReviewLogRepository.save(log);

        publishWorkbenchInvalidationAfterCommit(storeId);

        AdminRegistrationReviewResponse response = sendReviewMessageIfPresent(
                storeId,
                userId,
                formId,
                req,
                RegistrationMessageType.REJECT_REQUEST
        );
        response.setFormStatus(RegistrationFormStatus.REJECTED.name());
        return response;
    }

    /**
     * 定时任务入口：把到期的 REVIEWED 表单翻成 APPROVED，并向客人发送预设终审消息。
     * 条件更新保证幂等：状态已被人工处理时直接跳过，不重复发消息。
     */
    @Transactional
    public void autoFinalizeForm(Long storeId, Long formId, RegistrationReviewSettings settings) {
        int updated = registrationFormRepository.finalizeReviewed(storeId, formId, LocalDateTime.now());
        if (updated != 1) {
            return;
        }

        RegistrationForm form = registrationFormRepository.findById(formId).orElse(null);
        if (form != null) {
            RegistrationReviewLog log = new RegistrationReviewLog();
            log.setForm(form);
            log.setAction(RegistrationReviewAction.AUTO_APPROVE);
            log.setOperatorUserId(null);
            registrationReviewLogRepository.save(log);
        }

        RegistrationSendMessageRequest messageRequest = new RegistrationSendMessageRequest();
        messageRequest.setType(RegistrationMessageType.APPROVED_INFO);
        messageRequest.setContent(registrationReviewSettingsService.resolveFinalMessage(settings));
        messageRequest.setTranslateBeforeSend(true);
        try {
            registrationMessageService.sendMessage(storeId, null, formId, messageRequest);
        } catch (Exception ex) {
            logger.warn("[RegistrationAutoFinalize] send final message failed. storeId={}, formId={}, err={}",
                    storeId, formId, ex.getMessage());
        }

        publishWorkbenchInvalidationAfterCommit(storeId);
    }

    private AdminRegistrationReviewResponse sendReviewMessageIfPresent(
            Long storeId,
            Long userId,
            Long formId,
            AdminRegistrationReviewRequest req,
            RegistrationMessageType type
    ) {
        AdminRegistrationReviewResponse response = new AdminRegistrationReviewResponse();
        String guestMessage = req != null ? trimToNull(req.getGuestMessage()) : null;
        if (guestMessage == null) {
            return response;
        }

        response.setMessageAttempted(true);
        if (registrationMessageService == null) {
            response.setMessageError(ApiMessages.get("api.t.569f6d8d296d"));
            return response;
        }

        RegistrationSendMessageRequest messageRequest = new RegistrationSendMessageRequest();
        messageRequest.setType(type);
        messageRequest.setContent(guestMessage);
        messageRequest.setSenderName(req.getSenderName());
        messageRequest.setTranslateBeforeSend(true);

        try {
            response.setMessageLog(registrationMessageService.sendMessage(storeId, userId, formId, messageRequest));
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? ApiMessages.get("api.t.f8b613c0fd7e") : ex.getMessage();
            response.setMessageError(message);
        }
        return response;
    }

    private void publishWorkbenchInvalidationAfterCommit(Long storeId) {
        if (realtimeGateway == null) {
            return;
        }
        TransactionAfterCommitExecutor.execute(
                () -> realtimeGateway.broadcastWorkbenchInvalidated(storeId, "registration_review")
        );
    }

    private Reservation requireReservation(RegistrationForm form) {
        if (form.getReservation() == null || form.getReservation().getId() == null) {
            throw new RuntimeException(ApiMessages.get("api.t.b8768a4b0d04"));
        }
        return reservationRepository.findById(form.getReservation().getId())
                .orElseThrow(() -> new RuntimeException(ApiMessages.get("api.t.b8768a4b0d04")));
    }

    private static void validateReviewAllowed(RegistrationForm form, Reservation reservation) {
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new RegistrationReviewConflictException(ApiMessages.get(REVIEW_CANCELLED_RESERVATION_MESSAGE_KEY));
        }
        if (form.getStatus() != RegistrationFormStatus.SUBMITTED
                && form.getStatus() != RegistrationFormStatus.REVIEWED) {
            String message = form.getStatus() == RegistrationFormStatus.APPROVED
                    || form.getStatus() == RegistrationFormStatus.REJECTED
                    ? ApiMessages.get(REVIEW_ALREADY_REVIEWED_MESSAGE_KEY)
                    : ApiMessages.get(REVIEW_SUBMITTED_ONLY_MESSAGE_KEY);
            throw new RegistrationReviewConflictException(message);
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }
}
