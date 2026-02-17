
package io.leavesfly.evox.frameworks.base;

import io.leavesfly.evox.workflow.base.Workflow;
import io.leavesfly.evox.workflow.base.WorkflowNode;
import io.leavesfly.evox.workflow.execution.WorkflowContext;
import io.leavesfly.evox.workflow.graph.WorkflowGraph;
import io.leavesfly.evox.workflow.node.NodeHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 多智能体协同框架基类
 * 所有协同框架（debate、consensus、team、hierarchical、auction）的公共基类。
 * 封装了基于 Workflow DAG 引擎的构建和执行逻辑。
 *
 * <p>子类只需实现：
 * <ul>
 *   <li>{@link #buildWorkflowGraph(String)} - 构建描述协同流程的 DAG</li>
 *   <li>{@link #registerNodeHandlers(Workflow)} - 注册自定义节点处理器</li>
 * </ul>
 *
 * @author EvoX Team
 */
@Slf4j
@Data
public abstract class MultiAgentFramework {

    /**
     * 底层工作流实例
     */
    protected Workflow workflow;

    /**
     * 框架名称
     */
    protected String frameworkName;

    /**
     * 构建工作流 DAG 图（由子类实现）
     * 子类在此方法中定义协同流程的 DAG 拓扑结构。
     *
     * @param task 任务描述
     * @return 工作流图
     */
    protected abstract WorkflowGraph buildWorkflowGraph(String task);

    /**
     * 注册节点处理器（由子类实现）
     * 子类在此方法中将自定义的 NodeHandler 注册到 Workflow 执行器。
     *
     * @param workflow 工作流实例
     */
    protected abstract void registerNodeHandlers(Workflow workflow);

    /**
     * 执行前的初始化钩子（可选覆盖）
     *
     * @param task 任务描述
     */
    protected void beforeExecute(String task) {
        // 默认空实现，子类可覆盖
    }

    /**
     * 执行后的清理钩子（可选覆盖）
     *
     * @param rawResult 原始执行结果
     */
    protected void afterExecute(String rawResult) {
        // 默认空实现，子类可覆盖
    }

    /**
     * 构建并初始化工作流
     *
     * @param task 任务描述
     * @return 初始化完成的 Workflow 实例
     */
    protected Workflow buildAndInitWorkflow(String task) {
        // 构建 DAG
        WorkflowGraph graph = buildWorkflowGraph(task);

        // 创建 Workflow
        Workflow wf = new Workflow();
        wf.setName(frameworkName != null ? frameworkName : getClass().getSimpleName());
        wf.setGraph(graph);
        wf.setMaxExecutionSteps(getMaxExecutionSteps());
        wf.initModule();

        // 注册 NodeHandlers
        registerNodeHandlers(wf);

        this.workflow = wf;
        return wf;
    }

    /**
     * 执行工作流并返回原始结果
     *
     * @param task   任务描述
     * @param inputs 额外输入参数
     * @return 工作流执行的原始结果字符串
     */
    protected String executeWorkflow(String task, Map<String, Object> inputs) {
        log.info("Starting {} for task: {}", getClass().getSimpleName(), task);

        beforeExecute(task);

        Workflow wf = buildAndInitWorkflow(task);
        String result = wf.execute(inputs);

        afterExecute(result);

        log.info("{} completed", getClass().getSimpleName());
        return result;
    }

    /**
     * 获取最大执行步数（子类可覆盖）
     *
     * @return 最大执行步数
     */
    protected int getMaxExecutionSteps() {
        return 200;
    }

    /**
     * 获取工作流执行进度
     *
     * @return 进度百分比
     */
    public double getProgress() {
        if (workflow == null) {
            return 0.0;
        }
        return workflow.getProgress();
    }

    /**
     * 获取工作流状态
     *
     * @return 工作流状态
     */
    public Workflow.WorkflowStatus getWorkflowStatus() {
        if (workflow == null) {
            return Workflow.WorkflowStatus.PENDING;
        }
        return workflow.getStatus();
    }

    // ============= 工具方法 =============

    /**
     * 创建 COLLECT 类型的工作流节点
     *
     * @param name          节点名称
     * @param handlerName   处理器名称
     * @param handlerConfig 处理器配置
     * @return 工作流节点
     */
    protected WorkflowNode createCollectNode(String name, String handlerName, Map<String, Object> handlerConfig) {
        WorkflowNode node = new WorkflowNode();
        node.setName(name);
        node.setNodeType(WorkflowNode.NodeType.COLLECT);
        node.setHandlerName(handlerName);
        node.setHandlerConfig(handlerConfig);
        node.initModule();
        return node;
    }

    /**
     * 创建 DECISION 类型的工作流节点
     *
     * @param name      节点名称
     * @param condition 条件表达式
     * @param branches  分支映射
     * @return 工作流节点
     */
    protected WorkflowNode createDecisionNode(String name, String condition, Map<String, String> branches) {
        WorkflowNode node = new WorkflowNode();
        node.setName(name);
        node.setNodeType(WorkflowNode.NodeType.DECISION);
        node.setCondition(condition);
        node.setBranches(branches);
        node.initModule();
        return node;
    }

    /**
     * 创建 LOOP 类型的工作流节点
     *
     * @param name          节点名称
     * @param maxIterations 最大迭代次数
     * @param loopCondition 循环条件
     * @return 工作流节点
     */
    protected WorkflowNode createLoopNode(String name, int maxIterations, String loopCondition) {
        WorkflowNode node = new WorkflowNode();
        node.setName(name);
        node.setNodeType(WorkflowNode.NodeType.LOOP);
        node.setMaxIterations(maxIterations);
        node.setLoopCondition(loopCondition);
        node.initModule();
        return node;
    }

    /**
     * 创建 PARALLEL 类型的工作流节点
     *
     * @param name     节点名称
     * @param strategy 并行策略
     * @return 工作流节点
     */
    protected WorkflowNode createParallelNode(String name, WorkflowNode.ParallelStrategy strategy) {
        WorkflowNode node = new WorkflowNode();
        node.setName(name);
        node.setNodeType(WorkflowNode.NodeType.PARALLEL);
        node.setParallelStrategy(strategy);
        node.initModule();
        return node;
    }
}
