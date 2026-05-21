package io.leavesfly.evox.examples;

import io.leavesfly.evox.agents.base.Agent;
import io.leavesfly.evox.agents.manager.AgentManager;
import io.leavesfly.evox.core.agent.IAgent;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.memory.shortterm.ShortTermMemory;
import io.leavesfly.evox.tools.base.Toolkit;
import io.leavesfly.evox.tools.file.FileSystemTool;
import io.leavesfly.evox.tools.http.HttpTool;
import io.leavesfly.evox.tools.search.WebSearchTool;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 综合聊天机器人示例
 * 展示多Agent协作、工具使用和记忆管理
 */
@Slf4j
public class ComprehensiveChatBot {

    public static void main(String[] args) {
        try {
            log.info("=== ComprehensiveChatBot 示例开始 ===\n");
            
            // 1. 创建 AgentManager
            AgentManager agentManager = new AgentManager();
            log.info("✓ AgentManager 已创建");
            
            // 2. 创建工具集
            Toolkit toolkit = createToolkit();
            log.info("✓ Toolkit 已创建 ({} 个工具)", toolkit.getTools().size());
            
            // 3. 创建 Agents
            Agent routerAgent = createRouterAgent();
            Agent toolAgent = createToolAgent(toolkit);
            Agent chatAgent = createChatAgent();
            
            agentManager.addAgent(routerAgent);
            agentManager.addAgent(toolAgent);
            agentManager.addAgent(chatAgent);
            
            log.info("✓ Agents 已创建和注册");
            log.info("");
            
            // 4. 创建 Memory
            ShortTermMemory memory = new ShortTermMemory(20);
            log.info("✓ Memory 已创建（容量: {}）\n", memory.getMaxMessages());
            
            // 5. 运行对话演示
            runConversation(agentManager, memory);
            
            log.info("\n=== ComprehensiveChatBot 示例完成 ===");
            
        } catch (Exception e) {
            log.error("❌ 示例运行失败", e);
        }
    }

    /**
     * 创建工具集
     */
    private static Toolkit createToolkit() {
        Toolkit toolkit = new Toolkit();
        
        // 添加文件系统工具
        toolkit.addTool(new FileSystemTool());
        
        // 添加 HTTP 工具
        toolkit.addTool(new HttpTool());
        
        // 添加网络搜索工具
        toolkit.addTool(new WebSearchTool());
        
        return toolkit;
    }

    /**
     * 创建路由 Agent（决定使用哪个 Agent 处理）
     */
    private static Agent createRouterAgent() {
        RouterAgent agent = new RouterAgent();
        agent.setName("RouterAgent");
        agent.setDescription("分析用户输入并路由到合适的处理Agent");
        agent.initModule();
        return agent;
    }

    /**
     * 创建工具 Agent（处理需要工具的请求）
     */
    private static Agent createToolAgent(Toolkit toolkit) {
        ToolAgent agent = new ToolAgent(toolkit);
        agent.setName("ToolAgent");
        agent.setDescription("使用工具处理用户请求");
        agent.initModule();
        return agent;
    }

    /**
     * 创建聊天 Agent（处理普通对话）
     */
    private static Agent createChatAgent() {
        ChatAgentImpl agent = new ChatAgentImpl();
        agent.setName("ChatAgent");
        agent.setDescription("处理普通聊天对话");
        agent.initModule();
        return agent;
    }

