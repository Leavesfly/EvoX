package io.leavesfly.evox.examples.frameworks;

import io.leavesfly.evox.frameworks.consensus.ConsensusResult;
import io.leavesfly.evox.frameworks.consensus.ConsensusFramework;
import io.leavesfly.evox.frameworks.consensus.DefaultConsensusAgent;
import io.leavesfly.evox.frameworks.consensus.strategy.MajorityVotingStrategy;
import io.leavesfly.evox.models.provider.ollama.OllamaLLM;
import io.leavesfly.evox.models.provider.ollama.OllamaLLMConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 共识框架示例
 * 
 * <p>演示多个智能体通过投票和讨论达成共识的场景。
 * 共识框架适用于需要群体决策、意见统一的场景。
 * </p>
 * 
 * <p>支持的共识策略：
 * <ul>
 *   <li>MajorityVotingStrategy: 多数票决策</li>
 *   <li>UnanimousStrategy: 全票通过</li>
 *   <li>WeightedVotingStrategy: 加权投票</li>
 * </ul>
 * </p>
 */
public class ConsensusFrameworkExample {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("共识框架示例 (Consensus Framework)");
        System.out.println("========================================\n");

        // 创建LLM
        OllamaLLM llm = createLLM();

        // 执行共识框架演示
        demonstrateConsensusFramework(llm);

        System.out.println("\n========================================");
        System.out.println("共识框架示例演示完成!");
        System.out.println("========================================");
    }

    /**
     * 演示共识框架
     */
    private static void demonstrateConsensusFramework(OllamaLLM llm) {
        System.out.println("场景: 多个专家对\"公司是否应该远程办公\"达成共识");
        System.out.println();

        // 1. 创建共识智能体 - 使用 DefaultConsensusAgent
        List<ConsensusFramework.ConsensusAgent<String>> agents = new ArrayList<>();

        agents.add(DefaultConsensusAgent.<String>builder()
                .agentId("hr-expert-001")
                .name("人力资源专家")
                .systemPrompt("你是一个HR专家，关注员工福利、招聘竞争力和办公成本。")
                .llm(llm)
                .build());

        agents.add(DefaultConsensusAgent.<String>builder()
                .agentId("tech-lead-002")
                .name("技术主管")
                .systemPrompt("你是一个技术主管，关注团队协作效率、沟通成本和系统安全性。")
                .llm(llm)
                .build());

        agents.add(DefaultConsensusAgent.<String>builder()
                .agentId("cfo-003")
                .name("财务总监")
                .systemPrompt("你是一个CFO，关注租金支出、行政成本和运营利润。")
                .llm(llm)
                .build());

        // 2. 创建框架并指定策略 (多数票策略)
        ConsensusFramework<String> consensus = new ConsensusFramework<>(
                agents,
                new MajorityVotingStrategy<>()
        );

        // 3. 执行共识过程
        System.out.println("开始共识讨论...");
        ConsensusResult<String> result =
            consensus.reachConsensus("公司是否应该全面实施远程办公政策？");

        // 4. 输出结果
        System.out.println("\n共识结果摘要:");
        System.out.println("  是否达成共识: " + (result.isReached() ? "是" : "否"));
        System.out.println("  最终决策: " + result.getResult());
        System.out.println("  讨论轮次: " + result.getRounds());
        System.out.println("  置信度: " + result.getConfidence());

        System.out.println("\n✅ 共识框架演示完成");
    }

    /**
     * 创建LLM实例
     */
    private static OllamaLLM createLLM() {
        OllamaLLMConfig config = new OllamaLLMConfig();
        return new OllamaLLM(config);
    }
}