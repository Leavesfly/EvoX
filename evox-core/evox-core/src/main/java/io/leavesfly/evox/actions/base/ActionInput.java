package io.leavesfly.evox.actions.base;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Action输入实现
 *
 * @author EvoX Team
 */
@Data
public class ActionInput {

    /**
     * 输入数据
     */
    private Map<String, Object> data = new HashMap<>();

    public ActionInput() {
    }

    public ActionInput(Map<String, Object> data) {
        this.data = data;
    }

    /**
     * 验证输入参数
     *
     * @return 是否有效
     */
    public boolean validate() {
        return data != null;
    }

    /**
     * 转换为Map
     *
     * @return Map表示
     */
    public Map<String, Object> toMap() {
        return data;
    }
}
