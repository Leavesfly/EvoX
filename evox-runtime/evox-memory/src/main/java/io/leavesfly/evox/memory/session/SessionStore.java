package io.leavesfly.evox.memory.session;

import java.util.List;

/**
 * 会话存储接口，用于会话的持久化管理
 */
public interface SessionStore {

    /**
     * 保存会话数据
     *
     * @param sessionId 会话ID
     * @param data      会话数据
     * @return 保存的会话ID
     */
    String saveSession(String sessionId, SessionData data);

    /**
     * 加载会话数据
     *
     * @param sessionId 会话ID
     * @return 会话数据，如果不存在则返回null
     */
    SessionData loadSession(String sessionId);

    /**
     * 删除会话
     *
     * @param sessionId 会话ID
     * @return 删除成功返回true，否则返回false
     */
    boolean deleteSession(String sessionId);

    /**
     * 列出所有会话摘要
     *
     * @return 会话摘要列表
     */
    List<SessionSummary> listSessions();

    /**
     * 检查会话是否存在
     *
     * @param sessionId 会话ID
     * @return 存在返回true，否则返回false
     */
    boolean sessionExists(String sessionId);
}
