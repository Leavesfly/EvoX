package io.leavesfly.evox.examples;

import io.leavesfly.evox.agents.base.Agent;
import io.leavesfly.evox.agents.builder.AgentBuilder;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

/**
 * Builder 模式示例
 * 
 * <p>展示如何使用 AgentBuilder 快速创建 Agent</p>
 * 
 * <h3>对比:</h3>
 * <ul>
 *   <li>QuickStart: 传统方式，需要手动创建配置和实例</li>
 *   <li>BuilderExample: Builder 模式，链式调用更简洁</li>
 * </ul>
 * 
 * @author EvoX Team
 */
@Slf4j
public class BuilderExample {
    
    public static void main(String[] args) {
        // 使用 Builder 模式创建 Agent - 仅需 3 步！
        Agent agent = AgentBuilder.chatBot()
            .name("BuilderBot")
            .withOpenAI()  // 自动从环境变量读取 API Key
            .build();
        
        // 发送消息
        Message userMsg = Message.builder()
            .content("用一句话介绍 Builder 模式的优点")
            .messageType(MessageType.INPUT)
            .build();
        
        Message response = agent.execute("chat", Collections.singletonList(userMsg));
        
        // 输出结果
        log.info("用户: {}", userMsg.getContent());
        log.info("AI: {}", response.getContent());
    }
}
