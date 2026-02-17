package io.leavesfly.evox.examples.skill;

import io.leavesfly.evox.skill.*;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.List;

/**
 * Skill 系统使用示例
 *
 * <p>演示 EvoX 的声明式 Skill 系统（对齐 Claude Code 标准）：
 * <ul>
 *   <li>从 classpath 加载内置 SKILL.md 文件</li>
 *   <li>从文件系统加载自定义 SKILL.md 文件</li>
 *   <li>代码方式注册 Skill</li>
 *   <li>Skill 激活与上下文注入</li>
 *   <li>SkillTool 作为 Meta-Tool 的使用</li>
 * </ul>
 *
 * <p>Skill 是声明式的 Prompt 模板，不直接执行代码，
 * 而是通过上下文注入机制将专家指令注入到 LLM 对话中。
 *
 * @author EvoX Team
 */
@Slf4j
public class SkillExample {

    public static void main(String[] args) {
        log.info("=== EvoX Skill System Examples ===\n");

        // 1. 从 classpath 加载内置 Skill
        loadBuiltinSkillsExample();

        // 2. 从文件系统加载自定义 Skill
        loadCustomSkillsExample();

        // 3. 代码方式注册 Skill
        programmaticSkillExample();

        // 4. Skill 激活与上下文注入
        skillActivationExample();

        // 5. SkillTool Meta-Tool 使用
        skillToolExample();

        // 6. 完整工作流演示
        fullWorkflowExample();
    }

    /**
     * 示例 1: 从 classpath 加载内置 SKILL.md 文件
     *
     * <p>EvoX 内置了多个 Skill，以 SKILL.md 文件形式存储在 classpath 的 skills/ 目录下。
     * 每个 SKILL.md 包含 YAML frontmatter（元数据）和 Markdown 正文（Prompt 模板）。
     */
    private static void loadBuiltinSkillsExample() {
        log.info("\n--- Example 1: Load Built-in Skills from Classpath ---");

        SkillRegistry registry = new SkillRegistry();

        // 一行代码加载所有内置 Skill
        int loadedCount = registry.loadBuiltinSkills();
        log.info("Loaded {} built-in skills from classpath", loadedCount);

        // 列出所有已注册的 Skill
        List<String> skillNames = registry.getSkillNames();
        log.info("Available skills: {}", skillNames);

        // 查看每个 Skill 的详情
        for (BaseSkill skill : registry.getAllSkills()) {
            log.info("  - {} : {} (allowed-tools: {}, model: {})",
                    skill.getName(),
                    skill.getDescription(),
                    skill.getAllowedTools(),
                    skill.getModel());
        }
    }

    /**
     * 示例 2: 从文件系统加载自定义 SKILL.md 文件
     *
     * <p>用户可以在项目目录的 .claude/skills/ 或 ~/.evox/skills/ 下创建自定义 Skill。
     * 每个 Skill 是一个子目录，包含一个 SKILL.md 文件。
     *
     * <p>SKILL.md 格式：
     * <pre>
     * ---
     * name: my_skill
     * description: A custom skill
     * when_to_use: When the user asks for ...
     * allowed-tools:
     *   - shell
     *   - file_system
     * model: inherit
     * ---
     *
     * You are an expert at ...
     * </pre>
     */
    private static void loadCustomSkillsExample() {
        log.info("\n--- Example 2: Load Custom Skills from File System ---");

        SkillRegistry registry = new SkillRegistry();

        // 从项目目录加载自定义 Skill
        Path projectSkillsDir = Path.of(".", ".claude", "skills");
        int projectCount = registry.loadSkillsFromDirectory(projectSkillsDir);
        log.info("Loaded {} skills from project directory: {}", projectCount, projectSkillsDir);

        // 从用户级目录加载
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            Path userSkillsDir = Path.of(userHome, ".evox", "skills");
            int userCount = registry.loadSkillsFromDirectory(userSkillsDir);
            log.info("Loaded {} skills from user directory: {}", userCount, userSkillsDir);
        }

