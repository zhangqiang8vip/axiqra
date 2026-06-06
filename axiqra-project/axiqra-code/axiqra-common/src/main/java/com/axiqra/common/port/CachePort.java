package com.axiqra.common.port;

/**
 * 缓存端口（Cache Port）
 * <p>
 * 定义通用缓存操作接口，支持 TTL 和前缀隔离。
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
public interface CachePort {

    /**
     * 设置缓存（无过期时间）
     */
    void set(String key, Object value);

    /**
     * 设置缓存（带过期时间）
     *
     * @param key        缓存 key
     * @param value      缓存值
     * @param expireSeconds 过期秒数
     */
    void setEx(String key, Object value, long expireSeconds);

    /**
     * 获取缓存
     *
     * @param key 缓存 key
     * @return 缓存值（不存在返回 null）
     */
    Object get(String key);

    /**
     * 获取缓存并转为指定类型
     */
    <T> T get(String key, Class<T> clazz);

    /**
     * 删除缓存
     */
    void delete(String key);

    /**
     * 判断 key 是否存在
     */
    boolean exists(String key);

    /**
     * 增加数值（用于计数）
     *
     * @param key  缓存 key
     * @param delta 增量（可负数）
     * @return 增加后的值
     */
    long incr(String key, long delta);

    /**
     * 设置过期时间
     */
    void expire(String key, long expireSeconds);

    /**
     * 生成带前缀的 key
     *
     * @param prefix 前缀
     * @param id     标识
     * @return 完整 key
     */
    String key(String prefix, Object id);
}
