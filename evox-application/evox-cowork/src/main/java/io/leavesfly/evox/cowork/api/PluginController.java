package io.leavesfly.evox.cowork.api;

import io.leavesfly.evox.cowork.plugin.CoworkPlugin;
import io.leavesfly.evox.cowork.plugin.PluginManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/cowork/plugins")
@RequiredArgsConstructor
public class PluginController {

    private final PluginManager pluginManager;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getPlugins() {
        List<Map<String, Object>> plugins = pluginManager.getAllPlugins().stream()
                .map(plugin -> {
                    Map<String, Object> pluginInfo = new HashMap<>();
                    pluginInfo.put("id", plugin.getPluginId());
                    pluginInfo.put("name", plugin.getName());
                    pluginInfo.put("description", plugin.getDescription());
                    pluginInfo.put("category", plugin.getCategory());
                    pluginInfo.put("enabled", plugin.isEnabled());
                    pluginInfo.put("commands", plugin.getCommands().stream()
                            .map(CoworkPlugin.PluginCommand::getName)
                            .collect(Collectors.toList()));
                    return pluginInfo;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(plugins);
    }

    @PostMapping("/{pluginId}/enable")
    public ResponseEntity<Map<String, String>> enablePlugin(@PathVariable String pluginId) {
        pluginManager.enablePlugin(pluginId);
        return ResponseEntity.ok(Map.of("message", "Plugin enabled: " + pluginId));
    }

    @PostMapping("/{pluginId}/disable")
    public ResponseEntity<Map<String, String>> disablePlugin(@PathVariable String pluginId) {
        pluginManager.disablePlugin(pluginId);
        return ResponseEntity.ok(Map.of("message", "Plugin disabled: " + pluginId));
    }

    @GetMapping("/commands")
    public ResponseEntity<Map<String, String>> getAvailableCommands() {
        return ResponseEntity.ok(pluginManager.getAvailableCommands());
    }
}
