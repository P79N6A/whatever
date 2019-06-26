package org.apache.ibatis.session;

/**
 * 本地缓存Scope
 */
public enum LocalCacheScope {
    /**
     * 缓存会话中执行的所有查询
     */
    SESSION,
    /**
     * 语句执行Scope
     */
    STATEMENT
}
