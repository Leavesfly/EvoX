package io.leavesfly.evox.examples;

import io.leavesfly.evox.agents.action.ActionAgent;
import io.leavesfly.evox.agents.specialized.ToolAwareAgent;
import io.leavesfly.evox.tools.agent.AgentTool;
import io.leavesfly.evox.tools.api.ToolRegistry;
import io.leavesfly.evox.tools.base.BaseTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Subagent as Tool 示例
 * 演示如何将智能体（Agent）包装为工具（Tool），供其他智能体调用
 *
 * <p>核心思想：让一个主智能体通过 Tool 接口调用多个专业子智能体，
 * 实现"智能体即工具"的编排模式</p>
 *
 * @author EvoX Team
 */
public class SubagentAsToolExample {
    private static final Logger log = LoggerFactory.getLogger(SubagentAsToolExample.class);

    public static void main(String[] args) {
        SubagentAsToolExample example = new SubagentAsToolExample();

        // 示例1: 基础用法 - 将Agent包装为Tool并直接调用
        example.basicAgentAsTool();

        // 示例2: 多子智能体编排 - 翻译 + 摘要 + 情感分析
        example.multiSubagentOrchestration();

        // 示例3: ToolAwareAgent 集成 - 使用便捷方法注册子Agent
        example.toolAwareAgentIntegration();

        // 示例4: ToolRegistry 集成 - 通过注册表管理Agent工具
        example.toolRegistryIntegration();

        // 示例5: Builder高级配置 - 超时、自定义名称等
        example.advancedBuilderConfiguration();
    }

    // ===================================================================
    // 示例1: 基础用法
    // ===================================================================

    /**
     * 最简单的用法：将一个 Agent 包装为 Tool 并直接调用
     */
    private void basicAgentAsTool() {
        log.info("\n=== 示例1: 基础用法 - 将Agent包装为Tool ===\n");

        // 1. 创建一个专业的"计算器"智能体
        ActionAgent calculatorAgent = createCalculatorAgent();
        log.info("创建计算器智能体: {}", calculatorAgent.getName());

        // 2. 一行代码将其包装为Tool
        AgentTool calculatorTool = AgentTool.wrap(calculatorAgent);
        log.info("包装为工具: name={}, description={}", 
                calculatorTool.getName(), calculatorTool.getDescription());

        // 3. 查看自动生成的Tool Schema（用于LLM function calling）
        Map<String, Object> schema = calculatorTool.getToolSchema();
        log.info("自动生成的Tool Schema: {}", schema);

        // 4. 像普通工具一样调用
        Map<String, Object> params = new HashMap<>();
        params.put("query", "请计算 (10 + 5) * 3");
        params.put("context", "用户需要精确的数学计算结果");

        BaseTool.ToolResult result = calculatorTool.execute(params);
        log.info("调用结果 - 成功: {}, 数据: {}", result.isSuccess(), result.getData());
        if (result.getMetadata() != null) {
            log.info("元数据: {}", result.getMetadata());
        }
    }

    // ===================================================================
    // 示例2: 多子智能体编排
    // ===================================================================

    /**
     * 将多个专业子智能体包装为工具，演示编排场景
     */
    private void multiSubagentOrchestration() {
        log.info("\n=== 示例2: 多子智能体编排 ===\n");

        // 创建三个专业子智能体
        ActionAgent translatorAgent = createTranslatorAgent();
        ActionAgent summarizerAgent = createSummarizerAgent();
        ActionAgent sentimentAgent = createSentimentAgent();

        // 分别包装为工具
        AgentTool translatorTool = AgentTool.wrap(translatorAgent, "translator", "将文本翻译为目标语言");
        AgentTool summarizerTool = AgentTool.wrap(summarizerAgent, "summarizer", "生成文本摘要");
        AgentTool sentimentTool = AgentTool.wrap(sentimentAgent, "sentiment_analyzer", "分析文本情感倾向");

        log.info("已创建3个AgentTool:");
        log.info("  - {} : {}", translatorTool.getName(), translatorTool.getDescription());
        log.info("  - {} : {}", summarizerTool.getName(), summarizerTool.getDescription());
        log.info("  - {} : {}", sentimentTool.getName(), sentimentTool.getDescription());

        // 模拟编排：对同一段文本依次进行摘要、情感分析、翻译
        String originalText = "EvoX is an amazing enterprise AI agent framework. "
                + "It provides powerful multi-agent collaboration features and a flexible workflow engine. "
                + "Developers love using it for building production-ready AI applications.";

        log.info("\n原始文本: {}", originalText);

        // 步骤1: 摘要
        Map<String, Object> summarizeParams = new HashMap<>();
        summarizeParams.put("query", originalText);
        BaseTool.ToolResult summaryResult = summarizerTool.execute(summarizeParams);
        log.info("\n[步骤1] 摘要结果: {}", summaryResult.getData());

        // 步骤2: 情感分析
        Map<String, Object> sentimentParams = new HashMap<>();
        sentimentParams.put("query", originalText);
        BaseTool.ToolResult sentimentResult = sentimentTool.execute(sentimentParams);
        log.info("[步骤2] 情感分析: {}", sentimentResult.getData());

        // 步骤3: 翻译
        Map<String, Object> translateParams = new HashMap<>();
        translateParams.put("query", originalText);
        translateParams.put("context", "请翻译为中文");
        BaseTool.ToolResult translateResult = translatorTool.execute(translateParams);
        log.info("[步骤3] 翻译结果: {}", translateResult.getData());
    }

