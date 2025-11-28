package io.leavesfly.evox.workflow.operator;

import io.leavesfly.evox.models.base.BaseLLM;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自洽性集成算子
 * 使用自洽性从多个方案中选择最一致的解决方案
 * 
 * @author EvoX Team
 */
@Slf4j
public class ScEnsembleOperator extends Operator {

    private static final String DEFAULT_PROMPT = """
            Given multiple solutions to a problem, please select the most consistent and reasonable one.
            
            Problem: {problem}
            
            Solutions:
            {solutions}
            
            Please analyze which solution appears most frequently or is most consistent.
            Format your response as:
            <thought>Your analysis of the solutions</thought>
            <solution_letter>The letter of the best solution (A, B, C, etc.)</solution_letter>
            """;

    public ScEnsembleOperator(BaseLLM llm) {
        this(llm, DEFAULT_PROMPT);
    }

    public ScEnsembleOperator(BaseLLM llm, String prompt) {
        this.setName("ScEnsemble");
        this.setDescription("Uses self-consistency to select the most consistent solution");
        this.setOperatorInterface("sc_ensemble(solutions: List<String>, problem: String) -> Map with key 'response'");
        this.setLlm(llm);
        this.setPrompt(prompt);
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> inputs) {
        validateInputs(inputs, "solutions");
        
        @SuppressWarnings("unchecked")
        List<String> solutions = (List<String>) inputs.get("solutions");
        String problem = inputs.containsKey("problem") ? 
                        String.valueOf(inputs.get("problem")) : "";
        
        if (solutions.isEmpty()) {
            throw new IllegalArgumentException("Solutions list cannot be empty");
        }
        
        // 准备方案映射
        Map<String, Integer> answerMapping = new HashMap<>();
        StringBuilder solutionText = new StringBuilder();
        
        for (int i = 0; i < solutions.size(); i++) {
            char letter = (char) ('A' + i);
            answerMapping.put(String.valueOf(letter), i);
            solutionText.append(letter).append(": \n")
                       .append(solutions.get(i)).append("\n\n");
        }
        
        // 格式化提示词
        Map<String, Object> variables = new HashMap<>();
        variables.put("problem", problem);
        variables.put("solutions", solutionText.toString());
        String prompt = getPrompt(variables);
        
        // 调用LLM生成响应
        String response = getLlm().generate(prompt);
        
        // 解析响应
        String solutionLetter = extractXmlTag(response, "solution_letter").trim().toUpperCase();
        
        // 获取对应的解决方案
        Integer index = answerMapping.get(solutionLetter);
        String selectedSolution;
        
        if (index != null && index < solutions.size()) {
            selectedSolution = solutions.get(index);
        } else {
            log.warn("Invalid solution letter: {}, using first solution", solutionLetter);
            selectedSolution = solutions.get(0);
        }
        
        return buildOutput("response", selectedSolution);
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
        
        log.warn("Could not extract <{}> tag from response", tagName);
        return "";
    }
}
