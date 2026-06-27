package com.axiqra.core.service.impl;

import com.axiqra.common.domain.entity.UserEntity;
import com.axiqra.common.domain.entity.WorkspaceEntity;
import com.axiqra.common.domain.entity.MembershipEntity;
import com.axiqra.common.domain.enums.MemberRole;
import com.axiqra.common.domain.enums.MemberStatus;
import com.axiqra.common.domain.enums.WorkspaceType;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.common.util.PasswordHashUtil;
import com.axiqra.core.mapper.MembershipMapper;
import com.axiqra.core.mapper.UserMapper;
import com.axiqra.core.mapper.WorkspaceMapper;
import com.axiqra.core.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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
    private final WorkspaceMapper workspaceMapper;
    private final MembershipMapper membershipMapper;
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
        String defaultAvatar = generateRandomAvatar();

        UserEntity user = new UserEntity()
                .setUsername(username)
                .setPasswordHash(passwordHashUtil.hash(password))
                .setEmail(email)
                .setNickname(nickname)
                .setAvatar(defaultAvatar)
                .setDeleted(false);
        try {
            userMapper.insertSelective(user);
        } catch (DataIntegrityViolationException e) {
            if (isUniqueViolation(e)) {
                throw new BizException(ErrorCode.DUPLICATE_ENTRY, "用户名或邮箱已被注册");
            }
            log.error("用户注册数据约束异常: username={}, email={}", username, email, e);
            throw new BizException(ErrorCode.DATABASE_ERROR, "注册失败，请稍后重试");
        } catch (DataAccessException e) {
            log.error("用户注册数据库异常: username={}, email={}", username, email, e);
            throw new BizException(ErrorCode.DATABASE_ERROR, "注册失败，请稍后重试");
        }
        
        // 注册成功后自动创建个人空间
        createPersonalWorkspace(user.getId(), nickname);
        
        log.info("用户注册成功: userId={}", user.getId());
        return user;
    }

    /**
     * 为新用户创建个人空间
     */
    private void createPersonalWorkspace(Long userId, String nickname) {
        String workspaceName = (nickname != null && !nickname.isBlank()) 
                ? nickname + " 的空间" 
                : "我的空间";
        
        WorkspaceEntity workspace = new WorkspaceEntity()
                .setOwnerId(userId)
                .setWorkspaceName(workspaceName)
                .setWorkspaceType(WorkspaceType.PERSONAL)
                .setDeleted(false);
        
        workspaceMapper.insertSelective(workspace);
        
        // 自动将自己添加为 owner
        MembershipEntity membership = new MembershipEntity()
                .setWorkspaceId(workspace.getId())
                .setUserId(userId)
                .setRole(MemberRole.OWNER.getCode())
                .setStatus(MemberStatus.ACTIVE.getCode())
                .setDeleted(false);
        
        membershipMapper.insertSelective(membership);
        
        log.info("为用户创建个人空间: userId={}, workspaceId={}", userId, workspace.getId());
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
            rows = userMapper.updateSelective(userId, cleanedNickname, cleanedEmail, cleanedAvatar,
                    existing.getVersion(), Instant.now());
        } catch (DataIntegrityViolationException e) {
            if (isUniqueViolation(e)) {
                throw new BizException(ErrorCode.DUPLICATE_ENTRY, "邮箱已被其他用户使用");
            }
            log.error("更新用户资料数据库约束异常: userId={}", userId, e);
            throw new BizException(ErrorCode.DATABASE_ERROR, "更新失败，请稍后重试");
        } catch (DataAccessException e) {
            log.error("更新用户资料数据库异常: userId={}", userId, e);
            throw new BizException(ErrorCode.DATABASE_ERROR, "更新失败，请稍后重试");
        }

        if (rows == 0) {
            throw new BizException(ErrorCode.CONCURRENT_MODIFICATION, "数据已被其他人修改，请刷新后重试");
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

    private boolean isUniqueViolation(DataIntegrityViolationException e) {
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return msg.contains("unique") || msg.contains("duplicate") ||
               msg.contains("idx_username") || msg.contains("idx_email") ||
               msg.contains("23505");
    }

    private String generateRandomAvatar() {
        // DiceBear API 提供的头像风格
        String[] styles = {
            "adventurer", "adventurer-neutral", "avataaars", "big-ears", "big-ears-neutral",
            "big-smile", "bottts", "bottts-neutral", "croodles", "croodles-neutral",
            "dylan", "fun-emoji", "glass", "icons", "identicon", "initials",
            "lorelei", "lorelei-neutral", "micah", "miniavs", "notionists",
            "notionists-neutral", "open-peeps", "personas", "pixel-art", "pixel-art-neutral",
            "shapes", "thumbs"
        };

        // 丰富的背景色
        String[] backgroundColors = {
            "b6e3f4", "c0aede", "d1d4f9", "ffd5dc", "ffdfbf",
            "a0d2db", "d4a5a5", "9dd6c3", "e8c1a8", "b8d4e3",
            "c9e4de", "f0e6ef", "e8d5b7", "b5c7ed", "f5c3c2",
            "d5dce4", "e2d1c3", "f5e6cc", "d4e7ed", "c4d7f2"
        };

        String style = styles[ThreadLocalRandom.current().nextInt(styles.length)];
        String bgColor = backgroundColors[ThreadLocalRandom.current().nextInt(backgroundColors.length)];
        String seed = UUID.randomUUID().toString();

        return String.format(
            "https://api.dicebear.com/7.x/%s/svg?seed=%s&backgroundColor=%s",
            style,
            URLEncoder.encode(seed, StandardCharsets.UTF_8),
            bgColor
        );
    }
}
