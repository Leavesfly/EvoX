package io.leavesfly.evox.examples;

import io.leavesfly.evox.agents.base.Agent;
import io.leavesfly.evox.agents.builder.AgentBuilder;
import io.leavesfly.evox.core.agent.IAgentManager;
import io.leavesfly.evox.agents.manager.AgentManager;
import io.leavesfly.evox.models.provider.ollama.OllamaLLM;
import io.leavesfly.evox.models.provider.ollama.OllamaLLMConfig;
import io.leavesfly.evox.workflow.base.Workflow;
import io.leavesfly.evox.workflow.generation.WorkflowGenerator;
import io.leavesfly.evox.workflow.generation.WorkflowTemplateManager;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 多智能体工作流自动化生成演示
 * 
 * <p>本示例展示 EvoX 框架的自动化工作流生成功能：
 * <ul>
 *   <li>基于自然语言任务描述自动生成工作流</li>
 *   <li>多智能体协同执行生成的工作流</li>
 *   <li>模板驱动的工作流快速生成</li>
 * </ul>
 * </p>
 * 
 * @author EvoX Team
 */
@Slf4j
public class AutomaticWorkflowGenerationDemo {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("多智能体工作流自动化生成演示");
        System.out.println("========================================\n");

        // 创建LLM和基础组件
        OllamaLLM llm = createLLM();
        IAgentManager agentManager = createAgentManager(llm);

        // 示例1: 基于自然语言描述的自动工作流生成
        demonstrateNaturalLanguageWorkflowGeneration(llm, agentManager);

        // 示例2: 基于模板的快速工作流生成
        demonstrateTemplateBasedWorkflowGeneration(llm, agentManager);

        // 示例3: 复杂任务的自动化分解与执行
        demonstrateComplexTaskAutomation(llm, agentManager);

