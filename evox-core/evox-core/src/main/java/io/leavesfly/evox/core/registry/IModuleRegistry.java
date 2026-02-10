package io.leavesfly.evox.core.registry;

import java.util.Map;

/**
 * 模块注册表接口
 * 定义模块注册和发现的核心抽象，具体实现在运行时层
 *
 * @author EvoX Team
 */
public interface IModuleRegistry {

    /**
     * 注册模块类
     *
     * @param moduleName 模块名称
     * @param moduleClass 模块类
     */
    void registerModule(String moduleName, Class<?> moduleClass);

    /**
     * 获取模块类
     *
     * @param moduleName 模块名称
     * @return 模块类，不存在时返回 null
     */
    Class<?> getModule(String moduleName);

    /**
     * 检查模块是否存在
     *
     * @param moduleName 模块名称
     * @return 是否存在
     */
    boolean hasModule(String moduleName);

    /**
     * 获取所有已注册模块
     *
     * @return 模块映射表（不可变副本）
     */
    Map<String, Class<?>> getAllModules();

    /**
     * 清空所有模块
     */
    void clear();

    /**
     * 获取模块数量
     */
    int size();
}
