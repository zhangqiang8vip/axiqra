package com.axiqra.common.port;

import com.axiqra.common.domain.vo.ConnectSessionVO;

import java.util.List;
import java.util.Optional;

/**
 * Connect 会话存储端口
 */
public interface ConnectSessionPort {

    /**
     * 保存 Connect 会话。
     *
     * @param session 待保存的会话对象，不能为 null，且 sessionId/userId/channel/toolType 必须有值
     * @throws IllegalArgumentException 当 session 为 null 或缺少必填字段（sessionId/userId）时抛出
     * @throws org.springframework.dao.DataAccessException 当底层持久化失败时抛出（由实现类传播）
     */
    void save(ConnectSessionVO session);

    /**
     * 根据 sessionId 查询会话。
     *
     * @param sessionId 会话 ID
     * @return 不存在时返回 {@link Optional#empty()}，存在时返回带会话对象的 Optional
     */
    Optional<ConnectSessionVO> get(String sessionId);

    List<ConnectSessionVO> listByUser(Long userId);
}
