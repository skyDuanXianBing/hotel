package server.demo.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import server.demo.enums.PaymentAttemptStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

public final class IndependentSiteDtos {

    private IndependentSiteDtos() {
    }

    public record ConfigUpdateRequest(
            @NotBlank
            @Size(min = 3, max = 63)
            @Pattern(regexp = "[a-z0-9](?:[a-z0-9-]{1,61}[a-z0-9])?")
            String slug,
            boolean enabled,
            @NotNull Long defaultPricePlanId,
            @NotNull
            @DecimalMin(value = "-99.99")
            @DecimalMax(value = "1000.00")
            BigDecimal priceAdjustmentValue,
            boolean simulatedPaymentEnabled,
            Set<Long> publishedRoomTypeIds,
            Set<Long> publishedRoomIds
    ) {
    }

    public record ConfigResponse(
            Long id,
            String slug,
            boolean enabled,
            String publicPath,
            Long channelId,
            String channelCode,
            Long defaultPricePlanId,
            String defaultPricePlanName,
            String priceAdjustmentType,
            BigDecimal priceAdjustmentValue,
            String paymentProvider,
            boolean simulatedPaymentEnabled,
            Set<Long> publishedRoomTypeIds,
            Set<Long> publishedRoomIds,
            JsonNode publishedPageSchema,
            JsonNode draftPageSchema,
            OffsetDateTime draftUpdatedAt,
            Long draftVersion,
            OffsetDateTime publishedAt,
            Long version
    ) {
    }

    public record SiteCreateRequest(
            @NotBlank @Size(min = 1, max = 120) String name,
            @NotBlank
            @Size(min = 3, max = 63)
            @Pattern(regexp = "[a-z0-9](?:[a-z0-9-]{1,61}[a-z0-9])?")
            String slug,
            @Size(max = 30) String themeKey
    ) {
    }

    public record SiteUpdateRequest(
            @Size(min = 1, max = 120) String name,
            @NotBlank
            @Size(min = 3, max = 63)
            @Pattern(regexp = "[a-z0-9](?:[a-z0-9-]{1,61}[a-z0-9])?")
            String slug,
            boolean enabled,
            @Size(max = 30) String themeKey,
            @NotNull Long defaultPricePlanId,
            @NotNull
            @DecimalMin(value = "-99.99")
            @DecimalMax(value = "1000.00")
            BigDecimal priceAdjustmentValue,
            @Size(max = 30) String paymentProvider,
            boolean simulatedPaymentEnabled,
            Set<Long> publishedRoomTypeIds,
            Set<Long> publishedRoomIds
    ) {
    }

