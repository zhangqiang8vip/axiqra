package com.axiqra.core.mapper;

import com.axiqra.common.domain.entity.PublicCaseEntity;
import com.axiqra.common.domain.enums.PublicCaseStatus;
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

    String SEARCHABLE_STATUS_VERIFIED = "'" + PublicCaseStatus.VERIFIED.getCode() + "'";
    String SEARCHABLE_STATUS_STABLE = "'" + PublicCaseStatus.STABLE.getCode() + "'";
    String SEARCHABLE_STATUS_CANONICAL = "'" + PublicCaseStatus.CANONICAL.getCode() + "'";
    String SEARCHABLE_STATUSES_SQL = SEARCHABLE_STATUS_VERIFIED + "," + SEARCHABLE_STATUS_STABLE + "," + SEARCHABLE_STATUS_CANONICAL;

    @Select("SELECT * FROM axiqra_public_case WHERE id = #{id} AND is_deleted = FALSE LIMIT 1")
    PublicCaseEntity selectActiveById(@Param("id") Long id);

    @Select("SELECT * FROM axiqra_public_case WHERE source_case_id = #{sourceCaseId} AND is_deleted = FALSE LIMIT 1")
    PublicCaseEntity selectBySourceCaseId(@Param("sourceCaseId") Long sourceCaseId);

    @Select("SELECT * FROM axiqra_public_case WHERE is_deleted = FALSE AND status IN (" + SEARCHABLE_STATUSES_SQL + ") ORDER BY id DESC LIMIT #{limit}")
    List<PublicCaseEntity> selectPubliclySearchable(@Param("limit") int limit);
}
