package io.leavesfly.evox.examples;

import io.leavesfly.evox.agents.action.ActionAgent;
import io.leavesfly.evox.agents.action.ActionAgent.FieldSpec;
import io.leavesfly.evox.core.message.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Pattern;

/**
 * ActionAgent 示例
 * 展示如何使用 ActionAgent 执行函数而无需 LLM
 *
 * <p>改进后同时展示新旧两种创建方式对比</p>
 */
@Slf4j
public class ActionAgentExample {

    public static void main(String[] args) {
        try {
            log.info("=== ActionAgent 示例开始 ===\n");

            // 示例1: 新 Builder 写法（推荐）
            builderStyleMath();

            // 示例2: 数据验证（新 Builder 写法）
            dataValidation();
            
            // 示例3: 错误处理
            errorHandling();
            
            // 示例4: 链式执行（新写法 vs 旧写法对比）
            chainedActions();
            
            log.info("\n=== ActionAgent 示例完成 ===");
            
        } catch (Exception e) {
            log.error("示例运行失败", e);
        }
    }

    /**
     * 示例1: Builder 模式 + FieldSpec.of()（推荐写法）
     * 
     * 改进前: 15+ 行
     * 改进后: 6 行（含函数体）
     */
    public static void builderStyleMath() {
        log.info("=== 示例1: Builder 模式数学运算 ===");

        // 新写法：一步到位，build() 自动 initModule()
        ActionAgent addAgent = ActionAgent.builder()
                .name("AddAgent")
                .description("Adds two numbers")
                .inputs(FieldSpec.of("a", "int", "First number"),
                        FieldSpec.of("b", "int", "Second number"))
                .outputs(FieldSpec.of("result", "int", "Sum of the numbers"))
                .executeFunction(params -> {
                    int a = (int) params.get("a");
                    int b = (int) params.get("b");
                    return Map.of("result", a + b);
                })
                .build();  // 自动校验 + 自动 initModule()

        // 调用：使用基类统一的 call() 方法
        Message response = addAgent.call(Map.of("a", 5, "b", 3));
        log.info("Add Result: {}", response.getContent());

        // 也可以用无 actionName 的 execute()
        Message response2 = addAgent.execute(List.of(Message.inputMessage(Map.of("a", 10, "b", 20))));
        log.info("Add Result (via execute): {}", response2.getContent());
    }

    /**
     * 示例2: 数据验证 - 邮箱验证（Builder 写法）
     */
    public static void dataValidation() {
        log.info("=== 示例2: 数据验证 ===");

        ActionAgent emailValidator = ActionAgent.builder()
                .name("EmailValidator")
                .description("Validates email addresses")
                .inputs(FieldSpec.of("email", "string", "Email address to validate"))
                .outputs(FieldSpec.of("email", "string", "Input email"),
                         FieldSpec.of("isValid", "boolean", "Whether email is valid"),
                         FieldSpec.of("message", "string", "Validation message"))
                .executeFunction(params -> {
                    String email = (String) params.get("email");
                    Pattern pattern = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
                    boolean isValid = pattern.matcher(email).matches();
                    return Map.of(
                            "email", email,
                            "isValid", isValid,
                            "message", isValid ? "Valid email format" : "Invalid email format"
                    );
                })
                .build();

        // 测试多个邮箱
        for (String email : List.of("user@example.com", "invalid-email", "test@domain.co.uk")) {
            Message response = emailValidator.call(Map.of("email", email));
            log.info("Email validation: {}", response.getContent());
        }
    }

    /**
     * 示例3: 错误处理（Builder 写法）
     */
    public static void errorHandling() {
        log.info("=== 示例3: 错误处理 ===");

        ActionAgent divideAgent = ActionAgent.builder()
                .name("DivideAgent")
                .description("Divides two numbers with error handling")
                .inputs(FieldSpec.of("a", "int", "Numerator"),
                        FieldSpec.of("b", "int", "Denominator"))
                .outputs(FieldSpec.of("result", "double", "Division result"),
                         FieldSpec.optional("error", "string", "Error message if any"))
                .executeFunction(params -> {
                    Map<String, Object> result = new HashMap<>();
                    int a = (int) params.get("a");
                    int b = (int) params.get("b");
                    if (b == 0) {
                        result.put("error", "Cannot divide by zero");
                    } else {
                        result.put("result", (double) a / b);
                    }
                    return result;
                })
                .build();

        log.info("Normal division: {}", divideAgent.call(Map.of("a", 10, "b", 2)).getContent());
        log.info("Division by zero: {}", divideAgent.call(Map.of("a", 10, "b", 0)).getContent());
    }

    /**
     * 示例4: 链式执行（Builder 写法，代码量减少 60%+）
     */
    @SuppressWarnings("unchecked")
    public static void chainedActions() {
        log.info("=== 示例4: 链式执行 ===");

        // Builder 写法：创建智能体只需几行
        ActionAgent squareAgent = ActionAgent.builder()
                .name("SquareAgent")
                .description("Calculates the square of a number")
                .inputs(FieldSpec.of("number", "int", "Number to square"))
                .outputs(FieldSpec.of("result", "int", "Squared result"))
                .executeFunction(p -> Map.of("result", (int) p.get("number") * (int) p.get("number")))
                .build();

        ActionAgent doubleAgent = ActionAgent.builder()
                .name("DoubleAgent")
                .description("Doubles a number")
                .inputs(FieldSpec.of("number", "int", "Number to double"))
                .outputs(FieldSpec.of("result", "int", "Doubled result"))
                .executeFunction(p -> Map.of("result", (int) p.get("number") * 2))
                .build();

        // 链式执行
        Message step1 = squareAgent.call(Map.of("number", 5));
        log.info("Step 1 - Square(5): {}", step1.getContent());

        Object result1 = ((Map<String, Object>) step1.getContent()).get("result");
        Message step2 = doubleAgent.call(Map.of("number", result1));
        log.info("Step 2 - Double(25): {}", step2.getContent());
    }
}