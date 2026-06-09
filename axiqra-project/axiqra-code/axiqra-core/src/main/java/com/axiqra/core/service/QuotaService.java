package com.axiqra.core.service;

import com.axiqra.common.domain.vo.QuotaStatusVO;
import com.axiqra.common.exception.BizException;

public interface QuotaService {

    QuotaStatusVO getStatus(Long userId);

    /**
     * 消耗一次配额。
     *
     * @param userId 当前用户 ID
     * @param quotaType 配额类型标识，当前 Connect 会话链路使用 {@code connect_session_daily}
     * @return 消耗后的配额状态
     * @throws BizException 当配额耗尽时抛出 {@code ErrorCode.QUOTA_EXCEEDED}
     */
    QuotaStatusVO consumeOrThrow(Long userId, String quotaType);
}
