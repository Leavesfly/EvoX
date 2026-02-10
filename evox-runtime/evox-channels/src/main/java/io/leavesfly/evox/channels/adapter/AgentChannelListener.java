package io.leavesfly.evox.channels.adapter;

import io.leavesfly.evox.channels.core.ChannelMessage;
import io.leavesfly.evox.channels.core.ChannelRegistry;
import io.leavesfly.evox.channels.core.IChannel;
import io.leavesfly.evox.channels.core.IChannelListener;
import io.leavesfly.evox.core.agent.IAgent;
import io.leavesfly.evox.core.message.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class AgentChannelListener implements IChannelListener {

    private final IAgent agent;
    private final ChannelRegistry channelRegistry;
    private final ExecutorService executorService;

    public AgentChannelListener(IAgent agent, ChannelRegistry channelRegistry) {
        this(agent, channelRegistry, Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors()));
    }

    public AgentChannelListener(IAgent agent, ChannelRegistry channelRegistry,
                                ExecutorService executorService) {
        this.agent = agent;
        this.channelRegistry = channelRegistry;
        this.executorService = executorService;
    }

    @Override
    public void onMessage(ChannelMessage channelMessage) {
        CompletableFuture.runAsync(() -> processMessage(channelMessage), executorService);
    }

    private void processMessage(ChannelMessage channelMessage) {
        String channelId = channelMessage.getChannelId();
        String senderId = channelMessage.getSenderId();

        try {
            Message agentInput = MessageAdapter.toAgentMessage(channelMessage);

            Message agentResponse = agent.execute(null, Collections.singletonList(agentInput));

            IChannel channel = channelRegistry.getChannel(channelId);
            if (channel != null && agentResponse != null) {
                ChannelMessage replyMessage = MessageAdapter.fromAgentMessage(
                        agentResponse, channelId, senderId);

                if (channelMessage.getThreadId() != null) {
                    replyMessage.setThreadId(channelMessage.getThreadId());
                }

                channel.sendMessage(senderId, replyMessage)
                        .exceptionally(ex -> {
                            log.error("Failed to send reply to channel [{}], target [{}]",
                                    channelId, senderId, ex);
                            return null;
                        });
            }
        } catch (Exception e) {
            log.error("Error processing message from channel [{}], sender [{}]",
                    channelId, senderId, e);

            IChannel channel = channelRegistry.getChannel(channelId);
            if (channel != null) {
                ChannelMessage errorReply = ChannelMessage.textMessage(
                        channelId, senderId,
                        "抱歉，处理您的消息时出现了错误，请稍后重试。");
                channel.sendMessage(senderId, errorReply)
                        .exceptionally(ex -> {
                            log.error("Failed to send error reply", ex);
                            return null;
                        });
            }
        }
    }
}
