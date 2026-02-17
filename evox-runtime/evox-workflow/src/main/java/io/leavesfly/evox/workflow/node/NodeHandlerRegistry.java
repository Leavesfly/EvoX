
package io.leavesfly.evox.workflow.node;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 节点处理器注册表
 * 管理所有已注册的 NodeHandler 实例，供 WorkflowExecutor 在执行 COLLECT 节点时查找。
 *
 * @author EvoX Team
 */
@Slf4j
public class NodeHandlerRegistry {

    private final Map<String, NodeHandler> handlers = new ConcurrentHashMap<>();

    /**
     * 注册节点处理器
     *
     * @param handler 节点处理器实例
     */
    public void register(NodeHandler handler) {
        if (handler == null || handler.getHandlerName() == null) {
            throw new IllegalArgumentException("Handler and handler name cannot be null");
        }
        handlers.put(handler.getHandlerName(), handler);
        log.debug("Registered NodeHandler: {}", handler.getHandlerName());
    }

    /**
     * 通过名称注册节点处理器
     *
     * @param handlerName 处理器名称
     * @param handler     处理器实例
     */
    public void register(String handlerName, NodeHandler handler) {
        if (handlerName == null || handler == null) {
            throw new IllegalArgumentException("Handler name and handler cannot be null");
        }
        handlers.put(handlerName, handler);
        log.debug("Registered NodeHandler: {}", handlerName);
    }

    /**
     * 获取节点处理器
     *
     * @param handlerName 处理器名称
     * @return 处理器实例，不存在则返回 null
     */
    public NodeHandler getHandler(String handlerName) {
        return handlers.get(handlerName);
    }

    /**
     * 检查处理器是否已注册
     *
     * @param handlerName 处理器名称
     * @return 是否已注册
     */
    public boolean hasHandler(String handlerName) {
        return handlers.containsKey(handlerName);
    }

    /**
     * 移除节点处理器
     *
     * @param handlerName 处理器名称
     */
    public void unregister(String handlerName) {
        handlers.remove(handlerName);
        log.debug("Unregistered NodeHandler: {}", handlerName);
    }

    /**
     * 清空所有处理器
     */
    public void clear() {
        handlers.clear();
        log.debug("Cleared all NodeHandlers");
    }

    /**
     * 获取已注册的处理器数量
     */
    public int size() {
        return handlers.size();
    }
}
