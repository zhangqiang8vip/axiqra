package com.axiqra.core.service;

import com.axiqra.common.domain.dto.TraceConfirmRequest;
import com.axiqra.common.domain.dto.TraceCreateRequest;
import com.axiqra.common.domain.vo.TraceDetailVO;

/**
 * Trace 服务接口
 */
public interface TraceService {

    TraceDetailVO createDraft(Long userId, TraceCreateRequest request);

    TraceDetailVO confirm(Long userId, Long traceId, TraceConfirmRequest request);

    TraceDetailVO submit(Long userId, Long traceId);

    TraceDetailVO getDetail(Long userId, Long traceId);
}
