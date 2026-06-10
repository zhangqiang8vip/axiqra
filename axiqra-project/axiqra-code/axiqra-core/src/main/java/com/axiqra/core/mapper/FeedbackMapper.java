package com.axiqra.core.mapper;

import com.axiqra.common.domain.entity.FeedbackEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Feedback Mapper
 */
@Mapper
public interface FeedbackMapper extends BaseMapper<FeedbackEntity> {

    @Select("SELECT f.* FROM axiqra_feedback f INNER JOIN axiqra_invocation i ON i.id = f.invocation_id WHERE i.target_type = 'solution' AND i.target_id = #{solutionId} AND i.is_deleted = FALSE AND f.is_deleted = FALSE")
    List<FeedbackEntity> selectBySolutionId(@Param("solutionId") Long solutionId);
}
