package com.axiqra.core.service;

import com.axiqra.common.domain.dto.ProjectCaseCreateRequest;
import com.axiqra.common.domain.vo.PageResponse;
import com.axiqra.common.domain.vo.ProjectCaseDetailVO;

/**
 * Project Case 服务接口
 */
public interface ProjectCaseService {

    ProjectCaseDetailVO create(Long userId, ProjectCaseCreateRequest request);

    ProjectCaseDetailVO getDetail(Long userId, Long caseId);

    PageResponse<ProjectCaseDetailVO> listByWorkspace(Long userId, Long workspaceId, int page, int pageSize);

    ProjectCaseDetailVO requestPublish(Long userId, Long caseId, Long authorizationId);
}
