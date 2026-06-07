package com.axiqra.core.service.impl;

import com.axiqra.common.domain.entity.UserEntity;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.common.util.PasswordHashUtil;
import com.axiqra.core.mapper.UserMapper;
import com.axiqra.core.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务实现
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordHashUtil passwordHashUtil;

    @Override
    public UserEntity getByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    @Override
    public UserEntity getByEmail(String email) {
        return userMapper.selectByEmail(email);
    }

    @Override
    public UserEntity getById(Long id) {
        return userMapper.selectActiveById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserEntity register(String username, String password, String email, String nickname) {
        UserEntity user = new UserEntity()
                .setUsername(username)
                .setPasswordHash(passwordHashUtil.hash(password))
                .setEmail(email)
                .setNickname(nickname)
                .setIsDeleted(0);
        try {
            userMapper.insertSelective(user);
        } catch (DataIntegrityViolationException e) {
            if (isUsernameDuplicate(e)) {
                throw new BizException(ErrorCode.DUPLICATE_ENTRY, "用户名已存在");
            }
            if (isEmailDuplicate(e)) {
                throw new BizException(ErrorCode.DUPLICATE_ENTRY, "邮箱已被注册");
            }
            throw new BizException(ErrorCode.DUPLICATE_ENTRY, "记录已存在");
        }
        log.info("用户注册成功: userId={}", user.getId());
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserEntity updateProfile(Long userId, String nickname, String email, String avatar) {
        if (userId == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "userId 不能为空");
        }

        UserEntity existing = userMapper.selectActiveById(userId);
        if (existing == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        boolean hasNicknameUpdate = nickname != null && !nickname.isBlank();
        boolean hasEmailUpdate = email != null && !email.isBlank();
        boolean hasAvatarUpdate = avatar != null && !avatar.isBlank();

        if (!hasNicknameUpdate && !hasEmailUpdate && !hasAvatarUpdate) {
            return existing;
        }

        String cleanedNickname = hasNicknameUpdate ? nickname.trim() : null;
        String cleanedEmail = hasEmailUpdate ? email.trim() : null;
        String cleanedAvatar = hasAvatarUpdate ? avatar.trim() : null;

        int rows;
        try {
            rows = userMapper.updateSelective(userId, cleanedNickname, cleanedEmail, cleanedAvatar);
        } catch (DataIntegrityViolationException e) {
            if (isEmailDuplicate(e)) {
                throw new BizException(ErrorCode.DUPLICATE_ENTRY, "邮箱已被其他用户使用");
            }
            log.error("更新用户资料数据库约束异常: userId={}", userId, e);
            throw new BizException(ErrorCode.DATABASE_ERROR, "更新失败，请稍后重试");
        } catch (DataAccessException e) {
            log.error("更新用户资料数据库异常: userId={}", userId, e);
            throw new BizException(ErrorCode.DATABASE_ERROR, "更新失败，请稍后重试");
        }

        if (rows == 0) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        log.info("更新用户资料: userId={}", userId);
        UserEntity updated = userMapper.selectActiveById(userId);
        if (updated == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return updated;
    }

    @Override
    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return passwordHashUtil.matches(rawPassword, encodedPassword);
    }

    private boolean isEmailDuplicate(DataIntegrityViolationException e) {
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return msg.contains("email") || msg.contains("idx_email");
    }

    private boolean isUsernameDuplicate(DataIntegrityViolationException e) {
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return msg.contains("username") || msg.contains("idx_username");
    }
}
