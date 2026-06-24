package com.axiqra.core.service;

import com.axiqra.common.domain.dto.SearchRequest;
import com.axiqra.common.domain.vo.SearchResponseVO;

/**
 * Search 服务接口
 */
public interface SearchService {

    SearchResponseVO searchBeforeAct(Long userId, SearchRequest request);

    SearchResponseVO searchPublic(SearchRequest request);
}
