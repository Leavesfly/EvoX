package io.leavesfly.evox.models.aliyun;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import io.leavesfly.evox.core.llm.LLMConfig;
import io.leavesfly.evox.models.base.LLMProvider;
import io.leavesfly.evox.models.config.AliyunLLMConfig;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 阿里云通义千问 LLM实现
 * 使用DashScope SDK集成阿里云大模型服务
 *
 * @author EvoX Team
 */
@Slf4j
public class AliyunLLM implements LLMProvider {

    private final AliyunLLMConfig config;
    private final Generation generation;

    /**
     * 构造函数
     *
     * @param config 阿里云LLM配置
     */
    public AliyunLLM(AliyunLLMConfig config) {
        if (!config.isValid()) {
            throw new IllegalArgumentException("Invalid AliyunLLMConfig: aliyunApiKey is required");
        }

        this.config = config;
        this.generation = new Generation();

        log.info("Initialized Aliyun LLM with model: {}", config.getEffectiveModelName());
    }

    @Override
    public String generate(String prompt) {
        try {
            log.debug("Generating response for prompt: {}", prompt.substring(0, Math.min(50, prompt.length())));

            // 构建消息列表
            List<Message> messages = new ArrayList<>();
            messages.add(Message.builder()
                    .role(Role.USER.getValue())
                    .content(prompt)
                    .build());

            // 构建生成参数
            GenerationParam param = buildGenerationParam(messages, false);

            // 调用API
            GenerationResult result = generation.call(param);

            // 提取响应文本
            String response = extractResponseText(result);

            if (config.getOutputResponse()) {
                log.info("Aliyun Response: {}", response);
            }

            return response;
        } catch (NoApiKeyException e) {
            log.error("API key is missing", e);
            throw new RuntimeException("Aliyun API key is not set", e);
        } catch (ApiException | InputRequiredException e) {
            log.error("Error calling Aliyun API", e);
            throw new RuntimeException("Failed to generate response from Aliyun", e);
        }
    }

    @Override
    public Mono<String> generateAsync(String prompt) {
        return Mono.fromCallable(() -> generate(prompt));
    }

    @Override
    public Flux<String> generateStream(String prompt) {
        return Flux.create(sink -> {
            try {
                // 构建消息列表
                List<Message> messages = new ArrayList<>();
                messages.add(Message.builder()
                        .role(Role.USER.getValue())
                        .content(prompt)
                        .build());

                // 构建流式生成参数
                GenerationParam param = buildGenerationParam(messages, true);

                // 流式调用
                generation.streamCall(param, new GenerationStreamHandler(sink, config.getOutputResponse()));

            } catch (Exception e) {
                log.error("Error in stream generation", e);
                sink.error(new RuntimeException("Failed to generate stream from Aliyun", e));
            }
        });
    }

    @Override
    public String chat(List<io.leavesfly.evox.core.message.Message> messages) {
        try {
            log.debug("Chat with {} messages", messages.size());

            // 转换消息格式
            List<Message> dashscopeMessages = convertMessages(messages);

            // 构建生成参数
            GenerationParam param = buildGenerationParam(dashscopeMessages, false);

            // 调用API
            GenerationResult result = generation.call(param);

            // 提取响应文本
            String response = extractResponseText(result);

            if (config.getOutputResponse()) {
                log.info("Aliyun Chat Response: {}", response);
            }

            return response;
        } catch (NoApiKeyException e) {
            log.error("API key is missing", e);
            throw new RuntimeException("Aliyun API key is not set", e);
        } catch (ApiException | InputRequiredException e) {
            log.error("Error calling Aliyun API", e);
            throw new RuntimeException("Failed to chat with Aliyun", e);
        }
    }

    @Override
    public Mono<String> chatAsync(List<io.leavesfly.evox.core.message.Message> messages) {
        return Mono.fromCallable(() -> chat(messages));
    }

