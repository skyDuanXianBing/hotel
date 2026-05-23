package server.demo.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import server.demo.entity.Channel;
import server.demo.entity.OtaIntegration;
import server.demo.entity.PricePlan;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.entity.SuMessage;
import server.demo.entity.SuMessageThread;
import server.demo.entity.SuReservationWebhookEvent;
import server.demo.repository.SuMessageRepository;
import server.demo.service.ChannelE2ETestSupportService.ChannelSummary;
import server.demo.service.ChannelE2ETestSupportService.MessagingMessageSummary;
import server.demo.service.ChannelE2ETestSupportService.MessagingThreadSummary;
import server.demo.service.ChannelE2ETestSupportService.OtaIntegrationSummary;
import server.demo.service.ChannelE2ETestSupportService.PricePlanSummary;
import server.demo.service.ChannelE2ETestSupportService.ReservationSummary;
import server.demo.service.ChannelE2ETestSupportService.RoomSummary;
import server.demo.service.ChannelE2ETestSupportService.RoomTypeSummary;
import server.demo.service.ChannelE2ETestSupportService.WebhookEventSummary;

import java.util.ArrayList;
import java.util.List;

@Component
class ChannelE2ETestSupportResponseMapper {

    private static final int DEFAULT_EVENT_LIMIT = 50;
    private static final int MAX_EVENT_LIMIT = 200;
    private static final int DEFAULT_MESSAGING_THREAD_LIMIT = 20;
    private static final int MAX_MESSAGING_THREAD_LIMIT = 100;
    private static final int DEFAULT_MESSAGING_MESSAGE_LIMIT = 20;
    private static final int MAX_MESSAGING_MESSAGE_LIMIT = 100;

    private final SuMessageRepository suMessageRepository;

    ChannelE2ETestSupportResponseMapper(SuMessageRepository suMessageRepository) {
        this.suMessageRepository = suMessageRepository;
    }

    List<ChannelSummary> toChannelSummaries(List<Channel> channels) {
        List<ChannelSummary> items = new ArrayList<>();
        for (Channel channel : channels) {
            items.add(toChannelSummary(channel));
        }
        return items;
    }

    List<RoomTypeSummary> toRoomTypeSummaries(List<RoomType> roomTypes) {
        List<RoomTypeSummary> items = new ArrayList<>();
        for (RoomType roomType : roomTypes) {
            items.add(new RoomTypeSummary(
                    roomType.getId(),
                    roomType.getName(),
                    roomType.getCode(),
                    roomType.getTotalRooms(),
                    roomType.getMaxGuests(),
                    roomType.getMaxChildOccupancy(),
                    roomType.getDefaultPrice(),
                    roomType.getSuRoomType()
            ));
        }
        return items;
    }

    List<RoomSummary> toRoomSummaries(List<Room> rooms) {
        List<RoomSummary> items = new ArrayList<>();
        for (Room room : rooms) {
            items.add(toRoomSummary(room));
        }
        return items;
    }

    List<PricePlanSummary> toPricePlanSummaries(List<PricePlan> pricePlans) {
        List<PricePlanSummary> items = new ArrayList<>();
        for (PricePlan pricePlan : pricePlans) {
            items.add(new PricePlanSummary(
                    pricePlan.getId(),
                    pricePlan.getName(),
                    pricePlan.getNameEn(),
                    pricePlan.getMinNights(),
                    pricePlan.getMaxNights(),
                    pricePlan.getIncludeMeal(),
                    pricePlan.getDerivationType(),
                    pricePlan.getBasePlanId()
            ));
        }
        return items;
    }

    List<OtaIntegrationSummary> toOtaIntegrationSummaries(List<OtaIntegration> otaIntegrations) {
        List<OtaIntegrationSummary> items = new ArrayList<>();
        for (OtaIntegration integration : otaIntegrations) {
            Long defaultPricePlanId = null;
            if (integration.getDefaultPricePlan() != null) {
                defaultPricePlanId = integration.getDefaultPricePlan().getId();
            }
            items.add(new OtaIntegrationSummary(
                    integration.getId(),
                    integration.getName(),
                    integration.getCode(),
                    integration.getEnabled(),
                    integration.getIsConnected(),
                    integration.getPropertyId(),
                    integration.getSuPropertyId(),
                    integration.getAutoSyncPrice(),
                    defaultPricePlanId,
                    hasText(integration.getApiKey()),
                    hasText(integration.getApiSecret()),
                    hasText(integration.getSuWidgetToken())
            ));
        }
        return items;
    }