        System.out.println("\n========================================");
        System.out.println("所有自动化工作流生成演示完成!");
        System.out.println("========================================");
    }

    /**
     * 演示1: 基于自然语言描述的自动工作流生成
     */
    private static void demonstrateNaturalLanguageWorkflowGeneration(OllamaLLM llm, IAgentManager agentManager) {
        System.out.println("【示例 1】基于自然语言描述的自动工作流生成");
        System.out.println("----------------------------------------");
        System.out.println("场景: 用户输入自然语言任务，系统自动生成并执行工作流");
        System.out.println();

        // 1. 创建工作流生成器
        WorkflowGenerator generator = new WorkflowGenerator(llm);
        generator.setAvailableAgents(Arrays.asList(
            agentManager.getAgent("data_analyzer"),
            agentManager.getAgent("report_generator"),
            agentManager.getAgent("insight_extractor")
        ));
        
        // 2. 定义自然语言任务
        String taskDescription = "分析用户行为数据，识别关键趋势，并生成包含洞察的分析报告";

        System.out.println("输入任务描述:");
        System.out.println("  " + taskDescription);
        System.out.println();

        try {
            // 3. 自动生成工作流
            System.out.println("开始自动生成工作流...");
            Workflow workflow = generator.generateWorkflow(taskDescription);
            
            // 重要：设置AgentManager到工作流中，并重新创建executor
            workflow.setAgentManager(agentManager);
            // 重新创建executor以确保使用最新的agentManager
            workflow.setExecutor(new io.leavesfly.evox.workflow.execution.WorkflowExecutor(workflow, agentManager));
            
            System.out.println("✅ 工作流生成成功!");
            System.out.println("  工作流名称: " + workflow.getName());
            System.out.println("  工作流节点数: " + workflow.getGraph().getNodes().size());
            System.out.println();

            // 4. 执行生成的工作流
            System.out.println("开始执行自动生成的工作流...");
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("data", createSampleData());
            
            String result = workflow.execute(inputs);
            System.out.println("执行结果:");
            System.out.println(result);


            
        } catch (Exception e) {
            System.out.println("❌ 工作流生成或执行失败: " + e.getMessage());
            log.error("Workflow generation failed", e);
        }
        
        System.out.println("\n✅ 自然语言工作流生成演示完成\n");
    }

    /**
     * 演示2: 基于模板的快速工作流生成
     */
    private static void demonstrateTemplateBasedWorkflowGeneration(OllamaLLM llm, IAgentManager agentManager) {
        System.out.println("【示例 2】基于模板的快速工作流生成");
        System.out.println("----------------------------------------");
        System.out.println("场景: 使用预定义模板快速创建工作流");
        System.out.println();

        try {
            // 1. 创建工作流生成器（使用实际的LLM）
            WorkflowGenerator generator = new WorkflowGenerator(llm);
            generator.setAvailableAgents(Arrays.asList(
                agentManager.getAgent("data_processor"),
                agentManager.getAgent("validator"),
                agentManager.getAgent("summarizer")
            ));

            // 2. 创建模板管理器
            WorkflowTemplateManager templateManager = new WorkflowTemplateManager(generator);

            // 3. 列出可用模板
            System.out.println("可用的工作流模板:");
            List<String> templates = templateManager.listTemplates();
            for (int i = 0; i < templates.size(); i++) {
                String templateId = templates.get(i);
                var template = templateManager.getTemplate(templateId);
                System.out.println(String.format("  %d. %s - %s", 
                    i + 1, template.getName(), template.getDescription()));
            }
            System.out.println();

            // 4. 使用模板创建工作流
            String templateId = "data_processing";
            System.out.println("使用模板 '" + templateId + "' 创建工作流...");
            
            List<io.leavesfly.evox.core.agent.IAgent> agents = Arrays.asList(
                agentManager.getAgent("data_processor"),
                agentManager.getAgent("validator"),
                agentManager.getAgent("summarizer")
            );
            
            Workflow workflow = templateManager.createFromTemplate(templateId, agents);
            
            // 重要：设置AgentManager到工作流中，并重新创建executor
            workflow.setAgentManager(agentManager);
            // 重新创建executor以确保使用最新的agentManager
            workflow.setExecutor(new io.leavesfly.evox.workflow.execution.WorkflowExecutor(workflow, agentManager));
            
            System.out.println("✅ 基于模板的工作流创建成功!");
            System.out.println("  工作流名称: " + workflow.getName());
            System.out.println("  节点数量: " + workflow.getGraph().getNodes().size());
            System.out.println();

            // 5. 执行工作流
            System.out.println("执行基于模板的工作流...");
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("raw_data", "[{\"id\":1,\"value\":100},{\"id\":2,\"value\":200}]");
            
            String result = workflow.execute(inputs);
            System.out.println("执行结果预览:");
            System.out.println(result.substring(0, Math.min(200, result.length())) + "...");
            
        } catch (Exception e) {
            System.out.println("❌ 模板工作流创建失败: " + e.getMessage());
            log.error("Template workflow creation failed", e);
        }
        
        System.out.println("\n✅ 模板驱动工作流生成演示完成\n");
    }

    /**
     * 演示3: 复杂任务的自动化分解与执行
     */
    private static void demonstrateComplexTaskAutomation(OllamaLLM llm, IAgentManager agentManager) {
        System.out.println("【示例 3】复杂任务的自动化分解与执行");
        System.out.println("----------------------------------------");
        System.out.println("场景: 复杂业务任务的自动分解和多阶段执行");
        System.out.println();

        // 1. 定义复杂任务
        String complexTask = "开发一个智能客服系统，包括需求分析、技术选型、架构设计、开发实现、测试部署等完整流程";

        System.out.println("复杂任务描述:");
        System.out.println("  " + complexTask);
        System.out.println();

        // 2. 创建专门的项目规划智能体
        // 使用ChatBotAgent而不是抽象的Agent类
        Agent projectPlanner = AgentBuilder.chatBot()
                .name("project_planner")
                .description("项目规划专家，负责复杂任务的分解和规划")
                .withLLM(llm)
                .withSystemPrompt("你是一个资深的项目规划专家。请将复杂的业务任务分解为清晰的执行步骤，" +
                        "每个步骤都应该具体、可执行，并标明所需的输入和预期输出。")
                .build();

        try {
            // 3. 让规划智能体分析任务
            System.out.println("步骤1: 任务分析与分解...");
            var planningInputs = Arrays.asList(
                io.leavesfly.evox.core.message.Message.builder()
                    .content("请分析并分解以下任务: " + complexTask)
                    .messageType(io.leavesfly.evox.core.message.MessageType.INPUT)
                    .build()
            );
            
            var planResult = projectPlanner.execute("chat", planningInputs);
            System.out.println("任务分解结果:");
            System.out.println(planResult.getContent());
            System.out.println();

            // 4. 基于分解结果创建多阶段工作流
            System.out.println("步骤2: 创建多阶段执行工作流...");
            
            // 这里简化处理，实际应该解析planResult并创建相应的工作流
            // 演示多阶段协作模式
            System.out.println("  阶段1: 需求分析与规划");
            System.out.println("  阶段2: 技术架构设计");
            System.out.println("  阶段3: 系统开发实现");
            System.out.println("  阶段4: 测试验证");
            System.out.println("  阶段5: 部署上线");
            System.out.println();

            // 5. 模拟执行各阶段
            System.out.println("步骤3: 模拟多阶段执行...");
            
            String[] phases = {"需求分析", "架构设计", "系统开发", "测试验证", "部署上线"};
            for (int i = 0; i < phases.length; i++) {
                System.out.println(String.format("  执行阶段 %d [%s]...", i + 1, phases[i]));
                
                // 模拟每个阶段的执行
                try {
                    Thread.sleep(500); // 模拟执行时间
                    System.out.println(String.format("    ✅ 阶段 %d 完成", i + 1));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            System.out.println("\n🎉 复杂任务自动化执行完成!");
            
        } catch (Exception e) {
            System.out.println("❌ 复杂任务自动化执行失败: " + e.getMessage());
            log.error("Complex task automation failed", e);
        }
        
        System.out.println("\n✅ 复杂任务自动化演示完成\n");
    }

    /**
     * 创建LLM实例
     */
    private static OllamaLLM createLLM() {
        System.out.println("初始化 LLM 服务...");
        OllamaLLMConfig config = new OllamaLLMConfig();
        OllamaLLM llm = new OllamaLLM(config);
        System.out.println("✅ LLM 初始化完成\n");
        return llm;
    }

    /**
     * 创建智能体管理器
     */
    private static IAgentManager createAgentManager(OllamaLLM llm) {
        System.out.println("创建智能体...");
        
        IAgentManager manager = new AgentManager();
        
        // 创建不同类型的专业智能体
        Agent dataAnalyzer = AgentBuilder.chatBot()
                .name("data_analyzer")
                .description("数据分析专家")
                .withLLM(llm)
                .withSystemPrompt("你是一个数据分析专家，擅长从数据中发现模式和趋势。")
                .build();
        
        Agent reportGenerator = AgentBuilder.chatBot()
                .name("report_generator")
                .description("报告生成专家")
                .withLLM(llm)
                .withSystemPrompt("你是一个专业的报告撰写专家，能生成清晰、结构化的分析报告。")
                .build();
        
        Agent insightExtractor = AgentBuilder.chatBot()
                .name("insight_extractor")
                .description("洞察提取专家")
                .withLLM(llm)
                .withSystemPrompt("你是一个商业洞察专家，能从数据分析中提取有价值的业务洞察。")
                .build();
        
        Agent dataProcessor = AgentBuilder.chatBot()
                .name("data_processor")
                .description("数据处理专家")
                .withLLM(llm)
                .withSystemPrompt("你是一个数据处理专家，负责数据的清洗、转换和预处理。")
                .build();
        
        Agent validator = AgentBuilder.chatBot()
                .name("validator")
                .description("数据验证专家")
                .withLLM(llm)
                .withSystemPrompt("你是一个数据质量专家，负责验证数据的准确性和完整性。")
                .build();
        
        Agent summarizer = AgentBuilder.chatBot()
                .name("summarizer")
                .description("数据汇总专家")
                .withLLM(llm)
                .withSystemPrompt("你是一个数据汇总专家，能将处理后的数据进行有效汇总。")
                .build();

        // 添加到管理器
        manager.addAgent(dataAnalyzer);
        manager.addAgent(reportGenerator);
        manager.addAgent(insightExtractor);
        manager.addAgent(dataProcessor);
        manager.addAgent(validator);
        manager.addAgent(summarizer);
        
        System.out.println("✅ 创建了 " + manager.getAgentCount() + " 个专业智能体\n");
        return manager;
    }

    /**
     * 创建示例数据
     */
    private static String createSampleData() {
        return """
            [
                {"user_id": "U001", "action": "view_product", "timestamp": "2024-01-15T10:30:00"},
                {"user_id": "U002", "action": "add_to_cart", "timestamp": "2024-01-15T10:35:00"},
                {"user_id": "U001", "action": "purchase", "timestamp": "2024-01-15T10:45:00"},
                {"user_id": "U003", "action": "view_product", "timestamp": "2024-01-15T11:00:00"}
            ]
            """;
    }
}