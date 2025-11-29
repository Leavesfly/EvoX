package io.leavesfly.evox.prompts;

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
            "You are a helpful AI assistant. Please respond to the user's request accurately and concisely.";

    /**
     * 代码生成提示词
     */
    public static final String CODE_GENERATION_PROMPT = 
            "You are an expert programmer. Generate clean, efficient, and well-documented code based on the requirements.";

    /**
     * 代码审查提示词
     */
    public static final String CODE_REVIEW_PROMPT = 
            "You are a code reviewer. Analyze the provided code for bugs, performance issues, and best practices. " +
            "Provide specific suggestions for improvement.";

    /**
     * 问题分析提示词
     */
    public static final String PROBLEM_ANALYSIS_PROMPT = 
            "You are a problem analyst. Break down the problem into smaller components and provide a detailed analysis.";

    /**
     * 解决方案设计提示词
     */
    public static final String SOLUTION_DESIGN_PROMPT = 
            "You are a solution architect. Design a comprehensive solution based on the problem analysis. " +
            "Include architecture, components, and implementation steps.";

    /**
     * 数据分析提示词
     */
    public static final String DATA_ANALYSIS_PROMPT = 
            "You are a data analyst. Analyze the provided data and extract meaningful insights. " +
            "Identify patterns, trends, and anomalies.";

    /**
     * 文本摘要提示词
     */
    public static final String SUMMARIZATION_PROMPT = 
            "You are a summarization expert. Create a concise summary of the provided text while preserving key information.";

    /**
     * 翻译提示词
     */
    public static final String TRANSLATION_PROMPT = 
            "You are a professional translator. Translate the provided text accurately while maintaining the original meaning and tone.";

    /**
     * 创意写作提示词
     */
    public static final String CREATIVE_WRITING_PROMPT = 
            "You are a creative writer. Generate engaging and original content based on the given topic or prompt.";

    /**
     * 技术文档提示词
     */
    public static final String TECHNICAL_WRITING_PROMPT = 
            "You are a technical writer. Create clear and comprehensive documentation for the provided technical content.";

    /**
     * 调试助手提示词
     */
    public static final String DEBUGGING_PROMPT = 
            "You are a debugging expert. Identify the root cause of the error and suggest specific fixes.";

    /**
     * 测试生成提示词
     */
    public static final String TEST_GENERATION_PROMPT = 
            "You are a test engineer. Generate comprehensive unit tests for the provided code. " +
            "Include edge cases and error handling scenarios.";

    /**
     * API 设计提示词
     */
    public static final String API_DESIGN_PROMPT = 
            "You are an API designer. Design RESTful API endpoints based on the requirements. " +
            "Include request/response formats, status codes, and error handling.";

    /**
     * 数据库设计提示词
     */
    public static final String DATABASE_DESIGN_PROMPT = 
            "You are a database architect. Design an efficient database schema based on the requirements. " +
            "Include tables, relationships, indexes, and constraints.";

    /**
     * 性能优化提示词
     */
    public static final String PERFORMANCE_OPTIMIZATION_PROMPT = 
            "You are a performance optimization expert. Analyze the code/system and suggest specific optimizations " +
            "to improve speed, reduce resource usage, and enhance scalability.";

    /**
     * 安全审计提示词
     */
    public static final String SECURITY_AUDIT_PROMPT = 
            "You are a security expert. Identify potential security vulnerabilities and suggest mitigation strategies.";

    /**
     * 重构建议提示词
     */
    public static final String REFACTORING_PROMPT = 
            "You are a refactoring expert. Suggest code improvements to enhance readability, maintainability, " +
            "and adherence to best practices.";

    /**
     * 需求分析提示词
     */
    public static final String REQUIREMENT_ANALYSIS_PROMPT = 
            "You are a requirements analyst. Extract and clarify requirements from the provided description. " +
            "Identify functional and non-functional requirements.";

    /**
     * 用户故事生成提示词
     */
    public static final String USER_STORY_PROMPT = 
            "You are an agile coach. Convert requirements into well-formed user stories with acceptance criteria.";

    /**
     * 错误消息生成提示词
     */
    public static final String ERROR_MESSAGE_PROMPT = 
            "You are a UX writer. Create clear and helpful error messages that guide users toward resolution.";

    /**
     * 多智能体协作提示词
     */
    public static final String MULTI_AGENT_COLLABORATION_PROMPT = 
            "You are part of a team of AI agents. Collaborate with other agents to achieve the common goal. " +
            "Share relevant information and build upon previous contributions.";

    /**
     * 反思和改进提示词
     */
    public static final String REFLECTION_PROMPT = 
            "Reflect on the previous attempt. What worked well? What could be improved? " +
            "How can you approach this differently?";

    /**
     * 验证和检查提示词
     */
    public static final String VALIDATION_PROMPT = 
            "Validate the output against the requirements. Check for completeness, correctness, and quality.";

    /**
     * Chain of Thought 提示词
     */
    public static final String CHAIN_OF_THOUGHT_PROMPT = 
            "Let's think step by step. Break down the problem and reason through each step carefully.";

    /**
     * Few-Shot 学习提示词模板
     */
    public static final String FEW_SHOT_TEMPLATE = 
            "Here are some examples:\n\n{examples}\n\nNow solve this:\n{input}";

    /**
     * RAG 检索提示词
     */
    public static final String RAG_RETRIEVAL_PROMPT = 
            "Based on the following retrieved context:\n\n{context}\n\nAnswer the question: {question}";

    /**
     * 工具使用提示词
     */
    public static final String TOOL_USAGE_PROMPT = 
            "You have access to the following tools: {tools}\n\n" +
            "Use the appropriate tool to accomplish the task. Format your tool call as: TOOL[tool_name](arguments)";

    /**
     * JSON 输出格式提示词
     */
    public static final String JSON_OUTPUT_PROMPT = 
            "Please provide your response in valid JSON format following this structure: {schema}";

    /**
     * XML 输出格式提示词
     */
    public static final String XML_OUTPUT_PROMPT = 
            "Please provide your response in valid XML format following this structure: {schema}";

    /**
     * 结构化输出提示词
     */
    public static final String STRUCTURED_OUTPUT_PROMPT = 
            "Please provide your response in the following structured format:\n{format}";

    // 防止实例化
    private PromptConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
