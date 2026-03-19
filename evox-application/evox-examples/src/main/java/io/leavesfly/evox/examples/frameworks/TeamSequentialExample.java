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
 * 团队顺序协作框架示例 (SEQUENTIAL 模式)
 *
 * <p>演示多个 Agent 按照固定顺序依次执行，每个成员在前一个成员的输出基础上继续工作。
 * SEQUENTIAL 模式下成员严格按照列表顺序执行，后续成员可以看到前序成员的产出。
 * </p>
 *
 * <p>适用场景：具有明确流水线依赖关系的任务，如：
 * <ul>
 *   <li>文章撰写流水线（选题 → 大纲 → 正文 → 润色）</li>
 *   <li>代码开发流水线（需求分析 → 设计 → 编码 → 测试）</li>
 *   <li>内容生产流水线（创作 → 编辑 → 校对 → 发布）</li>
 * </ul>
 * </p>
 */
public class TeamSequentialExample {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("团队顺序协作框架示例 (SEQUENTIAL 模式)");
        System.out.println("========================================\n");

        OllamaLLM llm = createLLM();

        demonstrateSequentialTeam(llm);

        System.out.println("\n========================================");
        System.out.println("顺序协作框架示例演示完成!");
        System.out.println("========================================");
    }

    /**
     * 演示 SEQUENTIAL 模式：流水线式撰写一篇技术博客文章
     * 执行顺序：选题策划 → 大纲撰写 → 正文创作 → 内容润色
     */
    private static void demonstrateSequentialTeam(OllamaLLM llm) {
        System.out.println("场景: 流水线式协作撰写\"如何用 AI Agent 提升开发效率\"技术博客");
        System.out.println();

        // 1. 按执行顺序创建流水线成员
        List<TeamMember<String>> pipeline = new ArrayList<>();

        // 第一步：选题策划师 - 分析主题，确定核心角度和受众
        pipeline.add(DefaultTeamMember.builder()
                .agentId("topic-planner-001")
                .name("选题策划师")
                .role(TeamRole.COORDINATOR)
                .skills(List.of("内容策划", "受众分析", "热点洞察"))
                .systemPrompt("你是一位资深内容策划师，擅长分析技术主题的受众需求和传播价值。" +
                        "请针对给定主题，输出：1）目标读者画像 2）核心价值主张 3）差异化角度。控制在150字以内。")
                .llm(llm)
                .build());

        // 第二步：大纲架构师 - 基于策划方向，设计文章结构
        pipeline.add(DefaultTeamMember.builder()
                .agentId("outline-architect-002")
                .name("大纲架构师")
                .role(TeamRole.EXECUTOR)
                .skills(List.of("逻辑结构", "信息架构", "叙事设计"))
                .systemPrompt("你是一位擅长信息架构的技术写作专家。" +
                        "请根据前序策划方向，为文章设计清晰的大纲结构，包含引言、3-4个主要章节标题及每节要点。控制在200字以内。")
                .llm(llm)
                .build());

        // 第三步：正文创作者 - 基于大纲，撰写核心内容
        pipeline.add(DefaultTeamMember.builder()
                .agentId("content-writer-003")
                .name("正文创作者")
                .role(TeamRole.EXECUTOR)
                .skills(List.of("技术写作", "案例分析", "代码示例"))
                .systemPrompt("你是一位经验丰富的技术博客作者，文风深入浅出。" +
                        "请根据前序大纲，撰写文章的核心正文内容，重点展开最有价值的1-2个章节，配合具体示例。控制在300字以内。")
                .llm(llm)
                .build());

        // 第四步：内容润色师 - 对全文进行语言优化和质量把关
        pipeline.add(DefaultTeamMember.builder()
                .agentId("content-polisher-004")
                .name("内容润色师")
                .role(TeamRole.REVIEWER)
                .skills(List.of("文字润色", "逻辑校验", "SEO优化"))
                .systemPrompt("你是一位专业的内容编辑，擅长提升文章的可读性和传播力。" +
                        "请对前序正文进行润色，指出3个具体改进点，并给出优化后的开头段落作为示范。控制在200字以内。")
                .llm(llm)
                .build());

        // 2. 创建团队框架 - 使用顺序协作模式 (SEQUENTIAL)
        // SEQUENTIAL 模式严格按列表顺序执行，每个成员能看到前序成员的输出
        TeamFramework<String> team = new TeamFramework<>(
                pipeline,
                CollaborationMode.SEQUENTIAL
        );

        System.out.println("流水线执行顺序:");
        for (int i = 0; i < pipeline.size(); i++) {
            TeamMember<String> member = pipeline.get(i);
            System.out.println(String.format("  第 %d 步: %s (%s)",
                    i + 1, member.getMemberName(), member.getRole()));
        }
        System.out.println("\n协作模式: 顺序执行 (SEQUENTIAL) — 每步基于上一步产出继续工作");
        System.out.println();

        // 3. 执行顺序任务
        String topic = "如何用 AI Agent 提升开发效率：从自动化代码审查到智能需求分析的实践经验";

        System.out.println("写作主题:");
        System.out.println("  " + topic);
        System.out.println();
        System.out.println("开始流水线执行...");
        System.out.println("----------------------------------------");

        TeamResult<String> result = team.executeTeamTask(topic);

        // 4. 打印各步骤的执行过程与结果
        System.out.println("\n========== 流水线各步骤执行结果 ==========");
        List<TaskExecution<String>> contributions = result.getContributions();
        for (int i = 0; i < contributions.size(); i++) {
            TaskExecution<String> contribution = contributions.get(i);
            System.out.println(String.format("\n【第 %d 步】%s  |  耗时: %dms",
                    i + 1, contribution.getMemberId(), contribution.getDuration()));
            System.out.println("----------------------------------------");
            System.out.println(contribution.getResult());
            System.out.println("----------------------------------------");
        }

        // 5. 打印整体摘要
        System.out.println("\n========== 流水线执行摘要 ==========");
        System.out.println("  状态:     " + (result.isSuccess() ? "✅ 成功" : "❌ 失败"));
        System.out.println("  总耗时:   " + result.getDuration() + "ms");
        System.out.println("  流水线步数: " + contributions.size() + " 步");
        System.out.println("\n✅ 顺序团队协作演示完成");
    }

    /**
     * 创建 LLM 实例
     */
    private static OllamaLLM createLLM() {
        OllamaLLMConfig config = new OllamaLLMConfig();
        return new OllamaLLM(config);
    }
}
