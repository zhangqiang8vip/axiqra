package com.axiqra.core.mapper;

import com.axiqra.common.domain.entity.WorkspaceEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.Instant;
import java.util.List;

/**
 * 工作空间 Mapper
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Mapper
public interface WorkspaceMapper extends BaseMapper<WorkspaceEntity> {

    @Select("SELECT * FROM axiqra_workspace WHERE owner_id = #{ownerId} AND is_deleted = FALSE")
    List<WorkspaceEntity> selectByOwnerId(@Param("ownerId") Long ownerId);

    @Select("<script>" +
            "SELECT * FROM axiqra_workspace WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " AND is_deleted = FALSE" +
            "</script>")
    List<WorkspaceEntity> selectByWorkspaceIds(@Param("ids") List<Long> ids);

    @Select("SELECT * FROM axiqra_workspace WHERE owner_id = #{ownerId} AND workspace_name = #{workspaceName} AND is_deleted = FALSE LIMIT 1")
    WorkspaceEntity selectByOwnerAndName(@Param("ownerId") Long ownerId, @Param("workspaceName") String workspaceName);

    @Select("SELECT * FROM axiqra_workspace WHERE id = #{id} AND is_deleted = FALSE LIMIT 1")
    WorkspaceEntity selectById(@Param("id") Long id);

    @org.apache.ibatis.annotations.Update(
        "UPDATE axiqra_workspace SET is_deleted = TRUE, gmt_modified = #{gmtModified}, version = version + 1 " +
        "WHERE id = #{id} AND is_deleted = FALSE AND version = #{version}"
    )
    int softDeleteById(@Param("id") Long id, @Param("version") Long version, @Param("gmtModified") Instant gmtModified);

    @org.apache.ibatis.annotations.Update("<script>" +
            "UPDATE axiqra_workspace SET gmt_modified = #{gmtModified}, version = version + 1 " +
            "<if test='workspaceName != null and workspaceName.trim().length() &gt; 0'>" +
            ", workspace_name = #{workspaceName}" +
            "</if>" +
            "<if test='workspaceType != null and workspaceType.trim().length() &gt; 0'>" +
            ", workspace_type = #{workspaceType}" +
            "</if>" +
            " WHERE id = #{id} AND is_deleted = FALSE AND version = #{version}" +
            "</script>")
    int updateSelective(@Param("id") Long id,
                       @Param("workspaceName") String workspaceName,
                       @Param("workspaceType") String workspaceType,
                       @Param("version") Long version,
                       @Param("gmtModified") Instant gmtModified);
}
