package io.leavesfly.evox.agents.builder;

import io.leavesfly.evox.actions.base.Action;
import io.leavesfly.evox.agents.base.Agent;
import io.leavesfly.evox.agents.specialized.ChatBotAgent;
import io.leavesfly.evox.models.base.BaseLLM;
import io.leavesfly.evox.models.config.OpenAILLMConfig;
import io.leavesfly.evox.models.openai.OpenAILLM;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 流式构建器
 * 
 * <p>提供链式调用方式快速构建 Agent</p>
 * 
 * <h3>使用示例:</h3>
 * <pre>{@code
 * // 创建聊天机器人
 * Agent agent = AgentBuilder.chatBot()
 *     .name("MyBot")
 *     .description("智能助手")
 *     .withOpenAI(apiKey)
 *     .withSystemPrompt("你是一个专业的助手")
 *     .build();
 * 
 * // 创建自定义 Agent
 * Agent agent = AgentBuilder.custom(MyCustomAgent.class)
 *     .name("CustomBot")
 *     .withLLM(llm)
 *     .addAction(new MyAction())
 *     .build();
 * }</pre>
 * 
 * @author EvoX Team
 * @since 1.0.0
 */
public class AgentBuilder<T extends Agent> {
    
    private final Class<T> agentClass;
    private String name;
    private String description;
    private String systemPrompt;
    private BaseLLM llm;
    private List<Action> actions = new ArrayList<>();
    
    /**
     * 私有构造函数
     */
    private AgentBuilder(Class<T> agentClass) {
        this.agentClass = agentClass;
    }
    
    /**
     * 创建聊天机器人构建器
     */
    public static AgentBuilder<ChatBotAgent> chatBot() {
        return new AgentBuilder<>(ChatBotAgent.class);
    }
    
    /**
     * 创建自定义 Agent 构建器
     */
    public static <T extends Agent> AgentBuilder<T> custom(Class<T> agentClass) {
        return new AgentBuilder<>(agentClass);
    }
    
    /**
     * 设置 Agent 名称
     */
    public AgentBuilder<T> name(String name) {
        this.name = name;
        return this;
    }
    
    /**
     * 设置 Agent 描述
     */
    public AgentBuilder<T> description(String description) {
        this.description = description;
        return this;
    }
    
    /**
     * 设置系统提示词
     */
    public AgentBuilder<T> withSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
        return this;
    }
    
    /**
     * 配置 OpenAI LLM（从环境变量读取 API Key）
     */
    public AgentBuilder<T> withOpenAI() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException(
                "OPENAI_API_KEY environment variable is not set. " +
                "Please set it or use withOpenAI(String apiKey) method."
            );
        }
        return withOpenAI(apiKey, "gpt-4o-mini");
    }
    
    /**
     * 配置 OpenAI LLM（指定 API Key）
     */
    public AgentBuilder<T> withOpenAI(String apiKey) {
        return withOpenAI(apiKey, "gpt-4o-mini");
    }
    
    /**
     * 配置 OpenAI LLM（指定 API Key 和模型）
     */
    public AgentBuilder<T> withOpenAI(String apiKey, String model) {
        OpenAILLMConfig config = OpenAILLMConfig.builder()
            .apiKey(apiKey)
            .model(model)
            .temperature(0.7f)
            .maxTokens(2000)
            .build();
        this.llm = new OpenAILLM(config);
        return this;
    }
    
    /**
     * 配置自定义 LLM
     */
    public AgentBuilder<T> withLLM(BaseLLM llm) {
        this.llm = llm;
        return this;
    }
    
    /**
     * 添加动作
     */
    public AgentBuilder<T> addAction(Action action) {
        this.actions.add(action);
        return this;
    }
    
    /**
     * 批量添加动作
     */
    public AgentBuilder<T> addActions(Action... actions) {
        this.actions.addAll(List.of(actions));
        return this;
    }
    
    /**
     * 构建 Agent
     */
    public T build() {
        try {
            // 创建 Agent 实例
            T agent;
            if (agentClass == ChatBotAgent.class) {
                agent = agentClass.getConstructor(BaseLLM.class).newInstance(llm);
            } else {
                agent = agentClass.getDeclaredConstructor().newInstance();
            }
            
            // 设置属性
            if (name != null) {
                agent.setName(name);
            } else {
                agent.setName(agentClass.getSimpleName() + "-" + System.currentTimeMillis());
            }
            
            if (description != null) {
                agent.setDescription(description);
            }
            
            if (systemPrompt != null) {
                agent.setSystemPrompt(systemPrompt);
            }
            
            if (llm != null) {
                agent.setLlm(llm);
            }
            
            // 添加动作
            for (Action action : actions) {
                agent.addAction(action);
            }
            
            // 初始化
            agent.initModule();
            
            return agent;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to build agent: " + agentClass.getName(), e);
        }
    }
}
