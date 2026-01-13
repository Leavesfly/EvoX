package io.leavesfly.evox.frameworks.consensus;

import lombok.Data;

import java.util.List;

/**
 * 共识记录
 * 记录每一轮的提议和评估结果
 *
 * @param <T> 提议的类型
 * @author EvoX Team
 */
@Data
public class ConsensusRecord<T> {

    /**
     * 轮次
     */
    private int round;

    /**
     * 本轮所有提议
     */
    private List<T> proposals;

    /**
     * 本轮评估结果
     */
    private ConsensusEvaluation<T> evaluation;

    /**
     * 时间戳
     */
    private long timestamp;

    public ConsensusRecord(int round, List<T> proposals, long timestamp) {
        this.round = round;
        this.proposals = proposals;
        this.timestamp = timestamp;
    }
}