    // ===================================================================
    // 示例3: ToolAwareAgent 集成
    // ===================================================================

    /**
     * 使用 ToolAwareAgent 的便捷方法将子Agent注册为工具
     */
    private void toolAwareAgentIntegration() {
        log.info("\n=== 示例3: ToolAwareAgent 集成 ===\n");

        // 创建子智能体
        ActionAgent translatorAgent = createTranslatorAgent();
        ActionAgent calculatorAgent = createCalculatorAgent();

        // 创建主智能体（ToolAwareAgent）
        ToolAwareAgent orchestrator = ToolAwareAgent.builder()
                .name("Orchestrator")
                .description("主编排智能体，通过调用子智能体完成复杂任务")
                .autoExecuteTools(true)
                .maxToolCalls(10)
                .build();

        // 方式1: 最简单 - 一行代码添加
        orchestrator.addAgentAsTool(translatorAgent);
        log.info("方式1 - addAgentAsTool(agent): 已添加翻译子智能体");

        // 方式2: 自定义名称和描述
        orchestrator.addAgentAsTool(calculatorAgent, "math_expert", "数学计算专家，可以解决各类数学问题");
        log.info("方式2 - addAgentAsTool(agent, name, desc): 已添加计算子智能体");

        // 方式3: 使用Builder进行高级配置
        ActionAgent sentimentAgent = createSentimentAgent();
        AgentTool sentimentTool = orchestrator.agentAsToolBuilder(sentimentAgent)
                .toolName("emotion_detector")
                .toolDescription("检测文本中的情感和情绪")
                .includeMetadata(true)
                .timeoutMs(5000)
                .build();
        orchestrator.addTool(sentimentTool);
        log.info("方式3 - agentAsToolBuilder(): 已添加情感分析子智能体（带超时配置）");

        // 查看主智能体的所有可用工具
        log.info("\n主智能体 '{}' 的可用工具列表:", orchestrator.getName());
        for (BaseTool tool : orchestrator.getTools()) {
            log.info("  - {} : {} [类型: {}]",
                    tool.getName(), tool.getDescription(),
                    tool instanceof AgentTool ? "AgentTool(子智能体)" : "普通工具");
        }

        // 直接调用其中一个子智能体工具
        log.info("\n直接通过主智能体调用翻译工具:");
        BaseTool translatorTool = orchestrator.getToolMap().get("agent_translator");
        if (translatorTool != null) {
            Map<String, Object> params = new HashMap<>();
            params.put("query", "Hello, World!");
            params.put("context", "请翻译为中文");
            BaseTool.ToolResult result = translatorTool.execute(params);
            log.info("翻译结果: {}", result.getData());
        }
    }

    // ===================================================================
    // 示例4: ToolRegistry 集成
    // ===================================================================

