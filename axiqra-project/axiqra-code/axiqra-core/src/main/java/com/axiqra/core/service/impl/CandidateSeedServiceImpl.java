package com.axiqra.core.service.impl;

import com.axiqra.common.domain.dto.SearchRequest;
import com.axiqra.common.domain.entity.CandidateSeedEntity;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.mapper.CandidateSeedMapper;
import com.axiqra.core.service.CandidateSeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class CandidateSeedServiceImpl implements CandidateSeedService {

    private final CandidateSeedMapper candidateSeedMapper;

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public CandidateSeedCreationResult createOrReuseCandidateSeed(Long userId, SearchRequest request) {
        String queryHash = hashQuery(request.getQuery(), request.getWorkspaceId(), request.getTechStack(), request.getDomain());
        CandidateSeedEntity existing = candidateSeedMapper.selectByQueryHashAndWorkspaceId(queryHash, request.getWorkspaceId());
        if (existing != null) {
            return new CandidateSeedCreationResult(existing, false);
        }
        CandidateSeedEntity entity = new CandidateSeedEntity()
                .setWorkspaceId(request.getWorkspaceId())
                .setAuthorId(userId)
                .setQueryHash(queryHash)
                .setTaskGoal(request.getQuery().trim())
                .setTechStack(blankToNull(request.getTechStack()))
                .setCoverageGap(buildCoverageGap(request))
                .setStatus("open")
                .setAssigneeId(null)
                .setSolutionId(null)
                .setTenantId(null)
                .setDeleted(false);
        try {
            candidateSeedMapper.insertSelective(entity);
            return new CandidateSeedCreationResult(entity, true);
        } catch (DataIntegrityViolationException ex) {
            CandidateSeedEntity reused = candidateSeedMapper.selectByQueryHashAndWorkspaceId(queryHash, request.getWorkspaceId());
            if (reused != null) {
                return new CandidateSeedCreationResult(reused, false);
            }
            throw new BizException(ErrorCode.DUPLICATE_ENTRY, "候选种子已存在", ex);
        }
    }

    static String buildCoverageGap(SearchRequest request) {
        StringBuilder builder = new StringBuilder("search_empty");
        if (request.getDomain() != null && !request.getDomain().isBlank()) {
            builder.append(";domain=").append(request.getDomain().trim());
        }
        if (request.getTechStack() != null && !request.getTechStack().isBlank()) {
            builder.append(";techStack=").append(request.getTechStack().trim());
        }
        if (request.getMinVerificationLevel() != null) {
            builder.append(";minVerificationLevel=L").append(request.getMinVerificationLevel());
        }
        return builder.toString();
    }

    static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    static String hashQuery(String query, Long workspaceId, String techStack, String domain) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String raw = (query != null ? query.trim() : "") + "|"
                    + (workspaceId != null ? workspaceId : 0L) + "|"
                    + (blankToNull(techStack) != null ? blankToNull(techStack) : "") + "|"
                    + (blankToNull(domain) != null ? blankToNull(domain) : "");
            byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "query hash 生成失败", ex);
        }
    }
}
