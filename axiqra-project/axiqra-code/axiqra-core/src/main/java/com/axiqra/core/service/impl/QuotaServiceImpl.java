package com.axiqra.core.service.impl;

import com.axiqra.common.audit.AuditPort;
import com.axiqra.common.port.QuotaInfo;
import com.axiqra.common.port.QuotaPort;
import com.axiqra.common.domain.vo.QuotaStatusVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.service.QuotaService;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class QuotaServiceImpl implements QuotaService {

    private static final int DEFAULT_DAILY_LIMIT = 2000;

    private final QuotaPort quotaPort;
    private final AuditPort auditPort;

    @Override
    public QuotaStatusVO getStatus(Long userId) {
        QuotaInfo info = quotaPort.getQuotaInfo(userId);
        if (info == null) {
            info = QuotaInfo.of(0, DEFAULT_DAILY_LIMIT);
        }
        return toView(info);
    }

    @Override
    public QuotaStatusVO consumeOrThrow(Long userId, String quotaType) {
        QuotaInfo before = quotaPort.getQuotaInfo(userId);
        if (before == null) {
            before = QuotaInfo.of(0, DEFAULT_DAILY_LIMIT);
        }
        boolean consumed = quotaPort.tryConsumeQuota(userId);
        QuotaInfo after = quotaPort.getQuotaInfo(userId);
        if (after == null) {
            int used = consumed ? before.getUsed() + 1 : before.getUsed();
            after = QuotaInfo.of(used, before.getLimit() > 0 ? before.getLimit() : DEFAULT_DAILY_LIMIT);
        }

        auditPort.logQuotaEvent(new AuditPort.QuotaEvent(
                MDC.get("traceId"),
                "user:" + userId,
                quotaType,
                1,
                after.getUsed(),
                after.getRemaining(),
                nextResetAt(),
                consumed ? "consumed" : "exceeded"
        ));

        if (!consumed) {
            throw new BizException(
                    ErrorCode.QUOTA_EXCEEDED,
                    "每日配额已用尽，当前请求被拒绝（limit=" + after.getLimit() + "）"
            );
        }
        return toView(after);
    }

    private QuotaStatusVO toView(QuotaInfo info) {
        return QuotaStatusVO.builder()
                .used(info.getUsed())
                .remaining(info.getRemaining())
                .limit(info.getLimit())
                .exceeded(info.isExceeded())
                .resetAt(nextResetAt())
                .build();
    }

    private OffsetDateTime nextResetAt() {
        return OffsetDateTime.now(ZoneOffset.UTC)
                .plusDays(1)
                .truncatedTo(ChronoUnit.DAYS);
    }
}
