package com.axiqra.core.mapper;

import com.axiqra.common.domain.entity.PublicCaseEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Public Case Mapper
 */
@Mapper
public interface PublicCaseMapper extends BaseMapper<PublicCaseEntity> {

    @Select("SELECT * FROM axiqra_public_case WHERE id = #{id} AND is_deleted = FALSE LIMIT 1")
    PublicCaseEntity selectActiveById(@Param("id") Long id);

    @Select("SELECT * FROM axiqra_public_case WHERE source_case_id = #{sourceCaseId} AND is_deleted = FALSE LIMIT 1")
    PublicCaseEntity selectBySourceCaseId(@Param("sourceCaseId") Long sourceCaseId);

    @Select("SELECT * FROM axiqra_public_case WHERE is_deleted = FALSE AND status IN ('verified','stable','canonical') ORDER BY id DESC LIMIT #{limit}")
    List<PublicCaseEntity> selectPubliclySearchable(@Param("limit") int limit);
}
