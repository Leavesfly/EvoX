package io.leavesfly.evox.tools.api;

import io.leavesfly.evox.core.agent.IAgent;
import io.leavesfly.evox.tools.agent.AgentTool;
import io.leavesfly.evox.tools.base.BaseTool;
import io.leavesfly.evox.tools.database.DatabaseTool;
import io.leavesfly.evox.tools.file.FileSystemTool;
import io.leavesfly.evox.tools.http.HttpTool;
import io.leavesfly.evox.tools.json.JsonTool;
import io.leavesfly.evox.tools.search.WebSearchTool;
import io.leavesfly.evox.tools.search.WikipediaSearchTool;
import io.leavesfly.evox.tools.search.ArxivSearchTool;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具注册表
 * 集中管理和获取所有可用工具
 *
 * <p>支持两种使用方式：</p>
 * <ul>
 *   <li><b>Spring 环境</b>：通过 {@code new ToolRegistry()} 创建实例并注册为 Spring Bean</li>
 *   <li><b>非 Spring 环境</b>：通过 {@link #getInstance()} 获取全局单例</li>
 * </ul>
 * 
 * @author EvoX Team
 */
@Slf4j
public class ToolRegistry {

    /**
     * 工具映射表
     */
    private final Map<String, BaseTool> tools = new ConcurrentHashMap<>();
    
    /**
     * 工具分类
     */
    private final Map<String, List<String>> categories = new ConcurrentHashMap<>();
    
    /**
     * 全局单例实例（仅用于非 Spring 环境）
     */
    private static volatile ToolRegistry instance;
    
    /**
     * 创建一个新的 ToolRegistry 实例
     * <p>Spring 环境下推荐直接 new 并注册为 Bean；
     * 非 Spring 环境使用 {@link #getInstance()} 即可。</p>
     */
    public ToolRegistry() {
        // 初始化分类
        categories.put("api", new ArrayList<>());
        categories.put("search", new ArrayList<>());
        categories.put("file", new ArrayList<>());
        categories.put("data", new ArrayList<>());
        categories.put("utility", new ArrayList<>());
        categories.put("agent", new ArrayList<>());
    }
    
    /**
     * 获取全局单例实例（非 Spring 环境使用）
     *
     * <p>在 Spring 环境中，建议通过依赖注入获取 ToolRegistry Bean，
     * 而非调用此方法，以保证与 Spring 生命周期一致。</p>
     */
    public static ToolRegistry getInstance() {
        if (instance == null) {
            synchronized (ToolRegistry.class) {
                if (instance == null) {
                    instance = new ToolRegistry();
                }
            }
        }
        return instance;
    }
    
    /**
     * 重置全局单例（仅用于测试隔离）
     */
    public static void resetInstance() {
        synchronized (ToolRegistry.class) {
            instance = null;
        }
    }
    
    /**
     * 注册工具
     * 
     * @param tool 工具实例
     * @param category 工具分类
     */
    public void register(BaseTool tool, String category) {
        if (tool == null || tool.getName() == null) {
            log.warn("无法注册空工具或无名称的工具");
            return;
        }
        
        tools.put(tool.getName(), tool);
        
        if (category != null && !category.isEmpty()) {
            categories.computeIfAbsent(category, k -> new ArrayList<>()).add(tool.getName());
        }
        
        log.debug("注册工具: {} (分类: {})", tool.getName(), category);
    }
    
    /**
     * 注册工具（无分类）
     */
    public void register(BaseTool tool) {
        register(tool, "utility");
    }
    
    /**
     * 将智能体注册为工具（Subagent as Tool）
     * 
     * @param agent 要注册为工具的智能体
     * @return 创建的 AgentTool 实例
     */
    public AgentTool registerAgent(IAgent agent) {
        AgentTool agentTool = AgentTool.wrap(agent);
        register(agentTool, "agent");
        return agentTool;
    }
    
    /**
     * 将智能体注册为工具（自定义名称和描述）
     * 
     * @param agent       要注册为工具的智能体
     * @param toolName    工具名称
     * @param description 工具描述
     * @return 创建的 AgentTool 实例
     */
    public AgentTool registerAgent(IAgent agent, String toolName, String description) {
        AgentTool agentTool = AgentTool.wrap(agent, toolName, description);
        register(agentTool, "agent");
        return agentTool;
    }
    
    /**
     * 获取工具
     * 
     * @param name 工具名称
     * @return 工具实例，如果不存在返回null
     */
    public BaseTool get(String name) {
        return tools.get(name);
    }
    
    /**
     * 获取所有工具
     */
    public Collection<BaseTool> getAllTools() {
        return tools.values();
    }
    
    /**
     * 获取所有工具名称
     */
    public Set<String> getAllToolNames() {
        return tools.keySet();
    }
    
    /**
     * 按分类获取工具
     */
    public List<BaseTool> getToolsByCategory(String category) {
        List<String> toolNames = categories.get(category);
        if (toolNames == null) {
            return Collections.emptyList();
        }
        
        List<BaseTool> result = new ArrayList<>();
        for (String name : toolNames) {
            BaseTool tool = tools.get(name);
            if (tool != null) {
                result.add(tool);
            }
        }
        return result;
    }
    
    /**
     * 获取所有分类
     */
    public Set<String> getCategories() {
        return categories.keySet();
    }
    
    /**
     * 检查工具是否存在
     */
    public boolean exists(String name) {
        return tools.containsKey(name);
    }
    
    /**
     * 移除工具
     */
    public BaseTool remove(String name) {
        BaseTool removed = tools.remove(name);
        
        // 从分类中移除
        for (List<String> toolNames : categories.values()) {
            toolNames.remove(name);
        }
        
        return removed;
    }
    
    /**
     * 清空所有工具
     */
    public void clear() {
        tools.clear();
        for (List<String> toolNames : categories.values()) {
            toolNames.clear();
        }
    }
    
    /**
     * 获取工具数量
     */
    public int size() {
        return tools.size();
    }
    
    /**
     * 创建默认工具集
     * 包含所有内置工具的预配置实例
     */
    public static ToolRegistry createDefault() {
        ToolRegistry registry = new ToolRegistry();
        
        // API工具
        registry.register(new WeatherTool(), "api");
        registry.register(new DateTimeTool(), "api");
        registry.register(new TranslationTool(), "api");
        
        // 搜索工具
        registry.register(new WebSearchTool(), "search");
        registry.register(new WikipediaSearchTool(), "search");
        registry.register(new ArxivSearchTool(), "search");
        
        // 文件工具
        registry.register(new FileSystemTool(), "file");
        
        // 数据工具
        registry.register(new HttpTool(), "data");
        registry.register(new JsonTool(), "data");
        registry.register(new DatabaseTool(), "data");
        
        // 注意: CalculatorTool 没有继承 BaseTool，需要单独使用
        
        log.info("创建默认工具注册表，共 {} 个工具", registry.size());
        
        return registry;
    }
    
    /**
     * 创建轻量级工具集（只包含无需外部依赖的工具）
     */
    public static ToolRegistry createLightweight() {
        ToolRegistry registry = new ToolRegistry();
        
        // 只添加不需要网络请求的工具
        registry.register(new DateTimeTool(), "api");
        registry.register(new JsonTool(), "data");
        registry.register(new FileSystemTool(), "file");
        // 注意: CalculatorTool 没有继承 BaseTool，需要单独使用
        
        log.info("创建轻量级工具注册表，共 {} 个工具", registry.size());
        
        return registry;
    }
    
    /**
     * 获取工具描述列表（用于展示）
     */
    public List<Map<String, String>> getToolDescriptions() {
        List<Map<String, String>> descriptions = new ArrayList<>();
        
        for (BaseTool tool : tools.values()) {
            Map<String, String> desc = new HashMap<>();
            desc.put("name", tool.getName());
            desc.put("description", tool.getDescription());
            descriptions.add(desc);
        }
        
        return descriptions;
    }
    
    /**
     * 生成工具列表的Markdown文档
     */
    public String generateMarkdownDoc() {
        StringBuilder sb = new StringBuilder();
        sb.append("# 可用工具列表\n\n");
        
        for (String category : categories.keySet()) {
            List<BaseTool> categoryTools = getToolsByCategory(category);
            if (categoryTools.isEmpty()) continue;
            
            sb.append("## ").append(getCategoryDisplayName(category)).append("\n\n");
            
            for (BaseTool tool : categoryTools) {
                sb.append("### ").append(tool.getName()).append("\n");
                sb.append(tool.getDescription()).append("\n\n");
                
                if (tool.getInputs() != null && !tool.getInputs().isEmpty()) {
                    sb.append("**参数:**\n");
                    for (Map.Entry<String, Map<String, String>> entry : tool.getInputs().entrySet()) {
                        String paramName = entry.getKey();
                        Map<String, String> paramDef = entry.getValue();
                        sb.append("- `").append(paramName).append("` (").append(paramDef.get("type")).append("): ");
                        sb.append(paramDef.get("description")).append("\n");
                    }
                    sb.append("\n");
                }
            }
        }
        
        return sb.toString();
    }
    
    /**
     * 获取分类显示名称
     */
    private String getCategoryDisplayName(String category) {
        return switch (category) {
            case "api" -> "API集成工具";
            case "search" -> "搜索工具";
            case "file" -> "文件工具";
            case "data" -> "数据工具";
            case "utility" -> "实用工具";
            case "agent" -> "智能体工具";
            default -> category;
        };
    }
}
