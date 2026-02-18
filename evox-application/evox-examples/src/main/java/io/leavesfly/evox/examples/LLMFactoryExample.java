package io.leavesfly.evox.examples;

import io.leavesfly.evox.agents.customize.CustomizeAgent;
import io.leavesfly.evox.core.llm.LLMConfig;
import io.leavesfly.evox.models.provider.aliyun.AliyunLLMConfig;
import io.leavesfly.evox.models.config.LLMConfigs;
import io.leavesfly.evox.models.provider.ollama.OllamaLLMConfig;
import io.leavesfly.evox.models.provider.openai.OpenAILLMConfig;
import io.leavesfly.evox.models.provider.siliconflow.SiliconFlowLLMConfig;
import io.leavesfly.evox.models.config.LLMFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LLM 工厂与配置简化示例
 * 
 * <p>演示使用 LLMFactory、LLMConfig 静态工厂、Agent 懒初始化等新特性，
 * 对比改进前后创建和配置 LLM 的体验差异</p>
 *
 * @author EvoX Team
 */
public class LLMFactoryExample {
    private static final Logger log = LoggerFactory.getLogger(LLMFactoryExample.class);

    public static void main(String[] args) {
        LLMFactoryExample example = new LLMFactoryExample();

        // 示例1: LLMFactory 快捷创建（一行代码）
        example.factoryQuickCreate();

        // 示例2: LLMConfig 静态工厂方法
        example.configStaticFactory();

        // 示例3: LLMFactory.create() 自动匹配
        example.factoryAutoMatch();

        // 示例4: Agent 懒初始化（只设 config，自动创建 LLM）
        example.agentLazyInit();

        // 示例5: AgentBuilder 多 provider 支持
        example.agentBuilderProviders();

        // 示例6: 自定义 provider 注册
        example.customProviderRegistration();

        // 示例7: 新旧写法对比
        example.beforeAndAfterComparison();
    }

    // ===================================================================
    // 示例1: LLMFactory 快捷创建
    // ===================================================================

    /**
     * 一行代码创建任意 LLM 实例，不需要知道具体实现类
     */
    private void factoryQuickCreate() {
        log.info("\n=== 示例1: LLMFactory 快捷创建 ===\n");

        // 注意：以下代码需要对应的 API Key 才能实际运行
        // 这里仅展示 API 用法

        // --- OpenAI ---
        // LLMProvider openai = LLMFactory.openai("sk-xxx");
        // LLMProvider openai = LLMFactory.openai("sk-xxx", "gpt-4o");
        // LLMProvider openai = LLMFactory.openai();  // 从环境变量 OPENAI_API_KEY 读取
        log.info("OpenAI:      LLMFactory.openai(apiKey)");
        log.info("             LLMFactory.openai(apiKey, \"gpt-4o\")");
        log.info("             LLMFactory.openai()  // 环境变量 OPENAI_API_KEY");

        // --- 阿里云通义千问 ---
        // LLMProvider aliyun = LLMFactory.aliyun("sk-xxx");
        // LLMProvider aliyun = LLMFactory.aliyun("sk-xxx", "qwen-max");
        // LLMProvider aliyun = LLMFactory.aliyun();  // 从环境变量 DASHSCOPE_API_KEY 读取
        log.info("\n阿里云千问:   LLMFactory.aliyun(apiKey)");
        log.info("             LLMFactory.aliyun(apiKey, \"qwen-max\")");
        log.info("             LLMFactory.aliyun()  // 环境变量 DASHSCOPE_API_KEY");

        // --- Ollama 本地模型 ---
        // LLMProvider ollama = LLMFactory.ollama("llama3");
        // LLMProvider ollama = LLMFactory.ollama("codellama", "http://192.168.1.100:11434");
        log.info("\nOllama:      LLMFactory.ollama(\"llama3\")");
        log.info("             LLMFactory.ollama(\"codellama\", \"http://remote:11434\")");

        // --- SiliconFlow ---
        // LLMProvider sf = LLMFactory.siliconflow("sk-xxx");
        // LLMProvider sf = LLMFactory.siliconflow("sk-xxx", "Qwen/Qwen2.5-72B-Instruct");
        log.info("\nSiliconFlow: LLMFactory.siliconflow(apiKey)");
        log.info("             LLMFactory.siliconflow(apiKey, \"Qwen/Qwen2.5-72B-Instruct\")");

    }

    // ===================================================================
    // 示例2: LLMConfig 静态工厂方法
    // ===================================================================

