package com.axiqra.core.mapper;

import com.axiqra.common.domain.entity.InvocationEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Invocation Mapper
 *
 * @author Axiqra Team
 * @date 2026-06-11
 */
@Mapper
public interface InvocationMapper extends BaseMapper<InvocationEntity> {

    @Select("SELECT * FROM axiqra_invocation WHERE request_id = #{requestId} AND is_deleted = FALSE LIMIT 1")
    InvocationEntity selectByRequestId(@Param("requestId") String requestId);

    @Select("SELECT * FROM axiqra_invocation WHERE invocation_code = #{invocationCode} AND is_deleted = FALSE LIMIT 1")
    InvocationEntity selectByInvocationCode(@Param("invocationCode") String invocationCode);

    @Select("SELECT * FROM axiqra_invocation WHERE id = #{id} AND is_deleted = FALSE LIMIT 1")
    InvocationEntity selectById(@Param("id") Long id);
}