    /**
     * 运行对话演示
     */
    private static void runConversation(AgentManager agentManager, ShortTermMemory memory) {
        log.info("🚀 开始对话演示...\n");
        log.info("=" .repeat(80));
        
        String[] userInputs = {
            "你好！",
            "搜索 Java 最新版本",
            "读取文件 /tmp/test.txt",
            "今天天气怎么样？",
            "谢谢！"
        };
        
        for (int i = 0; i < userInputs.length; i++) {
            String userInput = userInputs[i];
            log.info("\n【轮次 {}】", i + 1);
            log.info("👤 用户: {}", userInput);
            
            // 创建用户消息
            Message userMessage = Message.builder()
                    .content(userInput)
                    .messageType(MessageType.INPUT)
                    .build();
            memory.addMessage(userMessage);
            
            // Step 1: 路由Agent分析
            IAgent routerAgent = agentManager.getAgent("RouterAgent");
            Message routeResult = routerAgent.execute(Collections.singletonList(userMessage));
            String selectedAgent = extractSelectedAgent(routeResult.getContent().toString());
            
            log.info("🔀 路由结果: 选择 {} 处理", selectedAgent);
            
            // Step 2: 执行选定的Agent
            IAgent selectedAgentInstance = agentManager.getAgent(selectedAgent);
            List<Message> context = memory.getLatestMessages(5);
            Message response = selectedAgentInstance.execute(context);
            
            log.info("🤖 {}: {}", selectedAgent, response.getContent());
            
            // 保存回复到记忆
            memory.addMessage(response);
            
            log.info("-" .repeat(80));
        }
        
        // 显示记忆统计
        log.info("\n📊 记忆统计:");
        log.info("  - 总消息数: {}", memory.size());
        log.info("  - 记忆容量: {}/{}", memory.size(), memory.getMaxMessages());
    }

    /**
     * 从路由结果提取选定的Agent名称
     */
    private static String extractSelectedAgent(String routeResult) {
        if (routeResult.contains("ToolAgent")) {
            return "ToolAgent";
        } else {
            return "ChatAgent";
        }
    }

    // ========== Custom Agents ==========

    /**
     * 路由 Agent - 决定使用哪个Agent
     */
    static class RouterAgent extends Agent {
        public RouterAgent() {
            super();
        }

        @Override
        public Message execute(List<Message> messages) {
            if (messages == null || messages.isEmpty()) {
                return Message.builder()
                        .messageType(MessageType.ERROR)
                        .content("没有消息可路由")
                        .build();
            }
            
            String userInput = messages.get(messages.size() - 1).getContent().toString().toLowerCase();
            
            // 判断是否需要工具
            boolean needTool = userInput.contains("搜索") || 
                             userInput.contains("读取") || 
                             userInput.contains("文件") ||
                             userInput.contains("天气");
            
            String selectedAgent = needTool ? "ToolAgent" : "ChatAgent";
            return Message.builder()
                    .messageType(MessageType.RESPONSE)
                    .content("选择: " + selectedAgent)
                    .build();
        }
    }

    /**
     * 工具 Agent - 使用工具处理请求
     */
    static class ToolAgent extends Agent {
        private final Toolkit toolkit;

        public ToolAgent(Toolkit toolkit) {
            super();
            this.toolkit = toolkit;
        }

        @Override
        public Message execute(List<Message> messages) {
            if (messages == null || messages.isEmpty()) {
                return Message.builder()
                        .messageType(MessageType.ERROR)
                        .content("没有消息可处理")
                        .build();
            }
            
            String userInput = messages.get(messages.size() - 1).getContent().toString();
            
            // 简单模拟工具调用
            String response = "工具处理结果: " + userInput;
            return Message.builder()
                    .messageType(MessageType.RESPONSE)
                    .content(response)
                    .build();
        }
    }

    /**
     * 聊天 Agent - 处理普通对话
     */
    static class ChatAgentImpl extends Agent {
        public ChatAgentImpl() {
            super();
        }

        @Override
        public Message execute(List<Message> messages) {
            if (messages == null || messages.isEmpty()) {
                return Message.builder()
                        .messageType(MessageType.ERROR)
                        .content("没有消息可处理")
                        .build();
            }
            
            String userInput = messages.get(messages.size() - 1).getContent().toString();
            
            // 简单模拟对话回复
            String response = "聊天回复: " + userInput;
            return Message.builder()
                    .messageType(MessageType.RESPONSE)
                    .content(response)
                    .build();
        }
    }
}