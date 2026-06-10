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
}
