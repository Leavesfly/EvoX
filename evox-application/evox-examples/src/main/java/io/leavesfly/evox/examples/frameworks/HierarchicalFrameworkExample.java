package io.leavesfly.evox.examples.frameworks;

import io.leavesfly.evox.frameworks.hierarchical.*;
import io.leavesfly.evox.models.provider.ollama.OllamaLLM;
import io.leavesfly.evox.models.provider.ollama.OllamaLLMConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 分层决策框架示例
 * 
 * <p>演示多层级管理与执行模式的决策场景。
 * 分层框架适用于复杂任务的分解与逐层执行。
 * </p>
 * 
 * <p>分层决策流程：
 * <ol>
 *   <li>高层制定宏观规划和目标分解</li>
 *   <li>中层将目标转化为具体任务</li>
 *   <li>底层执行具体任务并反馈结果</li>
 * </ol>
 * </p>
 */
public class HierarchicalFrameworkExample {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("分层决策框架示例 (Hierarchical Framework)");
        System.out.println("========================================\n");

        // 创建LLM
        OllamaLLM llm = createLLM();

        // 执行分层决策框架演示
        demonstrateHierarchicalFramework(llm);

        System.out.println("\n========================================");
        System.out.println("分层决策框架示例演示完成!");
        System.out.println("========================================");
    }

    /**
     * 演示分层决策框架
     */
    private static void demonstrateHierarchicalFramework(OllamaLLM llm) {
        System.out.println("场景: 软件开发项目的分层规划与执行");
        System.out.println();

        // 1. 创建决策层级
        List<DecisionLayer<String>> layers = new ArrayList<>();

        // 管理层 (Level 0): 负责宏观规划
        layers.add(DefaultDecisionLayer.builder()
                .agentId("project-manager-layer-001")
                .name("项目经理层")
                .level(0)
                .systemPrompt("你是一个资深项目经理，负责将复杂的项目目标分解为核心模块。")
                .llm(llm)
                .build());

        // 技术架构层 (Level 1): 负责技术细节分解
        layers.add(DefaultDecisionLayer.builder()
                .agentId("tech-architect-layer-002")
                .name("技术架构层")
                .level(1)
                .systemPrompt("你是一个技术架构师，负责将业务模块转化为具体的技术实施步骤。")
                .llm(llm)
                .build());

        // 2. 创建框架
        HierarchicalFramework<String> framework = new HierarchicalFramework<>(layers);

        // 3. 执行任务
        System.out.println("开始分层决策执行...");
        String task = "开发一个包含用户认证和支付功能的移动商城APP";
        HierarchicalResult<String> result = framework.executeHierarchical(task);

        // 4. 输出结果
        System.out.println("\n分层决策结果:");
        if (result.isSuccess()) {
            System.out.println("  总层级数: " + result.getLayers());
            System.out.println("\n执行路径详情:");
            for (ExecutionRecord<String> record : result.getHistory()) {
                System.out.println(String.format("  - 层级 [%s]: %s", record.getLayerId(), record.getTask()));
                System.out.println(String.format("    决策依据: %s", record.getDecision().getReasoning()));
            }
        } else {
            System.out.println("  决策失败: " + result.getError());
        }

        System.out.println("\n✅ 分层决策框架演示完成");
    }

    /**
     * 创建LLM实例
     */
    private static OllamaLLM createLLM() {
        OllamaLLMConfig config = new OllamaLLMConfig();
        return new OllamaLLM(config);
    }
}