    /**
     * 用 LLMConfigs.xxx() 一行创建配置对象
     */
    private void configStaticFactory() {
        log.info("\n=== 示例2: LLMConfigs 静态工厂方法 ===\n");

        // 统一入口，不需要记各种 Config 子类的名字
        OpenAILLMConfig openaiConfig = LLMConfigs.openAI("sk-xxx", "gpt-4o");
        AliyunLLMConfig aliyunConfig = LLMConfigs.aliyun("sk-xxx", "qwen-max");
        OllamaLLMConfig ollamaConfig = LLMConfigs.ollama("llama3");
        SiliconFlowLLMConfig sfConfig = LLMConfigs.siliconFlow("sk-xxx");

        log.info("OpenAI config:      provider={}, model={}", openaiConfig.getProvider(), openaiConfig.getModel());
        log.info("Aliyun config:      provider={}, model={}", aliyunConfig.getProvider(), aliyunConfig.getModel());
        log.info("Ollama config:      provider={}, model={}", ollamaConfig.getProvider(), ollamaConfig.getModel());
        log.info("SiliconFlow config: provider={}, model={}", sfConfig.getProvider(), sfConfig.getModel());

        // 对比旧写法：
        // OpenAILLMConfig old = OpenAILLMConfig.builder().apiKey("sk-xxx").model("gpt-4o").build();
        // 新写法：
        // OpenAILLMConfig config = LLMConfigs.openAI("sk-xxx", "gpt-4o");
        log.info("\n只需记住 LLMConfigs.xxx() 即可，无需记忆各子类名");
    }

    // ===================================================================
    // 示例3: LLMFactory.create() 自动匹配
    // ===================================================================

    /**
     * 传入任意 LLMConfig，工厂自动匹配对应的 LLM 实现
     */
    private void factoryAutoMatch() {
        log.info("\n=== 示例3: LLMFactory.create() 自动匹配 ===\n");

        // 创建不同 provider 的配置
        LLMConfig[] configs = {
                LLMConfigs.openAI("sk-xxx"),
                LLMConfigs.aliyun("sk-xxx"),
                LLMConfigs.ollama("llama3"),
                LLMConfigs.siliconFlow("sk-xxx")
        };

        for (LLMConfig config : configs) {
            // LLMFactory.create() 根据 config 类型自动推断用哪个 LLM 实现
            // 不需要用户知道 OpenAILLM / AliyunLLM / OllamaLLM 这些类
            log.info("LLMFactory.create({}) -> 自动创建 {} 实现",
                    config.getClass().getSimpleName(),
                    config.getProvider());
        }

        // 验证 provider 是否支持
        log.info("\n检查 provider 支持:");
        log.info("  openai: {}", LLMFactory.isSupported("openai"));
        log.info("  aliyun: {}", LLMFactory.isSupported("aliyun"));
        log.info("  deepseek: {} (尚未注册)", LLMFactory.isSupported("deepseek"));
    }

    // ===================================================================
    // 示例4: Agent 懒初始化
    // ===================================================================

    /**
     * Agent 只需设置 LLMConfig，调用 getLlm() 时自动通过 LLMFactory 创建实例
     */
    private void agentLazyInit() {
        log.info("\n=== 示例4: Agent 懒初始化 ===\n");

        // --- 改进前：需要同时设置 config 和 llm ---
        log.info("改进前（两步）:");
        log.info("  agent.setLlmConfig(config);");
        log.info("  agent.setLlm(new OpenAILLM(config));  // 手动创建，容易漏");

        // --- 改进后：只设 config，自动创建 llm ---
        log.info("\n改进后（一步）:");
        log.info("  agent.setLlmConfig(LLMConfigs.openAI(apiKey));");
        log.info("  // 不需要 setLlm()！getLlm() 时自动创建");

        // 实际演示
        CustomizeAgent agent = new CustomizeAgent();
        agent.setName("LazyInitAgent");
        agent.setDescription("演示懒初始化的Agent");
        agent.setPromptTemplate("请回答: {question}");

        // 只设配置，不设 LLM 实例
        agent.setLlmConfig(LLMConfigs.ollama("llama3"));

        log.info("\n实际演示:");
        log.info("  已设置 llmConfig: provider={}, model={}",
                agent.getLlmConfig().getProvider(), agent.getLlmConfig().getModel());

        // getLlm() 会自动通过 LLMFactory.create() 创建
        // 注意：这里因为没有运行 Ollama 服务所以不实际调用
        log.info("  调用 agent.getLlm() 时会自动创建 OllamaLLM 实例");
        log.info("  适用场景: Agent 序列化/反序列化后自动恢复 LLM 连接");
    }

    // ===================================================================
    // 示例5: AgentBuilder 多 provider 支持
    // ===================================================================

