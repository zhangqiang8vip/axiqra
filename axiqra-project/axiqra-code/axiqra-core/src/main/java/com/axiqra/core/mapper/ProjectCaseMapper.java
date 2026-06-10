package com.axiqra.core.mapper;

import com.axiqra.common.domain.entity.ProjectCaseEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Project Case Mapper
 */
@Mapper
public interface ProjectCaseMapper extends BaseMapper<ProjectCaseEntity> {

    @Select("SELECT * FROM axiqra_project_case WHERE id = #{id} AND is_deleted = FALSE LIMIT 1")
    ProjectCaseEntity selectActiveById(@Param("id") Long id);

    @Select("SELECT * FROM axiqra_project_case WHERE trace_id = #{traceId} AND is_deleted = FALSE LIMIT 1")
    ProjectCaseEntity selectByTraceId(@Param("traceId") Long traceId);
}
