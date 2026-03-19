package io.leavesfly.evox.examples.frameworks;

import io.leavesfly.evox.frameworks.team.CollaborationMode;
import io.leavesfly.evox.frameworks.team.TeamFramework;
import io.leavesfly.evox.frameworks.team.TeamMember;
import io.leavesfly.evox.frameworks.team.TeamRole;
import io.leavesfly.evox.frameworks.team.TeamResult;
import io.leavesfly.evox.frameworks.team.DefaultTeamMember;
import io.leavesfly.evox.models.provider.ollama.OllamaLLM;
import io.leavesfly.evox.models.provider.ollama.OllamaLLMConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 团队协作框架示例
 *
 * <p>演示多智能体以团队形式协作完成复杂任务的场景。
 * 团队框架支持多种协作模式，适用于需要不同角色分工的任务。
 * </p>
 *
 * <p>支持的协作模式：
 * <ul>
 *   <li>HIERARCHICAL: 分层执行模式，按角色优先级顺序执行</li>
 *   <li>PARALLEL: 并行执行模式，多角色同时工作</li>
 *   <li>SEQUENTIAL: 顺序执行模式，按指定顺序依次执行</li>
 * </ul>
 * </p>
 */
public class TeamFrameworkExample {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("团队协作框架示例 (Team Framework)");
        System.out.println("========================================\n");

        // 创建LLM
        OllamaLLM llm = createLLM();

        // 执行团队协作演示
        demonstrateTeamFramework(llm);

        System.out.println("\n========================================");
        System.out.println("团队协作框架示例演示完成!");
        System.out.println("========================================");
    }

    /**
     * 演示团队协作框架
     */
    private static void demonstrateTeamFramework(OllamaLLM llm) {
        System.out.println("场景: 团队协作完成\"开发一个智能客服系统\"的任务");
        System.out.println();

        // 1. 创建团队成员 - 使用 DefaultTeamMember
        List<TeamMember<String>> teamMembers = new ArrayList<>();

        teamMembers.add(DefaultTeamMember.builder()
                .agentId("product-manager-001")
                .name("产品经理")
                .role(TeamRole.COORDINATOR)
                .systemPrompt("你是一个经验丰富的产品经理，擅长分析用户需求并制定核心功能列表。")
                .llm(llm)
                .build());

        teamMembers.add(DefaultTeamMember.builder()
                .agentId("senior-developer-002")
                .name("高级开发")
                .role(TeamRole.EXECUTOR)
                .skills(List.of("Java", "LLM Integration", "System Architecture"))
                .systemPrompt("你是一个高级开发工程师，负责根据需求设计技术方案并给出实现思路。")
                .llm(llm)
                .build());

        teamMembers.add(DefaultTeamMember.builder()
                .agentId("test-engineer-003")
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
        System.out.println("----------------------------------------");
        TeamResult<String> result = team.executeTeamTask("设计并实现一个智能客服系统的核心流程");

        // 4. 打印各成员执行过程与结果
        System.out.println("\n========== 各成员执行过程与结果 ==========");
        List<io.leavesfly.evox.frameworks.team.TaskExecution<String>> contributions = result.getContributions();
        for (int i = 0; i < contributions.size(); i++) {
            io.leavesfly.evox.frameworks.team.TaskExecution<String> contribution = contributions.get(i);
            System.out.println(String.format("\n【第 %d 步】成员: %s  |  耗时: %dms",
                    i + 1, contribution.getMemberId(), contribution.getDuration()));
            System.out.println("----------------------------------------");
            System.out.println("执行结果:");
            System.out.println(contribution.getResult());
            System.out.println("----------------------------------------");
        }

        // 5. 打印整体摘要
        System.out.println("\n========== 任务结果摘要 ==========");
        System.out.println("  状态:   " + (result.isSuccess() ? "✅ 成功" : "❌ 失败"));
        System.out.println("  总耗时: " + result.getDuration() + "ms");
        System.out.println("  参与成员数: " + contributions.size());

        System.out.println("\n✅ 团队协作框架演示完成");
    }

    /**
     * 创建LLM实例
     */
    private static OllamaLLM createLLM() {
        OllamaLLMConfig config = new OllamaLLMConfig();
        return new OllamaLLM(config);
    }
}