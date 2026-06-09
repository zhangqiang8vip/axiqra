package com.axiqra.common.domain.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 配额检查结果
 */
@Data
@NoArgsConstructor
@Builder
public class QuotaStatusVO {

    private int used;
    private int remaining;
    private int limit;
    private boolean exceeded;
    private OffsetDateTime resetAt;

    public QuotaStatusVO(int used, int remaining, int limit, boolean exceeded, OffsetDateTime resetAt) {
        this.used = used;
        this.remaining = remaining;
        this.limit = limit;
        this.exceeded = exceeded;
        this.resetAt = resetAt;
    }
}
