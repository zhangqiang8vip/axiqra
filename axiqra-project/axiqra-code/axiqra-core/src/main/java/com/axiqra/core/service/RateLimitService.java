package com.axiqra.core.service;

import com.axiqra.common.domain.vo.RateLimitStatusVO;
import com.axiqra.common.exception.BizException;

public interface RateLimitService {

    /**
     * 执行限流检查并累计当前窗口内计数。
     *
     * @param userId 当前用户 ID
     * @param limiterKey 限流桶标识，当前 Connect 链路使用如 {@code connect:probe}、{@code connect:create} 这样的非空 key
     * @return 限流状态，包含窗口上限 {@code limit}、当前窗口累计值 {@code currentCount}、建议重试秒数 {@code retryAfterSeconds} 以及是否已限流 {@code limited}
     * @throws BizException 当当前窗口请求数超过限制时抛出 {@code ErrorCode.RATE_LIMITED}
     */
    RateLimitStatusVO checkOrThrow(Long userId, String limiterKey);
}
