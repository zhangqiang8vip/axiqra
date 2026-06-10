package com.axiqra.core.mapper;

import com.axiqra.common.domain.entity.TraceEvidenceRefEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Trace 证据引用 Mapper
 */
@Mapper
public interface TraceEvidenceRefMapper extends BaseMapper<TraceEvidenceRefEntity> {

    @Select("SELECT * FROM axiqra_trace_evidence_ref WHERE trace_id = #{traceId} AND is_deleted = FALSE ORDER BY id ASC")
    List<TraceEvidenceRefEntity> selectByTraceId(@Param("traceId") Long traceId);
}