    /**
     * AgentBuilder 新增了所有 LLM provider 的快捷方法
     */
    private void agentBuilderProviders() {
        log.info("\n=== 示例5: AgentBuilder 多 provider 支持 ===\n");

        // 以下展示 AgentBuilder 支持的所有 LLM 配置方式
        // 实际运行需要对应的 API Key

        log.info("AgentBuilder 现在支持:");
        log.info("");
        log.info("  // OpenAI");
        log.info("  AgentBuilder.chatBot().withOpenAI().build()");
        log.info("  AgentBuilder.chatBot().withOpenAI(apiKey).build()");
        log.info("  AgentBuilder.chatBot().withOpenAI(apiKey, \"gpt-4o\").build()");
        log.info("");
        log.info("  // 阿里云通义千问");
        log.info("  AgentBuilder.chatBot().withAliyun().build()");
        log.info("  AgentBuilder.chatBot().withAliyun(apiKey).build()");
        log.info("  AgentBuilder.chatBot().withAliyun(apiKey, \"qwen-max\").build()");
        log.info("");
        log.info("  // Ollama 本地模型");
        log.info("  AgentBuilder.chatBot().withOllama(\"llama3\").build()");
        log.info("  AgentBuilder.chatBot().withOllama(\"llama3\", \"http://remote:11434\").build()");
        log.info("");
        log.info("  // SiliconFlow");
        log.info("  AgentBuilder.chatBot().withSiliconFlow().build()");
        log.info("  AgentBuilder.chatBot().withSiliconFlow(apiKey).build()");
        log.info("  AgentBuilder.chatBot().withSiliconFlow(apiKey, model).build()");
        log.info("");
        log.info("  // 通用：传入任意 LLMConfig");
        log.info("  AgentBuilder.chatBot().withConfig(LLMConfigs.aliyun(key)).build()");
        log.info("");
        log.info("  // 通用：传入已有 LLMProvider 实例");
        log.info("  AgentBuilder.chatBot().withLLM(existingLlm).build()");
    }

    // ===================================================================
    // 示例6: 自定义 provider 注册
    // ===================================================================

    /**
     * 用 LLMFactory.register() 扩展自定义 LLM provider
     */
    private void customProviderRegistration() {
        log.info("\n=== 示例6: 自定义 provider 注册 ===\n");

        log.info("注册前: deepseek supported = {}", LLMFactory.isSupported("deepseek"));

        // 注册自定义 provider（这里用 OpenAI 兼容接口模拟 DeepSeek）
        LLMFactory.register("deepseek", config -> {
            // 将 DeepSeek 的 config 转为 OpenAI 兼容的调用
            // 实际场景：new DeepSeekLLM((DeepSeekConfig) config)
            log.info("  (模拟) 创建 DeepSeek LLM, model={}", config.getModel());
            // 这里返回 null 仅作演示，实际应返回 LLMProvider 实例
            return null;
        });

        log.info("注册后: deepseek supported = {}", LLMFactory.isSupported("deepseek"));

        log.info("\n扩展新 provider 只需一行代码:");
        log.info("  LLMFactory.register(\"deepseek\", config -> new DeepSeekLLM(config));");
        log.info("  然后就可以: LLMFactory.create(deepseekConfig)");
    }

    // ===================================================================
    // 示例7: 新旧写法对比
    // ===================================================================

    /**
     * 直观对比改进前后的代码量和复杂度
     */
    private void beforeAndAfterComparison() {
        log.info("\n=== 示例7: 新旧写法对比 ===\n");

        // ---------------------------------------------------------------
        log.info("场景A: 创建一个 OpenAI LLM 实例");
        log.info("-------");
        log.info("  改进前 (4行):");
        log.info("    OpenAILLMConfig config = OpenAILLMConfig.builder()");
        log.info("        .apiKey(key).model(\"gpt-4o\").temperature(0.7f).build();");
        log.info("    LLMProvider llm = new OpenAILLM(config);");
        log.info("");
        log.info("  改进后 (1行):");
        log.info("    LLMProvider llm = LLMFactory.openai(key, \"gpt-4o\");");

        // ---------------------------------------------------------------
        log.info("\n场景B: 创建一个使用阿里云千问的 Agent");
        log.info("-------");
        log.info("  改进前 (7行):");
        log.info("    AliyunLLMConfig config = AliyunLLMConfig.builder()");
        log.info("        .aliyunApiKey(key).model(\"qwen-max\").build();");
        log.info("    AliyunLLM llm = new AliyunLLM(config);");
        log.info("    agent.setLlmConfig(config);");
        log.info("    agent.setLlm(llm);");
        log.info("    agent.initModule();");
        log.info("");
        log.info("  改进后 (2行):");
        log.info("    agent.setLlmConfig(LLMConfigs.aliyun(key, \"qwen-max\"));");
        log.info("    agent.initModule();  // getLlm() 时自动创建");

        // ---------------------------------------------------------------
        log.info("\n场景C: 用 AgentBuilder 创建聊天机器人");
        log.info("-------");
        log.info("  改进前 (仅支持 OpenAI):");
        log.info("    AgentBuilder.chatBot().withOpenAI(key).build();");
        log.info("");
        log.info("  改进后 (支持所有 provider):");
        log.info("    AgentBuilder.chatBot().withAliyun(key, \"qwen-max\").build();");
        log.info("    AgentBuilder.chatBot().withOllama(\"llama3\").build();");
        log.info("    AgentBuilder.chatBot().withSiliconFlow(key).build();");
        log.info("    AgentBuilder.chatBot().withConfig(anyConfig).build();");

        // ---------------------------------------------------------------
        log.info("\n场景D: 切换 LLM provider（只需改一行）");
        log.info("-------");
        log.info("  // 从 OpenAI 切换到阿里云，只需改配置那一行");
        log.info("  // LLMConfig config = LLMConfigs.openAI(key);");
        log.info("  LLMConfig config = LLMConfigs.aliyun(key, \"qwen-max\");");
        log.info("  LLMProvider llm = LLMFactory.create(config);  // 同一行代码，自动适配");
    }
}
