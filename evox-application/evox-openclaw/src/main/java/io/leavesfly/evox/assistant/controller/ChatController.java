package io.leavesfly.evox.assistant.controller;

import io.leavesfly.evox.agents.skill.BaseSkill;
import io.leavesfly.evox.agents.skill.SkillRegistry;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.gateway.routing.GatewayRouter;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final GatewayRouter gatewayRouter;
    private final SkillRegistry skillRegistry;

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        try {
            Message inputMessage = new Message();
            inputMessage.setContent(request.getMessage());
            
            Message responseMessage = gatewayRouter.route(request.getChannelId(), request.getUserId(), inputMessage);
            
            ChatResponse chatResponse = new ChatResponse();
            chatResponse.setSuccess(true);
            chatResponse.setReply(responseMessage.getContent());
            return chatResponse;
        } catch (Exception e) {
            ChatResponse chatResponse = new ChatResponse();
            chatResponse.setSuccess(false);
            chatResponse.setError(e.getMessage());
            return chatResponse;
        }
    }

    @PostMapping("/skill")
    public Map<String, Object> chatWithSkill(@RequestBody SkillChatRequest request) {
        try {
            BaseSkill.SkillContext context;
            if (request.getParameters() != null) {
                context = new BaseSkill.SkillContext(request.getInput(), request.getParameters());
            } else {
                context = new BaseSkill.SkillContext(request.getInput());
            }
            
            BaseSkill.SkillResult result = skillRegistry.executeSkill(request.getSkillName(), context);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("output", result.getOutput());
            response.put("error", result.getError());
            return response;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to execute skill: " + e.getMessage());
            return error;
        }
    }

    @Data
    public static class ChatRequest {
        private String message;
        private String userId;
        private String channelId;
    }

    @Data
    public static class ChatResponse {
        private boolean success;
        private Object reply;
        private String error;
    }

    @Data
    public static class SkillChatRequest {
        private String skillName;
        private String input;
        private Map<String, Object> parameters;
    }
}