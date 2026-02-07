package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import server.demo.dto.ApiResponse;
import server.demo.dto.registration.PublicRegistrationBookingResponse;
import server.demo.dto.registration.PublicRegistrationResponse;
import server.demo.dto.registration.PublicRegistrationSaveRequest;
import server.demo.service.PublicRegistrationBookingService;
import server.demo.service.PublicRegistrationService;
import server.demo.service.RegistrationLinkService;

@RestController
@RequestMapping("/api/public/registration-booking")
public class PublicRegistrationBookingController {

    private static final Logger logger = LoggerFactory.getLogger(PublicRegistrationBookingController.class);

    @Autowired
    private RegistrationLinkService registrationLinkService;

    @Autowired
    private PublicRegistrationBookingService bookingService;

    @Autowired
    private PublicRegistrationService publicRegistrationService;

    @GetMapping("/{bookingKey}")
    public ApiResponse<PublicRegistrationBookingResponse> getBooking(
            @PathVariable String bookingKey,
            @RequestParam(name = "t") String token
    ) {
        RegistrationLinkService.Claims claims = registrationLinkService.verifyToken(bookingKey, token);
        logger.info("[PublicRegistration][Booking] storeId={}, bookingKey={}", claims.getStoreId(), bookingKey);
        PublicRegistrationBookingResponse resp = bookingService.getBooking(claims.getStoreId(), bookingKey);
        return ApiResponse.success("ok", resp);
    }

    @PutMapping("/{bookingKey}/rooms/{orderNumber}/guest-count")
    public ApiResponse<PublicRegistrationResponse> setGuestCount(
            @PathVariable String bookingKey,
            @PathVariable String orderNumber,
            @RequestParam(name = "t") String token,
            @RequestBody PublicRegistrationSaveRequest req
    ) {
        RegistrationLinkService.Claims claims = registrationLinkService.verifyToken(bookingKey, token);
        logger.info("[PublicRegistration][BookingGuestCount] storeId={}, bookingKey={}, orderNumber={}, guestCount={}",
                claims.getStoreId(),
                bookingKey,
                orderNumber,
                req != null ? req.getGuestCount() : null
        );
        bookingService.assertRoomBelongsToBooking(claims.getStoreId(), bookingKey, orderNumber);

        // reuse existing saveDraft logic; only guestCount is expected from booking page
        PublicRegistrationResponse resp = publicRegistrationService.saveDraft(claims.getStoreId(), orderNumber, req);
        return ApiResponse.success("ok", resp);
    }
}
