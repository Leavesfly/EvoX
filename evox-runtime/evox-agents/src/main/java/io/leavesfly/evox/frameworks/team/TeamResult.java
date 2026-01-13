package io.leavesfly.evox.frameworks.team;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 团队执行结果
 *
 * @param <T> 结果类型
 * @author EvoX Team
 */
@Data
@Builder
public class TeamResult<T> {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 最终结果
     */
    private T result;

    /**
     * 参与人数
     */
    private int participantCount;

    /**
     * 耗时(毫秒)
     */
    private long duration;

    /**
     * 成员贡献
     */
    private List<TaskExecution<T>> contributions;

    /**
     * 错误信息(如果失败)
     */
    private String error;

    /**
     * 元数据
     */
    private Map<String, Object> metadata;
}
