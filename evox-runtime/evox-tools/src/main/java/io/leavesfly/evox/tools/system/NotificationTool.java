package io.leavesfly.evox.tools.system;

import io.leavesfly.evox.tools.base.BaseTool;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NotificationTool extends BaseTool {

    public NotificationTool() {
        super();
        this.name = "notification";
        this.description = "Send desktop notifications on macOS and Linux. "
                + "Supports title, message, sound, and subtitle.";

        this.inputs = new HashMap<>();
        this.required = new ArrayList<>();

        Map<String, String> titleParam = new HashMap<>();
        titleParam.put("type", "string");
        titleParam.put("description", "Notification title");
        this.inputs.put("title", titleParam);
        this.required.add("title");

        Map<String, String> messageParam = new HashMap<>();
        messageParam.put("type", "string");
        messageParam.put("description", "Notification message body");
        this.inputs.put("message", messageParam);
        this.required.add("message");

        Map<String, String> subtitleParam = new HashMap<>();
        subtitleParam.put("type", "string");
        subtitleParam.put("description", "Notification subtitle (macOS only, optional)");
        this.inputs.put("subtitle", subtitleParam);

        Map<String, String> soundParam = new HashMap<>();
        soundParam.put("type", "boolean");
        soundParam.put("description", "Play notification sound (default: true)");
        this.inputs.put("sound", soundParam);
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        validateParameters(parameters);

        String title = getParameter(parameters, "title", "EvoX Assistant");
        String message = getParameter(parameters, "message", "");
        String subtitle = getParameter(parameters, "subtitle", "");
        Boolean sound = getParameter(parameters, "sound", true);

        String osName = System.getProperty("os.name").toLowerCase();

        try {
            if (osName.contains("mac")) {
                return sendMacNotification(title, message, subtitle, sound);
            } else if (osName.contains("linux")) {
                return sendLinuxNotification(title, message);
            } else {
                return ToolResult.failure("Notifications not supported on: " + osName
                        + ". Supported: macOS, Linux");
            }
        } catch (Exception e) {
            log.error("Failed to send notification", e);
            return ToolResult.failure("Failed to send notification: " + e.getMessage());
        }
    }

    private ToolResult sendMacNotification(String title, String message, String subtitle, boolean sound)
            throws IOException, InterruptedException {
        StringBuilder script = new StringBuilder();
        script.append("display notification ").append(escapeAppleScript(message));
        script.append(" with title ").append(escapeAppleScript(title));
        if (subtitle != null && !subtitle.isEmpty()) {
            script.append(" subtitle ").append(escapeAppleScript(subtitle));
        }
        if (sound) {
            script.append(" sound name \"default\"");
        }

        ProcessBuilder processBuilder = new ProcessBuilder("osascript", "-e", script.toString());
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        boolean finished = process.waitFor(10, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            return ToolResult.failure("Notification command timed out");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("platform", "macOS");
        result.put("title", title);
        result.put("message", message);
        result.put("sent", process.exitValue() == 0);

        return process.exitValue() == 0
                ? ToolResult.success(result)
                : ToolResult.failure("Failed to send macOS notification");
    }

    private ToolResult sendLinuxNotification(String title, String message)
            throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("notify-send", title, message);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        boolean finished = process.waitFor(10, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            return ToolResult.failure("Notification command timed out");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("platform", "Linux");
        result.put("title", title);
        result.put("message", message);
        result.put("sent", process.exitValue() == 0);

        return process.exitValue() == 0
                ? ToolResult.success(result)
                : ToolResult.failure("Failed to send Linux notification. Is notify-send installed?");
    }

    private String escapeAppleScript(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
