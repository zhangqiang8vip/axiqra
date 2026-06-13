package com.axiqra.core.mapper;

import com.axiqra.common.domain.entity.ContributionLedgerEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * ContributionLedger Mapper
 *
 * @author Axiqra Team
 * @date 2026-06-11
 */
@Mapper
public interface ContributionLedgerMapper extends BaseMapper<ContributionLedgerEntity> {

    @Select("SELECT * FROM axiqra_contribution_ledger WHERE actor_id = #{actorId} AND is_deleted = FALSE ORDER BY gmt_create DESC LIMIT #{limit}")
    List<ContributionLedgerEntity> selectByActorId(@Param("actorId") Long actorId, @Param("limit") int limit);

    @Select("SELECT SUM(points) FROM axiqra_contribution_ledger WHERE actor_id = #{actorId} AND is_deleted = FALSE")
    Integer sumPointsByActorId(@Param("actorId") Long actorId);

    @Select("SELECT COUNT(*) FROM axiqra_contribution_ledger WHERE actor_id = #{actorId} AND event_type = #{eventType} AND is_deleted = FALSE")
    int countByActorIdAndEventType(@Param("actorId") Long actorId, @Param("eventType") String eventType);
}
