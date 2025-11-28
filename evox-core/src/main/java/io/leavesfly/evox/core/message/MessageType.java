package io.leavesfly.evox.core.message;

/**
 * 消息类型枚举
 * 定义系统中所有可能的消息类型
 *
 * @author EvoX Team
 */
public enum MessageType {
    /**
     * 输入消息
     */
    INPUT,

    /**
     * 输出消息
     */
    OUTPUT,

    /**
     * 响应消息
     */
    RESPONSE,

    /**
     * 错误消息
     */
    ERROR,

    /**
     * 系统消息
     */
    SYSTEM,

    /**
     * 未知类型
     */
    UNKNOWN
}
