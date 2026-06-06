package com.axiqra.common.domain.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页响应 VO
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Data
@NoArgsConstructor
public class PageResponse<T> {

    /** 当前页数据 */
    private List<T> records;

    /** 当前页码 */
    private Long pageNum;

    /** 每页条数 */
    private Long pageSize;

    /** 总记录数 */
    private Long total;

    /** 总页数 */
    private Long pages;

    public PageResponse(List<T> records, Long pageNum, Long pageSize, Long total) {
        if (pageSize == null || pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be positive");
        }
        long safeTotal = (total != null) ? total : 0L;
        this.records = records;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.total = safeTotal;
        this.pages = (safeTotal + pageSize - 1) / pageSize;
    }

    public static <T> PageResponse<T> of(List<T> records, long pageNum, long pageSize, long total) {
        return new PageResponse<>(records, pageNum, pageSize, total);
    }
}
