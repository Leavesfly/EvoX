package io.leavesfly.evox.examples;

import io.leavesfly.evox.agents.customize.CustomizeAgent;
import io.leavesfly.evox.agents.plan.PlanAgent;
import io.leavesfly.evox.agents.react.ReActAgent;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.models.provider.ollama.OllamaLLMConfig;
import io.leavesfly.evox.models.provider.ollama.OllamaLLM;
import io.leavesfly.evox.tools.base.BaseTool;
import io.leavesfly.evox.tools.base.BaseTool.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 专用智能体示例
 *
 * @author EvoX Team
 */
public class SpecializedAgentsExample {
    private static final Logger log = LoggerFactory.getLogger(SpecializedAgentsExample.class);

    public static void main(String[] args) {
        SpecializedAgentsExample example = new SpecializedAgentsExample();

        // 示例1: CustomizeAgent - 自定义智能体
        example.demonstrateCustomizeAgent();

        // 示例2: PlanAgent - 任务规划智能体
        example.demonstratePlanAgent();

        // 示例3: ReActAgent - 推理行动智能体
        example.demonstrateReActAgent();
    }

    /**
     * 示例1: CustomizeAgent - 通过配置自定义智能体
     */
    private void demonstrateCustomizeAgent() {
        log.info("\n--- CustomizeAgent 示例 ---");
        log.info("注意: 需要有效的 OpenAI API Key 才能运行");

        // 创建文本摘要智能体
        CustomizeAgent summarizer = new CustomizeAgent();
        summarizer.setName("TextSummarizer");
        summarizer.setDescription("提取文本摘要");
        summarizer.setPromptTemplate("""
                请为以下文本生成一个简洁的摘要：
                
                {text}
                
                摘要应该:
                1. 不超过3句话
                2. 保留关键信息
                3. 语言简洁清晰
                """);

        // 设置输入输出规格
        summarizer.getInputs().add(
                new CustomizeAgent.InputSpec("text", "string", "需要摘要的文本")
        );
        summarizer.getOutputs().add(
                new CustomizeAgent.OutputSpec("summary", "string", "生成的摘要")
        );

        // 设置解析模式
        summarizer.setParseMode(CustomizeAgent.ParseMode.STRING);

        log.info("CustomizeAgent 配置完成: {}", summarizer.getName());
        log.info("输入字段: {}", summarizer.getInputs());
        log.info("输出字段: {}", summarizer.getOutputs());
    }

    /**
     * 示例2: PlanAgent - 任务规划和分解
     */
    private void demonstratePlanAgent() {
        log.info("\n--- PlanAgent 示例 ---");
        log.info("注意: 需要有效的 OpenAI API Key 才能运行");

        // 创建任务规划智能体
        PlanAgent planner = new PlanAgent();
        planner.setName("TaskPlanner");
        planner.setDescription("将复杂目标分解为子任务");
        planner.setPlanningPrompt("""
                请将以下目标分解为详细的执行步骤：
                
                目标: {goal}
                
                请按以下格式输出：
                Task 1: [第一步描述]
                Task 2: [第二步描述]
                Task 3: [第三步描述]
                ...
                """);

        log.info("PlanAgent 配置完成: {}", planner.getName());
        log.info("规划提示模板已设置");
    }

    /**
     * 示例3: ReActAgent - 推理 + 行动模式
     */
    private void demonstrateReActAgent() {
        log.info("\n--- ReActAgent 示例 ---");
        log.info("注意: 需要有效的 OpenAI API Key 才能运行");

        // 创建 ReAct 智能体
        ReActAgent reactAgent = new ReActAgent();
        reactAgent.setName("ReActSolver");
        reactAgent.setDescription("通过推理和行动解决问题");
        reactAgent.setMaxIterations(5);

        // 添加搜索工具
        BaseTool searchTool = new BaseTool() {
            @Override
            public String getName() {
                return "search";
            }

            @Override
            public String getDescription() {
                return "搜索信息";
            }

            @Override
            public ToolResult execute(Map<String, Object> parameters) {
                String query = parameters.get("input").toString();
                // 模拟搜索结果
                String result = "关于 '" + query + "' 的搜索结果: [模拟数据]";
                return ToolResult.success(result);
            }
        };

        // 添加计算工具
        BaseTool calcTool = new BaseTool() {
            @Override
            public String getName() {
                return "calculate";
            }

            @Override
            public String getDescription() {
                return "执行数学计算";
            }

            @Override
            public ToolResult execute(Map<String, Object> parameters) {
                String expression = parameters.get("input").toString();
                // 模拟计算
                String result = "计算 '" + expression + "' 的结果: [模拟结果]";
                return ToolResult.success(result);
            }
        };

        reactAgent.getTools().add(searchTool);
        reactAgent.getTools().add(calcTool);

        log.info("ReActAgent 配置完成: {}", reactAgent.getName());
        log.info("可用工具数量: {}", reactAgent.getTools().size());
        log.info("最大迭代次数: {}", reactAgent.getMaxIterations());
    }

    /**
     * 创建 Ollama LLM 配置（示例）
     */
    private static OllamaLLM createOllamaLLM() {
        OllamaLLMConfig config = new OllamaLLMConfig();
        return new OllamaLLM(config);
    }
}
