package com.axiqra.core.service;

import com.axiqra.common.domain.dto.SearchRequest;
import com.axiqra.common.domain.entity.CandidateSeedEntity;

public interface CandidateSeedService {

    CandidateSeedCreationResult createOrReuseCandidateSeed(Long userId, SearchRequest request);

    /**
     * 带自定义 coverage_gap / evidence_hint 的升级版
     *
     * @param userId         作者 ID
     * @param request        搜索请求 DTO（包含 query/workspaceId/techStack/domain）
     * @param customGap      自定义覆盖空白描述，非空时覆盖自动推断
     * @param evidenceHint   证据提示，可选
     */
    CandidateSeedCreationResult createOrReuseCandidateSeed(Long userId,
                                                            SearchRequest request,
                                                            String customGap,
                                                            String evidenceHint);

    record CandidateSeedCreationResult(CandidateSeedEntity seed, boolean created) {
    }
}
