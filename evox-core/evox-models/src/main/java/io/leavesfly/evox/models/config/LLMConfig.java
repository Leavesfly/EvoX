package io.leavesfly.evox.models.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * LLM配置基类（兼容别名）
 *
 * <p>实际定义已下沉到核心层 {@link io.leavesfly.evox.core.llm.LLMConfig}，
 * 本类作为类型别名保留，确保 evox-models 内部的子类（OpenAILLMConfig 等）
 * 和外部已有代码的 import 路径不受影响。</p>
 *
 * @author EvoX Team
 * @see io.leavesfly.evox.core.llm.LLMConfig
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public abstract class LLMConfig extends io.leavesfly.evox.core.llm.LLMConfig {

    public LLMConfig() {
        super();
    }
}
