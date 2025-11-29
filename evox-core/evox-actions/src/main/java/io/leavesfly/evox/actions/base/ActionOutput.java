package io.leavesfly.evox.actions.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Action输出实现
 *
 * @author EvoX Team
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ActionOutput {

    /**
     * 是否执行成功
     */
    private boolean success;

    /**
     * 输出数据
     */
    private Object data;

    /**
     * 错误信息
     */
    private String error;
}
