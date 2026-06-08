package com.axiqra.core.mapper;

import com.axiqra.common.domain.entity.MembershipEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 空间成员关系 Mapper
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Mapper
public interface MembershipMapper extends BaseMapper<MembershipEntity> {

    @Select("SELECT * FROM axiqra_membership WHERE user_id = #{userId} AND workspace_id = #{workspaceId} LIMIT 1")
    MembershipEntity selectByUserAndWorkspace(@Param("userId") Long userId, @Param("workspaceId") Long workspaceId);

    @Select("SELECT * FROM axiqra_membership WHERE user_id = #{userId} AND workspace_id = #{workspaceId} AND status = 'active' LIMIT 1")
    MembershipEntity selectActiveByUserAndWorkspace(@Param("userId") Long userId, @Param("workspaceId") Long workspaceId);

    @Select("SELECT * FROM axiqra_membership WHERE user_id = #{userId} AND status = 'active'")
    List<MembershipEntity> selectActiveByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM axiqra_membership WHERE workspace_id = #{workspaceId} AND status = 'active'")
    List<MembershipEntity> selectActiveByWorkspaceId(@Param("workspaceId") Long workspaceId);

    @Select("SELECT COUNT(*) FROM axiqra_membership WHERE workspace_id = #{workspaceId} AND status = 'active'")
    long countByWorkspaceId(@Param("workspaceId") Long workspaceId);

    @Select("SELECT * FROM axiqra_membership WHERE id = #{id} LIMIT 1")
    MembershipEntity selectById(@Param("id") Long id);

    @org.apache.ibatis.annotations.Update("UPDATE axiqra_membership SET role = #{role}, gmt_modified = now() WHERE id = #{id}")
    int updateRole(@Param("id") Long id, @Param("role") String role);

    @org.apache.ibatis.annotations.Update("UPDATE axiqra_membership SET status = #{status}, gmt_modified = now() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @org.apache.ibatis.annotations.Update("UPDATE axiqra_membership SET status = #{status}, is_deleted = TRUE, gmt_modified = now(), version = version + 1 WHERE id = #{id} AND version = #{version}")
    int softDelete(@Param("id") Long id, @Param("status") String status, @Param("version") Long version);

    @org.apache.ibatis.annotations.Update("UPDATE axiqra_membership SET status = #{status}, is_deleted = TRUE, gmt_modified = now(), version = version + 1 WHERE workspace_id = #{workspaceId} AND version = #{version}")
    int softDeleteByWorkspaceId(@Param("workspaceId") Long workspaceId, @Param("status") String status, @Param("version") Long version);
}
