package io.leavesfly.evox.models.support;

import io.leavesfly.evox.core.llm.LLMConfig;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.models.protocol.ChatCompletionRequest;
import io.leavesfly.evox.models.protocol.ChatCompletionResponse;
import io.leavesfly.evox.models.protocol.ChatCompletionResult;
import io.leavesfly.evox.models.protocol.OpenAiCompatibleClient;
import io.leavesfly.evox.models.protocol.ToolCall;
import io.leavesfly.evox.models.protocol.ToolDefinition;
import io.leavesfly.evox.models.spi.LLMProvider;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容 LLM 抽象基类
 * 提取 OpenAILLM、DeepSeekLLM、AliyunLLM 等使用 OpenAiCompatibleClient 的公共逻辑
 * 
 * <p>子类只需实现特定的配置和模型名称获取逻辑，其余功能由基类提供</p>
 *
 * @author EvoX Team
 */
@Slf4j
public abstract class OpenAiCompatibleLLM implements LLMProvider {

    protected final LLMConfig config;
    protected final OpenAiCompatibleClient client;

    /**
     * 构造函数
     *
     * @param config 配置对象
     * @param defaultBaseUrl 默认的 API 基础 URL
     */
    protected OpenAiCompatibleLLM(LLMConfig config, String defaultBaseUrl) {
        this.config = config;
        
        String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : defaultBaseUrl;
        this.client = new OpenAiCompatibleClient(baseUrl, config.getApiKey(), config.getTimeout());
        
        log.info("Initialized OpenAI-compatible LLM with model: {}", getEffectiveModel());
    }

    @Override
    public String generate(String prompt) {
        try {
            log.debug("Generating response for prompt: {}", prompt.substring(0, Math.min(50, prompt.length())));

            ChatCompletionRequest request = OpenAiCompatibleClient.buildChatRequest(
                    getEffectiveModel(), prompt,
                    getEffectiveTemperature(),
                    config.getMaxTokens(),
                    getEffectiveTopP(),
                    getEffectiveFrequencyPenalty(),
                    getEffectivePresencePenalty());

            String response = client.chatCompletion(request);

            if (Boolean.TRUE.equals(config.getOutputResponse())) {
                log.info("Response: {}", response);
            }

            return response;
        } catch (Exception e) {
            log.error("Error generating response", e);
            throw new RuntimeException("Failed to generate response", e);
        }
    }

    @Override
    public Mono<String> generateAsync(String prompt) {
        ChatCompletionRequest request = OpenAiCompatibleClient.buildChatRequest(
                getEffectiveModel(), prompt,
                getEffectiveTemperature(),
                config.getMaxTokens(),
                getEffectiveTopP(),
                getEffectiveFrequencyPenalty(),
                getEffectivePresencePenalty());

        return client.chatCompletionAsync(request);
    }

    @Override
    public Flux<String> generateStream(String prompt) {
        try {
            ChatCompletionRequest request = OpenAiCompatibleClient.buildChatRequest(
                    getEffectiveModel(), prompt,
                    getEffectiveTemperature(),
                    config.getMaxTokens(),
                    getEffectiveTopP(),
                    getEffectiveFrequencyPenalty(),
                    getEffectivePresencePenalty());

            return client.chatCompletionStream(request)
                    .doOnNext(chunk -> {
                        if (Boolean.TRUE.equals(config.getOutputResponse())) {
                            System.out.print(chunk);
                        }
                    })
                    .doOnComplete(() -> {
                        if (Boolean.TRUE.equals(config.getOutputResponse())) {
                            System.out.println();
                        }
                    });
        } catch (Exception e) {
            log.error("Error in stream generation", e);
            return Flux.error(new RuntimeException("Failed to generate stream", e));
        }
    }

    @Override
    public String chat(List<Message> messages) {
        try {
            List<ChatCompletionRequest.ChatMessage> chatMessages = convertMessages(messages);
            ChatCompletionRequest request = buildRequest(chatMessages, null, null);
            
            String response = client.chatCompletion(request);

            if (Boolean.TRUE.equals(config.getOutputResponse())) {
                log.info("Chat Response: {}", response);
            }

            return response;
        } catch (Exception e) {
            log.error("Error in chat", e);
            throw new RuntimeException("Failed to chat", e);
        }
    }

