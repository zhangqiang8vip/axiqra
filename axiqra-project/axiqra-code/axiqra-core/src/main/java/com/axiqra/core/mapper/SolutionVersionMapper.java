package com.axiqra.core.mapper;

import com.axiqra.common.domain.entity.SolutionVersionEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * SolutionVersion Mapper
 */
@Mapper
public interface SolutionVersionMapper extends BaseMapper<SolutionVersionEntity> {

    @Select("SELECT * FROM axiqra_solution_version WHERE solution_id = #{solutionId} ORDER BY version_number DESC, id DESC")
    List<SolutionVersionEntity> selectBySolutionId(@Param("solutionId") Long solutionId);
}
