package io.leavesfly.evox.workflow.environment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流执行环境
 * 提供隔离的执行上下文
 *
 * @author EvoX Team
 */
@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Environment {

    /**
     * 环境ID
     */
    private String environmentId;

    /**
     * 环境名称
     */
    private String name;

    /**
     * 环境变量
     */
    private Map<String, Object> variables;

    /**
     * 环境配置
     */
    private EnvironmentConfig config;

    /**
     * 环境状态
     */
    private EnvironmentStatus status;

    public Environment(String name) {
        this.environmentId = java.util.UUID.randomUUID().toString();
        this.name = name;
        this.variables = new ConcurrentHashMap<>();
        this.config = new EnvironmentConfig();
        this.status = EnvironmentStatus.INITIALIZED;
    }

    /**
     * 设置变量
     */
    public void setVariable(String key, Object value) {
        variables.put(key, value);
        log.debug("环境变量设置: {}={}", key, value);
    }

    /**
     * 获取变量
     */
    public Object getVariable(String key) {
        return variables.get(key);
    }

    /**
     * 获取变量(带默认值)
     */
    public Object getVariable(String key, Object defaultValue) {
        return variables.getOrDefault(key, defaultValue);
    }

    /**
     * 移除变量
     */
    public void removeVariable(String key) {
        variables.remove(key);
        log.debug("环境变量移除: {}", key);
    }

    /**
     * 清空所有变量
     */
    public void clearVariables() {
        variables.clear();
        log.info("环境变量已清空");
    }

    /**
     * 启动环境
     */
    public void start() {
        if (status == EnvironmentStatus.RUNNING) {
            log.warn("环境已在运行中: {}", name);
            return;
        }

        log.info("启动环境: {}", name);
        status = EnvironmentStatus.RUNNING;
    }

    /**
     * 停止环境
     */
    public void stop() {
        log.info("停止环境: {}", name);
        status = EnvironmentStatus.STOPPED;
    }

    /**
     * 重置环境
     */
    public void reset() {
        log.info("重置环境: {}", name);
        clearVariables();
        status = EnvironmentStatus.INITIALIZED;
    }

    /**
     * 环境配置
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnvironmentConfig {
        /**
         * 是否启用沙箱模式
         */
        private boolean sandboxEnabled = false;

        /**
         * 资源限制
         */
        private ResourceLimits resourceLimits = new ResourceLimits();

        /**
         * 超时设置(秒)
         */
        private int timeoutSeconds = 300;
    }

    /**
     * 资源限制
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceLimits {
        /**
         * 最大内存(MB)
         */
        private int maxMemoryMB = 1024;

        /**
         * 最大CPU核心数
         */
        private int maxCpuCores = 2;

        /**
         * 最大执行时间(秒)
         */
        private int maxExecutionSeconds = 600;
    }

    /**
     * 环境状态
     */
    public enum EnvironmentStatus {
        INITIALIZED,
        RUNNING,
        STOPPED,
        ERROR
    }
}
