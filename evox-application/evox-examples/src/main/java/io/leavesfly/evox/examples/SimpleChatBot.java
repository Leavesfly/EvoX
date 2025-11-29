package io.leavesfly.evox.examples;

import io.leavesfly.evox.actions.base.Action;
import io.leavesfly.evox.actions.base.ActionInput;
import io.leavesfly.evox.actions.base.ActionOutput;
import io.leavesfly.evox.actions.base.SimpleActionOutput;
import io.leavesfly.evox.agents.base.Agent;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.memory.shortterm.ShortTermMemory;
import io.leavesfly.evox.models.base.BaseLLM;
import io.leavesfly.evox.models.config.OpenAILLMConfig;
import io.leavesfly.evox.models.openai.OpenAILLM;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SimpleChatBot 示例应用
 * 
 * 展示如何使用 EvoX 框架构建一个简单的聊天机器人：
 * - 使用 Agent 管理对话
 * - 使用 Memory 保存历史消息
 * - 使用 Action 处理用户输入
 * - 集成 OpenAI LLM 进行智能回复
 */
public class SimpleChatBot {
    private static final Logger log = LoggerFactory.getLogger(SimpleChatBot.class);

    public static void main(String[] args) {
        SimpleChatBot example = new SimpleChatBot();
        
        // 检查 API Key
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            example.runWithMockMode();
        } else {
            example.runWithRealMode(apiKey);
        }
    }

    /**
     * 模拟模式运行（不需要 API Key）
     */
    private void runWithMockMode() {
        log.info("\n--- 模拟模式 ---");
        
        // 创建聊天机器人 Agent（使用模拟回复）
        ChatBotAgent agent = new ChatBotAgent(null);
        agent.setName("SimpleChatBot");
        agent.setDescription("一个简单的聊天机器人示例");
        agent.initModule();
        
        // 创建短期记忆（保存最近 10 条消息）
        ShortTermMemory memory = new ShortTermMemory(10);
        
        // 模拟对话
        String[] userInputs = {
            "你好！",
            "你叫什么名字？",
            "你能做什么？",
            "谢谢！"
        };
        
        for (String userInput : userInputs) {
            log.info("\n用户: {}", userInput);
            
            // 创建用户消息
            Message userMessage = Message.builder()
                    .content(userInput)
                    .messageType(MessageType.INPUT)
                    .build();
            
            // 添加到记忆
            memory.addMessage(userMessage);
            
            // 获取历史消息
            List<Message> history = memory.getLatestMessages(5);
            
            // 执行聊天动作
            Message response = agent.execute("chat", history);
            
            log.info("机器人: {}", response.getContent());
            
            // 保存机器人回复
            memory.addMessage(response);
        }
        
        // 显示记忆统计
        log.info("\n--- 记忆统计 ---");
        log.info("总消息数: {}", memory.size());
        log.info("记忆使用: {}/{}", memory.size(), memory.getMaxMessages());
    }

    /**
     * 真实模式运行（需要 API Key）
     */
    private void runWithRealMode(String apiKey) {
        log.info("\n--- 真实模式 ---");
        
        // 创建 OpenAI LLM 配置
        OpenAILLMConfig config = OpenAILLMConfig.builder()
                .apiKey(apiKey)
                .model("gpt-3.5-turbo")
                .temperature(0.7f)
                .maxTokens(150)
                .build();
        
        // 创建 OpenAI LLM 实例
        BaseLLM llm = new OpenAILLM(config);
        
        // 创建聊天机器人 Agent
        ChatBotAgent agent = new ChatBotAgent(llm);
        agent.setName("SimpleChatBot");
        agent.setDescription("一个基于 OpenAI 的聊天机器人");
        agent.initModule();
        
        // 创建短期记忆
        ShortTermMemory memory = new ShortTermMemory(10);
        
        // 模拟对话
        String[] userInputs = {
            "你好！请简单介绍一下你自己。",
            "你能帮我做什么？",
            "谢谢！"
        };
        
        for (String userInput : userInputs) {
            log.info("\n用户: {}", userInput);
            
            // 创建用户消息
            Message userMessage = Message.builder()
                    .content(userInput)
                    .messageType(MessageType.INPUT)
                    .build();
            
            memory.addMessage(userMessage);
            
            // 获取历史消息
            List<Message> history = memory.getLatestMessages(5);
            
            // 执行聊天动作
            Message response = agent.execute("chat", history);
            
            log.info("机器人: {}", response.getContent());
            
            memory.addMessage(response);
        }
        
        log.info("\n--- 记忆统计 ---");
        log.info("总消息数: {}", memory.size());
    }

    /**
     * 聊天机器人 Agent
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    @Slf4j
    static class ChatBotAgent extends Agent {
        private final BaseLLM llm;
        
        public ChatBotAgent(BaseLLM llm) {
            this.llm = llm;
            // 添加聊天动作
            addAction(new ChatAction(llm));
        }
        
        @Override
        public Message execute(String actionName, List<Message> messages) {
            Action action = getAction(actionName);
            if (action == null) {
                return Message.builder()
                        .content("未找到动作: " + actionName)
                        .messageType(MessageType.ERROR)
                        .build();
            }
            
            try {
                // 准备输入
                ChatActionInput input = new ChatActionInput();
                input.setMessages(messages);
                
                // 执行动作
                ActionOutput output = action.execute(input);
                
                if (output.isSuccess()) {
                    return Message.builder()
                            .content(output.getData())
                            .messageType(MessageType.RESPONSE)
                            .build();
                } else {
                    return Message.builder()
                            .content("错误: " + output.getError())
                            .messageType(MessageType.ERROR)
                            .build();
                }
            } catch (Exception e) {
                log.error("动作执行失败", e);
                return Message.builder()
                        .content("执行错误: " + e.getMessage())
                        .messageType(MessageType.ERROR)
                        .build();
            }
        }
    }

    /**
     * 聊天动作
     */
    @Slf4j
    static class ChatAction extends Action {
        private final BaseLLM llm;
        
        public ChatAction(BaseLLM llm) {
            this.llm = llm;
            setName("chat");
            setDescription("处理用户聊天消息");
        }
        
        @Override
        public ActionOutput execute(ActionInput input) {
            ChatActionInput chatInput = (ChatActionInput) input;
            List<Message> messages = chatInput.getMessages();
            
            if (messages.isEmpty()) {
                return SimpleActionOutput.failure("没有消息可处理");
            }
            
            // 如果没有 LLM，使用模拟回复
            if (llm == null) {
                return generateMockResponse(messages);
            }
            
            // 使用真实 LLM 生成回复
            return generateLLMResponse(messages);
        }
        
        /**
         * 生成模拟回复
         */
        private ActionOutput generateMockResponse(List<Message> messages) {
            Message lastMessage = messages.get(messages.size() - 1);
            String userInput = lastMessage.getContent().toString().toLowerCase();
            
            String response;
            if (userInput.contains("你好") || userInput.contains("hello")) {
                response = "你好！我是 SimpleChatBot，很高兴见到你！";
            } else if (userInput.contains("名字") || userInput.contains("name")) {
                response = "我是 SimpleChatBot，基于 EvoX 框架构建的聊天机器人。";
            } else if (userInput.contains("能做") || userInput.contains("功能")) {
                response = "我可以和你聊天，回答问题，并记住我们的对话历史。我使用 EvoX 的 Agent、Memory 和 Action 模块实现。";
            } else if (userInput.contains("谢谢") || userInput.contains("thanks")) {
                response = "不客气！很高兴能帮到你。";
            } else {
                response = "我理解了你的消息。我是一个基于 EvoX 框架的示例机器人。";
            }
            
            return SimpleActionOutput.success(response);
        }
        
        /**
         * 使用 LLM 生成回复
         */
        private ActionOutput generateLLMResponse(List<Message> messages) {
            try {
                // 构建对话上下文
                String prompt = buildPrompt(messages);
                
                // 调用 LLM
                String response = llm.generate(prompt);
                
                return SimpleActionOutput.success(response);
            } catch (Exception e) {
                log.error("LLM 调用失败", e);
                return SimpleActionOutput.failure("LLM 调用失败: " + e.getMessage());
            }
        }
        
        /**
         * 构建 LLM 提示词
         */
        private String buildPrompt(List<Message> messages) {
            StringBuilder sb = new StringBuilder();
            sb.append("你是一个友好的聊天机器人助手。请根据以下对话历史回复用户：\n\n");
            
            for (Message msg : messages) {
                if (msg.getMessageType() == MessageType.INPUT) {
                    sb.append("用户: ").append(msg.getContent()).append("\n");
                } else if (msg.getMessageType() == MessageType.RESPONSE) {
                    sb.append("助手: ").append(msg.getContent()).append("\n");
                }
            }
            
            sb.append("\n请用简洁友好的方式回复用户的最后一条消息。");
            return sb.toString();
        }
        
        @Override
        public String[] getInputFields() {
            return new String[]{"messages"};
        }
        
        @Override
        public String[] getOutputFields() {
            return new String[]{"response"};
        }
    }

    /**
     * 聊天动作输入
     */
    @Data
    static class ChatActionInput extends ActionInput {
        private List<Message> messages;
        
        @Override
        public boolean validate() {
            return messages != null && !messages.isEmpty();
        }
        
        @Override
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("messages", messages);
            return map;
        }
    }
}
