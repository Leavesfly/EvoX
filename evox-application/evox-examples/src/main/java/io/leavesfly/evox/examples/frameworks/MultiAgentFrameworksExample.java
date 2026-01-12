package io.leavesfly.evox.examples.frameworks;

import io.leavesfly.evox.agents.base.Agent;
import io.leavesfly.evox.agents.builder.AgentBuilder;
import io.leavesfly.evox.frameworks.debate.MultiAgentDebate;
import io.leavesfly.evox.frameworks.debate.MultiAgentDebate.DebateAgent;
import io.leavesfly.evox.frameworks.debate.MultiAgentDebate.DebateRecord;
import io.leavesfly.evox.frameworks.team.CollaborationMode;
import io.leavesfly.evox.frameworks.team.TeamFramework;
import io.leavesfly.evox.frameworks.team.TeamMember;
import io.leavesfly.evox.frameworks.team.TeamRole;
import io.leavesfly.evox.frameworks.team.TaskExecution;
import io.leavesfly.evox.frameworks.consensus.ConsensusFramework;
import io.leavesfly.evox.frameworks.consensus.ConsensusFramework.ConsensusAgent;

import io.leavesfly.evox.models.config.OpenAILLMConfig;
import io.leavesfly.evox.models.openai.OpenAILLM;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 多智能体协同框架示例
 * 
 * <p>本示例演示EvoX框架提供的多种多智能体协同模式：
 * <ul>
 *   <li>辩论框架(Debate): 多个智能体通过辩论达成最优解</li>
 *   <li>团队协作框架(Team): 多种协作模式完成复杂任务</li>
 *   <li>共识框架(Consensus): 通过投票和讨论达成共识</li>
 * </ul>
 * </p>
 */
@Slf4j
public class MultiAgentFrameworksExample {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("多智能体协同框架示例");
        System.out.println("========================================\n");

        // 创建LLM
        OpenAILLM llm = createLLM();

        // 示例1: 辩论框架
        demonstrateDebateFramework(llm);

        // 示例2: 团队协作框架
        demonstrateTeamFramework(llm);

        // 示例3: 共识框架
        demonstrateConsensusFramework(llm);