        log.info("Total skills after loading: {}", registry.getSkillCount());
    }

    /**
     * 示例 3: 代码方式注册 Skill（不使用 SKILL.md 文件）
     *
     * <p>除了文件加载，也可以通过代码直接创建和注册 Skill。
     * 这在需要动态生成 Skill 或测试时非常有用。
     */
    private static void programmaticSkillExample() {
        log.info("\n--- Example 3: Programmatic Skill Registration ---");

        SkillRegistry registry = new SkillRegistry();

        // 方式 A: 通过 SkillDefinitionFile 创建
        SkillDefinitionFile definition = SkillDefinitionFile.builder()
                .name("api_design")
                .description("Design RESTful APIs following best practices")
                .whenToUse("When the user asks to design, review, or improve an API")
                .allowedTools(List.of("file_system", "shell"))
                .model("inherit")
                .promptContent("""
                        You are an expert API designer specializing in RESTful APIs.
                        
                        When designing APIs, follow these principles:
                        1. Use proper HTTP methods (GET, POST, PUT, DELETE, PATCH)
                        2. Use plural nouns for resource names
                        3. Version your API (e.g., /api/v1/)
                        4. Return appropriate HTTP status codes
                        5. Use pagination for list endpoints
                        6. Include HATEOAS links where appropriate
                        7. Document with OpenAPI/Swagger
                        
                        Always provide:
                        - Endpoint definitions with methods and paths
                        - Request/response body schemas
                        - Error response formats
                        - Authentication requirements
                        """)
                .build();

        BaseSkill apiDesignSkill = BaseSkill.fromDefinition(definition);
        registry.registerSkill(apiDesignSkill);
        log.info("Registered skill via SkillDefinitionFile: {}", apiDesignSkill.getName());

        // 方式 B: 直接设置 BaseSkill 字段
        BaseSkill debugSkill = new BaseSkill();
        debugSkill.setName("debug_assistant");
        debugSkill.setDescription("Help debug code issues systematically");
        debugSkill.setWhenToUse("When the user encounters a bug or error");
        debugSkill.setSystemPrompt("""
                You are a systematic debugging assistant.
                
                Follow this debugging methodology:
                1. **Reproduce**: Understand and reproduce the issue
                2. **Isolate**: Narrow down the root cause
                3. **Identify**: Find the exact line/component causing the issue
                4. **Fix**: Apply the minimal fix
                5. **Verify**: Confirm the fix resolves the issue
                6. **Prevent**: Suggest tests or guards to prevent recurrence
                
                Always ask clarifying questions before jumping to conclusions.
                """);
        debugSkill.setAllowedTools(List.of("shell", "file_system", "grep"));
        debugSkill.setModel("inherit");

        registry.registerSkill(debugSkill);
        log.info("Registered skill via direct fields: {}", debugSkill.getName());

        // 验证注册结果
        log.info("Total registered skills: {}", registry.getSkillCount());
        log.info("Skill names: {}", registry.getSkillNames());
    }

    /**
     * 示例 4: Skill 激活与上下文注入
     *
     * <p>Skill 激活不直接执行代码，而是返回 {@link SkillActivationResult}，
     * 包含需要注入到对话上下文中的信息：
     * <ul>
     *   <li>metadataMessage: 用户可见的状态消息</li>
     *   <li>skillPrompt: 注入到 LLM 上下文的专家指令（用户不可见）</li>
     *   <li>allowedTools: 预批准的工具列表</li>
     *   <li>modelOverride: 可选的模型覆盖</li>
     * </ul>
     */
    private static void skillActivationExample() {
        log.info("\n--- Example 4: Skill Activation & Context Injection ---");

        // 准备一个带 Skill 的 Registry
        SkillRegistry registry = new SkillRegistry();
        registry.loadBuiltinSkills();

        // 激活 code_review Skill
        String skillName = "code_review";
        if (registry.hasSkill(skillName)) {
            SkillActivationResult activation = registry.activateSkill(skillName);

            log.info("Skill activation result:");
            log.info("  success: {}", activation.isSuccess());
            log.info("  skillName: {}", activation.getSkillName());
            log.info("  metadataMessage: {}", activation.getMetadataMessage());
            log.info("  allowedTools: {}", activation.getAllowedTools());
            log.info("  modelOverride: {}", activation.getModelOverride());
            log.info("  skillPrompt length: {} chars",
                    activation.getSkillPrompt() != null ? activation.getSkillPrompt().length() : 0);

            // 在实际使用中，CodingAgent 会将这些信息注入对话上下文：
            // 1. metadataMessage → 用户可见的流式输出
            // 2. skillPrompt → 作为 system message 注入对话（isMeta=true，用户不可见）
            // 3. allowedTools → 通过 PermissionManager.preApproveToolsForSkill() 预批准
            log.info("\nIn CodingAgent, this would:");
            log.info("  1. Stream '✨ {}' to the user", activation.getMetadataMessage());
            log.info("  2. Inject {} chars of expert prompt into conversation", 
                    activation.getSkillPrompt() != null ? activation.getSkillPrompt().length() : 0);
            log.info("  3. Pre-approve tools: {}", activation.getAllowedTools());
        } else {
            log.warn("Skill '{}' not found. Make sure built-in skills are loaded.", skillName);
        }

        // 尝试激活不存在的 Skill
        SkillActivationResult failedActivation = registry.activateSkill("nonexistent_skill");
        log.info("\nActivating nonexistent skill:");
        log.info("  success: {}", failedActivation.isSuccess());
        log.info("  error: {}", failedActivation.getError());
    }

    /**
     * 示例 5: SkillTool 作为 Meta-Tool
     *
     * <p>SkillTool 是一个特殊的 Meta-Tool，它出现在 LLM 的 tools 数组中。
     * 其 description 动态嵌入 {@code <available_skills>} 列表，
     * LLM 通过推理决定调用哪个 Skill。
     */
    private static void skillToolExample() {
        log.info("\n--- Example 5: SkillTool as Meta-Tool ---");

        SkillRegistry registry = new SkillRegistry();
        registry.loadBuiltinSkills();

        // 创建 SkillTool
        SkillTool skillTool = new SkillTool(registry);

        // 查看 SkillTool 的基本信息
        log.info("SkillTool name: {}", skillTool.getName());
        log.info("SkillTool tool name constant: {}", SkillTool.TOOL_NAME);

        // 查看动态生成的 description（包含 <available_skills> 列表）
        String description = skillTool.getDescription();
        log.info("SkillTool dynamic description:\n{}", description);

        // 查看 tool schema（用于 LLM function calling）
        var schema = skillTool.getToolSchema();
        log.info("SkillTool schema keys: {}", schema.keySet());

        // 模拟 LLM 调用 SkillTool
        log.info("\nSimulating LLM calling SkillTool with command='code_review':");
        var result = skillTool.execute(java.util.Map.of("command", "code_review"));
        log.info("  success: {}", result.isSuccess());
        log.info("  data type: {}", result.getData() != null ? result.getData().getClass().getSimpleName() : "null");

        // 模拟调用不存在的 Skill
        log.info("\nSimulating LLM calling SkillTool with command='nonexistent':");
        var failResult = skillTool.execute(java.util.Map.of("command", "nonexistent"));
        log.info("  success: {}", failResult.isSuccess());
        log.info("  error: {}", failResult.getError());
    }

    /**
     * 示例 6: 完整工作流演示
     *
     * <p>展示 Skill 系统在实际场景中的完整使用流程：
     * <ol>
     *   <li>创建 SkillRegistry 并加载 Skill</li>
     *   <li>创建 SkillTool 并注册到工具集</li>
     *   <li>LLM 决定调用 Skill（通过 SkillTool）</li>
     *   <li>获取 SkillActivationResult</li>
     *   <li>注入上下文并预批准工具</li>
     *   <li>LLM 在 Skill 指导下完成任务</li>
     * </ol>
     */
    private static void fullWorkflowExample() {
        log.info("\n--- Example 6: Full Workflow Demo ---");

        // Step 1: 创建 SkillRegistry 并加载 Skill（内置 + 自定义）
        SkillRegistry registry = new SkillRegistry();
        int builtinCount = registry.loadBuiltinSkills();
        log.info("Step 1: Loaded {} built-in skills", builtinCount);

        // Step 2: 创建 SkillTool（会被注册到 LLM 的 tools 数组中）
        SkillTool skillTool = new SkillTool(registry);
        log.info("Step 2: Created SkillTool with {} discoverable skills",
                registry.getAllSkills().stream().filter(BaseSkill::isDiscoverable).count());

        // Step 3: 模拟 LLM 决定使用 write_test Skill
        log.info("Step 3: LLM decides to activate 'write_test' skill");
        var toolResult = skillTool.execute(java.util.Map.of("command", "write_test"));

        if (toolResult.isSuccess()) {
            // Step 4: 从 SkillRegistry 获取 SkillActivationResult
            SkillActivationResult activation = registry.activateSkill("write_test");
            log.info("Step 4: Got SkillActivationResult");
            log.info("  - Metadata: {}", activation.getMetadataMessage());
            log.info("  - Prompt: {} chars of expert instructions", 
                    activation.getSkillPrompt() != null ? activation.getSkillPrompt().length() : 0);
            log.info("  - Allowed tools: {}", activation.getAllowedTools());

            // Step 5: 在 CodingAgent 中，这些会被自动处理：
            log.info("Step 5: Context injection (handled by CodingAgent):");
            log.info("  - User sees: '✨ {}'", activation.getMetadataMessage());
            log.info("  - LLM receives: {} chars of hidden expert prompt", 
                    activation.getSkillPrompt() != null ? activation.getSkillPrompt().length() : 0);
            log.info("  - PermissionManager pre-approves: {}", activation.getAllowedTools());

            // Step 6: LLM 在 Skill 指导下完成任务
            log.info("Step 6: LLM now has expert instructions and can use pre-approved tools");
            log.info("  The LLM will follow the injected prompt to write tests systematically.");
        }

        log.info("\n=== Skill System Examples Complete ===");
    }
}
