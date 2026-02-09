package io.leavesfly.evox.cowork.template;

import lombok.Data;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Data
public class WorkflowTemplate {
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

    public String render(Map<String, String> variableValues) {
        String rendered = promptTemplate;
        
        // Extract variable names from promptTemplate
        List<String> requiredVariables = getVariableNames();
        
        // Check required variables
        for (TemplateVariable varDef : variables) {
            if (varDef.isRequired() && !variableValues.containsKey(varDef.getName())) {
                throw new IllegalArgumentException("Required variable '" + varDef.getName() + "' is missing");
            }
        }
        
        // Replace placeholders with actual values
        Pattern pattern = Pattern.compile("\\{\\{(\\w+)\\}\\}");
        Matcher matcher = pattern.matcher(rendered);
        StringBuilder result = new StringBuilder();
        
        while (matcher.find()) {
            String varName = matcher.group(1);
            String value = variableValues.get(varName);
            
            if (value == null) {
                // Find variable definition
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

    public List<String> getVariableNames() {
        List<String> variableNames = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\{\\{(\\w+)\\}\\}");
        Matcher matcher = pattern.matcher(promptTemplate);
        
        while (matcher.find()) {
            variableNames.add(matcher.group(1));
        }
        
        return variableNames.stream().distinct().collect(Collectors.toList());
    }

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
