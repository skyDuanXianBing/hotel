package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.registration.*;
import server.demo.entity.*;
import server.demo.enums.RegistrationFormStatus;
import server.demo.enums.RegistrationMessageType;
import server.demo.enums.RegistrationReviewAction;
import server.demo.enums.ReservationStatus;
import server.demo.repository.*;
import server.demo.util.StoreContextUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RegistrationAdminService {
    private static final String ROOM_NUMBER_FILTER_SENTINEL = "__REGISTRATION_ADMIN_EMPTY_ROOM_NUMBER_FILTER__";
    private static final String REVIEW_CANCELLED_RESERVATION_MESSAGE = "已取消订单不能审核登记表";
    private static final String REVIEW_SUBMITTED_ONLY_MESSAGE = "只有已提交的登记表可以审核";
    private static final String REVIEW_STATE_CHANGED_MESSAGE = "登记表状态已变更，请刷新后重试";

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
                roomNumberFilterEnabled,
                queryRoomNumbers,
                roomGroupId,
                effectiveCheckInStartDate,
                effectiveCheckInEndDate,
                effectiveCheckOutStartDate,
                effectiveCheckOutEndDate
        );

        List<AdminRegistrationListItemDTO> out = new ArrayList<>();
        for (RegistrationForm form : forms) {
            Reservation reservation = form.getReservation();
            AdminRegistrationListItemDTO dto = new AdminRegistrationListItemDTO();
            dto.setFormId(form.getId());
            dto.setOrderNumber(form.getOrderNumber());
            dto.setStatus(form.getStatus());
            dto.setSubmittedAt(form.getSubmittedAt());
            dto.setUpdatedAt(form.getUpdatedAt());
            if (reservation != null) {
                dto.setGuestName(reservation.getGuestName());
                dto.setCheckInDate(reservation.getCheckInDate());
                dto.setCheckOutDate(reservation.getCheckOutDate());
                dto.setReservationStatus(reservation.getStatus());
                dto.setChannelOrderNumber(reservation.getChannelOrderNumber());
                dto.setChannelName(reservation.getChannel() != null ? reservation.getChannel().getName() : null);
            }
            out.add(dto);
        }
        return out;
    }

    @Transactional(readOnly = true)
    public AdminRegistrationDetailDTO detail(Long formId) {
        Long storeId = StoreContextUtils.requireStoreId();
        RegistrationForm form = registrationFormRepository.findById(formId)
                .orElseThrow(() -> new RuntimeException("登记表不存在"));
        if (!storeId.equals(form.getStoreId())) {
            throw new RuntimeException("无权限");
        }

        Reservation reservation = reservationRepository.findById(form.getReservation().getId())
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        AdminRegistrationDetailDTO dto = new AdminRegistrationDetailDTO();
        dto.setFormId(form.getId());
        dto.setOrderNumber(form.getOrderNumber());
        dto.setChannelOrderNumber(reservation.getChannelOrderNumber());
        dto.setStatus(form.getStatus());
        dto.setReservationStatus(reservation.getStatus());
        dto.setSubmittedAt(form.getSubmittedAt());
        dto.setApprovedAt(form.getApprovedAt());
        dto.setRejectedAt(form.getRejectedAt());
        dto.setReviewNote(form.getReviewNote());

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

    @Transactional
    public AdminRegistrationReviewResponse approve(Long formId, AdminRegistrationReviewRequest req) {
        Long storeId = StoreContextUtils.requireStoreId();
        Long userId = StoreContextUtils.requireUserId();

        RegistrationForm form = registrationFormRepository.findById(formId)
                .orElseThrow(() -> new RuntimeException("登记表不存在"));
        if (!storeId.equals(form.getStoreId())) {
            throw new RuntimeException("无权限");
        }

        Reservation reservation = requireReservation(form);
        validateReviewAllowed(form, reservation);

        String note = req != null ? req.getNote() : null;
        int updated = registrationFormRepository.approveSubmitted(storeId, formId, note, LocalDateTime.now());
        if (updated != 1) {
            throw new RuntimeException(REVIEW_STATE_CHANGED_MESSAGE);
        }

        RegistrationReviewLog log = new RegistrationReviewLog();
        log.setForm(form);
        log.setAction(RegistrationReviewAction.APPROVE);
        log.setOperatorUserId(userId);
        log.setNote(note);
        registrationReviewLogRepository.save(log);

        return sendReviewMessageIfPresent(
                storeId,
                userId,
                formId,
                req,
                RegistrationMessageType.APPROVED_INFO
        );
    }

    @Transactional
    public AdminRegistrationReviewResponse reject(Long formId, AdminRegistrationReviewRequest req) {
        Long storeId = StoreContextUtils.requireStoreId();
        Long userId = StoreContextUtils.requireUserId();

        RegistrationForm form = registrationFormRepository.findById(formId)
                .orElseThrow(() -> new RuntimeException("登记表不存在"));
        if (!storeId.equals(form.getStoreId())) {
            throw new RuntimeException("无权限");
        }

        Reservation reservation = requireReservation(form);
        validateReviewAllowed(form, reservation);

        String note = req != null ? req.getNote() : null;
        int updated = registrationFormRepository.rejectSubmitted(storeId, formId, note, LocalDateTime.now());
        if (updated != 1) {
            throw new RuntimeException(REVIEW_STATE_CHANGED_MESSAGE);
        }

        RegistrationReviewLog log = new RegistrationReviewLog();
        log.setForm(form);
        log.setAction(RegistrationReviewAction.REJECT);
        log.setOperatorUserId(userId);
        log.setNote(note);
        registrationReviewLogRepository.save(log);

        return sendReviewMessageIfPresent(
                storeId,
                userId,
                formId,
                req,
                RegistrationMessageType.REJECT_REQUEST
        );
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
            response.setMessageError("消息服务不可用");
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
            String message = ex.getMessage() == null ? "消息发送失败" : ex.getMessage();
            response.setMessageError(message);
        }
        return response;
    }

    private Reservation requireReservation(RegistrationForm form) {
        if (form.getReservation() == null || form.getReservation().getId() == null) {
            throw new RuntimeException("订单不存在");
        }
        return reservationRepository.findById(form.getReservation().getId())
                .orElseThrow(() -> new RuntimeException("订单不存在"));
    }

    private static void validateReviewAllowed(RegistrationForm form, Reservation reservation) {
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new RuntimeException(REVIEW_CANCELLED_RESERVATION_MESSAGE);
        }
        if (form.getStatus() != RegistrationFormStatus.SUBMITTED) {
            throw new RuntimeException(REVIEW_SUBMITTED_ONLY_MESSAGE);
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
