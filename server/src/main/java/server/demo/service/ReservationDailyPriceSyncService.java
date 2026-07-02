package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import server.demo.entity.Reservation;
import server.demo.entity.ReservationDailyPrice;
import server.demo.repository.ReservationDailyPriceRepository;
import server.demo.util.SuReservationParser;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReservationDailyPriceSyncService {

    private static final Logger dailyPriceLogger = LoggerFactory.getLogger("SU_DAILY_PRICE");

    private final ReservationDailyPriceRepository dailyPriceRepository;

    public ReservationDailyPriceSyncService(ReservationDailyPriceRepository dailyPriceRepository) {
        this.dailyPriceRepository = dailyPriceRepository;
    }

    public List<ReservationDailyPrice> syncDailyPrices(
            Long storeId,
            String suHotelId,
            Reservation reservation,
            JsonNode reservationNode,
            JsonNode roomStay
    ) {
        if (storeId == null || reservation == null || reservation.getId() == null) {
            return List.of();
        }

        LocalDate checkIn = reservation.getCheckInDate();
        LocalDate checkOut = reservation.getCheckOutDate();
        List<SuReservationParser.DailyRoomPrice> parsedPrices = SuReservationParser.extractDailyPrices(
                reservationNode,
                roomStay,
                checkIn,
                checkOut
        );
        if (parsedPrices.isEmpty()) {
            dailyPriceLogger.debug(
                    "[ReservationDailyPrice] skip. storeId={}, reservationId={}, suReservationId={}, roomReservationId={}, source=SU_DAILY_PRICE, reason=no_daily_price",
                    storeId,
                    reservation.getId(),
                    reservation.getSuReservationId(),
                    reservation.getRoomReservationId()
            );
            return List.of();
        }

        dailyPriceRepository.deleteByStoreIdAndReservationId(storeId, reservation.getId());

        List<ReservationDailyPrice> rows = new ArrayList<>();
        for (SuReservationParser.DailyRoomPrice parsedPrice : parsedPrices) {
            ReservationDailyPrice row = new ReservationDailyPrice();
            row.setStoreId(storeId);
            row.setReservation(reservation);
            row.setSuHotelId(suHotelId);
            row.setSuReservationId(reservation.getSuReservationId());
            row.setRoomReservationId(reservation.getRoomReservationId());
            row.setPriceDate(parsedPrice.priceDate());
            row.setCurrencyCode(reservation.getCurrencyCode());
            row.setRateId(parsedPrice.rateId());
            row.setMealplanId(parsedPrice.mealplanId());
            row.setMealplan(parsedPrice.mealplan());
            row.setTaxAmount(parsedPrice.taxAmount());
            row.setPriceBeforeTax(parsedPrice.priceBeforeTax());
            row.setPriceAfterTax(parsedPrice.priceAfterTax());
            rows.add(row);
        }

        List<ReservationDailyPrice> savedRows = dailyPriceRepository.saveAll(rows);
        for (ReservationDailyPrice row : savedRows) {
            dailyPriceLogger.info(
                    "[ReservationDailyPrice] saved source=SU_DAILY_PRICE storeId={} suReservationId={} roomReservationId={} reservationId={} date={} priceBeforeTax={} tax={} priceAfterTax={} currency={}",
                    row.getStoreId(),
                    row.getSuReservationId(),
                    row.getRoomReservationId(),
                    row.getReservation() != null ? row.getReservation().getId() : null,
                    row.getPriceDate(),
                    row.getPriceBeforeTax(),
                    row.getTaxAmount(),
                    row.getPriceAfterTax(),
                    row.getCurrencyCode()
            );
        }

        dailyPriceLogger.info(
                "[ReservationDailyPrice] summary source=SU_DAILY_PRICE storeId={} suReservationId={} roomReservationId={} reservationId={} rowCount={} checkIn={} checkOut={} currency={}",
                storeId,
                reservation.getSuReservationId(),
                reservation.getRoomReservationId(),
                reservation.getId(),
                savedRows.size(),
                checkIn,
                checkOut,
                reservation.getCurrencyCode()
        );
        return savedRows;
    }
}
