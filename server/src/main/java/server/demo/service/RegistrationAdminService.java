package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.registration.*;
import server.demo.entity.*;
import server.demo.enums.RegistrationFormStatus;
import server.demo.enums.RegistrationReviewAction;
import server.demo.repository.*;
import server.demo.util.StoreContextUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RegistrationAdminService {

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

    @Transactional(readOnly = true)
    public List<AdminRegistrationListItemDTO> list(RegistrationFormStatus status) {
        Long storeId = StoreContextUtils.requireStoreId();
        List<RegistrationForm> forms = (status == null)
                ? registrationFormRepository.findByStoreIdOrderByUpdatedAtDesc(storeId)
                : registrationFormRepository.findByStoreIdAndStatusOrderByUpdatedAtDesc(storeId, status);

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
        dto.setStatus(form.getStatus());
        dto.setSubmittedAt(form.getSubmittedAt());
        dto.setApprovedAt(form.getApprovedAt());
        dto.setRejectedAt(form.getRejectedAt());
        dto.setReviewNote(form.getReviewNote());

        dto.setGuestName(reservation.getGuestName());
        dto.setCheckInDate(reservation.getCheckInDate());
        dto.setCheckOutDate(reservation.getCheckOutDate());
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

    @Transactional
    public void approve(Long formId, AdminRegistrationReviewRequest req) {
        Long storeId = StoreContextUtils.requireStoreId();
        Long userId = StoreContextUtils.requireUserId();

        RegistrationForm form = registrationFormRepository.findById(formId)
                .orElseThrow(() -> new RuntimeException("登记表不存在"));
        if (!storeId.equals(form.getStoreId())) {
            throw new RuntimeException("无权限");
        }

        form.setStatus(RegistrationFormStatus.APPROVED);
        form.setApprovedAt(LocalDateTime.now());
        form.setReviewNote(req != null ? req.getNote() : null);
        registrationFormRepository.save(form);

        RegistrationReviewLog log = new RegistrationReviewLog();
        log.setForm(form);
        log.setAction(RegistrationReviewAction.APPROVE);
        log.setOperatorUserId(userId);
        log.setNote(req != null ? req.getNote() : null);
        registrationReviewLogRepository.save(log);
    }

    @Transactional
    public void reject(Long formId, AdminRegistrationReviewRequest req) {
        Long storeId = StoreContextUtils.requireStoreId();
        Long userId = StoreContextUtils.requireUserId();

        RegistrationForm form = registrationFormRepository.findById(formId)
                .orElseThrow(() -> new RuntimeException("登记表不存在"));
        if (!storeId.equals(form.getStoreId())) {
            throw new RuntimeException("无权限");
        }

        form.setStatus(RegistrationFormStatus.REJECTED);
        form.setRejectedAt(LocalDateTime.now());
        form.setReviewNote(req != null ? req.getNote() : null);
        registrationFormRepository.save(form);

        RegistrationReviewLog log = new RegistrationReviewLog();
        log.setForm(form);
        log.setAction(RegistrationReviewAction.REJECT);
        log.setOperatorUserId(userId);
        log.setNote(req != null ? req.getNote() : null);
        registrationReviewLogRepository.save(log);
    }
}
