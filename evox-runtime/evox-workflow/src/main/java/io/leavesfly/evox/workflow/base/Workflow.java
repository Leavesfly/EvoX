package io.leavesfly.evox.workflow.base;

import io.leavesfly.evox.core.agent.IAgentManager;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.core.module.BaseModule;
import io.leavesfly.evox.core.llm.ILLM;
import io.leavesfly.evox.workflow.execution.WorkflowContext;
import io.leavesfly.evox.workflow.execution.WorkflowExecutor;
import io.leavesfly.evox.workflow.graph.WorkflowGraph;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 工作流 - EvoX 工作流的核心类
 * 对应 Python 版本的 WorkFlow
 */
@Slf4j
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Workflow extends BaseModule {

    /**
     * 工作流唯一标识
     */
    private String workflowId;

    /**
     * 工作流名称
     */
    private String name;

    /**
     * 工作流图
     */
    private WorkflowGraph graph;

    /**
     * LLM 模型（可选）
     */
    private ILLM llm;

    /**
     * 智能体管理器（可选）
     */
    private IAgentManager agentManager;

    /**
     * 工作流执行器
     */
    private WorkflowExecutor executor;

    /**
     * 工作流版本
     */
    private Integer workflowVersion;

    /**
     * 最大执行步数
     */
    private int maxExecutionSteps;

    @Override
    public void initModule() {
        if (workflowId == null) {
            workflowId = UUID.randomUUID().toString();
        }
        if (workflowVersion == null) {
            workflowVersion = 1;
        }
        if (maxExecutionSteps == 0) {
            maxExecutionSteps = 100;
        }
        if (graph == null) {
            throw new IllegalStateException("Workflow graph cannot be null");
        }
        if (executor == null) {
            executor = new WorkflowExecutor(this, agentManager);
        }
    }

    /**
     * 同步执行工作流
     *
     * @param inputs 输入参数
     * @return 执行结果
     */
    public String execute(Map<String, Object> inputs) {
        try {
            String result = executeAsync(inputs).block();
            log.info("Workflow.execute() result: {}", result);
            return result;
        } catch (Exception e) {
            log.error("Workflow execution failed", e);
            throw new RuntimeException("Workflow execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * 异步执行工作流
     *
     * @param inputs 输入参数
     * @return Mono 包装的执行结果
     */
    public Mono<String> executeAsync(Map<String, Object> inputs) {
        return Mono.defer(() -> {
            try {
                // 验证工作流图
                graph.validate();

                // 准备输入
                Map<String, Object> preparedInputs = prepareInputs(inputs);

                // 创建执行上下文
                WorkflowContext context = new WorkflowContext(graph.getGoal(), preparedInputs);

                // 记录输入消息
                Message inputMessage = Message.builder()
                        .content(preparedInputs)
                        .messageType(MessageType.INPUT)
                        .build();
                context.addMessage(inputMessage);

                // 执行工作流
                return executor.execute(context)
                        .doOnSuccess(result -> log.info("Workflow execution completed successfully"))
                        .doOnError(error -> log.error("Workflow execution failed: {}", error.getMessage()));

            } catch (Exception e) {
                log.error("Failed to start workflow execution", e);
                return Mono.error(new RuntimeException("Failed to start workflow execution: " + e.getMessage(), e));
            }
        });
    }

    /**
     * 准备输入参数
     */
    private Map<String, Object> prepareInputs(Map<String, Object> inputs) {
        Map<String, Object> preparedInputs = new HashMap<>(inputs != null ? inputs : new HashMap<>());
        
        // 如果输入中没有 goal 但工作流图有 goal，则添加
        if (!preparedInputs.containsKey("goal") && graph.getGoal() != null) {
            preparedInputs.put("goal", graph.getGoal());
        }
        
        return preparedInputs;
    }

    /**
     * 获取工作流状态
     */
    public WorkflowStatus getStatus() {
        if (graph.isFailed()) {
            return WorkflowStatus.FAILED;
        }
        if (graph.isComplete()) {
            return WorkflowStatus.COMPLETED;
        }
        if (graph.getCurrentNodeId() != null) {
            return WorkflowStatus.RUNNING;
        }
        return WorkflowStatus.PENDING;
    }

    /**
     * 获取工作流进度
     */
    public double getProgress() {
        return graph.getProgress();
    }

    /**
     * 重置工作流（用于重新执行）
     */
    public void reset() {
        graph.reset();
        if (executor != null) {
            executor.reset();
        }
        log.info("Workflow {} reset", workflowId);
    }

    /**
     * 工作流状态枚举
     */
    public enum WorkflowStatus {
        /** 待执行 */
        PENDING,
        /** 运行中 */
        RUNNING,
        /** 已完成 */
        COMPLETED,
        /** 失败 */
        FAILED
    }
}
