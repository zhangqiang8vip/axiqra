package com.axiqra.core.service;

import com.axiqra.common.domain.entity.UserEntity;

/**
 * 用户服务接口
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
public interface UserService {

    UserEntity getByUsername(String username);

    UserEntity getByEmail(String email);

    UserEntity getById(Long id);

    UserEntity register(String username, String password, String email, String nickname);

    boolean checkPassword(String rawPassword, String encodedPassword);
}
