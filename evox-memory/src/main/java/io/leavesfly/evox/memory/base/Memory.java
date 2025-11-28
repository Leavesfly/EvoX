package io.leavesfly.evox.memory.base;

import io.leavesfly.evox.core.module.BaseModule;

/**
 * 记忆基类
 * 所有记忆实现的抽象基类
 * 
 * @author EvoX Team
 */
public abstract class Memory extends BaseModule {

    /**
     * 清空记忆
     */
    public abstract void clear();
}
