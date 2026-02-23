package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.demo.dto.registration.PublicRegistrationAttachmentDTO;
import server.demo.dto.registration.PublicRegistrationGuestDTO;
import server.demo.dto.registration.PublicRegistrationResponse;
import server.demo.dto.registration.PublicRegistrationSaveRequest;
import server.demo.entity.RegistrationAttachment;
import server.demo.entity.RegistrationForm;
import server.demo.entity.RegistrationGuest;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.enums.RegistrationAttachmentType;
import server.demo.enums.RegistrationFormStatus;
import server.demo.enums.ResidenceType;
import server.demo.repository.RegistrationAttachmentRepository;
import server.demo.repository.RegistrationFormRepository;
import server.demo.repository.RegistrationGuestRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.util.SuRoomIdParser;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class PublicRegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(PublicRegistrationService.class);

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

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

        int maxGuests = resolveMaxGuests(storeId, reservation);

        Integer existingCount = form.getGuestCount();
        int desired = existingCount != null ? clampGuestCount(existingCount, maxGuests) : clampGuestCount(sumGuests(reservation), maxGuests);
        if (!Objects.equals(form.getGuestCount(), desired)) {
            form.setGuestCount(desired);
            registrationFormRepository.save(form);
        }

        syncGuestRows(form, desired);
        return toResponse(form, reservation, maxGuests);
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

        int maxGuests = resolveMaxGuests(storeId, reservation);

        Integer requestedCount = req != null ? req.getGuestCount() : null;
        int desired = requestedCount != null
                ? clampGuestCount(requestedCount, maxGuests)
                : (form.getGuestCount() != null ? clampGuestCount(form.getGuestCount(), maxGuests) : clampGuestCount(sumGuests(reservation), maxGuests));

        if (!Objects.equals(form.getGuestCount(), desired)) {
            form.setGuestCount(desired);
        }

        syncGuestRows(form, desired);
        applyGuestUpdates(form, req != null ? req.getGuests() : null);

        form.setLastSavedAt(LocalDateTime.now());
        registrationFormRepository.save(form);

        return toResponse(form, reservation, maxGuests);
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

        return toResponse(form, reservation, resolveMaxGuests(storeId, reservation));
    }

    private void validateForSubmit(List<RegistrationGuest> guests) {
        if (guests == null || guests.isEmpty()) {
            throw new RuntimeException("请填写入住人员信息");
        }

        for (RegistrationGuest guest : guests) {
            if (isBlank(guest.getLastName()) || isBlank(guest.getFirstName())) {
                throw new RuntimeException("姓名必填");
            }
            if (isBlank(guest.getPhone())) {
                throw new RuntimeException("电话必填");
            }
            if (guest.getBirthday() == null) {
                throw new RuntimeException("生日必填");
            }
            if (guest.getResidenceType() == null) {
                throw new RuntimeException("请选择居住地");
            }

            if (guest.getResidenceType() == ResidenceType.JAPAN) {
                if (isBlank(guest.getAddress()) || guest.getAddress().trim().length() < 5) {
                    throw new RuntimeException("住所（地址）必填");
                }
                continue;
            }

            if (guest.getResidenceType() == ResidenceType.OTHER) {
                if (isBlank(guest.getNationality())) {
                    throw new RuntimeException("国籍必填");
                }
                if (isBlank(guest.getCountry())) {
                    throw new RuntimeException("国家必填");
                }
                if (isBlank(guest.getAddress1())) {
                    throw new RuntimeException("Address1 必填");
                }
                if (isBlank(guest.getCity())) {
                    throw new RuntimeException("City 必填");
                }
                if (isBlank(guest.getPassportNumber())) {
                    throw new RuntimeException("海外住客必须填写 Passport number");
                }
                if (guest.getId() == null || !registrationAttachmentRepository.existsByGuestIdAndType(guest.getId(), RegistrationAttachmentType.PASSPORT)) {
                    throw new RuntimeException("海外住客必须上传护照照片");
                }
            }
        }
    }

    private void syncGuestRows(RegistrationForm form, int desiredCount) {
        if (form == null || form.getId() == null) {
            return;
        }
        int safeCount = Math.max(1, desiredCount);
        List<RegistrationGuest> existing = registrationGuestRepository.findByFormIdOrderBySortOrderAsc(form.getId());

        if (existing.size() > safeCount) {
            for (RegistrationGuest g : existing) {
                if (g.getSortOrder() != null && g.getSortOrder() > safeCount) {
                    deleteGuestAttachmentsBestEffort(g);
                    registrationGuestRepository.delete(g);
                }
            }
            return;
        }

        if (existing.size() < safeCount) {
            int maxSort = 0;
            for (RegistrationGuest g : existing) {
                if (g.getSortOrder() != null) {
                    maxSort = Math.max(maxSort, g.getSortOrder());
                }
            }
            for (int i = maxSort + 1; i <= safeCount; i++) {
                RegistrationGuest g = new RegistrationGuest();
                g.setForm(form);
                g.setSortOrder(i);
                registrationGuestRepository.save(g);
            }
        }
    }

    private void applyGuestUpdates(RegistrationForm form, List<PublicRegistrationGuestDTO> guestDTOs) {
        if (form == null || form.getId() == null || guestDTOs == null) {
            return;
        }
        List<RegistrationGuest> existing = registrationGuestRepository.findByFormIdOrderBySortOrderAsc(form.getId());
        Map<Long, RegistrationGuest> byId = new HashMap<>();
        Map<Integer, RegistrationGuest> bySort = new HashMap<>();
        for (RegistrationGuest g : existing) {
            if (g.getId() != null) {
                byId.put(g.getId(), g);
            }
            if (g.getSortOrder() != null) {
                bySort.put(g.getSortOrder(), g);
            }
        }

        for (PublicRegistrationGuestDTO dto : guestDTOs) {
            RegistrationGuest g = null;
            if (dto.getId() != null) {
                g = byId.get(dto.getId());
            }
            if (g == null && dto.getSortOrder() != null) {
                g = bySort.get(dto.getSortOrder());
            }
            if (g == null) {
                continue;
            }
            apply(dto, g);
            normalizeAddress(g);
            registrationGuestRepository.save(g);
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
        g.setAddress1(dto.getAddress1());
        g.setAddress2(dto.getAddress2());
        g.setCity(dto.getCity());
        g.setState(dto.getState());
        g.setCountry(dto.getCountry());

        g.setPhone(dto.getPhone());
        g.setEmail(dto.getEmail());
        g.setPassportNumber(dto.getPassportNumber());
        g.setPriorStay(dto.getPriorStay());
        g.setNextDestination(dto.getNextDestination());
    }

    private void normalizeAddress(RegistrationGuest g) {
        if (g == null || g.getResidenceType() == null) {
            return;
        }
        if (g.getResidenceType() == ResidenceType.JAPAN) {
            // clear overseas-only fields when switching to JAPAN to avoid stale data
            g.setNationality(null);
            g.setAddress1(null);
            g.setAddress2(null);
            g.setCity(null);
            g.setState(null);
            g.setCountry(null);
            g.setPassportNumber(null);
            g.setPriorStay(null);
            g.setNextDestination(null);

            deleteGuestPassportAttachmentsBestEffort(g);
            return;
        }

        if (g.getResidenceType() == ResidenceType.OTHER) {
            List<String> parts = new ArrayList<>();
            if (!isBlank(g.getAddress1())) parts.add(g.getAddress1().trim());
            if (!isBlank(g.getAddress2())) parts.add(g.getAddress2().trim());
            if (!isBlank(g.getCity())) parts.add(g.getCity().trim());
            if (!isBlank(g.getState())) parts.add(g.getState().trim());
            if (!isBlank(g.getCountry())) parts.add(g.getCountry().trim());
            String composed = String.join(", ", parts);
            // Prevent carrying over a previously-saved JAPAN address when switching to OTHER.
            g.setAddress(composed.isBlank() ? null : composed);
        }
    }

    private void deleteGuestPassportAttachmentsBestEffort(RegistrationGuest guest) {
        if (guest == null || guest.getId() == null) {
            return;
        }
        try {
            List<RegistrationAttachment> atts = registrationAttachmentRepository.findByGuestId(guest.getId());
            if (atts == null || atts.isEmpty()) {
                return;
            }
            for (RegistrationAttachment att : atts) {
                if (att.getType() != RegistrationAttachmentType.PASSPORT) {
                    continue;
                }
                try {
                    if (att.getFilePath() != null && !att.getFilePath().isBlank()) {
                        java.nio.file.Files.deleteIfExists(java.nio.file.Path.of(att.getFilePath()));
                    }
                } catch (Exception ignored) {
                }
                registrationAttachmentRepository.delete(att);
            }
        } catch (Exception ignored) {
        }
    }

    private PublicRegistrationResponse toResponse(RegistrationForm form, Reservation reservation, int maxGuests) {
        PublicRegistrationResponse resp = new PublicRegistrationResponse();
        resp.setFormId(form.getId());
        resp.setOrderNumber(reservation.getOrderNumber());
        resp.setStatus(form.getStatus());
        resp.setGuestName(reservation.getGuestName());
        resp.setCheckInDate(reservation.getCheckInDate());
        resp.setCheckOutDate(reservation.getCheckOutDate());
        resp.setAdults(reservation.getAdults());
        resp.setChildren(reservation.getChildren());
        resp.setMaxGuests(maxGuests);
        resp.setGuestCount(form.getGuestCount());
        resp.setLastSavedAt(form.getLastSavedAt());
        resp.setCheckInGuideLink(resolveCheckInGuideLink(storeIdFrom(reservation), reservation));

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
            dto.setAddress1(g.getAddress1());
            dto.setAddress2(g.getAddress2());
            dto.setCity(g.getCity());
            dto.setState(g.getState());
            dto.setCountry(g.getCountry());
            dto.setPhone(g.getPhone());
            dto.setEmail(g.getEmail());
            dto.setPassportNumber(g.getPassportNumber());
            dto.setPriorStay(g.getPriorStay());
            dto.setNextDestination(g.getNextDestination());
            guestDTOs.add(dto);
        }
        resp.setGuests(guestDTOs);

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
        resp.setAttachments(attDTOs);
        return resp;
    }

    private int resolveMaxGuests(Long storeId, Reservation reservation) {
        if (reservation == null) {
            return 4;
        }
        Room room = reservation.getRoom();
        Long assignedRoomTypeId = (room != null && room.getRoomType() != null) ? room.getRoomType().getId() : null;
        String assignedRoomTypeName = (room != null && room.getRoomType() != null) ? room.getRoomType().getName() : null;
        Integer assignedMaxGuests = (room != null && room.getRoomType() != null) ? room.getRoomType().getMaxGuests() : null;

        Integer otaResolved = resolveMaxGuestsFromOta(storeId, reservation);

        if (otaResolved != null) {
            if (otaResolved <= 1) {
                logger.info(
                        "[PublicRegistration][MaxGuests] storeId={}, orderNumber={}, resolved={}, source=ota_room_type, assignedRoomId={}, assignedRoomTypeId={}, assignedRoomTypeName={}, assignedRoomTypeMaxGuests={}, otaResolved={}, otaRoomTypeId={}, otaRoomId={}",
                        storeId,
                        reservation.getOrderNumber(),
                        otaResolved,
                        room != null ? room.getId() : null,
                        assignedRoomTypeId,
                        assignedRoomTypeName,
                        assignedMaxGuests,
                        otaResolved,
                        reservation.getOtaRoomTypeId(),
                        reservation.getOtaRoomId()
                );
            }
            if (assignedMaxGuests != null) {
                int assignedResolved = Math.max(1, assignedMaxGuests);
                if (!Objects.equals(assignedResolved, otaResolved)) {
                    logger.info(
                            "[PublicRegistration][MaxGuests] storeId={}, orderNumber={}, resolved={}, source=ota_room_type, assignedRoomId={}, assignedRoomTypeId={}, assignedRoomTypeName={}, assignedRoomTypeMaxGuests={}, otaResolved={}, otaRoomTypeId={}, otaRoomId={}",
                            storeId,
                            reservation.getOrderNumber(),
                            otaResolved,
                            room != null ? room.getId() : null,
                            assignedRoomTypeId,
                            assignedRoomTypeName,
                            assignedMaxGuests,
                            otaResolved,
                            reservation.getOtaRoomTypeId(),
                            reservation.getOtaRoomId()
                    );
                }
            }
            return otaResolved;
        }

        if (assignedMaxGuests != null) {
            int resolved = Math.max(1, assignedMaxGuests);
            if (resolved <= 1) {
                logger.info(
                        "[PublicRegistration][MaxGuests] storeId={}, orderNumber={}, resolved={}, source=assigned_room, assignedRoomId={}, assignedRoomTypeId={}, assignedRoomTypeName={}, assignedRoomTypeMaxGuests={}, otaRoomTypeId={}, otaRoomId={}",
                        storeId,
                        reservation.getOrderNumber(),
                        resolved,
                        room != null ? room.getId() : null,
                        assignedRoomTypeId,
                        assignedRoomTypeName,
                        assignedMaxGuests,
                        reservation.getOtaRoomTypeId(),
                        reservation.getOtaRoomId()
                );
            }
            return resolved;
        }
        return 4;
    }

    private Integer resolveMaxGuestsFromOta(Long storeId, Reservation reservation) {
        if (reservation == null) {
            return null;
        }

        Long roomTypeId = reservation.getOtaRoomTypeId();
        if (roomTypeId == null) {
            SuRoomIdParser.ParsedRoomId parsed = SuRoomIdParser.parse(reservation.getOtaRoomId());
            roomTypeId = parsed != null ? parsed.roomTypeId() : null;
        }

        if (roomTypeId == null) {
            return null;
        }

        Optional<RoomType> rt = roomTypeRepository.findById(roomTypeId)
                .filter(x -> storeId != null && storeId.equals(x.getStoreId()));
        if (rt.isEmpty() || rt.get().getMaxGuests() == null) {
            return null;
        }
        return Math.max(1, rt.get().getMaxGuests());
    }

    private String resolveCheckInGuideLink(Long storeId, Reservation reservation) {
        if (reservation == null) {
            return null;
        }

        Room assignedRoom = reservation.getRoom();
        if (assignedRoom != null && assignedRoom.getRoomType() != null) {
            String assignedLink = normalizeCheckInGuideLink(assignedRoom.getRoomType().getCheckInGuideLink());
            if (assignedLink != null) {
                return assignedLink;
            }
        }

        Long roomTypeId = reservation.getOtaRoomTypeId();
        if (roomTypeId == null) {
            SuRoomIdParser.ParsedRoomId parsed = SuRoomIdParser.parse(reservation.getOtaRoomId());
            roomTypeId = parsed != null ? parsed.roomTypeId() : null;
        }
        if (roomTypeId == null) {
            return null;
        }

        Optional<RoomType> rt = roomTypeRepository.findById(roomTypeId)
                .filter(x -> storeId != null && storeId.equals(x.getStoreId()));
        if (rt.isEmpty()) {
            return null;
        }
        return normalizeCheckInGuideLink(rt.get().getCheckInGuideLink());
    }

    private Long storeIdFrom(Reservation reservation) {
        return reservation != null ? reservation.getStoreId() : null;
    }

    private String normalizeCheckInGuideLink(String link) {
        if (link == null) {
            return null;
        }
        String trimmed = link.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private int sumGuests(Reservation reservation) {
        if (reservation == null) {
            return 1;
        }
        int count = 0;
        if (reservation.getAdults() != null) {
            count += reservation.getAdults();
        }
        if (reservation.getChildren() != null) {
            count += reservation.getChildren();
        }
        return Math.max(1, count);
    }

    private int clampGuestCount(Integer count, int maxGuests) {
        int v = count == null ? 1 : Math.max(1, count);
        if (maxGuests > 0) {
            v = Math.min(v, maxGuests);
        }
        return v;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private void deleteGuestAttachmentsBestEffort(RegistrationGuest guest) {
        if (guest == null || guest.getId() == null) {
            return;
        }
        try {
            List<RegistrationAttachment> atts = registrationAttachmentRepository.findByGuestId(guest.getId());
            if (atts == null || atts.isEmpty()) {
                return;
            }
            for (RegistrationAttachment att : atts) {
                try {
                    if (att.getFilePath() != null && !att.getFilePath().isBlank()) {
                        java.nio.file.Files.deleteIfExists(java.nio.file.Path.of(att.getFilePath()));
                    }
                } catch (Exception ignored) {
                }
                registrationAttachmentRepository.delete(att);
            }
        } catch (Exception ignored) {
        }
    }
}
