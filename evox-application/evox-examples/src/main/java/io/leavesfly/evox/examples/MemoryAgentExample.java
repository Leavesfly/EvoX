package io.leavesfly.evox.examples;

import io.leavesfly.evox.agents.specialized.ChatBotAgent;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.memory.longterm.InMemoryLongTermMemory;
import io.leavesfly.evox.memory.manager.MemoryManager;
import io.leavesfly.evox.memory.shortterm.ShortTermMemory;
import io.leavesfly.evox.models.provider.ollama.OllamaLLMConfig;
import io.leavesfly.evox.models.provider.ollama.OllamaLLM;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

/**
 * 记忆智能体示例
 * 展示如何创建具有记忆能力的智能体
 *
 * @author EvoX Team
 */
@Slf4j
public class MemoryAgentExample {

    public static void main(String[] args) {
        log.info("=== EvoX 记忆智能体集成示例 ===");

        // 1. 配置 LLM
        OllamaLLMConfig config = new OllamaLLMConfig();
        OllamaLLM llm = new OllamaLLM(config);

        // 2. 配置记忆系统
        // 短期记忆：最近 5 条
        ShortTermMemory stm = new ShortTermMemory(5);
        // 长期记忆：内存实现（带去重）
        InMemoryLongTermMemory ltm = new InMemoryLongTermMemory();
        
        // 记忆管理器：统一管理
        MemoryManager memoryManager = new MemoryManager(stm, null); // 简化：仅使用 stm 进行演示，或配置 ltm
        memoryManager.initModule();

        // 3. 创建智能体
        ChatBotAgent agent = new ChatBotAgent(llm);
        agent.setName("MemoryBot");
        agent.initModule();

        // 4. 模拟多轮对话
        log.info("--- 开始对话 ---");
        
        String[] questions = {
            "你好，我叫小明。",
            "我喜欢喝绿茶。",
            "你还记得我叫什么吗？",
            "我刚才说我喜欢喝什么？"
        };

        for (String q : questions) {
            log.info("用户: {}", q);
            
            Message userMsg = Message.builder()
                    .content(q)
                    .messageType(MessageType.INPUT)
                    .build();
            
            // 存入记忆
            memoryManager.addMessage(userMsg);
            
            // 获取包含历史的消息列表
            List<Message> context = memoryManager.getLatestMessages(5);
            
            // 执行智能体
            Message response = agent.execute("chat", context);
            log.info("AI: {}", response.getContent());
            
            // 存入 AI 回复
            memoryManager.addMessage(response);
        }

        log.info("--- 记忆统计 ---");
        log.info("短期记忆消息数: {}", memoryManager.getShortTermSize());
    }
}
