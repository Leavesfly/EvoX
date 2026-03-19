package io.leavesfly.evox.examples.frameworks;

import io.leavesfly.evox.frameworks.debate.MultiAgentDebate;
import io.leavesfly.evox.frameworks.debate.DefaultDebateAgent;
import io.leavesfly.evox.models.provider.ollama.OllamaLLM;
import io.leavesfly.evox.models.provider.ollama.OllamaLLMConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 辩论框架示例
 * 
 * <p>演示多个智能体通过辩论达成最优解的场景。
 * 辩论框架适用于需要多角度分析、权衡利弊的复杂决策问题。
 * </p>
 * 
 * <p>辩论流程：
 * <ol>
 *   <li>多个智能体分别持有不同观点</li>
 *   <li>各智能体轮流发言阐述论点</li>
 *   <li>主持人汇总并达成最终结论</li>
 * </ol>
 * </p>
 */
public class DebateFrameworkExample {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("辩论框架示例 (Debate Framework)");
        System.out.println("========================================\n");

        // 创建LLM
        OllamaLLM llm = createLLM();

        // 执行辩论演示
        demonstrateDebateFramework(llm);

        System.out.println("\n========================================");
        System.out.println("辩论框架示例演示完成!");
        System.out.println("========================================");
    }

    /**
     * 演示辩论框架
     */
    private static void demonstrateDebateFramework(OllamaLLM llm) {
        System.out.println("场景: 多个智能体辩论\"AI是否会取代程序员\"");
        System.out.println();

        // 创建辩论智能体 - 使用 DefaultDebateAgent
        List<MultiAgentDebate.DebateAgent> debateAgents = new ArrayList<>();

        debateAgents.add(DefaultDebateAgent.builder()
                .agentId("optimist-001")
                .name("乐观派")
                .systemPrompt("作为技术乐观主义者，你认为AI会成为程序员的强大助手，提升效率而非取代。")
                .llm(llm)
                .build());

        debateAgents.add(DefaultDebateAgent.builder()
                .agentId("realist-002")
                .name("现实派")
                .systemPrompt("从务实角度看，你认为人类的创造力和复杂决策不可替代，未来是人机协作。")
                .llm(llm)
                .build());

        debateAgents.add(DefaultDebateAgent.builder()
                .agentId("skeptic-003")
                .name("怀疑派")
                .systemPrompt("你关注AI的局限性，如缺乏理解力、难以处理新颖问题，认为复杂系统仍需人类。")
                .llm(llm)
                .build());

        // 创建辩论框架 (配置主持人以进行共识检查)
        MultiAgentDebate debate = new MultiAgentDebate(
                debateAgents,
                3,
                llm // 使用同一个 LLM 作为主持人
        );

        System.out.println("参与辩论的智能体:");
        for (MultiAgentDebate.DebateAgent agent : debateAgents) {
            System.out.println("  - " + agent.getName());
        }
        System.out.println();

        // 执行辩论
        System.out.println("开始辩论...");
        MultiAgentDebate.DebateResult result = debate.debate("AI是否会完全取代程序员的工作？");
        
        System.out.println("\n辩论结果:");
        System.out.println(result.getFinalAnswer());
        System.out.println("\n✅ 辩论框架演示完成");
    }

    /**
     * 创建LLM实例
     */
    private static OllamaLLM createLLM() {
        OllamaLLMConfig config = new OllamaLLMConfig();
        return new OllamaLLM(config);
    }
}