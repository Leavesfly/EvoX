package io.leavesfly.evox.models.aliyun;

import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.ResultCallback;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.FluxSink;

/**
 * 阿里云流式生成处理器
 * 将DashScope的流式回调转换为Reactor的FluxSink
 *
 * @author EvoX Team
 */
@Slf4j
public class GenerationStreamHandler extends ResultCallback<GenerationResult> {

    private final FluxSink<String> sink;
    private final boolean outputResponse;
    private final StringBuilder fullResponse = new StringBuilder();

    public GenerationStreamHandler(FluxSink<String> sink, Boolean outputResponse) {
        this.sink = sink;
        this.outputResponse = outputResponse != null && outputResponse;
    }

    @Override
    public void onEvent(GenerationResult result) {
        if (result == null || result.getOutput() == null) {
            return;
        }

        try {
            if (result.getOutput().getChoices() != null && !result.getOutput().getChoices().isEmpty()) {
                String content = result.getOutput().getChoices().get(0).getMessage().getContent().toString();
                
                if (content != null && !content.isEmpty()) {
                    fullResponse.append(content);
                    
                    if (outputResponse) {
                        System.out.print(content);
                    }
                    
                    sink.next(content);
                }
            }
        } catch (Exception e) {
            log.error("Error processing stream event", e);
            sink.error(e);
        }
    }

    @Override
    public void onComplete() {
        if (outputResponse) {
            System.out.println();
        }
        log.debug("Stream completed, total length: {}", fullResponse.length());
        sink.complete();
    }

    @Override
    public void onError(Exception e) {
        log.error("Stream error occurred", e);
        sink.error(new RuntimeException("Stream generation failed", e));
    }
}
