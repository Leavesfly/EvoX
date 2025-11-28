package io.leavesfly.evox.workflow.operator;

import io.leavesfly.evox.models.base.BaseLLM;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 答案生成算子
 * 生成逐步推理的思考过程和最终答案
 * 
 * @author EvoX Team
 */
@Slf4j
public class AnswerGenerateOperator extends Operator {

    private static final String DEFAULT_PROMPT = """
            Please solve the following problem step by step.
            Provide your detailed thinking process and final answer.
            
            Problem: {input}
            
            Format your response as:
            <thought>Your step-by-step thinking process</thought>
            <answer>Your final answer</answer>
            """;

    public AnswerGenerateOperator(BaseLLM llm) {
        this(llm, DEFAULT_PROMPT);
    }

    public AnswerGenerateOperator(BaseLLM llm, String prompt) {
        this.setName("AnswerGenerate");
        this.setDescription("Generate step by step thinking and final answer");
        this.setOperatorInterface("answer_generate(input: String) -> Map with keys 'thought' and 'answer'");
        this.setLlm(llm);
        this.setPrompt(prompt);
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> inputs) {
        validateInputs(inputs, "input");
        
        String input = String.valueOf(inputs.get("input"));
        
        // 格式化提示词
        Map<String, Object> variables = Map.of("input", input);
        String prompt = getPrompt(variables);
        
        // 调用LLM生成响应
        String response = getLlm().generate(prompt);
        
        // 解析XML格式的响应
        String thought = extractXmlTag(response, "thought");
        String answer = extractXmlTag(response, "answer");
        
        Map<String, Object> output = new HashMap<>();
        output.put("thought", thought);
        output.put("answer", answer);
        
        return output;
    }

    /**
     * 从响应中提取XML标签内容
     */
    private String extractXmlTag(String response, String tagName) {
        Pattern pattern = Pattern.compile("<" + tagName + ">(.+?)</" + tagName + ">", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);
        
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        // 如果没有找到标签，返回空字符串
        log.warn("Could not extract <{}> tag from response", tagName);
        return "";
    }
}
