package io.leavesfly.evox.tools.base;

import io.leavesfly.evox.core.module.BaseModule;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具集 - 管理多个工具的集合
 * 对应 Python 版本的 Toolkit
 */
@Slf4j
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Toolkit extends BaseModule {

    /**
     * 工具集名称
     */
    private String name;

    /**
     * 工具集描述
     */
    private String description;

    /**
     * 工具列表
     */
    private List<BaseTool> tools;

    /**
     * 工具名称索引（用于快速查找）
     */
    private Map<String, BaseTool> toolIndex;

    public Toolkit(String name, String description) {
        this.name = name;
        this.description = description;
        this.tools = new ArrayList<>();
        this.toolIndex = new ConcurrentHashMap<>();
    }

    public Toolkit(String name, String description, List<BaseTool> tools) {
        this.name = name;
        this.description = description;
        this.tools = new ArrayList<>(tools);
        this.toolIndex = new ConcurrentHashMap<>();
        indexTools();
    }

    /**
     * 添加工具
     */
    public void addTool(BaseTool tool) {
        if (tool == null) {
            log.warn("Attempted to add null tool");
            return;
        }

        if (toolIndex.containsKey(tool.getName())) {
            log.warn("Tool already exists: {}, replacing...", tool.getName());
            removeTool(tool.getName());
        }

        tools.add(tool);
        toolIndex.put(tool.getName(), tool);
        log.info("Added tool: {}", tool.getName());
    }

    /**
     * 移除工具
     */
    public boolean removeTool(String toolName) {
        BaseTool tool = toolIndex.remove(toolName);
        if (tool != null) {
            tools.remove(tool);
            log.info("Removed tool: {}", toolName);
            return true;
        }
        log.warn("Tool not found: {}", toolName);
        return false;
    }

    /**
     * 获取工具
     */
    public BaseTool getTool(String toolName) {
        BaseTool tool = toolIndex.get(toolName);
        if (tool == null) {
            log.warn("Tool not found: {}", toolName);
        }
        return tool;
    }

    /**
     * 检查工具是否存在
     */
    public boolean hasTool(String toolName) {
        return toolIndex.containsKey(toolName);
    }

    /**
     * 获取所有工具
     */
    public List<BaseTool> getTools() {
        return new ArrayList<>(tools);
    }

    /**
     * 获取所有工具名称
     */
    public List<String> getToolNames() {
        return tools.stream()
                .map(BaseTool::getName)
                .toList();
    }

    /**
     * 获取所有工具描述
     */
    public List<String> getToolDescriptions() {
        return tools.stream()
                .map(BaseTool::getDescription)
                .toList();
    }

    /**
     * 获取所有工具的 Schema（用于 LLM function calling）
     */
    public List<Map<String, Object>> getToolSchemas() {
        return tools.stream()
                .map(BaseTool::getToolSchema)
                .toList();
    }

    /**
     * 执行指定工具
     */
    public BaseTool.ToolResult executeTool(String toolName, Map<String, Object> parameters) {
        BaseTool tool = getTool(toolName);
        if (tool == null) {
            return BaseTool.ToolResult.failure("Tool not found: " + toolName);
        }

        try {
            log.debug("Executing tool: {} with parameters: {}", toolName, parameters);
            BaseTool.ToolResult result = tool.execute(parameters);
            log.debug("Tool execution completed: {}", toolName);
            return result;
        } catch (Exception e) {
            log.error("Tool execution failed: {}", toolName, e);
            return BaseTool.ToolResult.failure("Execution failed: " + e.getMessage());
        }
    }

    /**
     * 获取工具数量
     */
    public int getToolCount() {
        return tools.size();
    }

    /**
     * 清空所有工具
     */
    public void clear() {
        tools.clear();
        toolIndex.clear();
        log.info("Cleared all tools from toolkit: {}", name);
    }

    /**
     * 重建工具索引
     */
    private void indexTools() {
        toolIndex.clear();
        for (BaseTool tool : tools) {
            toolIndex.put(tool.getName(), tool);
        }
        log.debug("Indexed {} tools", toolIndex.size());
    }

    /**
     * 获取工具统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("toolkit_name", name);
        stats.put("tool_count", tools.size());
        stats.put("tool_names", getToolNames());

        return stats;
    }

    @Override
    public String toString() {
        return String.format("Toolkit{name='%s', description='%s', tools=%d}",
                name, description, tools.size());
    }
}
