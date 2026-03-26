package io.leavesfly.evox.examples.optimizer;

import io.leavesfly.evox.models.provider.ollama.OllamaLLM;
import io.leavesfly.evox.workflow.base.Workflow;
import lombok.extern.slf4j.Slf4j;

/**
 * 优化器示例汇总入口 — EvoX Evolving Layer 三层六种优化器完整演示
 *
 * <p>基于 EvoAgentX 论文的 Evolving Layer 架构，本类作为统一入口，依次调用各子示例，
 * 完整展示 EvoX 优化器模块的全部能力。每个子示例也可作为独立 main 程序单独运行。</p>
 *
 * <h3>三层优化器架构</h3>
 * <pre>
 * Layer 1 - Agent Optimizer:    (Prompt(t+1), θ(t+1)) = O_agent(Prompt(t), θ(t), E)
 * Layer 2 - Workflow Optimizer:  W(t+1) = O_workflow(W(t), E)
 * Layer 3 - Memory Optimizer:    M(t+1) = O_memory(M(t), E)
 * </pre>
 *
 * <h3>子示例文件</h3>
 * <ul>
 *   <li>{@link TextGradOptimizerExample}  — Agent 层：文本梯度 Prompt 优化</li>
 *   <li>{@link MIPROOptimizerExample}      — Agent 层：贝叶斯优化 + 指令生成</li>
 *   <li>{@link EvoPromptOptimizerExample}  — Agent 层：进化算法 Prompt 优化</li>
 *   <li>{@link AFlowOptimizerExample}      — Workflow 层：MCTS 工作流结构优化</li>
 *   <li>{@link SEWOptimizerExample}        — Workflow 层：顺序工作流进化</li>
 *   <li>{@link MemoryOptimizerExample}     — Memory 层：记忆压缩/裁剪/摘要</li>
 *   <li>{@link EvaluationFeedbackExample}  — 统一机制：评估反馈载体</li>
 *   <li>{@link OptimizationContextExample} — 统一机制：优化上下文管理</li>
 *   <li>{@link OptimizerExampleUtils}      — 公共工具：LLM、数据集、记忆、打印工具</li>
 * </ul>
 */
@Slf4j
public class SimpleOptimizerExample {

    public static void main(String[] args) {
        OptimizerExampleUtils.printBanner("EvoX Evolving Layer 优化器完整示例", "三层六种优化器 + 统一评估反馈");

        // 准备共享资源
        Workflow testWorkflow = OptimizerExampleUtils.createTestWorkflow();
        Object testDataset = OptimizerExampleUtils.createMockDataset();
        OllamaLLM llm = OptimizerExampleUtils.createLLM();

        // ===== Layer 1: Agent Optimizer =====
        OptimizerExampleUtils.printLayerHeader("Layer 1", "Agent Optimizer",
                "(Prompt(t+1), θ(t+1)) = O_agent(Prompt(t), θ(t), E)");

        TextGradOptimizerExample.run(testDataset, llm);
        MIPROOptimizerExample.run(testDataset, llm);
        EvoPromptOptimizerExample.run(llm);

        // ===== Layer 2: Workflow Optimizer =====
        OptimizerExampleUtils.printLayerHeader("Layer 2", "Workflow Optimizer",
                "W(t+1) = O_workflow(W(t), E)");

        AFlowOptimizerExample.run(testWorkflow, testDataset, llm);
        SEWOptimizerExample.run(testWorkflow, testDataset, llm);

        // ===== Layer 3: Memory Optimizer =====
        OptimizerExampleUtils.printLayerHeader("Layer 3", "Memory Optimizer",
                "M(t+1) = O_memory(M(t), E)");

        MemoryOptimizerExample.run(testDataset, llm);

        // ===== 统一机制演示 =====
        OptimizerExampleUtils.printLayerHeader("统一机制", "EvaluationFeedback & OptimizationContext", "");

        EvaluationFeedbackExample.run(llm, testDataset);
        OptimizationContextExample.run();

        OptimizerExampleUtils.printBanner("所有优化器演示完成", "6 种优化器 × 3 层架构 + 统一评估反馈");
    }
}
