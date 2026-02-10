package io.leavesfly.evox.workflow.operator;

import io.leavesfly.evox.core.llm.ILLM;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 自定义算子
 * 基于自定义指令生成响应
 * 
 * @author EvoX Team
 */
@Slf4j
public class CustomOperator extends Operator {

    public CustomOperator(ILLM llm) {
        this.setName("Custom");
        this.setDescription("Generates anything based on customized input and instruction");
        this.setOperatorInterface("custom(input: String, instruction: String) -> Map with key 'response'");
        this.setLlm(llm);
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> inputs) {
        validateInputs(inputs, "input", "instruction");
        
        String input = String.valueOf(inputs.get("input"));
        String instruction = String.valueOf(inputs.get("instruction"));
        
        // 构建提示词
        String prompt = instruction + "\n\n" + input;
        
        // 调用LLM生成响应
        String response = getLlm().generate(prompt);
        
        return buildOutput("response", response);
    }
}
