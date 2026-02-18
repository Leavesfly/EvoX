package io.leavesfly.evox.agents.builder;

import io.leavesfly.evox.actions.base.Action;
import io.leavesfly.evox.agents.base.Agent;
import io.leavesfly.evox.agents.specialized.ChatBotAgent;
import io.leavesfly.evox.core.llm.ILLM;

import io.leavesfly.evox.core.llm.LLMConfig;
import io.leavesfly.evox.models.provider.openai.OpenAILLMConfig;
import io.leavesfly.evox.models.config.LLMFactory;
import io.leavesfly.evox.models.provider.openai.OpenAILLM;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

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
    
    private final Supplier<T> agentFactory;
    private final String agentTypeName;
    private String name;
    private String description;
    private String systemPrompt;
    private ILLM llm;
    private List<Action> actions = new ArrayList<>();
    
    /**
     * 通过 Supplier 工厂构造（推荐）
     *
     * @param agentFactory 创建 Agent 实例的工厂函数
     * @param agentTypeName Agent 类型名（用于默认命名和错误提示）
     */
    private AgentBuilder(Supplier<T> agentFactory, String agentTypeName) {
        this.agentFactory = agentFactory;
        this.agentTypeName = agentTypeName;
    }
    
    /**
     * 创建聊天机器人构建器
     */
    public static AgentBuilder<ChatBotAgent> chatBot() {
        // 使用延迟 Supplier — llm 在 build() 时才设置
        return new AgentBuilder<>(() -> null, "ChatBotAgent");
    }
    
    /**
     * 创建自定义 Agent 构建器（推荐：使用 Supplier）
     *
     * <pre>{@code
     * AgentBuilder.custom(MyAgent::new)
     *     .name("my-agent")
     *     .withOpenAI(apiKey)
     *     .build();
     * }</pre>
     *
     * @param factory 创建 Agent 实例的工厂函数
     * @param <T> Agent 类型
     * @return AgentBuilder 实例
     */
    public static <T extends Agent> AgentBuilder<T> custom(Supplier<T> factory) {
        return new AgentBuilder<>(factory, "CustomAgent");
    }

    /**
     * 创建自定义 Agent 构建器（兼容旧 API：使用 Class）
     *
     * @param agentClass Agent 类（必须有无参构造函数）
     * @param <T> Agent 类型
     * @return AgentBuilder 实例
     */
    public static <T extends Agent> AgentBuilder<T> custom(Class<T> agentClass) {
        return new AgentBuilder<>(() -> {
            try {
                return agentClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate " + agentClass.getName() +
                        ". Ensure it has a no-arg constructor, or use custom(Supplier) instead.", e);
            }
        }, agentClass.getSimpleName());
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
    public AgentBuilder<T> withLLM(ILLM llm) {
        this.llm = llm;
        return this;
    }

    /**
     * 从 LLMConfig 自动创建 LLM（支持所有已注册的 provider）
     *
     * <pre>{@code
     * AgentBuilder.chatBot()
     *     .withConfig(LLMConfigs.aliyun("sk-xxx", "qwen-max"))
     *     .build();
     * }</pre>
     */
    public AgentBuilder<T> withConfig(LLMConfig config) {
        this.llm = LLMFactory.create(config);
        return this;
    }

    /**
     * 配置阿里云通义千问（使用环境变量 DASHSCOPE_API_KEY）
     */
    public AgentBuilder<T> withAliyun() {
        this.llm = LLMFactory.aliyun();
        return this;
    }

    /**
     * 配置阿里云通义千问
     */
    public AgentBuilder<T> withAliyun(String apiKey) {
        this.llm = LLMFactory.aliyun(apiKey);
        return this;
    }

    /**
     * 配置阿里云通义千问
     */
    public AgentBuilder<T> withAliyun(String apiKey, String model) {
        this.llm = LLMFactory.aliyun(apiKey, model);
        return this;
    }

    /**
     * 配置 Ollama 本地模型
     */
    public AgentBuilder<T> withOllama(String model) {
        this.llm = LLMFactory.ollama(model);
        return this;
    }

    /**
     * 配置 Ollama（自定义地址）
     */
    public AgentBuilder<T> withOllama(String model, String baseUrl) {
        this.llm = LLMFactory.ollama(model, baseUrl);
        return this;
    }

    /**
     * 配置 SiliconFlow（使用环境变量 SILICONFLOW_API_KEY）
     */
    public AgentBuilder<T> withSiliconFlow() {
        this.llm = LLMFactory.siliconflow();
        return this;
    }

    /**
     * 配置 SiliconFlow
     */
    public AgentBuilder<T> withSiliconFlow(String apiKey) {
        this.llm = LLMFactory.siliconflow(apiKey);
        return this;
    }

    /**
     * 配置 SiliconFlow
     */
    public AgentBuilder<T> withSiliconFlow(String apiKey, String model) {
        this.llm = LLMFactory.siliconflow(apiKey, model);
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
     *
     * <p>自动完成属性设置和 {@code initModule()} 初始化。</p>
     */
    @SuppressWarnings("unchecked")
    public T build() {
        try {
            // 创建 Agent 实例
            T agent;
            if (agentTypeName.equals("ChatBotAgent")) {
                // ChatBotAgent 需要 LLMProvider 构造参数
                agent = (T) new ChatBotAgent(llm);
            } else {
                agent = agentFactory.get();
                if (agent == null) {
                    throw new IllegalStateException("Agent factory returned null");
                }
            }
            
            // 设置属性
            if (name != null) {
                agent.setName(name);
            } else {
                agent.setName(agentTypeName + "-" + System.currentTimeMillis());
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
            throw new RuntimeException("Failed to build agent: " + agentTypeName, e);
        }
    }
}
