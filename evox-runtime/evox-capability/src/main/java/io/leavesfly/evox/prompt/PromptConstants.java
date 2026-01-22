package io.leavesfly.evox.prompt;

/**
 * 提示词常量
 * 存储系统使用的各类提示词模板
 *
 * @author EvoX Team
 */
public class PromptConstants {

    /**
     * 默认系统提示词
     */
    public static final String DEFAULT_SYSTEM_PROMPT = 
            "你是一个有帮助的AI助手。请准确、简洁地回应用户的请求。";

    /**
     * 代码生成提示词
     */
    public static final String CODE_GENERATION_PROMPT = 
            "你是一名专业程序员。请根据需求生成简洁、高效且文档完善的代码。";

    /**
     * 代码审查提示词
     */
    public static final String CODE_REVIEW_PROMPT = 
            "你是一名代码审查员。请分析提供的代码，检查其中的Bug、性能问题和最佳实践。" +
            "提供具体的改进建议。";

    /**
     * 问题分析提示词
     */
    public static final String PROBLEM_ANALYSIS_PROMPT = 
            "你是一名问题分析师。请将问题分解为更小的组件，并提供详细的分析。";

    /**
     * 解决方案设计提示词
     */
    public static final String SOLUTION_DESIGN_PROMPT = 
            "你是一名解决方案架构师。请根据问题分析设计一个全面的解决方案。" +
            "包括架构、组件和实施步骤。";

    /**
     * 数据分析提示词
     */
    public static final String DATA_ANALYSIS_PROMPT = 
            "你是一名数据分析师。请分析提供的数据并提取有意义的见解。" +
            "识别模式、趋势和异常。";

    /**
     * 文本摘要提示词
     */
    public static final String SUMMARIZATION_PROMPT = 
            "你是一名摘要专家。请在保留关键信息的同时，为提供的文本创建简洁的摘要。";

    /**
     * 翻译提示词
     */
    public static final String TRANSLATION_PROMPT = 
            "你是一名专业翻译。请准确翻译提供的文本，同时保持原文的含义和语气。";

    /**
     * 创意写作提示词
     */
    public static final String CREATIVE_WRITING_PROMPT = 
            "你是一名创意写作者。请根据给定的主题或提示生成引人入胜且原创的内容。";

    /**
     * 技术文档提示词
     */
    public static final String TECHNICAL_WRITING_PROMPT = 
            "你是一名技术文档撰写者。请为提供的技术内容创建清晰、全面的文档。";

    /**
     * 调试助手提示词
     */
    public static final String DEBUGGING_PROMPT = 
            "你是一名调试专家。请找出错误的根本原因并提供具体的修复建议。";

    /**
     * 测试生成提示词
     */
    public static final String TEST_GENERATION_PROMPT = 
            "你是一名测试工程师。请为提供的代码生成全面的单元测试。" +
            "包括边界情况和错误处理场景。";

    /**
     * API 设计提示词
     */
    public static final String API_DESIGN_PROMPT = 
            "你是一名API设计师。请根据需求设计RESTful API端点。" +
            "包括请求/响应格式、状态码和错误处理。";

    /**
     * 数据库设计提示词
     */
    public static final String DATABASE_DESIGN_PROMPT = 
            "你是一名数据库架构师。请根据需求设计高效的数据库模式。" +
            "包括表、关系、索引和约束。";

    /**
     * 性能优化提示词
     */
    public static final String PERFORMANCE_OPTIMIZATION_PROMPT = 
            "你是一名性能优化专家。请分析代码/系统并提供具体的优化建议，" +
            "以提高速度、减少资源使用并增强可扩展性。";

    /**
     * 安全审计提示词
     */
    public static final String SECURITY_AUDIT_PROMPT = 
            "你是一名安全专家。请识别潜在的安全漏洞并提供缓解策略。";

    /**
     * 重构建议提示词
     */
    public static final String REFACTORING_PROMPT = 
            "你是一名重构专家。请提供代码改进建议，以增强可读性、可维护性" +
            "和对最佳实践的遵循。";

    /**
     * 需求分析提示词
     */
    public static final String REQUIREMENT_ANALYSIS_PROMPT = 
            "你是一名需求分析师。请从提供的描述中提取和澄清需求。" +
            "识别功能性和非功能性需求。";

    /**
     * 用户故事生成提示词
     */
    public static final String USER_STORY_PROMPT = 
            "你是一名敏捷教练。请将需求转换为格式规范的用户故事，包含验收标准。";

    /**
     * 错误消息生成提示词
     */
    public static final String ERROR_MESSAGE_PROMPT = 
            "你是一名用户体验文案撰写者。请创建清晰且有帮助的错误消息，引导用户解决问题。";

    /**
     * 多智能体协作提示词
     */
    public static final String MULTI_AGENT_COLLABORATION_PROMPT = 
            "你是AI智能体团队的一员。请与其他智能体协作以实现共同目标。" +
            "分享相关信息并在之前的贡献基础上继续推进。";

    /**
     * 反思和改进提示词
     */
    public static final String REFLECTION_PROMPT = 
            "请反思之前的尝试。哪些做得好？哪些可以改进？" +
            "你可以如何采用不同的方法？";

    /**
     * 验证和检查提示词
     */
    public static final String VALIDATION_PROMPT = 
            "请根据需求验证输出。检查完整性、正确性和质量。";

    /**
     * Chain of Thought 提示词
     */
    public static final String CHAIN_OF_THOUGHT_PROMPT = 
            "让我们一步一步来思考。将问题分解并仔细推理每个步骤。";

    /**
     * Few-Shot 学习提示词模板
     */
    public static final String FEW_SHOT_TEMPLATE = 
            "以下是一些示例：\n\n{examples}\n\n现在请解决这个问题：\n{input}";

    /**
     * RAG 检索提示词
     */
    public static final String RAG_RETRIEVAL_PROMPT = 
            "根据以下检索到的上下文：\n\n{context}\n\n请回答问题：{question}";

    /**
     * 工具使用提示词
     */
    public static final String TOOL_USAGE_PROMPT = 
            "你可以使用以下工具：{tools}\n\n" +
            "请使用适当的工具来完成任务。工具调用格式为：TOOL[tool_name](arguments)";

    /**
     * JSON 输出格式提示词
     */
    public static final String JSON_OUTPUT_PROMPT = 
            "请按照以下结构以有效的JSON格式提供你的响应：{schema}";

    /**
     * XML 输出格式提示词
     */
    public static final String XML_OUTPUT_PROMPT = 
            "请按照以下结构以有效的XML格式提供你的响应：{schema}";

    /**
     * 结构化输出提示词
     */
    public static final String STRUCTURED_OUTPUT_PROMPT = 
            "请按照以下结构化格式提供你的响应：\n{format}";

    // 防止实例化
    private PromptConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
