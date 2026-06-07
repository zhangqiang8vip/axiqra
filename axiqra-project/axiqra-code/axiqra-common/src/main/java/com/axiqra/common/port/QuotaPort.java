package com.axiqra.common.port;

/**
 * 配额端口（Quota Port）
 * <p>
 * 定义配额查询和扣减接口，由具体实现（如 Redis 实现）注入。
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
public interface QuotaPort {

    /**
     * 查询用户当日配额信息（推荐）
     *
     * @param userId 用户 ID
     * @return 配额信息（包含已使用、剩余、是否超限等）
     */
    QuotaInfo getQuotaInfo(Long userId);

    /**
     * 查询用户当日剩余配额（已废弃，请使用 getQuotaInfo）
     *
     * @param userId 用户 ID
     * @return 剩余配额（已保证 >= 0）
     * @deprecated 请使用 {@link #getQuotaInfo(Long)}，该方法将在未来版本移除
     */
    @Deprecated
    default int getRemainingQuota(Long userId) {
        QuotaInfo info = getQuotaInfo(userId);
        return info != null ? info.getRemaining() : 0;
    }

    /**
     * 尝试扣减配额（原子操作，超额返回 false）
     *
     * @param userId 用户 ID
     * @return true=扣减成功，false=配额已用尽
     */
    boolean tryConsumeQuota(Long userId);

    /**
     * 重置用户每日配额（定时任务调用）
     *
     * @param userId 用户 ID
     */
    void resetDailyQuota(Long userId);

    /**
     * 获取今日已使用配额
     *
     * @param userId 用户 ID
     * @return 已使用配额数
     */
    int getUsedQuota(Long userId);
}
