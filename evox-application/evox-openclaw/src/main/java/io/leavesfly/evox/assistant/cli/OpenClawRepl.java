package io.leavesfly.evox.assistant.cli;

import io.leavesfly.evox.skill.BaseSkill;
import io.leavesfly.evox.skill.SkillRegistry;
import io.leavesfly.evox.assistant.evolution.SelfEvolutionService;
import io.leavesfly.evox.assistant.evolution.SkillGenerator;
import io.leavesfly.evox.channels.core.ChannelRegistry;
import io.leavesfly.evox.channels.core.IChannel;
import io.leavesfly.evox.core.agent.IAgentManager;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.gateway.routing.GatewayRouter;
import io.leavesfly.evox.scheduler.core.TaskScheduler;
import io.leavesfly.evox.scheduler.heartbeat.HeartbeatRunner;
import io.leavesfly.evox.scheduler.heartbeat.SystemEvent;
import io.leavesfly.evox.scheduler.heartbeat.SystemEventQueue;
import io.leavesfly.evox.tools.api.ToolRegistry;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * OpenClaw CLI 交互式 REPL（Read-Eval-Print Loop）
 * 提供终端交互式体验，支持对话、技能调用、心跳管理、进化能力操控等。
 */
@Slf4j
public class OpenClawRepl {

    private final GatewayRouter gatewayRouter;
    private final SkillRegistry skillRegistry;
    private final ToolRegistry toolRegistry;
    private final ChannelRegistry channelRegistry;
    private final TaskScheduler taskScheduler;
    private final IAgentManager agentManager;
    private final HeartbeatRunner heartbeatRunner;
    private final SystemEventQueue systemEventQueue;
    private final SelfEvolutionService selfEvolutionService;
    private final SkillGenerator skillGenerator;
    private final OpenClawCliRenderer renderer;
    private final boolean colorEnabled;

    private volatile boolean running;
    private String currentUserId = "cli-user";

    public OpenClawRepl(GatewayRouter gatewayRouter,
                        SkillRegistry skillRegistry,
                        ToolRegistry toolRegistry,
                        ChannelRegistry channelRegistry,
                        TaskScheduler taskScheduler,
                        IAgentManager agentManager,
                        HeartbeatRunner heartbeatRunner,
                        SystemEventQueue systemEventQueue,
                        SelfEvolutionService selfEvolutionService,
                        SkillGenerator skillGenerator,
                        boolean colorEnabled) {
        this.gatewayRouter = gatewayRouter;
        this.skillRegistry = skillRegistry;
        this.toolRegistry = toolRegistry;
        this.channelRegistry = channelRegistry;
        this.taskScheduler = taskScheduler;
        this.agentManager = agentManager;
        this.heartbeatRunner = heartbeatRunner;
        this.systemEventQueue = systemEventQueue;
        this.selfEvolutionService = selfEvolutionService;
        this.skillGenerator = skillGenerator;
        this.colorEnabled = colorEnabled;
        this.renderer = new OpenClawCliRenderer(colorEnabled);
    }

    /**
     * 启动 REPL 循环
     */
    public void start() {
        running = true;
        renderer.printWelcome();
        renderer.printInfo("User: " + currentUserId);
        renderer.printInfo("Agents: " + agentManager.getAgentCount()
                + " | Channels: " + channelRegistry.getChannelCount()
                + " | Skills: " + skillRegistry.getSkillCount()
                + " | Tools: " + toolRegistry.size());
        renderer.println("");

        Scanner scanner = new Scanner(System.in);

        while (running) {
            renderer.printPrompt();

            if (!scanner.hasNextLine()) {
                break;
            }

            String input = scanner.nextLine();
            if (input == null || input.isBlank()) {
                continue;
            }

            String trimmedInput = input.trim();

            if (trimmedInput.startsWith("/")) {
                handleCommand(trimmedInput);
            } else {
                processChat(trimmedInput);
            }
        }

        renderer.printGoodbye();
    }

    /**
     * 停止 REPL
     */
    public void stop() {
        running = false;
    }

    // ==================== Chat ====================

    private void processChat(String userMessage) {
        renderer.printDivider();
        try {
            Message inputMessage = new Message();
            inputMessage.setContent(userMessage);

            Message responseMessage = gatewayRouter.route("cli", currentUserId, inputMessage);

            if (responseMessage != null && responseMessage.getContent() != null) {
                renderer.printReply(responseMessage.getContent().toString());
            } else {
                renderer.printWarning("No response from agent.");
            }
        } catch (Exception e) {
            log.error("Error processing chat input", e);
            renderer.printError("Error: " + e.getMessage());
        }
    }

    // ==================== Command Handling ====================

