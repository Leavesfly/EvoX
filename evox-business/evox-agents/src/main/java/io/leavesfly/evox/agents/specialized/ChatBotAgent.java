package io.leavesfly.evox.agents.specialized;

import io.leavesfly.evox.actions.base.Action;
import io.leavesfly.evox.actions.base.ActionInput;
import io.leavesfly.evox.actions.base.ActionOutput;
import io.leavesfly.evox.actions.base.SimpleActionOutput;
import io.leavesfly.evox.agents.base.Agent;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.models.base.BaseLLM;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ChatBotAgent - 简单聊天机器人 Agent
 * 
 * <p>专门用于聊天对话的 Agent，提供开箱即用的对话功能</p>
 * 
 * <h3>特性:</h3>
 * <ul>
 *   <li>内置聊天动作（chat）</li>
 *   <li>支持历史消息记录</li>
 *   <li>自动集成 LLM</li>
 *   <li>简单易用的 API</li>
 * </ul>
 * 
 * <h3>使用示例:</h3>
 * <pre>{@code
 * // 创建聊天机器人
 * OpenAILLM llm = new OpenAILLM(config);
 * ChatBotAgent agent = new ChatBotAgent(llm);
 * agent.setName("MyBot");
 * agent.initModule();
 * 
 * // 发送消息
 * Message response = agent.execute("chat", messages);
 * }</pre>
 * 
 * @author EvoX Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class ChatBotAgent extends Agent {
    
    private final BaseLLM llm;
    
    /**
     * 构造函数
     * 
     * @param llm LLM 实例，如果为 null 则使用模拟模式
     */
    public ChatBotAgent(BaseLLM llm) {
        this.llm = llm;
        // 添加聊天动作
        addAction(new ChatAction(llm));
    }
    
    @Override
    public Message execute(String actionName, List<Message> messages) {
        Action action = getAction(actionName);
        if (action == null) {
            log.error("Action not found: {}", actionName);
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
                        .content("执行失败: " + output.getError())
                        .messageType(MessageType.ERROR)
                        .build();
            }
            
        } catch (Exception e) {
            log.error("Error executing action: {}", actionName, e);
            return Message.builder()
                    .content("执行异常: " + e.getMessage())
                    .messageType(MessageType.ERROR)
                    .build();
        }
    }
    
    /**
     * 聊天动作 - 处理对话请求
     */
    @Data
    @EqualsAndHashCode(callSuper = false)
    static class ChatAction extends Action {
        private final BaseLLM llm;
        
        public ChatAction(BaseLLM llm) {
            super();
            setName("chat");
            setDescription("处理聊天对话");
            this.llm = llm;
        }
        
        @Override
        public String[] getInputFields() {
            return new String[]{"messages"};
        }
        
        @Override
        public String[] getOutputFields() {
            return new String[]{"response"};
        }
        
        @Override
        public ActionOutput execute(ActionInput input) {
            try {
                ChatActionInput chatInput = (ChatActionInput) input;
                List<Message> messages = chatInput.getMessages();
                
                // 如果没有 LLM，使用模拟模式
                if (llm == null) {
                    return mockResponse(messages);
                }
                
                // 使用 LLM 生成回复
                return generateResponse(messages);
                
            } catch (Exception e) {
                log.error("Chat action execution failed", e);
                return new SimpleActionOutput(false, "执行失败: " + e.getMessage(), null);
            }
        }
        
        /**
         * 模拟模式回复
         */
        private ActionOutput mockResponse(List<Message> messages) {
            String lastUserMessage = "";
            if (!messages.isEmpty()) {
                Object content = messages.get(messages.size() - 1).getContent();
                lastUserMessage = content != null ? content.toString() : "";
            }
            
            String response;
            if (lastUserMessage.contains("你好") || lastUserMessage.contains("hello")) {
                response = "你好！我是一个简单的聊天机器人。";
            } else if (lastUserMessage.contains("名字")) {
                response = "我叫 ChatBot，很高兴认识你！";
            } else if (lastUserMessage.contains("做什么") || lastUserMessage.contains("功能")) {
                response = "我可以和你聊天，回答问题。目前运行在模拟模式下。";
            } else {
                response = "我收到了你的消息：" + lastUserMessage;
            }
            
            return new SimpleActionOutput(true, "成功", response);
        }
        
        /**
         * 使用 LLM 生成回复
         */
        private ActionOutput generateResponse(List<Message> messages) {
            try {
                // 直接使用 Message 列表调用 LLM
                String response = llm.chat(messages);
                
                return new SimpleActionOutput(true, "成功", response);
                
            } catch (Exception e) {
                log.error("LLM generation failed", e);
                return new SimpleActionOutput(false, "LLM 调用失败: " + e.getMessage(), null);
            }
        }
    }
    
    /**
     * 聊天动作输入
     */
    @Data
    @EqualsAndHashCode(callSuper = false)
    static class ChatActionInput extends ActionInput {
        private List<Message> messages;
    }
}
