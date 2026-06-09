package com.axiqra.core.adapter;

import com.axiqra.common.domain.entity.ConnectSessionEntity;
import com.axiqra.common.domain.vo.ConnectDoctorVO;
import com.axiqra.common.domain.vo.ConnectSessionEventVO;
import com.axiqra.common.domain.vo.ConnectSessionVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.common.port.ConnectSessionPort;
import com.axiqra.core.mapper.ConnectSessionMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class DbConnectSessionAdapter implements ConnectSessionPort {

    private final ConnectSessionMapper connectSessionMapper;
    private final ObjectMapper objectMapper;

    @Value("${axiqra.tenant.id:0}")
    private Long tenantId;

    @Override
    public void save(ConnectSessionVO session) {
        validateSession(session);
        if (session.getUserId() != null && tenantId > 0) {
            ConnectSessionEntity existing = connectSessionMapper.selectBySessionId(session.getSessionId());
            if (existing != null && !tenantId.equals(existing.getTenantId())) {
                throw new BizException(ErrorCode.FORBIDDEN, "无权操作其他租户的 session");
            }
        }
        connectSessionMapper.insert(toEntity(session));
    }

    @Override
    public Optional<ConnectSessionVO> get(String sessionId) {
        ConnectSessionEntity entity = connectSessionMapper.selectBySessionId(sessionId);
        return Optional.ofNullable(entity).map(this::toView);
    }

    @Override
    public List<ConnectSessionVO> listByUser(Long userId) {
        return connectSessionMapper.selectByUserId(userId).stream()
                .map(this::toView)
                .toList();
    }

    private ConnectSessionEntity toEntity(ConnectSessionVO session) {
        ConnectSessionEntity entity = new ConnectSessionEntity()
                .setSessionId(session.getSessionId())
                .setUserId(session.getUserId())
                .setChannel(session.getChannel())
                .setToolType(session.getToolType())
                .setTargetType(session.getTargetType())
                .setTargetId(session.getTargetId())
                .setWorkspaceId(session.getWorkspaceId())
                .setStatus(session.getStatus())
                .setRiskLevel(session.getRiskLevel())
                .setConfirmationObtained(Boolean.TRUE.equals(session.getConfirmationObtained()) ? 1 : 0)
                .setExpiresAt(session.getExpiresAt())
                .setDoctorStatus(session.getDoctor() == null ? null : session.getDoctor().getStatus())
                .setDoctorSnapshot(toJson(session.getDoctor()))
                .setHistorySnapshot(toJson(session.getHistory()));
        entity.setGmtCreate(session.getCreatedAt() == null ? OffsetDateTime.now(ZoneOffset.UTC).toInstant() : session.getCreatedAt().toInstant());
        entity.setGmtModified(OffsetDateTime.now(ZoneOffset.UTC).toInstant());
        return entity;
    }

    private ConnectSessionVO toView(ConnectSessionEntity entity) {
        ConnectDoctorVO doctor = fromJson(entity.getDoctorSnapshot(), new TypeReference<>() {});
        List<ConnectSessionEventVO> history = fromJson(entity.getHistorySnapshot(), new TypeReference<>() {});
        return ConnectSessionVO.builder()
                .sessionId(entity.getSessionId())
                .userId(entity.getUserId())
                .channel(entity.getChannel())
                .toolType(entity.getToolType())
                .targetType(entity.getTargetType())
                .targetId(entity.getTargetId())
                .workspaceId(entity.getWorkspaceId())
                .status(entity.getStatus())
                .riskLevel(entity.getRiskLevel())
                .confirmationObtained(entity.getConfirmationObtained() != null && entity.getConfirmationObtained() == 1)
                .createdAt(entity.getGmtCreate() == null ? null : entity.getGmtCreate().atOffset(ZoneOffset.UTC))
                .expiresAt(entity.getExpiresAt())
                .doctor(doctor)
                .history(history == null ? Collections.emptyList() : history)
                .build();
    }

    private void validateSession(ConnectSessionVO session) {
        if (session == null) {
            throw new IllegalArgumentException("connect session must not be null");
        }
        if (session.getSessionId() == null || session.getSessionId().isBlank()) {
            throw new IllegalArgumentException("connect sessionId must not be blank");
        }
        if (session.getUserId() == null) {
            throw new IllegalArgumentException("connect userId must not be null");
        }
        if (session.getChannel() == null || session.getChannel().isBlank()) {
            throw new IllegalArgumentException("connect channel must not be blank");
        }
        if (session.getToolType() == null || session.getToolType().isBlank()) {
            throw new IllegalArgumentException("connect toolType must not be blank");
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serialize connect session payload failed", e);
        }
    }

    private <T> T fromJson(String value, TypeReference<T> typeReference) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(value, typeReference);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("deserialize connect session payload failed", e);
        }
    }
}
