package com.axiqra.core.service;

import com.axiqra.common.domain.dto.SolutionCreateFromProjectCaseRequest;
import com.axiqra.common.domain.vo.SolutionDetailVO;
import com.axiqra.common.domain.vo.SearchResultItemVO;

import java.util.List;

/**
 * Solution 服务接口
 */
public interface SolutionService {

    SolutionDetailVO createFromProjectCase(Long userId, SolutionCreateFromProjectCaseRequest request);

    SolutionDetailVO getDetail(Long userId, Long solutionId);

    SolutionDetailVO getPublicDetail(Long solutionId);

    List<SearchResultItemVO> listPublicSolutions(String query,
                                                 String domain,
                                                 String techStack,
                                                 Integer minVerificationLevel,
                                                 Integer limit);
}
