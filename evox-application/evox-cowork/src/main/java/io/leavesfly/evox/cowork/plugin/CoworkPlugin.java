package io.leavesfly.evox.cowork.plugin;

import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class CoworkPlugin {
    private String pluginId;
    private String name;
    private String description;
    private String version;
    private String author;
    private String category;
    private List<PluginCommand> commands;
    private List<String> connectors;
    private boolean enabled = true;
    private Map<String, Object> configProperties;

    @Data
    public static class PluginCommand {
        private String name;
        private String description;
        private String promptTemplate;
        private List<String> requiredInputs;
    }

    // 根据命令名称获取命令对象
    public PluginCommand getCommand(String commandName) {
        String normalizedCommandName = commandName.startsWith("/") ? commandName : "/" + commandName;
        return commands.stream()
                .filter(cmd -> cmd.getName().equals(commandName) || cmd.getName().equals(normalizedCommandName))
                .findFirst()
                .orElse(null);
    }

    // 获取所有命令名称列表
    public List<String> getCommandNames() {
        return commands.stream()
                .map(PluginCommand::getName)
                .collect(Collectors.toList());
    }

    // 检查是否包含指定命令
    public boolean hasCommand(String commandName) {
        return getCommand(commandName) != null;
    }
}