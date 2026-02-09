package io.leavesfly.evox.agents.skill.builtin;

import io.leavesfly.evox.agents.skill.BaseSkill;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 编写测试技能
 * 为指定代码自动生成单元测试，覆盖正常路径、边界条件和异常场景
 */
@Slf4j
public class WriteTestSkill extends BaseSkill {

    public WriteTestSkill() {
        setName("write_test");
        setDescription("Generate comprehensive unit tests for the specified code. "
                + "Covers normal paths, edge cases, error handling, and boundary conditions. "
                + "Follows the project's existing test framework and conventions.");

        setSystemPrompt(buildWriteTestSystemPrompt());

        setRequiredTools(List.of("file_system", "file_edit", "grep", "glob"));

        Map<String, Map<String, String>> inputParams = new LinkedHashMap<>();

        Map<String, String> targetParam = new HashMap<>();
        targetParam.put("type", "string");
        targetParam.put("description", "File path or class/method name to generate tests for");
        inputParams.put("target", targetParam);

        Map<String, String> frameworkParam = new HashMap<>();
        frameworkParam.put("type", "string");
        frameworkParam.put("description", "Test framework to use: 'junit5', 'junit4', 'testng', 'pytest', 'jest', or 'auto' (default: 'auto')");
        inputParams.put("framework", frameworkParam);

        Map<String, String> coverageParam = new HashMap<>();
        coverageParam.put("type", "string");
        coverageParam.put("description", "Coverage level: 'basic' (happy path only), 'standard' (+ edge cases), 'comprehensive' (+ error handling, concurrency) (default: 'standard')");
        inputParams.put("coverage", coverageParam);

        setInputParameters(inputParams);
        setRequiredInputs(List.of("target"));
    }

    @Override
    public SkillResult execute(SkillContext context) {
        validateInputs(context.getParameters());

        String target = context.getInput();
        String framework = context.getParameters().getOrDefault("framework", "auto").toString();
        String coverage = context.getParameters().getOrDefault("coverage", "standard").toString();

        String prompt = buildPrompt(target, context.getAdditionalContext());

        log.info("Write test skill executing: target={}, framework={}, coverage={}", target, framework, coverage);

        String testPrompt = prompt
                + "\n\nTest framework: " + framework
                + "\nCoverage level: " + coverage
                + "\n\nPlease generate tests following these guidelines:\n"
                + "1. **Test Structure**: Use Arrange-Act-Assert (AAA) pattern\n"
                + "2. **Naming**: Use descriptive test method names that explain the scenario\n"
                + "3. **Coverage**:\n"
                + "   - Normal/happy path scenarios\n"
                + "   - Boundary conditions (null, empty, max values)\n"
                + "   - Error/exception handling\n"
                + "   - Edge cases specific to the business logic\n"
                + "4. **Mocking**: Use appropriate mocking for external dependencies\n"
                + "5. **Assertions**: Use specific assertions, not just assertTrue/assertFalse\n"
                + "6. **Independence**: Each test should be independent and idempotent";

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("skillName", "write_test");
        metadata.put("target", target);
        metadata.put("framework", framework);
        metadata.put("coverage", coverage);

        return SkillResult.success(testPrompt, metadata);
    }

    private String buildWriteTestSystemPrompt() {
        return """
                You are an expert test engineer who writes thorough, maintainable unit tests.
                
                When writing tests, you should:
                1. Analyze the source code to understand all code paths and branches
                2. Identify all public methods and their contracts
                3. Generate tests for normal operation, edge cases, and error conditions
                4. Use the project's existing test framework and conventions
                5. Create meaningful test data and fixtures
                6. Mock external dependencies appropriately
                7. Write clear, descriptive test names that document behavior
                8. Ensure tests are independent and can run in any order
                
                Test naming convention: should_[expectedBehavior]_when_[condition]
                
                Always include:
                - At least one test per public method
                - Null/empty input tests where applicable
                - Boundary value tests for numeric parameters
                - Exception/error handling tests
                - Integration points verification with mocks""";
    }
}
