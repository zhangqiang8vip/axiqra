package com.axiqra.core.service;

import com.axiqra.common.domain.dto.TraceConfirmRequest;
import com.axiqra.common.domain.dto.TraceCreateRequest;
import com.axiqra.common.domain.dto.TracePathDTO;
import com.axiqra.common.domain.vo.TraceDetailVO;

import java.util.List;

/**
 * Trace 服务接口
 */
public interface TraceService {

    TraceDetailVO createDraft(Long userId, TraceCreateRequest request);

    TraceDetailVO confirm(Long userId, Long traceId, TraceConfirmRequest request);

    TraceDetailVO submit(Long userId, Long traceId);

    TraceDetailVO getDetail(Long userId, Long traceId);

    /**
     * 记录 Engineering Trace 路径
     */
    void recordTracePath(Long invocationId, TracePathDTO tracePath);

    /**
     * 提交证据路径 (Evidence Path)
     * @param traceId Trace ID
     * @param evidenceRefs 证据引用列表 (日志、diff、测试结果、截图等)
     */
    void submitEvidence(Long userId, Long traceId, List<String> evidenceRefs);
}
