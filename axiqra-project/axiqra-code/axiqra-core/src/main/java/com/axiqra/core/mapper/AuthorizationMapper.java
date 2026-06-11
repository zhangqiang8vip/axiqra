package com.axiqra.core.mapper;

import com.axiqra.common.domain.entity.AuthorizationEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Authorization Mapper
 */
@Mapper
public interface AuthorizationMapper extends BaseMapper<AuthorizationEntity> {

    @Select("SELECT * FROM axiqra_authorization WHERE id = #{id} AND is_deleted = FALSE LIMIT 1")
    AuthorizationEntity selectActiveById(@Param("id") Long id);
}
