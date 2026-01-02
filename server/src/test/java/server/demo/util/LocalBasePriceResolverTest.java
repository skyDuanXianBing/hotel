package server.demo.util;

import org.junit.jupiter.api.Test;
import server.demo.entity.RoomPrice;
import server.demo.entity.RoomType;
import server.demo.entity.RoomTypePricePlan;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class LocalBasePriceResolverTest {

    @Test
    void resolve_roomPriceWinsAndCarriesMinMaxStay() {
        RoomType roomType = new RoomType();
        roomType.setWeekdayPrice(new BigDecimal("100"));

        RoomPrice roomPrice = new RoomPrice();
        roomPrice.setPrice(new BigDecimal("123.45"));
        roomPrice.setMinStay(2);
        roomPrice.setMaxStay(10);

        LocalBasePriceResolver.Result res = LocalBasePriceResolver.resolve(
                roomPrice,
                null,
                roomType,
                LocalDate.of(2025, 12, 26)
        );

        assertEquals(new BigDecimal("123.45"), res.basePrice());
        assertEquals(2, res.minStay());
        assertEquals(10, res.maxStay());
        assertEquals(LocalBasePriceResolver.Source.ROOM_PRICE, res.source());
    }

    @Test
    void resolve_roomTypePricePlanWinsWhenNoRoomPrice() {
        RoomTypePricePlan rtpp = new RoomTypePricePlan();
        rtpp.setFridayPrice(new BigDecimal("200"));

        LocalBasePriceResolver.Result res = LocalBasePriceResolver.resolve(
                null,
                rtpp,
                new RoomType(),
                LocalDate.of(2025, 12, 26) // Friday
        );

        assertEquals(new BigDecimal("200"), res.basePrice());
        assertNull(res.minStay());
        assertNull(res.maxStay());
        assertEquals(LocalBasePriceResolver.Source.ROOM_TYPE_PRICE_PLAN, res.source());
    }

    @Test
    void resolve_roomTypeFallbackUsesWeekendWeekdayDefaultOrder() {
        RoomType roomType = new RoomType();
        roomType.setWeekdayPrice(new BigDecimal("111"));
        roomType.setWeekendPrice(new BigDecimal("222"));
        roomType.setDefaultPrice(new BigDecimal("333"));

        LocalBasePriceResolver.Result sat = LocalBasePriceResolver.resolve(
                null,
                null,
                roomType,
                LocalDate.of(2025, 12, 27) // Saturday
        );
        assertEquals(new BigDecimal("222"), sat.basePrice());

        LocalBasePriceResolver.Result mon = LocalBasePriceResolver.resolve(
                null,
                null,
                roomType,
                LocalDate.of(2025, 12, 29) // Monday
        );
        assertEquals(new BigDecimal("111"), mon.basePrice());
    }

    @Test
    void resolve_roomTypeFallbackFallsBackToDefaultWhenWeekdayWeekendMissing() {
        RoomType roomType = new RoomType();
        roomType.setDefaultPrice(new BigDecimal("999"));

        LocalBasePriceResolver.Result res = LocalBasePriceResolver.resolve(
                null,
                null,
                roomType,
                LocalDate.of(2025, 12, 29)
        );

        assertEquals(new BigDecimal("999"), res.basePrice());
        assertEquals(LocalBasePriceResolver.Source.ROOM_TYPE, res.source());
    }
}

