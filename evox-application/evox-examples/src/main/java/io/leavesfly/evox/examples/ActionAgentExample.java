package io.leavesfly.evox.examples;

import io.leavesfly.evox.agents.action.ActionAgent;
import io.leavesfly.evox.core.message.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * ActionAgent 示例
 * 展示如何使用 ActionAgent 执行函数而无需 LLM
 */
@Slf4j
public class ActionAgentExample {

    public static void main(String[] args) {
        try {
            log.info("=== ActionAgent 示例开始 ===\n");
            
            // 示例1: 基本数学运算
            basicMathOperations();
            
            // 示例2: 数据验证
            dataValidation();
            
            // 示例3: 错误处理
            errorHandling();
            
            // 示例4: 链式执行
            chainedActions();
            
            log.info("\n=== ActionAgent 示例完成 ===");
            
        } catch (Exception e) {
            log.error("示例运行失败", e);
        }
    }

    /**
     * 示例1: 基本数学运算 - 加法
     */
    public static void basicMathOperations() {
        log.info("=== 示例1: 基本数学运算 ===");
        
        // 定义输入
        List<ActionAgent.FieldSpec> inputs = new ArrayList<>();
        inputs.add(new ActionAgent.FieldSpec("a", "int", "First number"));
        inputs.add(new ActionAgent.FieldSpec("b", "int", "Second number"));
        
        // 定义输出
        List<ActionAgent.FieldSpec> outputs = new ArrayList<>();
        outputs.add(new ActionAgent.FieldSpec("result", "int", "Sum of the numbers"));
        
        // 创建加法智能体
        ActionAgent addAgent = new ActionAgent();
        addAgent.setName("AddAgent");
        addAgent.setDescription("Adds two numbers");
        addAgent.setInputs(inputs);
        addAgent.setOutputs(outputs);
        addAgent.setExecuteFunction((Map<String, Object> params) -> {
            int a = (int) params.get("a");
            int b = (int) params.get("b");
            Map<String, Object> result = new HashMap<>();
            result.put("result", a + b);
            return result;
        });
        addAgent.initModule();
        
        // 执行
        Map<String, Object> inputValues = new HashMap<>();
        inputValues.put("a", 5);
        inputValues.put("b", 3);
        
        Message response = addAgent.call(inputValues);
        log.info("Add Result: {}", response.getContent());
    }

    /**
     * 示例2: 数据验证 - 邮箱验证
     */
    public static void dataValidation() {
        log.info("=== 示例2: 数据验证 ===");

        // 定义输入
        List<ActionAgent.FieldSpec> inputs = new ArrayList<>();
        inputs.add(new ActionAgent.FieldSpec("email", "string", "Email address to validate", true));

        // 定义输出
        List<ActionAgent.FieldSpec> outputs = new ArrayList<>();
        outputs.add(new ActionAgent.FieldSpec("email", "string", "Input email"));
        outputs.add(new ActionAgent.FieldSpec("isValid", "boolean", "Whether email is valid"));
        outputs.add(new ActionAgent.FieldSpec("message", "string", "Validation message"));

        // 创建邮箱验证智能体
        ActionAgent emailValidator = new ActionAgent();
        emailValidator.setName("EmailValidator");
        emailValidator.setDescription("Validates email addresses");
        emailValidator.setInputs(inputs);
        emailValidator.setOutputs(outputs);
        emailValidator.setExecuteFunction((Map<String, Object> params) -> {
            String email = (String) params.get("email");
            Pattern pattern = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
            boolean isValid = pattern.matcher(email).matches();

            Map<String, Object> result = new HashMap<>();
            result.put("email", email);
            result.put("isValid", isValid);
            result.put("message", isValid ? "Valid email format" : "Invalid email format");
            return result;
        });
        emailValidator.initModule();

        // 测试多个邮箱
        String[] testEmails = {"user@example.com", "invalid-email", "test@domain.co.uk"};

        for (String email : testEmails) {
            Map<String, Object> inputValues = new HashMap<>();
            inputValues.put("email", email);

            Message response = emailValidator.call(inputValues);
            log.info("Email validation: {}", response.getContent());
        }
    }