    @Override
    public Mono<String> chatAsync(List<Message> messages) {
        return Mono.fromCallable(() -> chat(messages));
    }

    @Override
    public Flux<String> chatStream(List<Message> messages) {
        StringBuilder promptBuilder = new StringBuilder();
        for (Message msg : messages) {
            if (msg.getContent() != null) {
                promptBuilder.append(msg.getContent().toString()).append("\n");
            }
        }
        return generateStream(promptBuilder.toString());
    }

    @Override
    public String getModelName() {
        return getEffectiveModel();
    }

    @Override
    public LLMConfig getConfig() {
        return config;
    }

    @Override
    public boolean supportsToolUse() {
        return true;
    }

    @Override
    public Map<String, Object> chatWithTools(List<Message> messages,
                                             List<Map<String, Object>> toolSchemas,
                                             String toolChoice) {
        try {
            log.debug("Chat with tools, messages count: {}, tools count: {}", 
                    messages.size(), toolSchemas.size());

            List<ChatCompletionRequest.ChatMessage> chatMessages = convertMessages(messages);
            
            List<ToolDefinition> tools = new ArrayList<>();
            for (Map<String, Object> toolSchema : toolSchemas) {
                ToolDefinition toolDef = ToolDefinition.fromToolSchema(toolSchema);
                if (toolDef != null) {
                    tools.add(toolDef);
                }
            }

            ChatCompletionRequest request = buildRequest(chatMessages, tools, toolChoice);
            ChatCompletionResult result = client.chatCompletionWithTools(request);

            if (Boolean.TRUE.equals(config.getOutputResponse())) {
                log.info("Tool Call Result: content={}, toolCalls={}, finishReason={}", 
                        result.getContent(), result.getToolCalls(), result.getFinishReason());
            }

            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("content", result.getContent());
            resultMap.put("tool_calls", result.getToolCalls());
            resultMap.put("finish_reason", result.getFinishReason());
            resultMap.put("usage", result.getUsage());

            return resultMap;
        } catch (Exception e) {
            log.error("Error in chat with tools", e);
            throw new RuntimeException("Failed to chat with tools", e);
        }
    }

    @Override
    public ChatCompletionResult chatWithToolDefinitions(List<Message> messages,
                                                         List<ToolDefinition> toolDefinitions,
                                                         String toolChoice) {
        try {
            log.debug("Chat with tool definitions, messages count: {}, tools count: {}", 
                    messages.size(), toolDefinitions.size());

            List<ChatCompletionRequest.ChatMessage> chatMessages = convertMessages(messages);
            ChatCompletionRequest request = buildRequest(chatMessages, toolDefinitions, toolChoice);
            
            return client.chatCompletionWithTools(request);
        } catch (Exception e) {
            log.error("Error in chat with tool definitions", e);
            throw new RuntimeException("Failed to chat with tool definitions", e);
        }
    }

    @Override
    public Flux<ChatCompletionResponse> chatWithToolDefinitionsStream(List<Message> messages,
                                                                      List<ToolDefinition> toolDefinitions,
                                                                      String toolChoice) {
        try {
            log.debug("Streaming chat with tool definitions, messages count: {}, tools count: {}",
                    messages.size(), toolDefinitions.size());

            List<ChatCompletionRequest.ChatMessage> chatMessages = convertMessages(messages);
            ChatCompletionRequest request = buildRequest(chatMessages, toolDefinitions, toolChoice);
            request.setStream(true);

            request.setStreamOptions(Map.of("include_usage", true));

            return client.chatCompletionStreamRaw(request);
        } catch (Exception e) {
            log.error("Error in streaming chat with tool definitions", e);
            return Flux.error(new RuntimeException("Failed to stream chat with tool definitions", e));
        }
    }

    /**
     * 获取有效的模型名称
     * 子类可以覆盖此方法以提供自定义的模型名称逻辑
     * 例如 AliyunLLM 使用 effectiveModelName 而非 model
     *
     * @return 有效的模型名称
     */
    protected String getEffectiveModel() {
        return config.getModel();
    }

    /**
     * 获取有效的温度参数
     * 子类可以覆盖此方法以提供自定义的温度参数逻辑
     *
     * @return 有效的温度参数
     */
    protected Float getEffectiveTemperature() {
        return config.getTemperature() != null ? config.getTemperature() : 0.7f;
    }

