package com.axiqra.core.mapper;

import com.axiqra.common.domain.entity.UserEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.Instant;

/**
 * 用户 Mapper
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {

    @Select("SELECT * FROM axiqra_user WHERE username = #{username} AND is_deleted = FALSE LIMIT 1")
    UserEntity selectByUsername(@Param("username") String username);

    @Select("SELECT * FROM axiqra_user WHERE id = #{id} AND is_deleted = FALSE LIMIT 1")
    UserEntity selectActiveById(@Param("id") Long id);

    @Select("SELECT * FROM axiqra_user WHERE email = #{email} AND is_deleted = FALSE LIMIT 1")
    UserEntity selectByEmail(@Param("email") String email);

    @Update("<script>" +
            "UPDATE axiqra_user SET gmt_modified = #{gmtModified}, version = version + 1 " +
            "<if test='nickname != null and nickname.trim().length() &gt; 0'>" +
            ", nickname = #{nickname}" +
            "</if>" +
            "<if test='email != null and email.trim().length() &gt; 0'>" +
            ", email = #{email}" +
            "</if>" +
            "<if test='avatar != null and avatar.trim().length() &gt; 0'>" +
            ", avatar = #{avatar}" +
            "</if>" +
            " WHERE id = #{id} AND is_deleted = FALSE AND version = #{version}" +
            "</script>")
    int updateSelective(@Param("id") Long id,
                       @Param("nickname") String nickname,
                       @Param("email") String email,
                       @Param("avatar") String avatar,
                       @Param("version") Long version,
                       @Param("gmtModified") Instant gmtModified);
}
