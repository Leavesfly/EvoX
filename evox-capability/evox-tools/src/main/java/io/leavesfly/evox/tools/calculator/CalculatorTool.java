package io.leavesfly.evox.tools.calculator;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.HashMap;
import java.util.Map;

/**
 * 计算器工具
 * 提供数学表达式计算功能
 *
 * @author EvoX Team
 */
@Slf4j
@Data
public class CalculatorTool {

    /**
     * 脚本引擎
     */
    private ScriptEngine engine;

    /**
     * 构造函数
     */
    public CalculatorTool() {
        ScriptEngineManager manager = new ScriptEngineManager();
        this.engine = manager.getEngineByName("JavaScript");
    }

    /**
     * 执行计算
     *
     * @param params 参数
     * @return 计算结果
     */
    public Map<String, Object> execute(Map<String, Object> params) {
        String expression = (String) params.get("expression");

        if (expression == null || expression.isEmpty()) {
            return error("Expression is required");
        }

        try {
            // 清理表达式,移除潜在的危险代码
            String cleanedExpression = sanitizeExpression(expression);
            
            // 执行计算
            Object result = engine.eval(cleanedExpression);
            
            log.info("Calculated: {} = {}", expression, result);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("expression", expression);
            response.put("result", result);
            response.put("resultType", result.getClass().getSimpleName());
            
            return response;
            
        } catch (Exception e) {
            log.error("Calculation failed: {}", e.getMessage());
            return error("Calculation failed: " + e.getMessage());
        }
    }

    /**
     * 基础运算
     */
    public Map<String, Object> add(double a, double b) {
        return calculate(a, b, "+");
    }

    public Map<String, Object> subtract(double a, double b) {
        return calculate(a, b, "-");
    }

    public Map<String, Object> multiply(double a, double b) {
        return calculate(a, b, "*");
    }

    public Map<String, Object> divide(double a, double b) {
        if (b == 0) {
            return error("Division by zero");
        }
        return calculate(a, b, "/");
    }

    /**
     * 高级数学函数
     */
    public Map<String, Object> power(double base, double exponent) {
        try {
            double result = Math.pow(base, exponent);
            return success(result);
        } catch (Exception e) {
            return error("Power calculation failed: " + e.getMessage());
        }
    }

    public Map<String, Object> sqrt(double value) {
        if (value < 0) {
            return error("Cannot calculate square root of negative number");
        }
        try {
            double result = Math.sqrt(value);
            return success(result);
        } catch (Exception e) {
            return error("Square root calculation failed: " + e.getMessage());
        }
    }

    public Map<String, Object> log(double value) {
        if (value <= 0) {
            return error("Logarithm of non-positive number");
        }
        try {
            double result = Math.log(value);
            return success(result);
        } catch (Exception e) {
            return error("Logarithm calculation failed: " + e.getMessage());
        }
    }

    public Map<String, Object> sin(double angle) {
        try {
            double result = Math.sin(Math.toRadians(angle));
            return success(result);
        } catch (Exception e) {
            return error("Sine calculation failed: " + e.getMessage());
        }
    }

    public Map<String, Object> cos(double angle) {
        try {
            double result = Math.cos(Math.toRadians(angle));
            return success(result);
        } catch (Exception e) {
            return error("Cosine calculation failed: " + e.getMessage());
        }
    }

    public Map<String, Object> tan(double angle) {
        try {
            double result = Math.tan(Math.toRadians(angle));
            return success(result);
        } catch (Exception e) {
            return error("Tangent calculation failed: " + e.getMessage());
        }
    }

    /**
     * 统计函数
     */
    public Map<String, Object> mean(double[] values) {
        if (values == null || values.length == 0) {
            return error("Empty array");
        }
        
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        double result = sum / values.length;
        return success(result);
    }

    public Map<String, Object> max(double[] values) {
        if (values == null || values.length == 0) {
            return error("Empty array");
        }
        
        double max = values[0];
        for (double value : values) {
            if (value > max) {
                max = value;
            }
        }
        return success(max);
    }

    public Map<String, Object> min(double[] values) {
        if (values == null || values.length == 0) {
            return error("Empty array");
        }
        
        double min = values[0];
        for (double value : values) {
            if (value < min) {
                min = value;
            }
        }
        return success(min);
    }

    /**
     * 清理表达式,移除危险字符
     */
    private String sanitizeExpression(String expression) {
        // 只允许数字、运算符和括号
        String cleaned = expression.replaceAll("[^0-9+\\-*/().\\s]", "");
        return cleaned;
    }

    /**
     * 执行二元运算
     */
    private Map<String, Object> calculate(double a, double b, String operator) {
        try {
            double result;
            switch (operator) {
                case "+":
                    result = a + b;
                    break;
                case "-":
                    result = a - b;
                    break;
                case "*":
                    result = a * b;
                    break;
                case "/":
                    result = a / b;
                    break;
                default:
                    return error("Unknown operator: " + operator);
            }
            
            log.info("Calculated: {} {} {} = {}", a, operator, b, result);
            return success(result);
            
        } catch (Exception e) {
            return error("Calculation failed: " + e.getMessage());
        }
    }

    /**
     * 构造成功响应
     */
    private Map<String, Object> success(double result) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("result", result);
        return response;
    }

    /**
     * 构造错误响应
     */
    private Map<String, Object> error(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return response;
    }
}
