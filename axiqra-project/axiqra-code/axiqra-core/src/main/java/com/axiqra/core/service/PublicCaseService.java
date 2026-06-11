package com.axiqra.core.service;

import com.axiqra.common.domain.vo.PublicCaseDetailVO;

import java.util.List;

/**
 * Public Case 服务接口
 */
public interface PublicCaseService {

    PublicCaseDetailVO publish(Long userId, Long projectCaseId);

    PublicCaseDetailVO getDetail(Long userId, Long publicCaseId);

    List<PublicCaseDetailVO> listPublicCases(Long userId, Integer limit);
}