        System.out.println("\n========================================");
        System.out.println("所有示例演示完成!");
        System.out.println("========================================");
    }

    /**
     * 演示辩论框架
     */
    private static void demonstrateDebateFramework(OpenAILLM llm) {
        System.out.println("【示例 1】辩论框架 (Debate Framework)");
        System.out.println("----------------------------------------");
        System.out.println("场景: 多个智能体辩论\"AI是否会取代程序员\"");
        System.out.println();

        // 创建辩论智能体 - 使用匿名类实现接口
        List<DebateAgent> debateAgents = new ArrayList<>();
        
        debateAgents.add(new DebateAgent() {
            @Override
            public String getName() {
                return "乐观派";
            }
            
            @Override
            public String respond(String question, List<DebateRecord> history) {
                return "作为技术乐观主义者，我认为AI会成为程序员的强大助手，极大提升开发效率，" +
                       "而不是完全取代程序员。AI擅长处理重复性工作，程序员将专注于更有创造性的架构设计和问题解决。";
            }
        });

        debateAgents.add(new DebateAgent() {
            @Override
            public String getName() {
                return "现实派";
            }
            
            @Override
            public String respond(String question, List<DebateRecord> history) {
                return "从务实角度看，AI确实会改变编程方式，但人类的创造力、领域知识和复杂决策能力仍然不可替代。" +
                       "未来可能是人机协作的模式，而非完全替代。";
            }
        });

        debateAgents.add(new DebateAgent() {
            @Override
            public String getName() {
                return "怀疑派";
            }
            
            @Override
            public String respond(String question, List<DebateRecord> history) {
                return "我关注AI的局限性：缺乏真正的理解能力、难以处理新颖问题、无法进行深度创新。" +
                       "AI可能在简单编码任务上有帮助，但复杂系统设计仍需人类专业知识。";
            }
        });

        // 创建辩论框架
        MultiAgentDebate debate = new MultiAgentDebate(
                debateAgents,
                3  // 最妑3轮辩论
        );

        System.out.println("参与辩论的智能体:");
        for (DebateAgent agent : debateAgents) {
            System.out.println("  - " + agent.getName());
        }
        System.out.println();

        // 执行辩论
        System.out.println("开始辩论...");
        String result = debate.debate("AI是否会完全取代程序员的工作？");
        
        System.out.println("\n辩论结果:");
        System.out.println(result);
        System.out.println("\n✅ 辩论框架演示完成\n");
    }

    /**
     * 演示团队协作框架
     */
    private static void demonstrateTeamFramework(OpenAILLM llm) {
        System.out.println("【示例 2】团队协作框架 (Team Framework)");
        System.out.println("----------------------------------------");
        System.out.println("场景: 团队协作完成产品设计任务");
        System.out.println();

        // 创建团队成员 - 使用匿名类实现接口
        List<TeamMember<String>> teamMembers = new ArrayList<>();

        teamMembers.add(new TeamMember<String>() {
            @Override
            public String getMemberId() { return "PM"; }
            @Override
            public String getMemberName() { return "Product Manager"; }
            @Override
            public TeamRole getRole() { return TeamRole.COORDINATOR; }
            @Override
            public String execute(String task, String previousResult, List<TaskExecution<String>> history) {
                return "产品需求: " + task + " - 已完成需求分析";
            }
        });

        teamMembers.add(new TeamMember<String>() {
            @Override
            public String getMemberId() { return "Designer"; }
            @Override
            public String getMemberName() { return "UI Designer"; }
            @Override
            public TeamRole getRole() { return TeamRole.EXECUTOR; }
            @Override
            public String execute(String task, String previousResult, List<TaskExecution<String>> history) {
                return "UI设计: 基于" + (previousResult != null ? previousResult : task) + " - 已完成界面设计";
            }
        });

        teamMembers.add(new TeamMember<String>() {
            @Override
            public String getMemberId() { return "Developer"; }
            @Override
            public String getMemberName() { return "Engineer"; }
            @Override
            public TeamRole getRole() { return TeamRole.EXECUTOR; }
            @Override
            public String execute(String task, String previousResult, List<TaskExecution<String>> history) {
                return "开发实现: 基于设计稿 - 已完成功能开发";
            }
        });

        teamMembers.add(new TeamMember<String>() {
            @Override
            public String getMemberId() { return "Tester"; }
            @Override
            public String getMemberName() { return "QA Engineer"; }
            @Override
            public TeamRole getRole() { return TeamRole.REVIEWER; }
            @Override
            public String execute(String task, String previousResult, List<TaskExecution<String>> history) {
                return "质量保证: 已完成功能测试，通过";
            }
        });

        // 创建团队框架 - 顺序协作模式
        TeamFramework<String> team = new TeamFramework<>(
                teamMembers,
                CollaborationMode.SEQUENTIAL
        );

        System.out.println("团队成员:");
        for (TeamMember<String> member : teamMembers) {
            System.out.println("  - " + member.getMemberId() + ": " + member.getRole());
        }
        System.out.println("\n协作模式: 顺序执行 (SEQUENTIAL)");
        System.out.println();

        // 执行团队任务
        System.out.println("开始执行任务...");
        var result = team.executeTeamTask("开发一个用户登录功能");
        
        System.out.println("\n任务结果:");
        System.out.println("  状态: " + (result.isSuccess() ? "成功" : "失败"));
        System.out.println("  参与人数: " + result.getParticipantCount());
        System.out.println("  执行时间: " + result.getDuration() + "ms");
        System.out.println("\n各成员贡献:");
        result.getContributions().forEach(contribution -> {
            System.out.println("  - " + contribution.getMemberId() + ": " + contribution.getResult());
        });
        
        System.out.println("\n✅ 团队协作框架演示完成\n");
    }

    /**
     * 演示共识框架
     */
    private static void demonstrateConsensusFramework(OpenAILLM llm) {
        System.out.println("【示例 3】共识框架 (Consensus Framework)");
        System.out.println("----------------------------------------");
        System.out.println("场景: 多个专家对技术方案达成共识");
        System.out.println();

        System.out.println("注意: ConsensusFramework示例需要额外的策略类实现。");
        System.out.println("请参考evox-frameworks模块中的完整实现。");
        
        System.out.println("\n✅ 共识框架演示完成\n");
    }

    /**
     * 创建LLM实例
     */
    private static OpenAILLM createLLM() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = "your-api-key-here";
            log.warn("未设置OPENAI_API_KEY环境变量，使用占位符");
        }

        OpenAILLMConfig config = OpenAILLMConfig.builder()
                .model("gpt-4o-mini")
                .apiKey(apiKey)
                .temperature(0.7f)
                .maxTokens(1000)
                .build();

        return new OpenAILLM(config);
    }
}
