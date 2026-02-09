package io.leavesfly.evox.cowork.plugin;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Data
public class PluginManager {
    private final Map<String, CoworkPlugin> plugins;
    private final PluginLoader pluginLoader;
    private final String pluginDirectory;

    public PluginManager(String pluginDirectory) {
        this.pluginDirectory = pluginDirectory;
        this.plugins = new ConcurrentHashMap<>();
        this.pluginLoader = new PluginLoader();
        loadPlugins();
    }

    public void loadPlugins() {
        List<CoworkPlugin> builtinPlugins = pluginLoader.loadBuiltinPlugins();
        for (CoworkPlugin plugin : builtinPlugins) {
            registerPlugin(plugin);
        }

        List<CoworkPlugin> customPlugins = pluginLoader.loadFromDirectory(pluginDirectory);
        for (CoworkPlugin plugin : customPlugins) {
            registerPlugin(plugin);
        }
    }

    public void registerPlugin(CoworkPlugin plugin) {
        plugins.put(plugin.getPluginId(), plugin);
        log.info("Registered plugin: {} ({})", plugin.getName(), plugin.getPluginId());
    }

    public void unregisterPlugin(String pluginId) {
        CoworkPlugin removed = plugins.remove(pluginId);
        if (removed != null) {
            log.info("Unregistered plugin: {}", pluginId);
        }
    }

    public CoworkPlugin getPlugin(String pluginId) {
        return plugins.get(pluginId);
    }

    public List<CoworkPlugin> getAllPlugins() {
        return List.copyOf(plugins.values());
    }

    public List<CoworkPlugin> getEnabledPlugins() {
        return plugins.values().stream()
                .filter(CoworkPlugin::isEnabled)
                .collect(Collectors.toList());
    }

    public List<CoworkPlugin> getPluginsByCategory(String category) {
        return plugins.values().stream()
                .filter(plugin -> category.equals(plugin.getCategory()))
                .collect(Collectors.toList());
    }

    public void enablePlugin(String pluginId) {
        CoworkPlugin plugin = plugins.get(pluginId);
        if (plugin != null) {
            plugin.setEnabled(true);
            log.info("Enabled plugin: {}", pluginId);
        }
    }

    public void disablePlugin(String pluginId) {
        CoworkPlugin plugin = plugins.get(pluginId);
        if (plugin != null) {
            plugin.setEnabled(false);
            log.info("Disabled plugin: {}", pluginId);
        }
    }

    public PluginCommandResult findCommand(String commandName) {
        for (CoworkPlugin plugin : getEnabledPlugins()) {
            CoworkPlugin.PluginCommand command = plugin.getCommand(commandName);
            if (command != null) {
                return new PluginCommandResult(plugin, command);
            }
        }
        return null;
    }

    public Map<String, String> getAvailableCommands() {
        Map<String, String> commands = new ConcurrentHashMap<>();
        for (CoworkPlugin plugin : getEnabledPlugins()) {
            for (CoworkPlugin.PluginCommand command : plugin.getCommands()) {
                commands.put(command.getName(), command.getDescription());
            }
        }
        return commands;
    }

    public String generatePluginDescriptions() {
        StringBuilder sb = new StringBuilder();
        sb.append("Available Plugins:\n\n");
        
        for (CoworkPlugin plugin : getEnabledPlugins()) {
            sb.append(String.format("- %s (%s): %s\n", 
                    plugin.getName(), 
                    plugin.getPluginId(), 
                    plugin.getDescription()));
            sb.append("  Commands:\n");
            for (CoworkPlugin.PluginCommand command : plugin.getCommands()) {
                sb.append(String.format("    %s: %s\n", 
                        command.getName(), 
                        command.getDescription()));
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }

    public record PluginCommandResult(CoworkPlugin plugin, CoworkPlugin.PluginCommand command) {
    }
}
