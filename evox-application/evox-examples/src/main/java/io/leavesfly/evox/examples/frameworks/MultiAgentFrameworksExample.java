package io.leavesfly.evox.examples.frameworks;


import io.leavesfly.evox.frameworks.consensus.ConsensusResult;
import io.leavesfly.evox.frameworks.debate.MultiAgentDebate;
import io.leavesfly.evox.frameworks.team.CollaborationMode;
import io.leavesfly.evox.frameworks.team.TeamFramework;
import io.leavesfly.evox.frameworks.team.TeamMember;
import io.leavesfly.evox.frameworks.team.TeamRole;
import io.leavesfly.evox.frameworks.team.TeamResult;

import io.leavesfly.evox.frameworks.team.DefaultTeamMember;
import io.leavesfly.evox.frameworks.consensus.ConsensusFramework;

import io.leavesfly.evox.frameworks.consensus.DefaultConsensusAgent;
import io.leavesfly.evox.frameworks.consensus.strategy.MajorityVotingStrategy;
import io.leavesfly.evox.frameworks.auction.*;
import io.leavesfly.evox.frameworks.hierarchical.*;
import io.leavesfly.evox.frameworks.debate.DefaultDebateAgent;

import io.leavesfly.evox.models.provider.ollama.OllamaLLMConfig;
import io.leavesfly.evox.models.provider.ollama.OllamaLLM;
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
 *   <li>拍卖框架(Auction): 支持多种拍卖机制的资源分配</li>
 *   <li>分层决策框架(Hierarchical): 多层级管理与执行模式</li>
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
        OllamaLLM llm = createLLM();

        // 示例1: 辩论框架
        demonstrateDebateFramework(llm);

        // 示例2: 团队协作框架
        demonstrateTeamFramework(llm);

        // 示例3: 共识框架
        demonstrateConsensusFramework(llm);

        // 示例4: 拍卖框架
        demonstrateAuctionFramework(llm);

        // 示例5: 分层决策框架
        demonstrateHierarchicalFramework(llm);

        System.out.println("\n========================================");
        System.out.println("所有示例演示完成!");
        System.out.println("========================================");
    }

    /**
     * 演示辩论框架
     */
    private static void demonstrateDebateFramework(OllamaLLM llm) {
        System.out.println("【示例 1】辩论框架 (Debate Framework)");
        System.out.println("----------------------------------------");
        System.out.println("场景: 多个智能体辩论\"AI是否会取代程序员\"");
        System.out.println();

        // 创建辩论智能体 - 使用 DefaultDebateAgent
        List<MultiAgentDebate.DebateAgent> debateAgents = new ArrayList<>();

        debateAgents.add(DefaultDebateAgent.builder()
                .name("乐观派")
                .systemPrompt("作为技术乐观主义者，你认为AI会成为程序员的强大助手，提升效率而非取代。")
                .llm(llm)
                .build());

        debateAgents.add(DefaultDebateAgent.builder()
                .name("现实派")
                .systemPrompt("从务实角度看，你认为人类的创造力和复杂决策不可替代，未来是人机协作。")
                .llm(llm)
                .build());

        debateAgents.add(DefaultDebateAgent.builder()
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
        System.out.println("\n✅ 辩论框架演示完成\n");
    }

    /**
     * 演示团队协作框架
     */
    private static void demonstrateTeamFramework(OllamaLLM llm) {
        System.out.println("【示例 2】团队协作框架 (Team Framework)");
        System.out.println("----------------------------------------");
        System.out.println("场景: 团队协作完成\"开发一个智能客服系统\"的任务");
        System.out.println();

        // 1. 创建团队成员 - 使用 DefaultTeamMember
        List<TeamMember<String>> teamMembers = new ArrayList<>();

        teamMembers.add(DefaultTeamMember.builder()
                .name("产品经理")
                .role(TeamRole.COORDINATOR)
                .systemPrompt("你是一个经验丰富的产品经理，擅长分析用户需求并制定核心功能列表。")
                .llm(llm)
                .build());

        teamMembers.add(DefaultTeamMember.builder()
                .name("高级开发")
                .role(TeamRole.EXECUTOR)
                .skills(List.of("Java", "LLM Integration", "System Architecture"))
                .systemPrompt("你是一个高级开发工程师，负责根据需求设计技术方案并给出实现思路。")
                .llm(llm)
                .build());

        teamMembers.add(DefaultTeamMember.builder()
                .name("测试工程师")
                .role(TeamRole.REVIEWER)
                .systemPrompt("你是一个严谨的测试工程师，负责对技术方案进行压力测试和边界情况审核。")
                .llm(llm)
                .build());

        // 2. 创建团队框架 - 使用分层协作模式 (HIERARCHICAL)
        // 分层模式会根据角色的优先级 (Priority) 自动排序执行
        TeamFramework<String> team = new TeamFramework<>(
                teamMembers,
                CollaborationMode.HIERARCHICAL
        );

        System.out.println("团队成员及角色:");
        for (TeamMember<String> member : teamMembers) {
            System.out.println(String.format("  - %s (%s)", member.getMemberName(), member.getRole()));
        }
        System.out.println("\n协作模式: 分层执行 (HIERARCHICAL)");
        System.out.println();

        // 3. 执行团队任务
        System.out.println("开始执行任务...");
        TeamResult<String> result = team.executeTeamTask("设计并实现一个智能客服系统的核心流程");
        
        System.out.println("\n任务结果摘要:");
        System.out.println("  状态: " + (result.isSuccess() ? "成功" : "失败"));
        System.out.println("  总耗时: " + result.getDuration() + "ms");
        
        System.out.println("\n各阶段产出精华:");
        result.getContributions().forEach(contribution -> {
            String summary = contribution.getResult();
            if (summary.length() > 100) {
                summary = summary.substring(0, 100) + "...";
            }
            System.out.println(String.format("  - [%s]: %s", contribution.getMemberId(), summary));
        });
        
        System.out.println("\n✅ 团队协作框架演示完成\n");
    }

    /**
     * 演示共识框架
     */
    private static void demonstrateConsensusFramework(OllamaLLM llm) {
        System.out.println("【示例 3】共识框架 (Consensus Framework)");
        System.out.println("----------------------------------------");
        System.out.println("场景: 多个专家对\"公司是否应该远程办公\"达成共识");
        System.out.println();

        // 1. 创建共识智能体 - 使用 DefaultConsensusAgent
        List<ConsensusFramework.ConsensusAgent<String>> agents = new ArrayList<>();

        agents.add(DefaultConsensusAgent.<String>builder()
                .name("人力资源专家")
                .systemPrompt("你是一个HR专家，关注员工福利、招聘竞争力和办公成本。")
                .llm(llm)
                .build());

        agents.add(DefaultConsensusAgent.<String>builder()
                .name("技术主管")
                .systemPrompt("你是一个技术主管，关注团队协作效率、沟通成本和系统安全性。")
                .llm(llm)
                .build());

        agents.add(DefaultConsensusAgent.<String>builder()
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

        System.out.println("\n✅ 共识框架演示完成\n");
    }

    /**
     * 演示拍卖框架
     */
    private static void demonstrateAuctionFramework(OllamaLLM llm) {
        System.out.println("【示例 4】拍卖框架 (Auction Framework)");
        System.out.println("----------------------------------------");
        System.out.println("场景: 三个智能体竞拍\"一张稀有的复古海报\"");
        System.out.println();

        // 1. 创建竞价者
        List<Bidder<String>> bidders = new ArrayList<>();
        
        bidders.add(DefaultBidder.<String>builder()
                .name("收藏家A")
                .valuation(500.0)
                .budget(600.0)
                .systemPrompt("你是一个谨慎的收藏家，对海报估值为500，最多愿意出到600。")
                .llm(llm)
                .build());

        bidders.add(DefaultBidder.<String>builder()
                .name("收藏家B")
                .valuation(450.0)
                .budget(800.0)
                .systemPrompt("你是一个资金雄厚的收藏家，虽然估值450，但只要心情好，预算可以到800。")
                .llm(llm)
                .build());

        bidders.add(DefaultBidder.<String>builder()
                .name("收藏家C")
                .valuation(550.0)
                .budget(580.0)
                .systemPrompt("你是一个理性的收藏家，严格遵守估值，绝不超支。")
                .llm(llm)
                .build());

        // 2. 配置拍卖参数 (英式拍卖)
        AuctionConfig config = AuctionConfig.builder()
                .startingPrice(100.0)
                .priceIncrement(50.0)
                .maxRounds(10)
                .build();

        // 3. 创建并启动拍卖
        AuctionFramework<String> auction = new AuctionFramework<>(
                "复古海报",
                AuctionMechanism.ENGLISH,
                bidders,
                config
        );

        System.out.println("开始英式拍卖...");
        AuctionResult<String> result = auction.startAuction();

        // 4. 输出结果
        System.out.println("\n拍卖结束结果:");
        if (result.isSuccess()) {
            System.out.println("  获胜者: " + result.getWinner().getBidderName());
            System.out.println("  最终价格: " + result.getFinalPrice());
            System.out.println("  总轮次: " + result.getTotalRounds());
        } else {
            System.out.println("  拍卖失败: " + result.getError());
        }

        System.out.println("\n✅ 拍卖框架演示完成\n");
    }

    /**
     * 演示分层决策框架
     */
    private static void demonstrateHierarchicalFramework(OllamaLLM llm) {
        System.out.println("【示例 5】分层决策框架 (Hierarchical Framework)");
        System.out.println("----------------------------------------");
        System.out.println("场景: 软件开发项目的分层规划与执行");
        System.out.println();

        // 1. 创建决策层级
        List<DecisionLayer<String>> layers = new ArrayList<>();

        // 管理层 (Level 0): 负责宏观规划
        layers.add(DefaultDecisionLayer.builder()
                .name("项目经理层")
                .level(0)
                .systemPrompt("你是一个资深项目经理，负责将复杂的项目目标分解为核心模块。")
                .llm(llm)
                .build());

        // 技术架构层 (Level 1): 负责技术细节分解
        layers.add(DefaultDecisionLayer.builder()
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

        System.out.println("\n✅ 分层决策框架演示完成\n");
    }

    /**
     * 创建LLM实例
     */
    private static OllamaLLM createLLM() {
        OllamaLLMConfig config = new OllamaLLMConfig();
        return new OllamaLLM(config);
    }
}
