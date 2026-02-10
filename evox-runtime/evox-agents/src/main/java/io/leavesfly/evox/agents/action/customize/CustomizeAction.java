package io.leavesfly.evox.agents.action.customize;

import io.leavesfly.evox.actions.base.Action;
import io.leavesfly.evox.actions.base.ActionInput;
import io.leavesfly.evox.actions.base.ActionOutput;

// BaseTool暂未实现,使用Object替代
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 自定义动作
 * 支持使用工具和自定义提示词的灵活动作
 * 
 * @author EvoX Team
 */
@Slf4j
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CustomizeAction extends Action {

    /**
     * 提示词模板
     */
    private String promptTemplate;

    /**
     * 解析模式: title, str, json, xml
     */
    private String parseMode = "str";

    /**
     * 可用工具列表
     */
    private List<Object> tools;

    /**
     * 最大工具调用次数
     */
    private int maxToolTry = 2;

    @Override
    public ActionOutput execute(ActionInput input) {
        try {
            // 准备提示词
            String prompt = preparePrompt(input);

            // 调用LLM
            String response = getLlm().generate(prompt);

            // 解析响应
            Object parsedOutput = parseResponse(response);

            // 返回结果
            ActionOutput output = new ActionOutput();
            output.setSuccess(true);
            output.setData(parsedOutput);
            
            return output;

        } catch (Exception e) {
            log.error("CustomizeAction execution failed", e);
            ActionOutput output = new ActionOutput();
            output.setSuccess(false);
            output.setError(e.getMessage());
            return output;
        }
    }

    /**
     * 准备提示词
     */
    private String preparePrompt(ActionInput input) {
        if (promptTemplate != null && !promptTemplate.isEmpty()) {
            // 使用模板替换变量
            String prompt = promptTemplate;
            Map<String, Object> params = input.getData();
            
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                String value = String.valueOf(entry.getValue());
                prompt = prompt.replace(placeholder, value);
            }
            
            // 如果有工具,添加工具描述
            if (tools != null && !tools.isEmpty()) {
                prompt += "\n\n" + generateToolsDescription();
            }
            
            return prompt;
        } else {
            // 直接使用输入作为提示词
            return String.valueOf(input.getData().getOrDefault("prompt", ""));
        }
    }

    /**
     * 生成工具描述
     */
    private String generateToolsDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("Available Tools:\n");
        
        for (Object tool : tools) {
            sb.append("- ").append(tool.toString()).append("\n");
        }
        
        return sb.toString();
    }

    /**
     * 解析响应
     */
    private Object parseResponse(String response) {
        switch (parseMode.toLowerCase()) {
            case "json":
                return parseJsonResponse(response);
            case "xml":
                return parseXmlResponse(response);
            case "title":
                return parseTitleResponse(response);
            default:
                return response;
        }
    }

    /**
     * 解析JSON响应
     */
    private Object parseJsonResponse(String response) {
        // 简化实现：提取JSON块
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        
        return response;
    }

    /**
     * 解析XML响应
     */
    private Object parseXmlResponse(String response) {
        // 简化实现：返回原始响应
        return response;
    }

    /**
     * 解析标题格式响应
     */
    private Object parseTitleResponse(String response) {
        // 提取## 标题下的内容
        Map<String, String> sections = new HashMap<>();
        String[] lines = response.split("\n");
        String currentTitle = null;
        StringBuilder currentContent = new StringBuilder();
        
        for (String line : lines) {
            if (line.startsWith("## ")) {
                if (currentTitle != null) {
                    sections.put(currentTitle, currentContent.toString().trim());
                }
                currentTitle = line.substring(3).trim();
                currentContent = new StringBuilder();
            } else if (currentTitle != null) {
                currentContent.append(line).append("\n");
            }
        }
        
        if (currentTitle != null) {
            sections.put(currentTitle, currentContent.toString().trim());
        }
        
        return sections.isEmpty() ? response : sections;
    }

    @Override
    public String[] getInputFields() {
        return new String[]{"prompt", "inputs"};
    }

    @Override
    public String[] getOutputFields() {
        return new String[]{"response", "data"};
    }

    /**
     * 添加工具
     */
    public void addTool(Object tool) {
        if (tools == null) {
            tools = new ArrayList<>();
        }
        tools.add(tool);
        log.debug("Added tool to action: {}", getName());
    }

    /**
     * 添加多个工具
     */
    public void addTools(List<Object> tools) {
        if (tools != null) {
            for (Object tool : tools) {
                addTool(tool);
            }
        }
    }
}