    private void handleCommand(String command) {
        String[] parts = command.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();
        String argument = parts.length > 1 ? parts[1].trim() : null;

        switch (cmd) {
            case "/help" -> renderer.printHelp();
            case "/status" -> showStatus();
            case "/skills" -> listSkills();
            case "/skill" -> executeSkill(argument);
            case "/tools" -> listTools();
            case "/channels" -> listChannels();
            case "/heartbeat" -> showHeartbeatStatus();
            case "/wake" -> triggerWake();
            case "/event" -> sendSystemEvent(argument);
            case "/evolution" -> showEvolutionStatus();
            case "/generate" -> generateSkill(argument);
            case "/clear" -> {
                renderer.printSuccess("Conversation cleared. Starting fresh session.");
                currentUserId = "cli-user-" + System.currentTimeMillis();
                renderer.printInfo("New user session: " + currentUserId);
            }
            case "/quit", "/exit", "/q" -> running = false;
            default -> renderer.printWarning("Unknown command: " + cmd + ". Type /help for available commands.");
        }
    }

    // ==================== Status ====================

    private void showStatus() {
        renderer.printSectionHeader("System Status");
        renderer.printKeyValue("Agents:", String.valueOf(agentManager.getAgentCount()));
        renderer.printKeyValue("Channels:", String.valueOf(channelRegistry.getChannelCount()));
        renderer.printKeyValue("Scheduled Tasks:", String.valueOf(taskScheduler.getTaskCount()));
        renderer.printKeyValue("Skills:", String.valueOf(skillRegistry.getSkillCount()));
        renderer.printKeyValue("Tools:", String.valueOf(toolRegistry.size()));

        if (heartbeatRunner != null) {
            renderer.printKeyValue("Heartbeat:",
                    heartbeatRunner.isRunning() ? renderer.green("RUNNING") : renderer.yellow("STOPPED"));
        } else {
            renderer.printKeyValue("Heartbeat:", renderer.dim("DISABLED"));
        }

        if (selfEvolutionService != null) {
            renderer.printKeyValue("Self-Evolution:", renderer.green("ENABLED"));
        } else {
            renderer.printKeyValue("Self-Evolution:", renderer.dim("DISABLED"));
        }

        if (skillGenerator != null) {
            renderer.printKeyValue("Skill Generator:", renderer.green("READY"));
        } else {
            renderer.printKeyValue("Skill Generator:", renderer.dim("DISABLED"));
        }

        renderer.println("");
    }

    // ==================== Skills ====================

    private void listSkills() {
        List<BaseSkill> skills = skillRegistry.getAllSkills();
        if (skills.isEmpty()) {
            renderer.printInfo("No skills registered.");
            return;
        }

        renderer.printSectionHeader("Registered Skills (" + skills.size() + ")");
        for (BaseSkill skill : skills) {
            renderer.println("  " + renderer.cyan(skill.getName()) + "  " + renderer.dim(skill.getDescription()));
        }
        renderer.println("");
        renderer.println("  Use " + renderer.cyan("/skill <name> <input>") + " to execute a skill.");
        renderer.println("");
    }

    private void executeSkill(String argument) {
        if (argument == null || argument.isBlank()) {
            renderer.printWarning("Usage: /skill <name> <input>");
            return;
        }

        String[] skillParts = argument.split("\\s+", 2);
        String skillName = skillParts[0];
        String skillInput = skillParts.length > 1 ? skillParts[1] : "";

        try {
            BaseSkill.SkillContext context = new BaseSkill.SkillContext(skillInput);
            BaseSkill.SkillResult result = skillRegistry.executeSkill(skillName, context);

            if (result.isSuccess()) {
                renderer.printSuccess("Skill '" + skillName + "' executed successfully.");
                renderer.println("  " + result.getOutput());
            } else {
                renderer.printError("Skill '" + skillName + "' failed: " + result.getError());
            }
        } catch (Exception e) {
            renderer.printError("Failed to execute skill '" + skillName + "': " + e.getMessage());
        }
        renderer.println("");
    }

    // ==================== Tools ====================

    private void listTools() {
        var toolNames = toolRegistry.getAllToolNames();
        if (toolNames.isEmpty()) {
            renderer.printInfo("No tools registered.");
            return;
        }

        renderer.printSectionHeader("Registered Tools (" + toolNames.size() + ")");
        for (String toolName : toolNames) {
            renderer.println("  " + renderer.cyan(toolName));
        }
        renderer.println("");
    }

    // ==================== Channels ====================

    private void listChannels() {
        var channels = channelRegistry.getAllChannels();
        if (channels.isEmpty()) {
            renderer.printInfo("No channels registered.");
            return;
        }

        renderer.printSectionHeader("Channels (" + channels.size() + ")");
        for (IChannel channel : channels) {
            String statusText = switch (channel.getStatus()) {
                case RUNNING -> renderer.green("RUNNING");
                case STOPPED -> renderer.yellow("STOPPED");
                case ERROR -> renderer.red("ERROR");
                default -> renderer.dim(channel.getStatus().name());
            };
            renderer.println("  " + renderer.cyan(channel.getChannelId())
                    + "  " + statusText
                    + "  " + renderer.dim(channel.getChannelName()));
        }
        renderer.println("");
    }

    // ==================== Heartbeat ====================

