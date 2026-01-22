package io.leavesfly.evox.prompt;

import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Prompt模板基类
 * 用于管理和渲染各种提示模板
 *
 * @author EvoX Team
 */
@Data
public class PromptTemplate {

    /**
     * 指令内容
     */
    private String instruction;

    /**
     * 上下文信息
     */
    private String context;

    /**
     * 约束条件
     */
    private List<String> constraints;

    /**
     * 示例列表
     */
    private List<Map<String, Object>> demonstrations;

    /**
     * 历史对话
     */
    private List<Map<String, String>> history;

    /**
     * 渲染指令部分
     */
    public String renderInstruction() {
        if (instruction == null || instruction.isEmpty()) {
            return "";
        }
        return "### 指令\n这是你必须遵循的主要任务指令：\n" + instruction + "\n";
    }

    /**
     * 渲染上下文部分
     */
    public String renderContext() {
        if (context == null || context.isEmpty()) {
            return "";
        }
        return "### 上下文\n以下是帮助你理解任务的背景信息：\n" + context + "\n";
    }

    /**
     * 渲染约束部分
     */
    public String renderConstraints() {
        if (constraints == null || constraints.isEmpty()) {
            return "";
        }
        String constraintsStr = constraints.stream()
                .map(c -> "- " + c)
                .collect(Collectors.joining("\n"));
        return "### 约束条件\n生成输出时你必须遵循以下规则或约束：\n" + constraintsStr + "\n";
    }

    /**
     * 渲染输出格式
     */
    public String renderOutputFormat(String format) {
        if (format == null || format.isEmpty()) {
            return "### 输出格式\n请生成最符合任务指令的响应。\n";
        }
        return "### 输出格式\n生成输出时你必须严格遵循以下格式：\n\n" + format + "\n";
    }

    /**
     * 格式化完整的提示
     */
    public String format(Map<String, Object> inputs, String outputFormat) {
        List<String> pieces = new ArrayList<>();

        pieces.add(renderInstruction());

        if (context != null && !context.isEmpty()) {
            pieces.add(renderContext());
        }

        if (constraints != null && !constraints.isEmpty()) {
            pieces.add(renderConstraints());
        }

        if (inputs != null && !inputs.isEmpty()) {
            pieces.add(renderInputs(inputs));
        }

        pieces.add(renderOutputFormat(outputFormat));

        return pieces.stream()
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.joining("\n"))
                .trim();
    }

    /**
     * 渲染输入部分
     */
    protected String renderInputs(Map<String, Object> inputs) {
        StringBuilder sb = new StringBuilder("### 输入\n以下是提供的输入值：\n");
        for (Map.Entry<String, Object> entry : inputs.entrySet()) {
            sb.append("[[ **").append(entry.getKey()).append("** ]]:\n");
            sb.append(entry.getValue()).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * 清理占位符
     */
    protected String clearPlaceholders(String text) {
        if (text == null) {
            return null;
        }
        // 简化版本: 将 {xx} 替换为 `xx`
        return text.replaceAll("\\{([^{}]+)\\}", "`$1`");
    }

    /**
     * 构建器模式
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String instruction;
        private String context;
        private List<String> constraints = new ArrayList<>();
        private List<Map<String, Object>> demonstrations = new ArrayList<>();
        private List<Map<String, String>> history = new ArrayList<>();

        public Builder instruction(String instruction) {
            this.instruction = instruction;
            return this;
        }

        public Builder context(String context) {
            this.context = context;
            return this;
        }

        public Builder constraint(String constraint) {
            this.constraints.add(constraint);
            return this;
        }

        public Builder constraints(List<String> constraints) {
            this.constraints = constraints;
            return this;
        }

        public Builder demonstration(Map<String, Object> demo) {
            this.demonstrations.add(demo);
            return this;
        }

        public Builder demonstrations(List<Map<String, Object>> demonstrations) {
            this.demonstrations = demonstrations;
            return this;
        }

        public Builder history(List<Map<String, String>> history) {
            this.history = history;
            return this;
        }

        public PromptTemplate build() {
            PromptTemplate template = new PromptTemplate();
            template.setInstruction(instruction);
            template.setContext(context);
            template.setConstraints(constraints);
            template.setDemonstrations(demonstrations);
            template.setHistory(history);
            return template;
        }
    }
}
