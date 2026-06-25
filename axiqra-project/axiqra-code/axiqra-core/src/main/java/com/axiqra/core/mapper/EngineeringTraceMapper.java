package com.axiqra.core.mapper;

import com.axiqra.common.domain.entity.EngineeringTraceEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Engineering Trace Mapper
 */
@Mapper
public interface EngineeringTraceMapper extends BaseMapper<EngineeringTraceEntity> {

    @Select("SELECT * FROM axiqra_engineering_trace WHERE id = #{id} AND is_deleted = FALSE LIMIT 1")
    EngineeringTraceEntity selectActiveById(@Param("id") Long id);

    @Select("SELECT * FROM axiqra_engineering_trace WHERE idempotency_key = #{idempotencyKey} AND is_deleted = FALSE LIMIT 1")
    EngineeringTraceEntity selectByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    @Select("SELECT * FROM axiqra_engineering_trace WHERE author_id = #{userId} AND is_deleted = FALSE ORDER BY gmt_modified DESC LIMIT 100")
    List<EngineeringTraceEntity> selectByAuthorId(@Param("userId") Long userId);
}