    /**
     * 获取有效的 Top-p 参数
     * 子类可以覆盖此方法以提供自定义的 Top-p 参数逻辑
     *
     * @return 有效的 Top-p 参数
     */
    protected Float getEffectiveTopP() {
        return config.getTopP() != null ? config.getTopP() : 1.0f;
    }

    /**
     * 获取有效的频率惩罚参数
     * 子类可以覆盖此方法以提供自定义的频率惩罚参数逻辑
     *
     * @return 有效的频率惩罚参数
     */
    protected Float getEffectiveFrequencyPenalty() {
        return config.getFrequencyPenalty() != null ? config.getFrequencyPenalty() : 0.0f;
    }

    /**
     * 获取有效的存在惩罚参数
     * 子类可以覆盖此方法以提供自定义的存在惩罚参数逻辑
     *
     * @return 有效的存在惩罚参数
     */
    protected Float getEffectivePresencePenalty() {
        return config.getPresencePenalty() != null ? config.getPresencePenalty() : 0.0f;
    }

    /**
     * 获取 OpenAI 兼容客户端
     * 供子类访问底层的客户端对象
     *
     * @return OpenAI 兼容客户端
     */
    protected OpenAiCompatibleClient getClient() {
        return client;
    }

    /**
     * 转换消息格式
     * 将核心层的 Message 对象转换为 OpenAI 兼容的 ChatMessage 格式
     *
     * @param messages 核心层消息列表
     * @return OpenAI 兼容的消息列表
     */
    private List<ChatCompletionRequest.ChatMessage> convertMessages(List<Message> messages) {
        List<ChatCompletionRequest.ChatMessage> chatMessages = new ArrayList<>();
        
        for (Message message : messages) {
            ChatCompletionRequest.ChatMessage.ChatMessageBuilder builder = 
                    ChatCompletionRequest.ChatMessage.builder();
            
            MessageType messageType = message.getMessageType();
            
            if (messageType == MessageType.SYSTEM) {
                builder.role("system");
            } else if (messageType == MessageType.INPUT) {
                builder.role("user");
            } else if (messageType == MessageType.OUTPUT || messageType == MessageType.RESPONSE) {
                builder.role("assistant");
                
                if (message.getMetadata() != null && message.getMetadata().containsKey("tool_calls")) {
                    Object toolCallsObj = message.getMetadata().get("tool_calls");
                    if (toolCallsObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<ToolCall> toolCalls = (List<ToolCall>) toolCallsObj;
                        builder.toolCalls(toolCalls);
                    }
                }
            } else if ("tool_result".equals(message.getAction())) {
                builder.role("tool");
                if (message.getMetadata() != null && message.getMetadata().containsKey("tool_call_id")) {
                    builder.toolCallId((String) message.getMetadata().get("tool_call_id"));
                }
            } else {
                builder.role("user");
            }

            if (message.getContent() != null) {
                builder.content(message.getContent().toString());
            }

            chatMessages.add(builder.build());
        }
        
        return chatMessages;
    }

    /**
     * 构建 Chat Completion 请求
     *
     * @param chatMessages 转换后的消息列表
     * @param tools 工具定义列表（可为 null）
     * @param toolChoice 工具选择策略（可为 null）
     * @return 构建好的请求对象
     */
    private ChatCompletionRequest buildRequest(List<ChatCompletionRequest.ChatMessage> chatMessages,
                                                List<ToolDefinition> tools,
                                                String toolChoice) {
        ChatCompletionRequest.ChatCompletionRequestBuilder builder = ChatCompletionRequest.builder()
                .model(getEffectiveModel())
                .messages(chatMessages)
                .temperature(getEffectiveTemperature())
                .maxTokens(config.getMaxTokens())
                .topP(getEffectiveTopP())
                .frequencyPenalty(getEffectiveFrequencyPenalty())
                .presencePenalty(getEffectivePresencePenalty())
                .stream(false);

        if (tools != null && !tools.isEmpty()) {
            builder.tools(tools);
            builder.parallelToolCalls(true);
        }

        if (toolChoice != null && !toolChoice.isEmpty()) {
            builder.toolChoice(toolChoice);
        }

        return builder.build();
    }
}
