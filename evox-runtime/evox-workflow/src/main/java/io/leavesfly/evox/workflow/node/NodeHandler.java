
package io.leavesfly.evox.workflow.node;

import io.leavesfly.evox.workflow.base.WorkflowNode;
import io.leavesfly.evox.workflow.execution.WorkflowContext;
import reactor.core.publisher.Mono;

/**
 * 节点处理器接口
 * 允许外部模块（如 frameworks）注册自定义的节点执行逻辑，
 * 使得 WorkflowExecutor 可以执行 COLLECT 等自定义节点类型。
 *
 * @author EvoX Team
 */
public interface NodeHandler {

    /**
     * 执行节点处理逻辑
     *
     * @param context 工作流执行上下文
     * @param node    当前节点
     * @return 执行结果的 Mono 包装
     */
    Mono<Object> handle(WorkflowContext context, WorkflowNode node);

    /**
     * 获取处理器名称
     *
     * @return 处理器名称，用于在注册表中唯一标识
     */
    String getHandlerName();
}
