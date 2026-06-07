package com.axiqra.common.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页请求 DTO
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Data
@NoArgsConstructor
public class PageRequest {

    /** 页码（从 1 开始） */
    @Min(value = 1, message = "页码必须 >= 1")
    private Integer pageNum = 1;

    /** 每页条数 */
    @Min(value = 1, message = "每页条数最少为 1")
    @Max(value = 100, message = "每页条数最多为 100")
    private Integer pageSize = 20;

    /**
     * 计算 OFFSET。
     * null 值按默认值处理（pageNum=1, pageSize=20）。
     *
     * @return offset 值，始终 >= 0
     */
    public int getOffset() {
        int num = (pageNum != null) ? pageNum : 1;
        int size = (pageSize != null) ? pageSize : 20;
        return (num - 1) * size;
    }

    /**
     * 获取 LIMIT。
     * null 值按默认值处理。
     *
     * @return pageSize，始终 >= 1
     */
    public int getLimit() {
        return (pageSize != null && pageSize >= 1) ? pageSize : 20;
    }
}
