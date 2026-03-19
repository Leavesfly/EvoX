package io.leavesfly.evox.examples.frameworks;

import io.leavesfly.evox.frameworks.team.CollaborationMode;
import io.leavesfly.evox.frameworks.team.DefaultTeamMember;
import io.leavesfly.evox.frameworks.team.TaskExecution;
import io.leavesfly.evox.frameworks.team.TeamFramework;
import io.leavesfly.evox.frameworks.team.TeamMember;
import io.leavesfly.evox.frameworks.team.TeamResult;
import io.leavesfly.evox.frameworks.team.TeamRole;
import io.leavesfly.evox.models.provider.ollama.OllamaLLM;
import io.leavesfly.evox.models.provider.ollama.OllamaLLMConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 团队并行协作框架示例 (PARALLEL 模式)
 *
 * <p>演示多个专家 Agent 并行独立工作，同时从各自专业视角分析同一个任务。
 * PARALLEL 模式下所有成员接收相同的任务输入，互不依赖，并发执行。
 * </p>
 *
 * <p>适用场景：需要多维度独立评估的任务，如：
 * <ul>
 *   <li>商业计划书多角度评审</li>
 *   <li>代码多维度审查（安全、性能、可维护性）</li>
 *   <li>方案多专家并行论证</li>
 * </ul>
 * </p>
 */
public class TeamParallelExample {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("团队并行协作框架示例 (PARALLEL 模式)");
        System.out.println("========================================\n");

        OllamaLLM llm = createLLM();

        demonstrateParallelTeam(llm);

        System.out.println("\n========================================");
        System.out.println("并行协作框架示例演示完成!");
        System.out.println("========================================");
    }

    /**
     * 演示 PARALLEL 模式：多专家并行分析同一商业计划书
     */
    private static void demonstrateParallelTeam(OllamaLLM llm) {
        System.out.println("场景: 多专家并行评审\"AI 驱动的智能招聘平台\"商业计划书");
        System.out.println();

        // 1. 创建多个独立专家成员，每人从不同维度分析
        List<TeamMember<String>> experts = new ArrayList<>();

        experts.add(DefaultTeamMember.builder()
                .agentId("market-analyst-001")
                .name("市场分析师")
                .role(TeamRole.EXECUTOR)
                .skills(List.of("市场调研", "竞品分析", "用户画像"))
                .systemPrompt("你是一位资深市场分析师，专注于评估市场规模、竞争格局和目标用户群体。" +
                        "请从市场可行性角度给出简洁专业的分析意见，控制在200字以内。")
                .llm(llm)
                .build());

        experts.add(DefaultTeamMember.builder()
                .agentId("tech-architect-002")
                .name("技术架构师")
                .role(TeamRole.EXECUTOR)
                .skills(List.of("系统架构", "AI/ML", "技术选型", "可扩展性"))
                .systemPrompt("你是一位经验丰富的技术架构师，专注于评估技术方案的可行性、架构合理性和技术风险。" +
                        "请从技术实现角度给出简洁专业的分析意见，控制在200字以内。")
                .llm(llm)
                .build());

        experts.add(DefaultTeamMember.builder()
                .agentId("financial-advisor-003")
                .name("财务顾问")
                .role(TeamRole.EXECUTOR)
                .skills(List.of("财务建模", "ROI分析", "融资规划", "成本控制"))
                .systemPrompt("你是一位专业财务顾问，专注于评估商业模式的盈利能力、资金需求和财务风险。" +
                        "请从财务可行性角度给出简洁专业的分析意见，控制在200字以内。")
                .llm(llm)
                .build());

        experts.add(DefaultTeamMember.builder()
                .agentId("legal-advisor-004")
                .name("法律顾问")
                .role(TeamRole.EXECUTOR)
                .skills(List.of("劳动法", "数据隐私", "合规审查", "知识产权"))
                .systemPrompt("你是一位专业法律顾问，专注于评估业务合规性、数据安全风险和法律潜在障碍。" +
                        "请从法律合规角度给出简洁专业的分析意见，控制在200字以内。")
                .llm(llm)
                .build());

        // 2. 创建团队框架 - 使用并行协作模式 (PARALLEL)
        // PARALLEL 模式下所有成员同时接收任务，独立执行，互不依赖
        TeamFramework<String> team = new TeamFramework<>(
                experts,
                CollaborationMode.PARALLEL
        );

        System.out.println("参与评审的专家团队:");
        for (TeamMember<String> expert : experts) {
            System.out.println(String.format("  - %s (%s) | 专长: %s",
                    expert.getMemberName(),
                    expert.getRole(),
                    expert.getSkills() != null ? String.join(", ", expert.getSkills()) : "通用"));
        }
        System.out.println("\n协作模式: 并行执行 (PARALLEL) — 所有专家同时独立分析，互不影响");
        System.out.println();

        // 3. 执行并行任务
        String businessPlan = "我们计划开发一个 AI 驱动的智能招聘平台，利用大语言模型自动筛选简历、" +
                "生成面试问题、评估候选人匹配度，并为 HR 提供决策建议。" +
                "目标客户为中大型企业，采用 SaaS 订阅模式，预计首年营收 500 万元。";

        System.out.println("评审任务:");
        System.out.println("  " + businessPlan);
        System.out.println();
        System.out.println("开始并行评审...");
        System.out.println("----------------------------------------");

        TeamResult<String> result = team.executeTeamTask(businessPlan);

        // 4. 打印各专家的独立分析结果
        System.out.println("\n========== 各专家独立评审结果 ==========");
        List<TaskExecution<String>> contributions = result.getContributions();
        for (int i = 0; i < contributions.size(); i++) {
            TaskExecution<String> contribution = contributions.get(i);
            System.out.println(String.format("\n【专家 %d】%s  |  耗时: %dms",
                    i + 1, contribution.getMemberId(), contribution.getDuration()));
            System.out.println("----------------------------------------");
            System.out.println(contribution.getResult());
            System.out.println("----------------------------------------");
        }

        // 5. 打印整体摘要
        System.out.println("\n========== 并行评审汇总 ==========");
        System.out.println("  状态:     " + (result.isSuccess() ? "✅ 成功" : "❌ 失败"));
        System.out.println("  总耗时:   " + result.getDuration() + "ms");
        System.out.println("  参与专家: " + contributions.size() + " 位");
        System.out.println("\n✅ 并行团队协作演示完成");
    }

    /**
     * 创建 LLM 实例
     */
    private static OllamaLLM createLLM() {
        OllamaLLMConfig config = new OllamaLLMConfig();
        return new OllamaLLM(config);
    }
}
