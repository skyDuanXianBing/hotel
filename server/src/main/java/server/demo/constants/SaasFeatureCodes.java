package server.demo.constants;

/**
 * SaaS 功能字典 feature_code 常量（种子数据见 db_migration_saas_v1.sql）。
 */
public final class SaasFeatureCodes {

    /** 独立站模块（BOOLEAN） */
    public static final String INDEPENDENT_WEBSITE = "independent_website";

    /** AI 建站生成次数（QUOTA，按月重置，按"生成动作"扣 1 次） */
    public static final String AI_WEBSITE_GEN = "ai_website_gen";

    /** 可存在房间数量上限（CAPACITY，软限制：仅阻断新增） */
    public static final String ROOM_COUNT = "room_count";

    private SaasFeatureCodes() {}
}
