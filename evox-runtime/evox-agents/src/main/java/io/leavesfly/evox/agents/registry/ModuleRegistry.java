package io.leavesfly.evox.agents.registry;

import io.leavesfly.evox.core.registry.IModuleRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模块注册表实现
 * 用于注册和管理所有模块类
 *
 * @author EvoX Team
 */
@Slf4j
@Component
public class ModuleRegistry implements IModuleRegistry {

    private final Map<String, Class<?>> moduleMap = new ConcurrentHashMap<>();

    @Override
    public void registerModule(String moduleName, Class<?> moduleClass) {
        if (moduleName == null || moduleClass == null) {
            log.warn("Cannot register null module: name={}, class={}", moduleName, moduleClass);
            return;
        }

        moduleMap.put(moduleName, moduleClass);
        log.debug("Registered module: {} -> {}", moduleName, moduleClass.getName());
    }

    @Override
    public Class<?> getModule(String moduleName) {
        Class<?> moduleClass = moduleMap.get(moduleName);
        if (moduleClass == null) {
            log.warn("Module not found: {}", moduleName);
        }
        return moduleClass;
    }

    @Override
    public boolean hasModule(String moduleName) {
        return moduleMap.containsKey(moduleName);
    }

    @Override
    public Map<String, Class<?>> getAllModules() {
        return new ConcurrentHashMap<>(moduleMap);
    }

    @Override
    public void clear() {
        moduleMap.clear();
        log.info("Cleared all registered modules");
    }

    @Override
    public int size() {
        return moduleMap.size();
    }
}
