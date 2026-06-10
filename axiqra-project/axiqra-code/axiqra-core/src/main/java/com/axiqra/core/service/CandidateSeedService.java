package com.axiqra.core.service;

import com.axiqra.common.domain.dto.SearchRequest;
import com.axiqra.common.domain.entity.CandidateSeedEntity;

public interface CandidateSeedService {

    CandidateSeedCreationResult createOrReuseCandidateSeed(Long userId, SearchRequest request);

    record CandidateSeedCreationResult(CandidateSeedEntity seed, boolean created) {
    }
}