    /**
     * 示例3: 错误处理
     */
    public static void errorHandling() {
        log.info("=== 示例3: 错误处理 ===");

        // 定义输入
        List<ActionAgent.FieldSpec> inputs = new ArrayList<>();
        inputs.add(new ActionAgent.FieldSpec("a", "int", "Numerator"));
        inputs.add(new ActionAgent.FieldSpec("b", "int", "Denominator"));

        // 定义输出
        List<ActionAgent.FieldSpec> outputs = new ArrayList<>();
        outputs.add(new ActionAgent.FieldSpec("result", "double", "Division result"));
        outputs.add(new ActionAgent.FieldSpec("error", "string", "Error message if any"));

        // 创建除法智能体
        ActionAgent divideAgent = new ActionAgent();
        divideAgent.setName("DivideAgent");
        divideAgent.setDescription("Divides two numbers with error handling");
        divideAgent.setInputs(inputs);
        divideAgent.setOutputs(outputs);
        divideAgent.setExecuteFunction((Map<String, Object> params) -> {
            Map<String, Object> result = new HashMap<>();
            try {
                int a = (int) params.get("a");
                int b = (int) params.get("b");

                if (b == 0) {
                    result.put("error", "Cannot divide by zero");
                } else {
                    result.put("result", (double) a / b);
                }
            } catch (Exception e) {
                result.put("error", "Error: " + e.getMessage());
            }
            return result;
        });
        divideAgent.initModule();

        // 正常执行
        Map<String, Object> normalInput = new HashMap<>();
        normalInput.put("a", 10);
        normalInput.put("b", 2);
        Message normalResult = divideAgent.call(normalInput);
        log.info("Normal division: {}", normalResult.getContent());

        // 错误情况 - 除以零
        Map<String, Object> errorInput = new HashMap<>();
        errorInput.put("a", 10);
        errorInput.put("b", 0);
        Message errorResult = divideAgent.call(errorInput);
        log.info("Division by zero: {}", errorResult.getContent());
    }

    /**
     * 示例4: 链式执行
     */
    public static void chainedActions() {
        log.info("=== 示例4: 链式执行 ===");

        // 第一个智能体: 计算平方
        ActionAgent squareAgent = createSquareAgent();

        // 第二个智能体: 加倍
        ActionAgent doubleAgent = createDoubleAgent();

        // 链式执行: 先平方,再加倍
        Map<String, Object> input = new HashMap<>();
        input.put("number", 5);

        Message step1Result = squareAgent.call(input);
        log.info("Step 1 - Square: {}", step1Result.getContent());

        // 使用第一步的结果作为第二步的输入
        Map<String, Object> step2Input = new HashMap<>();
        step2Input.put("number", ((Map<String, Object>) step1Result.getContent()).get("result"));

        Message step2Result = doubleAgent.call(step2Input);
        log.info("Step 2 - Double: {}", step2Result.getContent());
    }

    /**
     * 创建平方智能体
     */
    private static ActionAgent createSquareAgent() {
        List<ActionAgent.FieldSpec> inputs = new ArrayList<>();
        inputs.add(new ActionAgent.FieldSpec("number", "int", "Number to square"));

        List<ActionAgent.FieldSpec> outputs = new ArrayList<>();
        outputs.add(new ActionAgent.FieldSpec("result", "int", "Squared result"));

        ActionAgent agent = new ActionAgent();
        agent.setName("SquareAgent");
        agent.setDescription("Calculates the square of a number");
        agent.setInputs(inputs);
        agent.setOutputs(outputs);
        agent.setExecuteFunction((Map<String, Object> params) -> {
            int number = (int) params.get("number");
            Map<String, Object> result = new HashMap<>();
            result.put("result", number * number);
            return result;
        });
        agent.initModule();
        return agent;
    }

    /**
     * 创建加倍智能体
     */
    private static ActionAgent createDoubleAgent() {
        List<ActionAgent.FieldSpec> inputs = new ArrayList<>();
        inputs.add(new ActionAgent.FieldSpec("number", "int", "Number to double"));

        List<ActionAgent.FieldSpec> outputs = new ArrayList<>();
        outputs.add(new ActionAgent.FieldSpec("result", "int", "Doubled result"));

        ActionAgent agent = new ActionAgent();
        agent.setName("DoubleAgent");
        agent.setDescription("Doubles a number");
        agent.setInputs(inputs);
        agent.setOutputs(outputs);
        agent.setExecuteFunction((Map<String, Object> params) -> {
            int number = (int) params.get("number");
            Map<String, Object> result = new HashMap<>();
            result.put("result", number * 2);
            return result;
        });
        agent.initModule();
        return agent;
    }
}