package com.axiqra.common.domain.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"gmtCreate", "gmtModified", "version"})
@Accessors(chain = true)
public abstract class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（应用侧生成，避免 CockroachDB 默认值无法回填到实体） */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    protected Long id;

    /** 创建时间（数据库 DEFAULT now()） */
    protected Instant gmtCreate;

    /** 修改时间（数据库 ON UPDATE now()） */
    @Column("gmt_modified")
    protected Instant gmtModified;

    /** 乐观锁版本号 */
    @Column(value = "version", version = true)
    protected Long version;
}
