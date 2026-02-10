package io.leavesfly.evox.cowork.template;

import lombok.Data;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Data
public class WorkflowTemplate {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    private String templateId;
    private String name;
    private String description;
    private String category;
    private String promptTemplate;
    private List<TemplateVariable> variables;
    private List<String> tags;
    private long createdAt;
    private long updatedAt;
    private int usageCount;

    public WorkflowTemplate() {
        this.templateId = UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.usageCount = 0;
        this.variables = new ArrayList<>();
        this.tags = new ArrayList<>();
    }

    // 渲染模板：使用提供的变量值替换占位符
    public String render(Map<String, String> variableValues) {
        String rendered = promptTemplate;
        
        // Extract variable names from promptTemplate
        // 从模板中提取变量名
        List<String> requiredVariables = getVariableNames();
        
        // Check required variables
        // 检查必需变量是否存在
        for (TemplateVariable varDef : variables) {
            if (varDef.isRequired() && !variableValues.containsKey(varDef.getName())) {
                throw new IllegalArgumentException("Required variable '" + varDef.getName() + "' is missing");
            }
        }
        
        // Replace placeholders with actual values
        // 替换占位符为实际值
        Matcher matcher = VARIABLE_PATTERN.matcher(rendered);
        StringBuilder result = new StringBuilder();
        
        while (matcher.find()) {
            String varName = matcher.group(1);
            String value = variableValues.get(varName);
            
            if (value == null) {
                // Find variable definition
                // 查找变量定义
                TemplateVariable varDef = variables.stream()
                    .filter(v -> v.getName().equals(varName))
                    .findFirst()
                    .orElse(null);
                
                if (varDef != null && varDef.getDefaultValue() != null) {
                    value = varDef.getDefaultValue();
                } else {
                    value = "{{" + varName + "}}";
                }
            }
            
            matcher.appendReplacement(result, value);
        }
        matcher.appendTail(result);
        
        return result.toString();
    }

    // 提取模板中所有变量名
    public List<String> getVariableNames() {
        List<String> variableNames = new ArrayList<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(promptTemplate);
        
        while (matcher.find()) {
            variableNames.add(matcher.group(1));
        }
        
        return variableNames.stream().distinct().collect(Collectors.toList());
    }

    // 增加使用计数
    public void incrementUsage() {
        this.usageCount++;
        this.updatedAt = System.currentTimeMillis();
    }

    @Data
    public static class TemplateVariable {
        private String name;
        private String description;
        private String defaultValue;
        private boolean required;
    }
}