    /**
     * 通过 ToolRegistry 集中管理 Agent 工具
     */
    private void toolRegistryIntegration() {
        log.info("\n=== 示例4: ToolRegistry 集成 ===\n");

        ToolRegistry registry = ToolRegistry.getInstance();

        // 创建多个专业智能体
        ActionAgent translatorAgent = createTranslatorAgent();
        ActionAgent summarizerAgent = createSummarizerAgent();
        ActionAgent calculatorAgent = createCalculatorAgent();

        // 方式1: 快速注册（自动命名）
        AgentTool tool1 = registry.registerAgent(translatorAgent);
        log.info("注册智能体工具: {} -> 分类: agent", tool1.getName());

        // 方式2: 自定义名称注册
        AgentTool tool2 = registry.registerAgent(summarizerAgent, "text_summarizer", "智能文本摘要工具");
        log.info("注册智能体工具: {} -> 分类: agent", tool2.getName());

        AgentTool tool3 = registry.registerAgent(calculatorAgent, "smart_calculator", "智能数学计算工具");
        log.info("注册智能体工具: {} -> 分类: agent", tool3.getName());

        // 查看注册表状态
        log.info("\n注册表统计:");
        log.info("  总工具数: {}", registry.size());
        log.info("  智能体工具: {}", registry.getToolsByCategory("agent").size());
        log.info("  所有工具名: {}", registry.getAllToolNames());

        // 从注册表获取并调用
        BaseTool fetchedTool = registry.get("text_summarizer");
        if (fetchedTool != null) {
            log.info("\n从注册表获取工具 'text_summarizer' 并调用:");
            Map<String, Object> params = new HashMap<>();
            params.put("query", "Artificial intelligence is transforming every industry...");
            BaseTool.ToolResult result = fetchedTool.execute(params);
            log.info("结果: {}", result.getData());
        }

        // 生成工具文档
        log.info("\n生成工具文档:");
        String doc = registry.generateMarkdownDoc();
        log.info("\n{}", doc);
    }

    // ===================================================================
    // 示例5: Builder 高级配置
    // ===================================================================

    /**
     * 使用 Builder 进行高级配置
     */
    private void advancedBuilderConfiguration() {
        log.info("\n=== 示例5: Builder 高级配置 ===\n");

        ActionAgent codeReviewAgent = createCodeReviewAgent();

        // 使用Builder进行完整配置
        AgentTool codeReviewTool = AgentTool.builder(codeReviewAgent)
                .toolName("code_reviewer")                  // 自定义工具名
                .toolDescription("专业代码审查工具，可以分析代码质量、发现潜在问题并提供改进建议")  // 自定义描述
                .defaultActionName("function_action")       // 默认执行的动作
                .includeMetadata(true)                      // 在结果中包含Agent元数据
                .timeoutMs(10000)                           // 10秒超时
                .build();

        log.info("高级配置的AgentTool:");
        log.info("  工具名: {}", codeReviewTool.getName());
        log.info("  描述: {}", codeReviewTool.getDescription());
        log.info("  被包装Agent: {}", codeReviewTool.getWrappedAgent().getName());

        // 查看完整Schema
        Map<String, Object> schema = codeReviewTool.getToolSchema();
        log.info("  Tool Schema: {}", schema);

        // 执行代码审查
        Map<String, Object> params = new HashMap<>();
        params.put("query", "public void process(String data) { System.out.println(data); }");
        params.put("context", "请审查这段Java代码的质量");

        log.info("\n执行代码审查...");
        BaseTool.ToolResult result = codeReviewTool.execute(params);
        log.info("审查结果 - 成功: {}", result.isSuccess());
        log.info("审查内容: {}", result.getData());
        log.info("元数据: {}", result.getMetadata());
    }

    // ===================================================================
    // 辅助方法 - 创建模拟智能体
    // ===================================================================

    /**
     * 创建计算器智能体（模拟，不需要LLM）
     */
    private ActionAgent createCalculatorAgent() {
        ActionAgent agent = new ActionAgent();
        agent.setName("Calculator");
        agent.setDescription("执行数学计算，支持基本四则运算和复杂表达式");

        agent.setExecuteFunction(inputs -> {
            String query = extractQuery(inputs);
            Map<String, Object> result = new HashMap<>();
            // 模拟计算结果
            result.put("expression", query);
            result.put("result", 45.0);
            result.put("explanation", "计算表达式: " + query + " = 45.0");
            return result;
        });

        agent.setInputs(List.of(
                new ActionAgent.FieldSpec("query", "string", "数学表达式或计算问题")
        ));
        agent.setOutputs(List.of(
                new ActionAgent.FieldSpec("result", "number", "计算结果"),
                new ActionAgent.FieldSpec("explanation", "string", "计算说明")
        ));

        agent.initModule();
        return agent;
    }

