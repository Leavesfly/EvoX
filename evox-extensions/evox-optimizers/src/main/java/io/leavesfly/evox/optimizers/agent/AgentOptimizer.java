package io.leavesfly.evox.optimizers.agent;

import io.leavesfly.evox.models.spi.LLMProvider;
import io.leavesfly.evox.optimizers.Optimizer;
import io.leavesfly.evox.optimizers.base.EvaluationFeedback;
import io.leavesfly.evox.optimizers.base.OptimizationType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Agent 级优化器抽象基类 - Evolving Layer Agent Optimizer
 *
 * 对应论文公式 (3):
 *   (Prompt(t+1), θ(t+1)) = O_agent(Prompt(t), θ(t), E)
 *
 * Agent 优化器旨在优化 agent 的 prompt 模板、工具配置和行动策略，
 * 以提升每个 agent 在多样化任务上的表现。
 * TextGrad 和 MIPRO 优化器用于 agent 优化，联合应用基于梯度的
 * prompt 调优、上下文学习和偏好引导的精炼。
 *
 * @author EvoX Team
 */
@Slf4j
@Data
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public abstract class AgentOptimizer extends Optimizer {

    /**
     * 用于优化的 LLM（生成梯度/候选指令）
     */
    protected LLMProvider optimizerLLM;

    /**
     * 当前正在优化的 prompt 模板
     */
    protected String currentPrompt;

    /**
     * 优化后的最佳 prompt 模板
     */
    protected String bestPrompt;

    /**
     * Agent 配置参数（工具配置、行动策略等）
     */
    protected Map<String, Object> agentConfig;

    /**
     * 优化后的最佳 Agent 配置
     */
    protected Map<String, Object> bestAgentConfig;

    @Override
    public final OptimizationType getOptimizationType() {
        return OptimizationType.AGENT;
    }

    /**
     * 执行 Agent 级优化操作
     * 子类实现具体的优化算法（如 TextGrad 的梯度下降、MIPRO 的贝叶斯优化）
     *
     * @param currentPrompt 当前 prompt
     * @param agentConfig 当前 agent 配置
     * @param feedback 评估反馈
     * @return 优化后的 prompt
     */
    public abstract String optimizePrompt(String currentPrompt, Map<String, Object> agentConfig,
                                          EvaluationFeedback feedback);

    /**
     * 优化 Agent 配置（工具配置、行动策略等）
     *
     * @param agentConfig 当前配置
     * @param feedback 评估反馈
     * @return 优化后的配置
     */
    public abstract Map<String, Object> optimizeConfig(Map<String, Object> agentConfig,
                                                       EvaluationFeedback feedback);

    /**
     * 恢复优化过程中找到的最佳 prompt 和配置
     */
    public void restoreBest() {
        if (bestPrompt != null) {
            this.currentPrompt = bestPrompt;
            log.info("Restored best prompt with score: {}", bestScore);
        }
        if (bestAgentConfig != null) {
            this.agentConfig = bestAgentConfig;
            log.info("Restored best agent config");
        }
    }

    /**
     * 获取当前优化的 prompt
     */
    public String getCurrentPrompt() {
        return currentPrompt;
    }

    /**
     * 获取最佳 prompt
     */
    public String getBestPrompt() {
        return bestPrompt;
    }
}
