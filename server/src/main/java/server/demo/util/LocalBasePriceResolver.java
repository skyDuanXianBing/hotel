package server.demo.util;

import server.demo.entity.PricePlan;
import server.demo.entity.RoomPrice;
import server.demo.entity.RoomType;
import server.demo.entity.RoomTypePricePlan;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * 本地兜底价格解析：
 * room_prices(房型+价格计划+日期) > room_type_price_plans(周几价) > room_type(weekday/weekend/default)
 */
public final class LocalBasePriceResolver {

    private LocalBasePriceResolver() {}

    public enum Source {
        ROOM_PRICE,
        ROOM_TYPE_PRICE_PLAN,
        ROOM_TYPE
    }

    public record Result(
            BigDecimal basePrice,
            Integer minStay,
            Integer maxStay,
            Source source
    ) {}

    public static Result resolve(RoomPrice roomPrice, RoomTypePricePlan roomTypePricePlan, RoomType roomType, LocalDate date) {
        if (date == null) {
            return new Result(null, null, null, Source.ROOM_TYPE);
        }

        if (roomPrice != null && roomPrice.getPrice() != null) {
            return new Result(roomPrice.getPrice(), roomPrice.getMinStay(), roomPrice.getMaxStay(), Source.ROOM_PRICE);
        }

        if (roomTypePricePlan != null) {
            BigDecimal weekly = resolveWeeklyPrice(roomTypePricePlan, date.getDayOfWeek());
            if (weekly != null) {
                return new Result(weekly, null, null, Source.ROOM_TYPE_PRICE_PLAN);
            }
        }

        if (roomType == null) {
            return new Result(null, null, null, Source.ROOM_TYPE);
        }

        boolean isWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
        BigDecimal base = null;
        if (isWeekend && roomType.getWeekendPrice() != null) {
            base = roomType.getWeekendPrice();
        } else if (!isWeekend && roomType.getWeekdayPrice() != null) {
            base = roomType.getWeekdayPrice();
        } else if (roomType.getDefaultPrice() != null) {
            base = roomType.getDefaultPrice();
        }

        return new Result(base, null, null, Source.ROOM_TYPE);
    }

    private static BigDecimal resolveWeeklyPrice(RoomTypePricePlan roomTypePricePlan, DayOfWeek dayOfWeek) {
        if (roomTypePricePlan == null || dayOfWeek == null) {
            return null;
        }

        return switch (dayOfWeek) {
            case MONDAY -> roomTypePricePlan.getMondayPrice();
            case TUESDAY -> roomTypePricePlan.getTuesdayPrice();
            case WEDNESDAY -> roomTypePricePlan.getWednesdayPrice();
            case THURSDAY -> roomTypePricePlan.getThursdayPrice();
            case FRIDAY -> roomTypePricePlan.getFridayPrice();
            case SATURDAY -> roomTypePricePlan.getSaturdayPrice();
            case SUNDAY -> roomTypePricePlan.getSundayPrice();
        };
    }
}

