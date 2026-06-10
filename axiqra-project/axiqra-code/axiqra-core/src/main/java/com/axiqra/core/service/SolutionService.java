package com.axiqra.core.service;

import com.axiqra.common.domain.vo.SolutionDetailVO;

/**
 * Solution 服务接口
 */
public interface SolutionService {

    SolutionDetailVO getDetail(Long userId, Long solutionId);
}
