package com.axiqra.core.adapter;

import com.axiqra.common.domain.vo.ConnectSessionVO;
import com.axiqra.common.port.ConnectSessionPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryConnectSessionAdapter implements ConnectSessionPort {

    private final Map<String, ConnectSessionVO> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(ConnectSessionVO session) {
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
        sessions.put(session.getSessionId(), session);
    }

    @Override
    public Optional<ConnectSessionVO> get(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public List<ConnectSessionVO> listByUser(Long userId) {
        return sessions.values().stream()
                .filter(session -> session != null && userId.equals(session.getUserId()))
                .sorted(Comparator.comparing(
                        ConnectSessionVO::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
}
