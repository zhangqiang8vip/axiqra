package com.axiqra.common.domain.enums;

import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

/**
 * Scope 权限范围枚举，定义系统内所有可授权的操作范围。
 * 格式：resource:action（资源:操作）
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Getter
public enum ScopeEnum {

    // ==================== Connect 会话 ====================
    CONNECT_READ("connect:read", "查看 Connect 会话"),
    CONNECT_WRITE("connect:write", "创建 Connect 会话"),
    CONNECT_ADMIN("connect:admin", "管理所有 Connect 会话"),

    // ==================== 搜索 ====================
    SEARCH_READ("search:read", "发起搜索请求"),
    SEARCH_PUBLIC("search:public", "公开搜索"),
    SEARCH_ADMIN("search:admin", "搜索管理"),

    // ==================== Solution ====================
    SOLUTION_READ("solution:read", "查看 Solution"),
    SOLUTION_WRITE("solution:write", "创建/编辑 Solution"),
    SOLUTION_DELETE("solution:delete", "删除 Solution"),
    SOLUTION_PUBLISH("solution:publish", "发布 Solution"),

    // ==================== Trace ====================
    TRACE_READ("trace:read", "查看 Trace"),
    TRACE_WRITE("trace:write", "提交 Engineering Trace"),
    TRACE_CONFIRM("trace:confirm", "确认 Trace 执行结果"),
    TRACE_ADMIN("trace:admin", "管理所有 Trace"),

    // ==================== Case ====================
    CASE_READ("case:read", "查看 Case"),
    CASE_WRITE("case:write", "创建/编辑 Case"),
    CASE_PUBLISH("case:publish", "发布 Case"),
    CASE_ADMIN("case:admin", "管理所有 Case"),

    // ==================== Review 审核 ====================
    REVIEW_READ("review:read", "查看待审核列表"),
    REVIEW_WRITE("review:write", "执行审核操作"),
    REVIEW_ADMIN("review:admin", "审核管理"),

    // ==================== 贡献/积分 ====================
    CONTRIBUTION_READ("contribution:read", "查看贡献记录"),
    CONTRIBUTION_WRITE("contribution:write", "记录贡献"),
    CONTRIBUTION_ADMIN("contribution:admin", "贡献管理"),

    // ==================== 审计 ====================
    AUDIT_READ("audit:read", "查询审计日志"),

    // ==================== 公共读取 ====================
    PUBLIC_READ("public:read", "读取公开资源"),

    // ==================== 管理员 ====================
    ADMIN_ALL("admin:all", "全量管理权限");

    @EnumValue
    private final String code;
    private final String desc;

    ScopeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ScopeEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (ScopeEnum s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        return null;
    }
}
