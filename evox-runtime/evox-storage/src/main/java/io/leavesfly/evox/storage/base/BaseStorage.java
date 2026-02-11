package io.leavesfly.evox.storage.base;

/**
 * 存储基类接口
 * 所有存储系统的基础接口
 * 
 * @author EvoX Team
 */
public interface BaseStorage {

    /**
     * 初始化存储
     */
    void initialize();

    /**
     * 关闭存储
     */
    void close();

    /**
     * 清空存储
     */
    void clear();

    /**
     * 检查存储是否已初始化
     * 
     * @return 是否已初始化
     */
    boolean isInitialized();
}