    private void showHeartbeatStatus() {
        if (heartbeatRunner == null) {
            renderer.printInfo("Heartbeat is not configured.");
            return;
        }

        renderer.printSectionHeader("Heartbeat Status");
        renderer.printKeyValue("Status:",
                heartbeatRunner.isRunning() ? renderer.green("RUNNING") : renderer.yellow("STOPPED"));
        renderer.printKeyValue("Total Heartbeats:", String.valueOf(heartbeatRunner.getTotalHeartbeats()));
        renderer.printKeyValue("Events Processed:", String.valueOf(heartbeatRunner.getTotalEventsProcessed()));
        renderer.printKeyValue("Pending Events:", String.valueOf(heartbeatRunner.getPendingEventCount()));

        Instant lastTime = heartbeatRunner.getLastHeartbeatTime();
        renderer.printKeyValue("Last Heartbeat:", lastTime != null ? lastTime.toString() : "Never");
        renderer.println("");
        renderer.println("  Use " + renderer.cyan("/wake") + " to trigger immediate heartbeat.");
        renderer.println("  Use " + renderer.cyan("/event <message>") + " to send a system event.");
        renderer.println("");
    }

    private void triggerWake() {
        if (heartbeatRunner == null) {
            renderer.printWarning("Heartbeat is not configured.");
            return;
        }
        if (!heartbeatRunner.isRunning()) {
            renderer.printWarning("Heartbeat is not running.");
            return;
        }

        heartbeatRunner.wakeNow();
        renderer.printSuccess("Immediate heartbeat wake triggered!");
    }

    private void sendSystemEvent(String message) {
        if (message == null || message.isBlank()) {
            renderer.printWarning("Usage: /event <message>");
            return;
        }

        SystemEvent event = SystemEvent.builder()
                .source("cli")
                .message(message)
                .wakeMode(SystemEvent.WakeMode.NOW)
                .build();

        if (heartbeatRunner != null) {
            heartbeatRunner.enqueueEvent(event);
            renderer.printSuccess("System event sent and immediate wake triggered.");
        } else {
            systemEventQueue.enqueue(event);
            renderer.printSuccess("System event enqueued (heartbeat not active, will be processed on next heartbeat).");
        }
    }

    // ==================== Evolution ====================

    private void showEvolutionStatus() {
        renderer.printSectionHeader("Evolution Capabilities");

        // Heartbeat
        if (heartbeatRunner != null) {
            renderer.println("  " + renderer.bold("💓 Heartbeat: ")
                    + (heartbeatRunner.isRunning() ? renderer.green("RUNNING") : renderer.yellow("STOPPED"))
                    + "  (heartbeats: " + heartbeatRunner.getTotalHeartbeats()
                    + ", pending: " + heartbeatRunner.getPendingEventCount() + ")");
        } else {
            renderer.println("  " + renderer.bold("💓 Heartbeat: ") + renderer.dim("DISABLED"));
        }

        // Self-Evolution
        if (selfEvolutionService != null) {
            Map<String, Object> stats = selfEvolutionService.getStatistics();
            renderer.println("  " + renderer.bold("🧠 Self-Evolution: ") + renderer.green("ENABLED")
                    + "  (optimizations: " + stats.get("totalOptimizations")
                    + ", improvements: " + stats.get("totalImprovements")
                    + ", pending feedback: " + stats.get("pendingFeedback") + ")");
        } else {
            renderer.println("  " + renderer.bold("🧠 Self-Evolution: ") + renderer.dim("DISABLED"));
        }

        // Skill Generator
        if (skillGenerator != null) {
            int generatedCount = skillGenerator.getGeneratedSkills().size();
            renderer.println("  " + renderer.bold("🛠️ Skill Generator: ") + renderer.green("READY")
                    + "  (generated skills: " + generatedCount + ")");
        } else {
            renderer.println("  " + renderer.bold("🛠️ Skill Generator: ") + renderer.dim("DISABLED"));
        }

        renderer.println("");
    }

    private void generateSkill(String description) {
        if (skillGenerator == null) {
            renderer.printWarning("Skill Generator is not configured.");
            return;
        }
        if (description == null || description.isBlank()) {
            renderer.printWarning("Usage: /generate <skill description>");
            return;
        }

        renderer.printInfo("Generating skill from description: \"" + description + "\"...");

        try {
            SkillGenerator.GenerationResult result = skillGenerator.generateAndInstall(description);

            if (result.isSuccess()) {
                renderer.printSuccess("Skill generated and installed: " + result.getSkillName());
                renderer.println("  " + renderer.dim(result.getSkillDescription()));
                renderer.println("  Use " + renderer.cyan("/skill " + result.getSkillName() + " <input>")
                        + " to try it out.");
            } else {
                renderer.printError("Skill generation failed: " + result.getMessage());
            }
        } catch (Exception e) {
            log.error("Skill generation error", e);
            renderer.printError("Skill generation error: " + e.getMessage());
        }
        renderer.println("");
    }
}
