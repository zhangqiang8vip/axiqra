package com.axiqra.core.mapper;

import com.axiqra.common.domain.entity.ReviewEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Review Mapper
 *
 * @author Axiqra Team
 * @date 2026-06-11
 */
@Mapper
public interface ReviewMapper extends BaseMapper<ReviewEntity> {

    @Select("SELECT * FROM axiqra_review WHERE object_type = #{objectType} AND object_id = #{objectId} AND is_deleted = FALSE LIMIT 1")
    ReviewEntity selectByObject(@Param("objectType") String objectType, @Param("objectId") Long objectId);

    @Select("SELECT * FROM axiqra_review WHERE id = #{id} AND is_deleted = FALSE LIMIT 1")
    ReviewEntity selectById(@Param("id") Long id);

    @Select("SELECT * FROM axiqra_review WHERE queue = #{queue} AND status = #{status} AND is_deleted = FALSE ORDER BY gmt_create ASC LIMIT #{limit}")
    List<ReviewEntity> selectPendingByQueue(@Param("queue") String queue, @Param("status") String status, @Param("limit") int limit);

    @Select("SELECT * FROM axiqra_review WHERE reviewer_id = #{reviewerId} AND is_deleted = FALSE ORDER BY gmt_create DESC LIMIT #{limit}")
    List<ReviewEntity> selectByReviewerId(@Param("reviewerId") Long reviewerId, @Param("limit") int limit);
}
