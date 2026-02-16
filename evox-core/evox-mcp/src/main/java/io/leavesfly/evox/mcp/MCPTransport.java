package io.leavesfly.evox.mcp;

import java.util.function.Consumer;

/**
 * MCP传输层
 * 处理客户端与服务器之间的消息传输
 *
 * @author EvoX Team
 */
public class MCPTransport {

    /**
     * 内存传输实现
     * 用于测试和本地通信
     */
    public static class InMemoryTransport {

        private InMemoryTransport connectedTransport;
        private boolean running;
        private Consumer<String> messageHandler;
        private final Object lock = new Object();

        /**
         * 连接到另一个传输实例
         */
        public void connectTo(InMemoryTransport other) {
            synchronized (lock) {
                this.connectedTransport = other;
                other.connectedTransport = this;
            }
        }

        /**
         * 启动传输
         */
        public void start() {
            synchronized (lock) {
                this.running = true;
            }
        }

        /**
         * 停止传输
         */
        public void stop() {
            synchronized (lock) {
                this.running = false;
                this.connectedTransport = null;
            }
        }

        /**
         * 是否已连接
         */
        public boolean isConnected() {
            synchronized (lock) {
                return running && connectedTransport != null;
            }
        }

        /**
         * 设置消息处理器
         */
        public void setMessageHandler(Consumer<String> handler) {
            synchronized (lock) {
                this.messageHandler = handler;
            }
        }

        /**
         * 发送消息
         */
        public void send(String message) {
            synchronized (lock) {
                if (!running || connectedTransport == null) {
                    throw new IllegalStateException("传输未启动或未连接");
                }

                InMemoryTransport target = connectedTransport;
                Consumer<String> targetHandler = target.messageHandler;

                // 在目标传输的锁之外调用处理器，避免死锁
                if (targetHandler != null) {
                    targetHandler.accept(message);
                }
            }
        }
    }
}
