package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.registration.PublicRegistrationGuestDTO;
import server.demo.dto.registration.PublicRegistrationResponse;
import server.demo.dto.registration.PublicRegistrationSaveRequest;
import server.demo.entity.RegistrationForm;
import server.demo.entity.RegistrationAttachment;
import server.demo.entity.RegistrationGuest;
import server.demo.entity.Reservation;
import server.demo.enums.RegistrationFormStatus;
import server.demo.enums.ResidenceType;
import server.demo.repository.RegistrationFormRepository;
import server.demo.repository.RegistrationGuestRepository;
import server.demo.repository.RegistrationAttachmentRepository;
import server.demo.repository.ReservationRepository;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class PublicRegistrationService {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private RegistrationFormRepository registrationFormRepository;

    @Autowired
    private RegistrationGuestRepository registrationGuestRepository;

    @Autowired
    private RegistrationAttachmentRepository registrationAttachmentRepository;

    @Transactional
    public PublicRegistrationResponse getOrCreate(Long storeId, String orderNumber) {
        Reservation reservation = reservationRepository.findByStoreIdAndOrderNumber(storeId, orderNumber)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        RegistrationForm form = registrationFormRepository.findByStoreIdAndReservation_Id(storeId, reservation.getId())
                .orElseGet(() -> {
                    RegistrationForm created = new RegistrationForm();
                    created.setReservation(reservation);
                    created.setStatus(RegistrationFormStatus.DRAFT);
                    created.setLastSavedAt(LocalDateTime.now());
                    return registrationFormRepository.save(created);
                });

        ensureGuestRows(form, reservation);

        return toResponse(form, reservation);
    }

    @Transactional
    public PublicRegistrationResponse saveDraft(Long storeId, String orderNumber, PublicRegistrationSaveRequest req) {
        Reservation reservation = reservationRepository.findByStoreIdAndOrderNumber(storeId, orderNumber)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        RegistrationForm form = registrationFormRepository.findByStoreIdAndReservation_Id(storeId, reservation.getId())
                .orElseThrow(() -> new RuntimeException("登记表不存在"));

        if (form.getStatus() != RegistrationFormStatus.DRAFT && form.getStatus() != RegistrationFormStatus.REJECTED) {
            throw new RuntimeException("当前状态不可修改");
        }

        upsertGuests(form, req.getGuests());
        form.setLastSavedAt(LocalDateTime.now());
        registrationFormRepository.save(form);

        return toResponse(form, reservation);
    }

    @Transactional
    public PublicRegistrationResponse submit(Long storeId, String orderNumber) {
        Reservation reservation = reservationRepository.findByStoreIdAndOrderNumber(storeId, orderNumber)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        RegistrationForm form = registrationFormRepository.findByStoreIdAndReservation_Id(storeId, reservation.getId())
                .orElseThrow(() -> new RuntimeException("登记表不存在"));

        if (form.getStatus() != RegistrationFormStatus.DRAFT && form.getStatus() != RegistrationFormStatus.REJECTED) {
            throw new RuntimeException("当前状态不可提交");
        }

        List<RegistrationGuest> guests = registrationGuestRepository.findByFormIdOrderBySortOrderAsc(form.getId());
        validateForSubmit(guests);

        form.setStatus(RegistrationFormStatus.SUBMITTED);
        form.setSubmittedAt(LocalDateTime.now());
        form.setLastSavedAt(LocalDateTime.now());
        registrationFormRepository.save(form);

        return toResponse(form, reservation);
    }

    private void validateForSubmit(List<RegistrationGuest> guests) {
        if (guests == null || guests.isEmpty()) {
            throw new RuntimeException("请填写入住人员信息");
        }

        for (RegistrationGuest guest : guests) {
            if (guest.getResidenceType() == ResidenceType.OTHER) {
                if (guest.getPassportNumber() == null || guest.getPassportNumber().isBlank()) {
                    throw new RuntimeException("海外住客必须填写Passport number");
                }
                if (guest.getId() == null || !registrationAttachmentRepository.existsByGuestIdAndType(guest.getId(), server.demo.enums.RegistrationAttachmentType.PASSPORT)) {
                    throw new RuntimeException("海外住客必须上传护照照片");
                }
            }
        }
    }

    private void ensureGuestRows(RegistrationForm form, Reservation reservation) {
        List<RegistrationGuest> existing = registrationGuestRepository.findByFormIdOrderBySortOrderAsc(form.getId());
        if (existing != null && !existing.isEmpty()) {
            return;
        }
        int count = 0;
        if (reservation.getAdults() != null) {
            count += reservation.getAdults();
        }
        if (reservation.getChildren() != null) {
            count += reservation.getChildren();
        }
        count = Math.max(count, 1);
        for (int i = 1; i <= count; i++) {
            RegistrationGuest g = new RegistrationGuest();
            g.setForm(form);
            g.setSortOrder(i);
            registrationGuestRepository.save(g);
        }
    }

    private void upsertGuests(RegistrationForm form, List<PublicRegistrationGuestDTO> guestDTOs) {
        List<RegistrationGuest> existing = registrationGuestRepository.findByFormIdOrderBySortOrderAsc(form.getId());
        Map<Long, RegistrationGuest> existingById = new HashMap<>();
        for (RegistrationGuest g : existing) {
            existingById.put(g.getId(), g);
        }

        Set<Long> keepIds = new HashSet<>();
        if (guestDTOs != null) {
            for (PublicRegistrationGuestDTO dto : guestDTOs) {
                RegistrationGuest g;
                if (dto.getId() != null && existingById.containsKey(dto.getId())) {
                    g = existingById.get(dto.getId());
                } else {
                    g = new RegistrationGuest();
                    g.setForm(form);
                }
                apply(dto, g);
                RegistrationGuest saved = registrationGuestRepository.save(g);
                keepIds.add(saved.getId());
            }
        }

        for (RegistrationGuest g : existing) {
            if (!keepIds.contains(g.getId())) {
                registrationGuestRepository.delete(g);
            }
        }
    }

    private void apply(PublicRegistrationGuestDTO dto, RegistrationGuest g) {
        g.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : g.getSortOrder());
        g.setLastName(dto.getLastName());
        g.setFirstName(dto.getFirstName());
        g.setLastNameKana(dto.getLastNameKana());
        g.setFirstNameKana(dto.getFirstNameKana());
        g.setGender(dto.getGender());
        g.setBirthday(dto.getBirthday());
        g.setNationality(dto.getNationality());
        g.setResidenceType(dto.getResidenceType());
        g.setAddress(dto.getAddress());
        g.setPhone(dto.getPhone());
        g.setEmail(dto.getEmail());
        g.setPassportNumber(dto.getPassportNumber());
        g.setPriorStay(dto.getPriorStay());
        g.setNextDestination(dto.getNextDestination());
    }

    private PublicRegistrationResponse toResponse(RegistrationForm form, Reservation reservation) {
        PublicRegistrationResponse resp = new PublicRegistrationResponse();
        resp.setFormId(form.getId());
        resp.setOrderNumber(reservation.getOrderNumber());
        resp.setStatus(form.getStatus());
        resp.setGuestName(reservation.getGuestName());
        resp.setCheckInDate(reservation.getCheckInDate());
        resp.setCheckOutDate(reservation.getCheckOutDate());
        resp.setAdults(reservation.getAdults());
        resp.setChildren(reservation.getChildren());
        resp.setLastSavedAt(form.getLastSavedAt());

        List<RegistrationGuest> guests = registrationGuestRepository.findByFormIdOrderBySortOrderAsc(form.getId());
        List<PublicRegistrationGuestDTO> guestDTOs = new ArrayList<>();
        for (RegistrationGuest g : guests) {
            PublicRegistrationGuestDTO dto = new PublicRegistrationGuestDTO();
            dto.setId(g.getId());
            dto.setSortOrder(g.getSortOrder());
            dto.setLastName(g.getLastName());
            dto.setFirstName(g.getFirstName());
            dto.setLastNameKana(g.getLastNameKana());
            dto.setFirstNameKana(g.getFirstNameKana());
            dto.setGender(g.getGender());
            dto.setBirthday(g.getBirthday());
            dto.setNationality(g.getNationality());
            dto.setResidenceType(g.getResidenceType());
            dto.setAddress(g.getAddress());
            dto.setPhone(g.getPhone());
            dto.setEmail(g.getEmail());
            dto.setPassportNumber(g.getPassportNumber());
            dto.setPriorStay(g.getPriorStay());
            dto.setNextDestination(g.getNextDestination());
            guestDTOs.add(dto);
        }
        resp.setGuests(guestDTOs);

        List<RegistrationAttachment> atts = registrationAttachmentRepository.findByFormId(form.getId());
        List<server.demo.dto.registration.PublicRegistrationAttachmentDTO> attDTOs = new ArrayList<>();
        if (atts != null) {
            for (RegistrationAttachment a : atts) {
                server.demo.dto.registration.PublicRegistrationAttachmentDTO ad = new server.demo.dto.registration.PublicRegistrationAttachmentDTO();
                ad.setId(a.getId());
                ad.setType(a.getType());
                ad.setOriginalName(a.getOriginalName());
                if (a.getGuest() != null) {
                    ad.setGuestId(a.getGuest().getId());
                }
                attDTOs.add(ad);
            }
        }
        resp.setAttachments(attDTOs);
        return resp;
    }
}
