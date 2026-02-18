package io.leavesfly.evox.examples.hitl;

import io.leavesfly.evox.agents.base.Agent;
import io.leavesfly.evox.agents.manager.AgentManager;
import io.leavesfly.evox.agents.builder.AgentBuilder;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.hitl.*;
import io.leavesfly.evox.models.provider.ollama.OllamaLLMConfig;
import io.leavesfly.evox.models.provider.ollama.OllamaLLM;
import io.leavesfly.evox.workflow.base.Workflow;
import io.leavesfly.evox.workflow.builder.WorkflowBuilder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * HITL审批示例：邮件发送前的人工审批流程
 * 
 * <p>本示例演示如何使用HITL(Human-in-the-Loop)功能，在敏感操作执行前进行人工审批</p>
 * 
 * <p>工作流程：
 * <ol>
 *   <li>数据提取代理：从原始文本中提取邮件信息</li>
 *   <li>HITL拦截器：拦截并请求人工审批</li>
 *   <li>邮件发送代理：仅在批准后发送邮件</li>
 * </ol>
 * </p>
 */
@Slf4j
public class EmailSendingWithApprovalExample {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("HITL 示例：邮件发送审批流程");
        System.out.println("========================================\n");

        // 创建HITL管理器并激活
        HITLManager hitlManager = new HITLManager();
        hitlManager.activate();
        hitlManager.setDefaultTimeout(600); // 10分钟超时
        
        System.out.println("✅ HITL管理器已激活");
        System.out.println("⏱️  审批超时时间: 10分钟\n");

        // 创建LLM
        OllamaLLM llm = createLLM();

        // 创建智能体管理器
        AgentManager agentManager = new AgentManager();

        // 创建数据提取代理
        Agent extractorAgent = createExtractorAgent(llm);
        agentManager.addAgent(extractorAgent);

        // 创建HITL拦截器代理
        HITLInterceptorAgent interceptorAgent = createHITLInterceptor(hitlManager);
        agentManager.addAgent(interceptorAgent);

        // 创建邮件发送代理
        Agent emailAgent = createEmailAgent(llm);
        agentManager.addAgent(emailAgent);

        // 构建工作流
        Workflow workflow = WorkflowBuilder.sequential()
                .name("email-approval-workflow")
                .goal("审批并发送订单确认邮件")
                .step("extract_email_data", extractorAgent)
                .step("hitl_approval", interceptorAgent)
                .step("send_email", emailAgent)
                .build();

        System.out.println("📋 工作流已构建:");
        System.out.println("  1. extract_email_data (数据提取)");
        System.out.println("  2. hitl_approval (人工审批)");
        System.out.println("  3. send_email (发送邮件)\n");

        // 准备输入数据
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("order_info", "订单号: ORD20260112001\n" +
                "客户邮箱: customer@example.com\n" +
                "商品: EvoX框架企业版\n" +
                "金额: ¥9999.00\n" +
                "下单时间: 2026-01-12 15:30:00");

        System.out.println("📧 准备发送订单确认邮件...\n");
        System.out.println("订单信息:");
        System.out.println(inputs.get("order_info"));
        System.out.println();

        try {
            // 执行工作流
            System.out.println("🚀 开始执行工作流...\n");
            String result = workflow.execute(inputs);
            
            System.out.println("\n========================================");
            System.out.println("✅ 工作流执行完成!");
            System.out.println("========================================");
            System.out.println("执行结果:");
            System.out.println(result);
            
        } catch (Exception e) {
            System.out.println("\n========================================");
            System.out.println("❌ 工作流执行失败!");
            System.out.println("========================================");
            System.out.println("错误信息: " + e.getMessage());
            log.error("Workflow execution failed", e);
        } finally {
            // 关闭HITL管理器资源
            hitlManager.close();
        }

        System.out.println("\n示例演示完成!");
    }

    /**
     * 创建数据提取代理
     */
    private static Agent createExtractorAgent(OllamaLLM llm) {
        return AgentBuilder.custom(Agent.class)
                .name("extractor_agent")
                .description("从原始文本中提取邮件信息")
                .withLLM(llm)
                .withSystemPrompt("你是一个数据提取专家。从用户提供的订单信息中提取邮件发送所需的数据。" +
                        "请提取以下字段：客户邮箱、订单号、商品名称、金额。" +
                        "以JSON格式返回提取的数据。")
                .build();
    }

    /**
     * 创建HITL拦截器
     */
    private static HITLInterceptorAgent createHITLInterceptor(HITLManager hitlManager) {
        return HITLInterceptorAgent.builder()
                .name("hitl_interceptor")
                .targetAgentName("email_agent")
                .targetActionName("EmailSendingAction")
                .interactionType(HITLInteractionType.APPROVE_REJECT)
                .mode(HITLMode.PRE_EXECUTION)
                .hitlManager(hitlManager)
                .description("拦截邮件发送操作，请求人工审批")
                .build();
    }

    /**
     * 创建邮件发送代理
     */
    private static Agent createEmailAgent(OllamaLLM llm) {
        // 创建一个简单的邮件发送代理
        Agent agent = AgentBuilder.custom(Agent.class)
                .name("email_agent")
                .description("发送订单确认邮件")
                .withLLM(llm)
                .withSystemPrompt("你是一个邮件发送助手。根据提取的数据生成订单确认邮件内容。" +
                        "邮件应该专业、友好，包含所有订单详情。")
                .build();
        
        return agent;
    }

    /**
     * 创建LLM实例
     */
    private static OllamaLLM createLLM() {
        OllamaLLMConfig config = new OllamaLLMConfig();
        return new OllamaLLM(config);
    }
}
