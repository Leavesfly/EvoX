package io.leavesfly.evox.actions.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ActionOutput简单实现
 *
 * @author EvoX Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimpleActionOutput extends ActionOutput {

    private boolean success;
    private Object data;
    private String error;

    /**
     * 创建成功的输出
     */
    public static SimpleActionOutput success(Object data) {
        return new SimpleActionOutput(true, data, null);
    }

    /**
     * 创建失败的输出
     */
    public static SimpleActionOutput failure(String error) {
        return new SimpleActionOutput(false, null, error);
    }
}
