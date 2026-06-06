package com.axiqra.common.port;

/**
 * 签名端口（Signature Port）
 * <p>
 * 定义 API 签名生成和验签接口，支持算法可插拔。
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
public interface SignaturePort {

    /**
     * 生成签名
     * <p>
     * 签名源字符串格式为：appId|timestamp|nonce|body
     * <ul>
     *   <li>appId: 应用 ID</li>
     *   <li>timestamp: Unix 时间戳（秒），由调用方传入</li>
     *   <li>nonce: 随机字符串（建议 16~64 字符）</li>
     *   <li>body: 请求体原文，无 body 时为空字符串 ""</li>
     * </ul>
     * 各字段之间用 ASCII 竖线符 "|" 分隔，字符编码为 UTF-8。
     * 签名算法为 HMAC-SHA256，结果以十六进制小写（Base16）输出。
     *
     * @param appId     应用 ID
     * @param timestamp 时间戳（秒）
     * @param nonce     随机字符串
     * @param body      请求体（空字符串代表无 body）
     * @return HMAC-SHA256 签名（Base16 十六进制小写）
     */
    String sign(String appId, long timestamp, String nonce, String body);

    /**
     * 验签
     * <p>
     * 验签时必须校验以下两项，否则存在安全风险：
     * <ul>
     *   <li>时间戳新鲜度：拒绝时间戳超出配置窗口（建议默认 ±5 分钟）之外的请求，
     *       以防止重放攻击。可配置 clock skew 容差。</li>
     *   <li>Nonce 唯一性：维护每个 appId 的 nonce 缓存（内存或持久化，带 TTL），
     *       确保同一 nonce 在窗口内只被接受一次。过期 nonce 应被清理。</li>
     * </ul>
     * 验签失败时返回 false（不抛异常）。
     *
     * @param appId     应用 ID
     * @param timestamp 时间戳（秒）
     * @param nonce     随机字符串
     * @param body      请求体
     * @param signature 待验证签名
     * @return true=验签通过，false=验签失败（时间戳过期/nonce 重放/签名不匹配）
     */
    boolean verify(String appId, long timestamp, String nonce, String body, String signature);
}