    /**
     * 创建翻译智能体（模拟，不需要LLM）
     */
    private ActionAgent createTranslatorAgent() {
        ActionAgent agent = new ActionAgent();
        agent.setName("Translator");
        agent.setDescription("多语言翻译助手，支持中英日韩等多种语言互译");

        agent.setExecuteFunction(inputs -> {
            String query = extractQuery(inputs);
            Map<String, Object> result = new HashMap<>();
            // 模拟翻译结果
            result.put("original", query);
            result.put("translated", "[模拟翻译] " + query + " 的翻译结果");
            result.put("source_lang", "en");
            result.put("target_lang", "zh");
            return result;
        });

        agent.setInputs(List.of(
                new ActionAgent.FieldSpec("query", "string", "待翻译文本")
        ));
        agent.setOutputs(List.of(
                new ActionAgent.FieldSpec("translated", "string", "翻译结果")
        ));

        agent.initModule();
        return agent;
    }

    /**
     * 创建摘要智能体（模拟，不需要LLM）
     */
    private ActionAgent createSummarizerAgent() {
        ActionAgent agent = new ActionAgent();
        agent.setName("Summarizer");
        agent.setDescription("文本摘要生成器，能够提取文本核心内容生成简洁摘要");

        agent.setExecuteFunction(inputs -> {
            String query = extractQuery(inputs);
            Map<String, Object> result = new HashMap<>();
            // 模拟摘要结果
            String summary = query.length() > 50 ? query.substring(0, 50) + "..." : query;
            result.put("original_length", query.length());
            result.put("summary", "[摘要] " + summary);
            result.put("compression_ratio", "60%");
            return result;
        });

        agent.setInputs(List.of(
                new ActionAgent.FieldSpec("query", "string", "待摘要文本")
        ));
        agent.setOutputs(List.of(
                new ActionAgent.FieldSpec("summary", "string", "生成的摘要")
        ));

        agent.initModule();
        return agent;
    }

    /**
     * 创建情感分析智能体（模拟，不需要LLM）
     */
    private ActionAgent createSentimentAgent() {
        ActionAgent agent = new ActionAgent();
        agent.setName("SentimentAnalyzer");
        agent.setDescription("文本情感分析器，判断文本的情感倾向（正面/负面/中性）");

        agent.setExecuteFunction(inputs -> {
            String query = extractQuery(inputs);
            Map<String, Object> result = new HashMap<>();
            // 模拟情感分析
            result.put("text", query);
            result.put("sentiment", "positive");
            result.put("confidence", 0.92);
            result.put("details", Map.of(
                    "positive", 0.92,
                    "neutral", 0.06,
                    "negative", 0.02
            ));
            return result;
        });

        agent.setInputs(List.of(
                new ActionAgent.FieldSpec("query", "string", "待分析文本")
        ));
        agent.setOutputs(List.of(
                new ActionAgent.FieldSpec("sentiment", "string", "情感倾向"),
                new ActionAgent.FieldSpec("confidence", "number", "置信度")
        ));

        agent.initModule();
        return agent;
    }

    /**
     * 创建代码审查智能体（模拟，不需要LLM）
     */
    private ActionAgent createCodeReviewAgent() {
        ActionAgent agent = new ActionAgent();
        agent.setName("CodeReviewer");
        agent.setDescription("代码审查专家，分析代码质量并提供改进建议");

        agent.setExecuteFunction(inputs -> {
            String query = extractQuery(inputs);
            Map<String, Object> result = new HashMap<>();
            result.put("code", query);
            result.put("quality_score", 7.5);
            result.put("issues", List.of(
                    Map.of("severity", "warning", "message", "建议添加输入参数校验"),
                    Map.of("severity", "info", "message", "建议使用日志框架替代 System.out.println")
            ));
            result.put("suggestions", List.of(
                    "添加 null 检查",
                    "使用 SLF4J Logger 替代 System.out",
                    "考虑添加单元测试"
            ));
            return result;
        });

        agent.setInputs(List.of(
                new ActionAgent.FieldSpec("query", "string", "待审查代码")
        ));
        agent.setOutputs(List.of(
                new ActionAgent.FieldSpec("quality_score", "number", "质量评分(1-10)"),
                new ActionAgent.FieldSpec("issues", "array", "发现的问题"),
                new ActionAgent.FieldSpec("suggestions", "array", "改进建议")
        ));

        agent.initModule();
        return agent;
    }

    /**
     * 从Agent的输入消息中提取query内容
     */
    @SuppressWarnings("unchecked")
    private static String extractQuery(Map<String, Object> inputs) {
        // ActionAgent的inputs来自Message.content，可能是Map或String
        Object content = inputs;
        if (content instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) content;
            // 尝试从多个可能的key中提取
            if (map.containsKey("query")) {
                return String.valueOf(map.get("query"));
            }
            if (map.containsKey("content")) {
                return String.valueOf(map.get("content"));
            }
            return map.toString();
        }
        return String.valueOf(content);
    }
}
