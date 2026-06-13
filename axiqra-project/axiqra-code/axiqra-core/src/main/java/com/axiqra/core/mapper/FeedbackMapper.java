package com.axiqra.core.mapper;

import com.axiqra.common.domain.entity.FeedbackEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Feedback Mapper
 *
 * @author Axiqra Team
 * @date 2026-06-11
 */
@Mapper
public interface FeedbackMapper extends BaseMapper<FeedbackEntity> {

    @Select("SELECT * FROM axiqra_feedback WHERE invocation_id = #{invocationId} AND is_deleted = FALSE LIMIT 1")
    FeedbackEntity selectByInvocationId(@Param("invocationId") Long invocationId);

    @Select("SELECT * FROM axiqra_feedback WHERE invocation_id IN " +
            "(SELECT id FROM axiqra_invocation WHERE target_type = 'solution' AND target_id = #{solutionId} AND is_deleted = FALSE) " +
            "AND is_deleted = FALSE ORDER BY gmt_create DESC")
    List<FeedbackEntity> selectBySolutionId(@Param("solutionId") Long solutionId);

    @Select("SELECT feedback_type, COUNT(*) as count FROM axiqra_feedback " +
            "WHERE invocation_id IN " +
            "(SELECT id FROM axiqra_invocation WHERE target_type = 'solution' AND target_id = #{solutionId} AND is_deleted = FALSE) " +
            "AND is_deleted = FALSE GROUP BY feedback_type")
    List<FeedbackStatRow> selectFeedbackStatsBySolutionId(@Param("solutionId") Long solutionId);

    interface FeedbackStatRow {
        String getFeedbackType();
        Long getCount();
    }
}
