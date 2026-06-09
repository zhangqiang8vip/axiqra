package com.axiqra.common.domain.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 限流检查结果
 */
@Data
@NoArgsConstructor
@Builder
public class RateLimitStatusVO {

    private int limit;
    private int currentCount;
    private int retryAfterSeconds;
    private boolean limited;

    public RateLimitStatusVO(int limit, int currentCount, int retryAfterSeconds, boolean limited) {
        this.limit = limit;
        this.currentCount = currentCount;
        this.retryAfterSeconds = retryAfterSeconds;
        this.limited = limited;
    }
}