    @Override
    public Flux<String> chatStream(List<io.leavesfly.evox.core.message.Message> messages) {
        return Flux.create(sink -> {
            try {
                // 转换消息格式
                List<Message> dashscopeMessages = convertMessages(messages);

                // 构建流式生成参数
                GenerationParam param = buildGenerationParam(dashscopeMessages, true);

                // 流式调用
                generation.streamCall(param, new GenerationStreamHandler(sink, config.getOutputResponse()));

            } catch (Exception e) {
                log.error("Error in chat stream", e);
                sink.error(new RuntimeException("Failed to stream chat with Aliyun", e));
            }
        });
    }

    @Override
    public String getModelName() {
        return config.getEffectiveModelName();
    }

    @Override
    public LLMConfig getConfig() {
        return config;
    }

    /**
     * 构建生成参数
     *
     * @param messages 消息列表
     * @param stream   是否流式输出
     * @return 生成参数
     */
    private GenerationParam buildGenerationParam(List<Message> messages, boolean stream) {
        GenerationParam.GenerationParamBuilder<?, ?> builder = GenerationParam.builder()
                .model(config.getEffectiveModelName())
                .messages(messages)
                .apiKey(config.getAliyunApiKey())
                .resultFormat(GenerationParam.ResultFormat.MESSAGE);

        // 设置温度参数
        if (config.getTemperature() != null) {
            builder.temperature(config.getTemperature());
        }

        // 设置最大token数
        if (config.getMaxTokens() != null) {
            builder.maxTokens(config.getMaxTokens());
        }

        // 设置top-p参数
        if (config.getTopP() != null) {
            builder.topP(config.getTopP().doubleValue());
        }

        // 设置top-k参数
        if (config.getTopK() != null) {
            builder.topK(config.getTopK());
        }

        // 设置重复惩罚
        if (config.getRepetitionPenalty() != null) {
            builder.repetitionPenalty(config.getRepetitionPenalty());
        }

        // 设置是否启用搜索
        if (config.getEnableSearch() != null && config.getEnableSearch()) {
            builder.enableSearch(true);
        }

        // 设置流式输出
        if (stream || (config.getStream() != null && config.getStream())) {
            builder.incrementalOutput(true);
        }

        return builder.build();
    }

    /**
     * 从生成结果中提取响应文本
     *
     * @param result 生成结果
     * @return 响应文本
     */
    private String extractResponseText(GenerationResult result) {
        if (result == null || result.getOutput() == null) {
            throw new RuntimeException("Invalid response from Aliyun: result is null");
        }

        if (result.getOutput().getChoices() == null || result.getOutput().getChoices().isEmpty()) {
            throw new RuntimeException("Invalid response from Aliyun: no choices returned");
        }

        Message responseMessage = result.getOutput().getChoices().get(0).getMessage();
        if (responseMessage == null || responseMessage.getContent() == null) {
            throw new RuntimeException("Invalid response from Aliyun: message content is null");
        }

        return responseMessage.getContent().toString();
    }

    /**
     * 转换消息格式
     *
     * @param messages EvoX消息列表
     * @return DashScope消息列表
     */
    private List<Message> convertMessages(List<io.leavesfly.evox.core.message.Message> messages) {
        return messages.stream()
                .map(msg -> {
                    // EvoX Message没有role字段,根据messageType推断
                    String role = inferRoleFromMessageType(msg);
                    String content = msg.getContent() != null ? msg.getContent().toString() : "";

                    return Message.builder()
                            .role(mapRole(role))
                            .content(content)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 从消息类型推断角色
     *
     * @param msg EvoX消息
     * @return 角色字符串
     */
    private String inferRoleFromMessageType(io.leavesfly.evox.core.message.Message msg) {
        if (msg.getMessageType() == null) {
            return "user";
        }
        
        switch (msg.getMessageType()) {
            case SYSTEM:
                return "system";
            case RESPONSE:
            case OUTPUT:
                return "assistant";
            case INPUT:
            default:
                return "user";
        }
    }

    /**
     * 映射角色类型
     *
     * @param role EvoX角色
     * @return DashScope角色
     */
    private String mapRole(String role) {
        switch (role.toLowerCase()) {
            case "system":
                return Role.SYSTEM.getValue();
            case "assistant":
                return Role.ASSISTANT.getValue();
            case "user":
            default:
                return Role.USER.getValue();
        }
    }
}
