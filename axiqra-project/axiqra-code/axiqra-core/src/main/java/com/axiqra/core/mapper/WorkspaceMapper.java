package com.axiqra.core.mapper;

import com.axiqra.common.domain.entity.WorkspaceEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 工作空间 Mapper
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Mapper
public interface WorkspaceMapper extends BaseMapper<WorkspaceEntity> {

    @Select("SELECT * FROM axiqra_workspace WHERE owner_id = #{ownerId} AND is_deleted = 0")
    List<WorkspaceEntity> selectByOwnerId(@Param("ownerId") Long ownerId);

    @Select("<script>" +
            "SELECT * FROM axiqra_workspace WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " AND is_deleted = 0" +
            "</script>")
    List<WorkspaceEntity> selectByWorkspaceIds(@Param("ids") List<Long> ids);

    @Select("SELECT * FROM axiqra_workspace WHERE owner_id = #{ownerId} AND workspace_name = #{workspaceName} AND is_deleted = 0 LIMIT 1")
    WorkspaceEntity selectByOwnerAndName(@Param("ownerId") Long ownerId, @Param("workspaceName") String workspaceName);

    @Select("SELECT * FROM axiqra_workspace WHERE id = #{id} AND is_deleted = 0 LIMIT 1")
    WorkspaceEntity selectById(@Param("id") Long id);

    @org.apache.ibatis.annotations.Update("<script>" +
            "UPDATE axiqra_workspace SET is_deleted = 1, gmt_modified = now() " +
            "<where>id = #{id}</where>" +
            "</script>")
    int updateSoftDelete(@Param("id") Long id);

    @org.apache.ibatis.annotations.Update("UPDATE axiqra_workspace SET is_deleted = 1, gmt_modified = now() WHERE id = #{id}")
    int softDeleteById(@Param("id") Long id);

    @org.apache.ibatis.annotations.Update("<script>" +
            "UPDATE axiqra_workspace SET gmt_modified = now() " +
            "<if test='workspaceName != null and workspaceName.length() > 0'>" +
            ", workspace_name = #{workspaceName}" +
            "</if>" +
            "<if test='workspaceType != null and workspaceType.length() > 0'>" +
            ", workspace_type = #{workspaceType}" +
            "</if>" +
            " WHERE id = #{id} AND is_deleted = 0" +
            "</script>")
    int updateSelective(@Param("id") Long id,
                        @Param("workspaceName") String workspaceName,
                        @Param("workspaceType") String workspaceType);
}
