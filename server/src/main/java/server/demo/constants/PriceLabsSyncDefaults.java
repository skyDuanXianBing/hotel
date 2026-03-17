package server.demo.constants;

/**
 * PriceLabs sync defaults.
 */
public final class PriceLabsSyncDefaults {
    public static final int DEFAULT_SYNC_DAYS = 365;
    public static final int INITIAL_SYNC_DAYS = 370;
    // PriceLabs calendar_trigger_url ideally supports 2 years / 730 days forward.
    public static final int MAX_SYNC_DAYS = 730;

    /**
     * When PMS changes availability (e.g. close-out / blocked), we debounce calendar pushes to PriceLabs
     * so that bulk operations can be merged into a single /calendar call.
     */
    public static final long CALENDAR_PUSH_DEBOUNCE_MS = 2000L;

    private PriceLabsSyncDefaults() {}
}
