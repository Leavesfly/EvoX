package io.leavesfly.evox.optimizers.base;

/**
 * 优化器类型枚举 - 对应 Evolving Layer 三层优化器
 *
 * @author EvoX Team
 */
public enum OptimizationType {

    /**
     * Agent 级优化 - 优化 prompt 模板、工具配置和行动策略
     * 对应公式: (Prompt(t+1), θ(t+1)) = O_agent(Prompt(t), θ(t), E)
     */
    AGENT,

    /**
     * Workflow 级优化 - 优化任务分解和执行流程的图结构
     * 对应公式: W(t+1) = O_workflow(W(t), E)
     */
    WORKFLOW,

    /**
     * Memory 级优化 - 优化记忆的选择性保留、动态裁剪和优先级检索
     * 对应公式: M(t+1) = O_memory(M(t), E)
     */
    MEMORY
}
