package com.axiqra.core.mapper;

import com.axiqra.common.domain.entity.UserEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {

    @Select("SELECT * FROM axiqra_user WHERE username = #{username} AND is_deleted = 0 LIMIT 1")
    UserEntity selectByUsername(@Param("username") String username);

    @Select("SELECT * FROM axiqra_user WHERE id = #{id} AND is_deleted = 0 LIMIT 1")
    UserEntity selectActiveById(@Param("id") Long id);

    @Select("SELECT * FROM axiqra_user WHERE email = #{email} AND is_deleted = 0 LIMIT 1")
    UserEntity selectByEmail(@Param("email") String email);
}
