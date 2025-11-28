package io.leavesfly.evox.core.registry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模块注册表
 * 用于注册和管理所有模块类
 *
 * @author EvoX Team
 */
@Slf4j
@Component
public class ModuleRegistry {

    private final Map<String, Class<?>> moduleMap = new ConcurrentHashMap<>();

    /**
     * 注册模块类
     *
     * @param moduleName 模块名称
     * @param moduleClass 模块类
     */
    public void registerModule(String moduleName, Class<?> moduleClass) {
        if (moduleName == null || moduleClass == null) {
            log.warn("Cannot register null module: name={}, class={}", moduleName, moduleClass);
            return;
        }
        
        moduleMap.put(moduleName, moduleClass);
        log.debug("Registered module: {} -> {}", moduleName, moduleClass.getName());
    }

    /**
     * 获取模块类
     *
     * @param moduleName 模块名称
     * @return 模块类
     */
    public Class<?> getModule(String moduleName) {
        Class<?> moduleClass = moduleMap.get(moduleName);
        if (moduleClass == null) {
            log.warn("Module not found: {}", moduleName);
        }
        return moduleClass;
    }

    /**
     * 检查模块是否存在
     *
     * @param moduleName 模块名称
     * @return 是否存在
     */
    public boolean hasModule(String moduleName) {
        return moduleMap.containsKey(moduleName);
    }

    /**
     * 获取所有已注册模块
     *
     * @return 模块映射表
     */
    public Map<String, Class<?>> getAllModules() {
        return new ConcurrentHashMap<>(moduleMap);
    }

    /**
     * 清空所有模块
     */
    public void clear() {
        moduleMap.clear();
        log.info("Cleared all registered modules");
    }

    /**
     * 获取模块数量
     */
    public int size() {
        return moduleMap.size();
    }
}
