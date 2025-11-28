package io.leavesfly.evox.workflow.operator;

import io.leavesfly.evox.core.module.BaseModule;
import io.leavesfly.evox.models.base.BaseLLM;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 算子基类 - 工作流中的可重用操作单元
 * 对应 Python 版本的 Operator
 */
@Slf4j
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class Operator extends BaseModule {

    /**
     * 算子名称
     */
    private String name;

    /**
     * 算子描述
     */
    private String description;

    /**
     * 算子接口定义（描述输入输出）
     */
    private String operatorInterface;

    /**
     * 提示词模板
     */
    private String prompt;

    /**
     * 使用的 LLM（可选）
     */
    private BaseLLM llm;

    @Override
    public void initModule() {
        if (name == null || name.isEmpty()) {
            name = this.getClass().getSimpleName();
        }
    }

    /**
     * 同步执行算子
     *
     * @param inputs 输入参数
     * @return 执行结果
     */
    public abstract Map<String, Object> execute(Map<String, Object> inputs);

    /**
     * 异步执行算子
     *
     * @param inputs 输入参数
     * @return Mono 包装的执行结果
     */
    public Mono<Map<String, Object>> executeAsync(Map<String, Object> inputs) {
        return Mono.fromCallable(() -> execute(inputs));
    }

    /**
     * 获取提示词
     */
    public String getPrompt(Map<String, Object> variables) {
        if (prompt == null) {
            return "";
        }

        String result = prompt;
        if (variables != null) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                result = result.replace(placeholder, String.valueOf(entry.getValue()));
            }
        }
        return result;
    }

    /**
     * 设置提示词
     */
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    /**
     * 验证输入参数
     */
    protected void validateInputs(Map<String, Object> inputs, String... requiredKeys) {
        if (inputs == null) {
            throw new IllegalArgumentException("Inputs cannot be null");
        }
        
        for (String key : requiredKeys) {
            if (!inputs.containsKey(key)) {
                throw new IllegalArgumentException("Required input missing: " + key);
            }
        }
    }

    /**
     * 构建输出结果
     */
    protected Map<String, Object> buildOutput(String key, Object value) {
        Map<String, Object> output = new HashMap<>();
        output.put(key, value);
        return output;
    }

    /**
     * 构建输出结果（多个键值对）
     */
    protected Map<String, Object> buildOutput(Map<String, Object> data) {
        return new HashMap<>(data);
    }
}
