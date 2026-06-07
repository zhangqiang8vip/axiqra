package com.axiqra.common.port;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 配额信息（查询结果）
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class QuotaInfo {

    /** 当日已使用配额 */
    private int used;

    /** 当日剩余配额（>= 0） */
    private int remaining;

    /** 当日配额上限 */
    private int limit;

    /** 是否已超限 */
    private boolean exceeded;

    public static QuotaInfo of(int used, int limit) {
        if (used < 0) {
            throw new IllegalArgumentException("used must be non-negative, but was: " + used);
        }
        if (limit < 0) {
            throw new IllegalArgumentException("limit must be non-negative, but was: " + limit);
        }
        int remaining = Math.max(0, limit - used);
        return new QuotaInfo(used, remaining, limit, used > limit);
    }
}
