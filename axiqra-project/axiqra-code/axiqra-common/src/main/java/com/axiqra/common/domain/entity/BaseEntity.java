package com.axiqra.common.domain.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * 所有 Entity 实体的公共基类。
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public abstract class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（数据库 auto-generate） */
    protected Long id;

    /** 创建时间（数据库 DEFAULT now()） */
    protected Instant gmtCreate;

    /** 修改时间（数据库 ON UPDATE now()） */
    protected Instant gmtModified;
}
