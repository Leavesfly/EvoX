package io.leavesfly.evox.examples;

import io.leavesfly.evox.agents.specialized.ChatBotAgent;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.models.provider.ollama.OllamaLLMConfig;
import io.leavesfly.evox.models.provider.ollama.OllamaLLM;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

/**
 * 极简快速开始示例
 * 
 * <p>最简单的 5 分钟上手示例，展示如何快速创建一个聊天机器人</p>
 * 
 * <h3>使用步骤:</h3>
 * <ol>
 *   <li>设置环境变量: export OPENAI_API_KEY=sk-your-key</li>
 *   <li>运行本程序</li>
 *   <li>查看 AI 回复</li>
 * </ol>
 * 
 * @author EvoX Team
 */
@Slf4j
public class QuickStart {
    
    public static void main(String[] args) {
        // 第 1 步: 配置 Ollama
        OllamaLLMConfig config = new OllamaLLMConfig();
        
        // 第 2 步: 创建聊天机器人
        ChatBotAgent agent = new ChatBotAgent(new OllamaLLM(config));
        agent.setName("QuickBot");
        agent.initModule();
        
        // 第 3 步: 发送消息并获取回复
        Message userMsg = Message.builder()
            .content("你好！请用一句话介绍你自己。")
            .messageType(MessageType.INPUT)
            .build();
        
        Message response = agent.execute("chat", Collections.singletonList(userMsg));
        
        // 第 4 步: 输出结果
        log.info("用户: {}", userMsg.getContent());
        log.info("AI: {}", response.getContent());
    }
}