    public record SiteSummaryResponse(
            Long id,
            String name,
            String slug,
            boolean enabled,
            String themeKey,
            String paymentProvider,
            String publicPath,
            long pageCount,
            long publicationCount,
            boolean isDefault,
            OffsetDateTime publishedAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record SiteDetailResponse(
            Long id,
            String name,
            String slug,
            boolean enabled,
            String themeKey,
            String publicPath,
            Long channelId,
            String channelCode,
            Long defaultPricePlanId,
            String defaultPricePlanName,
            String priceAdjustmentType,
            BigDecimal priceAdjustmentValue,
            String paymentProvider,
            boolean simulatedPaymentEnabled,
            Set<Long> publishedRoomTypeIds,
            Set<Long> publishedRoomIds,
            OffsetDateTime publishedAt,
            Long version,
            List<PageSummaryResponse> pages,
            boolean stripeAvailable
    ) {
    }

    public record PageSummaryResponse(
            Long id,
            String path,
            String type,
            String title,
            boolean enabled,
            int sortOrder,
            Long roomTypeId,
            OffsetDateTime draftUpdatedAt,
            OffsetDateTime publishedAt,
            boolean hasUnpublishedChanges,
            String format
    ) {
    }

    public record PageCreateRequest(
            @NotBlank @Size(max = 255) String path,
            @NotBlank @Size(min = 1, max = 120) String title,
            @Size(max = 20) String type,
            @Size(max = 300) String seoDescription,
            Integer sortOrder
    ) {
    }

    public record PageUpdateRequest(
            @Size(min = 1, max = 120) String title,
            @Size(max = 300) String seoDescription,
            @Size(max = 255) String path,
            Boolean enabled,
            Integer sortOrder,
            JsonNode draftSchema,
            Long expectedDraftVersion
    ) {
    }

    public record PageDetailResponse(
            Long id,
            Long siteId,
            String path,
            String type,
            String title,
            String seoDescription,
            Long roomTypeId,
            boolean enabled,
            int sortOrder,
            JsonNode draftSchema,
            JsonNode publishedSchema,
            Long draftVersion,
            OffsetDateTime draftUpdatedAt,
            OffsetDateTime publishedAt,
            boolean hasAiBackup,
            String format
    ) {
    }

    public record AiEditPageRequest(
            @NotBlank @Size(min = 1, max = 2000) String instruction
    ) {
    }

    public record ImportPageFromUrlRequest(
            @NotBlank @Size(max = 2000) String url,
            @NotBlank @Size(max = 20) String mode,
            Long pageId,
            @Size(max = 255) String path,
            @Size(min = 1, max = 120) String title
    ) {
    }

    public record GenerateRoomPagesResponse(
            int generated,
            int refreshed,
            List<SkippedRoomPage> skipped,
            List<PageSummaryResponse> pages
    ) {
    }

    public record SkippedRoomPage(
            Long roomTypeId,
            String reason
    ) {
    }

    public record PublicPageNavItem(
            String path,
            String title,
            String type,
            Long roomTypeId
    ) {
    }

    public record PublicPageResponse(
            String path,
            String title,
            String seoDescription,
            String type,
            Long roomTypeId,
            JsonNode schema,
            String format,
            boolean closed
    ) {
    }

    public record PageDraftRequest(
            @NotBlank @Size(min = 10, max = 2000) String prompt,
            @Size(max = 32) String language,
            @Size(max = 30) String style
    ) {
    }

    public record PageDraftResponse(
            String providerStatus,
            String schemaVersion,
            boolean publishable,
            JsonNode pageSchema,
            List<String> warnings
    ) {
    }

    public record PageDraftSaveRequest(
            @NotNull JsonNode pageSchema,
            Long expectedDraftVersion
    ) {
    }

    public record PageDraftStateResponse(
            Long siteId,
            String schemaVersion,
            JsonNode pageSchema,
            OffsetDateTime updatedAt,
            Long draftVersion
    ) {
    }

    public record PublishPageDraftRequest(
            @NotNull Long draftVersion
    ) {
    }

    public record PublicSiteResponse(
            String slug,
            String name,
            String description,
            String logo,
            String address,
            String city,
            String state,
            String country,
            String currency,
            JsonNode pageSchema,
            List<PublicRoomType> roomTypes,
            List<PublicRoom> rooms,
            String paymentProvider,
            boolean simulatedPaymentEnabled,
            String paymentNotice,
            String themeKey,
            List<PublicPageNavItem> pages,
            String format,
            boolean closed
    ) {
    }

    public record PublicRoomType(
            Long id,
            String name,
            String code,
            String description,
            Integer maxGuests,
            Integer maxChildren,
            BigDecimal size,
            String sizeUnit,
            List<String> desktopPhotoUrls,
            List<String> mobilePhotoUrls,
            List<FacilityDTO> facilities
    ) {
    }

    public record PublicRoom(
            Long id,
            Long roomTypeId,
            String roomNumber
    ) {
    }

    public record QuoteRequest(
            @NotNull Long roomTypeId,
            @NotNull LocalDate checkInDate,
            @NotNull LocalDate checkOutDate,
            @Min(1) @Max(10) int rooms,
            @Min(1) @Max(100) int adults,
            @Min(0) @Max(100) int children
    ) {
    }

    public record QuoteResponse(
            String slug,
            Long roomTypeId,
            String roomTypeName,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            int rooms,
            int adults,
            int children,
            int availableRooms,
            String currency,
            BigDecimal adjustmentPercent,
            List<NightlyRate> nightlyRates,
            BigDecimal totalAmount,
            OffsetDateTime quotedAt,
            OffsetDateTime expiresAt
    ) {
    }

    public record NightlyRate(
            LocalDate date,
            BigDecimal baseRoomPrice,
            BigDecimal adjustedRoomPrice,
            BigDecimal extraGuestAmount,
            BigDecimal nightTotal
    ) {
    }

    public record HoldRequest(
            @NotBlank
            @Pattern(regexp = "[A-Za-z0-9._:-]{8,100}")
            String idempotencyKey,
            @NotNull Long roomTypeId,
            @NotNull LocalDate checkInDate,
            @NotNull LocalDate checkOutDate,
            @Min(1) @Max(10) int rooms,
            @Min(1) @Max(100) int adults,
            @Min(0) @Max(100) int children,
            @NotNull @Valid Guest guest
    ) {
        public QuoteRequest toQuoteRequest() {
            return new QuoteRequest(roomTypeId, checkInDate, checkOutDate, rooms, adults, children);
        }
    }

    public record Guest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 255) String phone,
            @Email @Size(max = 254) String email,
            @Size(max = 1000) String specialRequests
    ) {
    }

    public record PaymentAttemptResponse(
            String paymentAttemptId,
            PaymentAttemptStatus status,
            BigDecimal amount,
            String currency,
            OffsetDateTime expiresAt,
            OffsetDateTime completedAt,
            String groupOrderNo,
            List<String> reservationOrderNumbers,
            String failureReason,
            boolean simulated,
            String provider
    ) {
    }

    public record StripeIntentResponse(
            String clientSecret,
            String publishableKey,
            String status
    ) {
    }

    /**
     * 门店 Stripe 设置管理端视图：sk/whsec 明文永不回传，仅给 configured 布尔与尾 4 位；
     * configured = 三密钥齐全（可解密），与站点可选 STRIPE 的门槛同源。
     */
    public record StripeSettingsResponse(
            boolean configured,
            String publishableKey,
            boolean secretKeyConfigured,
            String secretKeyLast4,
            boolean webhookSecretConfigured,
            String webhookSecretLast4
    ) {
    }

    /** 三字段均可空：缺省/空串 = 保持不变，填新值 = 覆盖；不提供单独清除。 */
    public record StripeSettingsUpdateRequest(
            @Size(max = 255) String publishableKey,
            @Size(max = 255) String secretKey,
            @Size(max = 255) String webhookSecret
    ) {
    }
}
