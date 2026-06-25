package com.axiqra.common.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Search 聚合响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResponseVO {

    private String query;
    private int totalHits;
    private int returnedHits;
    private boolean empty;
    private boolean candidateSeedCreated;
    private String emptyReason;
    private CandidateSeedVO candidateSeed;
    private List<SearchResultItemVO> items;

    // ========== 向量搜索相关字段 ==========

    /** 是否使用了向量搜索 */
    private Boolean vectorSearchEnabled;

    /** 向量搜索命中的结果数 */
    private Integer vectorSearchHits;

    /** 关键词搜索命中的结果数 */
    private Integer keywordSearchHits;

    /** 混合搜索命中数 */
    private Integer hybridSearchHits;
}
