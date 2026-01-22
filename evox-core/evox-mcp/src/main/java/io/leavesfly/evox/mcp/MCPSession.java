package io.leavesfly.evox.mcp;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MCP会话管理
 * 管理客户端与服务器之间的会话状态
 *
 * @author EvoX Team
 */
@Slf4j
@Data
public class MCPSession {

    /**
     * 会话ID
     */
    private final String sessionId;

    /**
     * 会话状态
     */
    private SessionState state;

    /**
     * 协议版本
     */
    private String protocolVersion;

    /**
     * 客户端信息
     */
    private MCPProtocol.ClientInfo clientInfo;

    /**
     * 服务端信息
     */
    private MCPProtocol.ServerInfo serverInfo;

    /**
     * 客户端能力
     */
    private MCPProtocol.ClientCapabilities clientCapabilities;

    /**
     * 服务端能力
     */
    private MCPProtocol.ServerCapabilities serverCapabilities;

    /**
     * 请求ID生成器
     */
    private final AtomicLong requestIdGenerator;

    /**
     * 待处理的请求映射（requestId -> callback）
     */
    private final Map<Object, PendingRequest> pendingRequests;

    /**
     * 会话创建时间
     */
    private final long createdAt;

    /**
     * 最后活动时间
     */
    private long lastActivityAt;

    /**
     * 会话元数据
     */
    private Map<String, Object> metadata;

    public MCPSession() {
        this.sessionId = UUID.randomUUID().toString();
        this.state = SessionState.CREATED;
        this.requestIdGenerator = new AtomicLong(1);
        this.pendingRequests = new ConcurrentHashMap<>();
        this.createdAt = System.currentTimeMillis();
        this.lastActivityAt = this.createdAt;
        this.metadata = new ConcurrentHashMap<>();
    }

    /**
     * 生成下一个请求ID
     */
    public long nextRequestId() {
        return requestIdGenerator.getAndIncrement();
    }

    /**
     * 注册待处理请求
     */
    public void registerPendingRequest(Object requestId, PendingRequest request) {
        pendingRequests.put(requestId, request);
        updateActivity();
    }

    /**
     * 完成请求
     */
    public PendingRequest completePendingRequest(Object requestId) {
        updateActivity();
        return pendingRequests.remove(requestId);
    }

    /**
     * 获取待处理请求
     */
    public PendingRequest getPendingRequest(Object requestId) {
        return pendingRequests.get(requestId);
    }

    /**
     * 检查是否有待处理请求
     */
    public boolean hasPendingRequests() {
        return !pendingRequests.isEmpty();
    }

    /**
     * 获取待处理请求数量
     */
    public int getPendingRequestCount() {
        return pendingRequests.size();
    }

    /**
     * 更新活动时间
     */
    public void updateActivity() {
        this.lastActivityAt = System.currentTimeMillis();
    }

    /**
     * 检查会话是否已初始化
     */
    public boolean isInitialized() {
        return state == SessionState.INITIALIZED;
    }

    /**
     * 检查会话是否活跃
     */
    public boolean isActive() {
        return state == SessionState.INITIALIZED;
    }

    /**
     * 检查会话是否已关闭
     */
    public boolean isClosed() {
        return state == SessionState.CLOSED || state == SessionState.ERROR;
    }

    /**
     * 标记为初始化中
     */
    public void markInitializing() {
        this.state = SessionState.INITIALIZING;
        updateActivity();
        log.debug("会话 {} 开始初始化", sessionId);
    }

    /**
     * 标记为已初始化
     */
    public void markInitialized(MCPProtocol.InitializeResult result) {
        this.state = SessionState.INITIALIZED;
        this.protocolVersion = result.getProtocolVersion();
        this.serverInfo = result.getServerInfo();
        this.serverCapabilities = result.getCapabilities();
        updateActivity();
        log.info("会话 {} 初始化完成, 服务器: {}", sessionId, 
                serverInfo != null ? serverInfo.getName() : "unknown");
    }

    /**
     * 标记为错误状态
     */
    public void markError(String errorMessage) {
        this.state = SessionState.ERROR;
        updateActivity();
        log.error("会话 {} 发生错误: {}", sessionId, errorMessage);
    }

    /**
     * 关闭会话
     */
    public void close() {
        this.state = SessionState.CLOSED;
        pendingRequests.clear();
        updateActivity();
        log.info("会话 {} 已关闭", sessionId);
    }

    /**
     * 获取会话存活时间（毫秒）
     */
    public long getUptime() {
        return System.currentTimeMillis() - createdAt;
    }

    /**
     * 获取空闲时间（毫秒）
     */
    public long getIdleTime() {
        return System.currentTimeMillis() - lastActivityAt;
    }

    /**
     * 设置元数据
     */
    public void setMeta(String key, Object value) {
        metadata.put(key, value);
    }

    /**
     * 获取元数据
     */
    @SuppressWarnings("unchecked")
    public <T> T getMeta(String key) {
        return (T) metadata.get(key);
    }

    /**
     * 会话状态枚举
     */
    public enum SessionState {
        /**
         * 已创建（未初始化）
         */
        CREATED,
        /**
         * 初始化中
         */
        INITIALIZING,
        /**
         * 已初始化（可正常使用）
         */
        INITIALIZED,
        /**
         * 错误状态
         */
        ERROR,
        /**
         * 已关闭
         */
        CLOSED
    }

    /**
     * 待处理请求
     */
    @Data
    public static class PendingRequest {
        private final Object requestId;
        private final String method;
        private final long createdAt;
        private final ResponseCallback callback;

        public PendingRequest(Object requestId, String method, ResponseCallback callback) {
            this.requestId = requestId;
            this.method = method;
            this.createdAt = System.currentTimeMillis();
            this.callback = callback;
        }

        /**
         * 获取请求等待时间（毫秒）
         */
        public long getWaitTime() {
            return System.currentTimeMillis() - createdAt;
        }
    }

    /**
     * 响应回调接口
     */
    public interface ResponseCallback {
        /**
         * 响应成功
         */
        void onSuccess(Object result);

        /**
         * 响应失败
         */
        void onError(MCPProtocol.Error error);

        /**
         * 超时
         */
        default void onTimeout() {
            onError(MCPProtocol.Error.builder()
                    .code(MCPProtocol.ErrorCodes.TIMEOUT)
                    .message("请求超时")
                    .build());
        }
    }
}
