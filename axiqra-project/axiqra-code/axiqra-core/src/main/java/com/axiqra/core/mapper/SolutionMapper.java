package com.axiqra.core.mapper;

import com.axiqra.common.domain.entity.SolutionEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Solution Mapper
 */
@Mapper
public interface SolutionMapper extends BaseMapper<SolutionEntity> {

    @Select("SELECT * FROM axiqra_solution WHERE id = #{id} AND is_deleted = FALSE LIMIT 1")
    SolutionEntity selectActiveById(@Param("id") Long id);

    @Select("SELECT * FROM axiqra_solution WHERE solution_code = #{solutionCode} AND is_deleted = FALSE LIMIT 1")
    SolutionEntity selectBySolutionCode(@Param("solutionCode") String solutionCode);

    @Select("SELECT * FROM axiqra_solution WHERE source_case_id = #{sourceCaseId} AND is_deleted = FALSE ORDER BY id DESC LIMIT 1")
    SolutionEntity selectBySourceCaseId(@Param("sourceCaseId") Long sourceCaseId);

    @Select("<script>" +
            "SELECT * FROM axiqra_solution WHERE is_deleted = FALSE " +
            "AND status IN ('candidate', 'reviewed', 'verified', 'stable', 'canonical') " +
            "<if test='workspaceId != null'> AND workspace_id = #{workspaceId} </if>" +
            "<if test='visibleWorkspaceIds != null and visibleWorkspaceIds.size() &gt; 0'>" +
            " AND (visibility_scope = 'public' OR workspace_id IN " +
            "<foreach collection='visibleWorkspaceIds' item='workspaceIdItem' open='(' separator=',' close=')'>#{workspaceIdItem}</foreach>)" +
            "</if>" +
            "<if test='visibleWorkspaceIds == null or visibleWorkspaceIds.size() == 0'> AND visibility_scope = 'public' </if>" +
            "<if test='minVerificationLevel != null'> AND verification_level &gt;= #{minVerificationLevel} </if>" +
            "<if test='query != null and query.trim().length() &gt; 0'>" +
            " AND (LOWER(title) LIKE CONCAT('%', LOWER(#{query}), '%')" +
            " OR LOWER(COALESCE(domain, '')) LIKE CONCAT('%', LOWER(#{query}), '%')" +
            " OR LOWER(COALESCE(tech_stack, '')) LIKE CONCAT('%', LOWER(#{query}), '%')" +
            " OR LOWER(solution_code) LIKE CONCAT('%', LOWER(#{query}), '%'))" +
            "</if>" +
            "<if test='domain != null and domain.trim().length() &gt; 0'> AND LOWER(COALESCE(domain, '')) = LOWER(#{domain}) </if>" +
            "<if test='techStack != null and techStack.trim().length() &gt; 0'> AND LOWER(COALESCE(tech_stack, '')) LIKE CONCAT('%', LOWER(#{techStack}), '%') </if>" +
            " ORDER BY " +
            " CASE status WHEN 'canonical' THEN 6 WHEN 'stable' THEN 5 WHEN 'verified' THEN 4 WHEN 'reviewed' THEN 3 WHEN 'candidate' THEN 2 ELSE 1 END DESC," +
            " verification_level DESC, gmt_modified DESC LIMIT #{limit}" +
            "</script>")
    List<SolutionEntity> searchVisibleSolutions(@Param("query") String query,
                                                @Param("workspaceId") Long workspaceId,
                                                @Param("visibleWorkspaceIds") List<Long> visibleWorkspaceIds,
                                                @Param("domain") String domain,
                                                @Param("techStack") String techStack,
                                                @Param("minVerificationLevel") Integer minVerificationLevel,
                                                @Param("limit") int limit);
}
