package io.leavesfly.evox.benchmark;

import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.memory.longterm.InMemoryLongTermMemory;
import io.leavesfly.evox.memory.shortterm.ShortTermMemory;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 消息处理性能基准测试
 * 
 * 测试指标：
 * - 消息创建性能
 * - 短期记忆添加/检索性能
 * - 长期记忆添加/检索/去重性能
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class MessageBenchmark {

    private ShortTermMemory shortTermMemory;
    private InMemoryLongTermMemory longTermMemory;
    private Message testMessage;

    @Setup
    public void setup() {
        shortTermMemory = new ShortTermMemory();
        longTermMemory = new InMemoryLongTermMemory();
        longTermMemory.initModule();
        
        testMessage = Message.builder()
                .content("Test message content")
                .messageType(MessageType.INPUT)
                .build();
    }

    /**
     * 基准测试 1: 消息创建性能
     */
    @Benchmark
    public Message benchmarkMessageCreation() {
        return Message.builder()
                .content("Benchmark message")
                .messageType(MessageType.INPUT)
                .build();
    }

    /**
     * 基准测试 2: 短期记忆添加性能
     */
    @Benchmark
    public void benchmarkShortTermMemoryAdd() {
        Message msg = Message.builder()
                .content("Short term message")
                .messageType(MessageType.RESPONSE)
                .build();
        shortTermMemory.addMessage(msg);
    }

    /**
     * 基准测试 3: 短期记忆检索性能
     */
    @Benchmark
    public List<Message> benchmarkShortTermMemoryRetrieval() {
        return shortTermMemory.getLatestMessages(10);
    }

    /**
     * 基准测试 4: 长期记忆添加性能（含去重）
     */
    @Benchmark
    public void benchmarkLongTermMemoryAdd() {
        Message msg = Message.builder()
                .content("Long term message " + System.nanoTime())
                .messageType(MessageType.RESPONSE)
                .build();
        longTermMemory.add(msg);
    }

    /**
     * 基准测试 5: 长期记忆搜索性能
     */
    @Benchmark
    public Map<String, Message> benchmarkLongTermMemorySearch() {
        return longTermMemory.search("message", 10);
    }

    /**
     * 基准测试 6: 长期记忆检索所有消息
     */
    @Benchmark
    public List<Message> benchmarkLongTermMemoryRetrieveAll() {
        return longTermMemory.getAll();
    }

    /**
     * 运行基准测试
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(MessageBenchmark.class.getSimpleName())
                .forks(1)
                .build();
        
        new Runner(opt).run();
    }
}