    ReservationSummary toReservationSummary(Reservation reservation) {
        ChannelSummary channelSummary = null;
        if (reservation.getChannel() != null) {
            channelSummary = toChannelSummary(reservation.getChannel());
        }

        RoomSummary roomSummary = null;
        if (reservation.getRoom() != null) {
            roomSummary = toRoomSummary(reservation.getRoom());
        }

        return new ReservationSummary(
                reservation.getId(),
                reservation.getOrderNumber(),
                reservation.getChannelOrderNumber(),
                reservation.getExternalBookingKey(),
                reservation.getSuHotelId(),
                reservation.getSuReservationId(),
                reservation.getReservationNotifId(),
                reservation.getRoomReservationId(),
                reservation.getStatus() != null ? reservation.getStatus().name() : null,
                reservation.getGuestName(),
                reservation.getGuestPhone(),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                reservation.getAdults(),
                reservation.getChildren(),
                reservation.getTotalAmount(),
                reservation.getCurrencyCode(),
                reservation.getOtaRoomId(),
                reservation.getOtaRoomTypeId(),
                reservation.getOtaRoomNumber(),
                channelSummary,
                roomSummary,
                reservation.getCreatedAt(),
                reservation.getUpdatedAt()
        );
    }

    WebhookEventSummary toWebhookEventSummary(SuReservationWebhookEvent event) {
        return new WebhookEventSummary(
                event.getId(),
                event.getStoreId(),
                event.getHotelId(),
                event.getReservationNotifId(),
                event.getEventType() != null ? event.getEventType().name() : null,
                event.getStatus() != null ? event.getStatus().name() : null,
                event.getRetryCount(),
                event.getNextRetryAt(),
                event.getLastError(),
                event.getPayloadJson(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }

    MessagingThreadSummary toMessagingThreadSummary(
            Long storeId,
            SuMessageThread thread,
            int messageLimit
    ) {
        List<SuMessage> messages = suMessageRepository.findByStoreIdAndThreadIdOrderBySentAtAsc(
                storeId,
                thread.getId(),
                PageRequest.of(0, messageLimit)
        );

        List<MessagingMessageSummary> messageSummaries = new ArrayList<>();
        for (SuMessage message : messages) {
            messageSummaries.add(toMessagingMessageSummary(message));
        }

        return new MessagingThreadSummary(
                thread.getId(),
                thread.getStoreId(),
                thread.getSuHotelId(),
                thread.getChannelId(),
                thread.getThreadKey(),
                thread.getThreadId(),
                thread.getBookingId(),
                thread.getGuestId(),
                thread.getBookingFlag(),
                thread.getListingId(),
                thread.getGuestName(),
                thread.getListingName(),
                thread.getLastMessage(),
                thread.getLastActivity(),
                thread.getClosed(),
                thread.getCreatedAt(),
                thread.getUpdatedAt(),
                messageSummaries
        );
    }

    int clampEventLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_EVENT_LIMIT;
        }
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, MAX_EVENT_LIMIT);
    }

    int clampMessagingThreadLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_MESSAGING_THREAD_LIMIT;
        }
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, MAX_MESSAGING_THREAD_LIMIT);
    }

    int clampMessagingMessageLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_MESSAGING_MESSAGE_LIMIT;
        }
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, MAX_MESSAGING_MESSAGE_LIMIT);
    }

    private ChannelSummary toChannelSummary(Channel channel) {
        Long defaultPricePlanId = null;
        if (channel.getDefaultPricePlan() != null) {
            defaultPricePlanId = channel.getDefaultPricePlan().getId();
        }
        return new ChannelSummary(
                channel.getId(),
                channel.getName(),
                channel.getCode(),
                channel.getType() != null ? channel.getType().name() : null,
                channel.getEnabled(),
                channel.getIsActive(),
                channel.getAutoSyncPrice(),
                channel.getOtaPropertyId(),
                defaultPricePlanId
        );
    }

    private RoomSummary toRoomSummary(Room room) {
        RoomType roomType = room.getRoomType();
        Long roomTypeId = null;
        String roomTypeName = null;
        String roomTypeCode = null;
        if (roomType != null) {
            roomTypeId = roomType.getId();
            roomTypeName = roomType.getName();
            roomTypeCode = roomType.getCode();
        }
        return new RoomSummary(
                room.getId(),
                room.getRoomNumber(),
                roomTypeId,
                roomTypeName,
                roomTypeCode,
                room.getFloor(),
                room.getStatus() != null ? room.getStatus().name() : null
        );
    }

    private MessagingMessageSummary toMessagingMessageSummary(SuMessage message) {
        Long threadDatabaseId = null;
        if (message.getThread() != null) {
            threadDatabaseId = message.getThread().getId();
        }

        return new MessagingMessageSummary(
                message.getId(),
                threadDatabaseId,
                message.getExternalMessageId(),
                message.getSenderType() != null ? message.getSenderType().name() : null,
                message.getSenderName(),
                message.getContent(),
                message.getSentAt(),
                message.getIsRead(),
                message.getDeliveryStatus(),
                message.getRawJson()
        );
    }

    private boolean hasText(String value) {
        return normalize(value) != null;
    }

    private String normalize(String value) {
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
