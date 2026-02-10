package io.leavesfly.evox.tools.clipboard;

import io.leavesfly.evox.tools.base.BaseTool;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.datatransfer.*;
import java.util.*;

@Slf4j
public class ClipboardTool extends BaseTool {

    public ClipboardTool() {
        super();
        this.name = "clipboard";
        this.description = "Read from or write to the system clipboard. "
                + "Supports text content for copy and paste operations.";

        this.inputs = new HashMap<>();
        this.required = new ArrayList<>();

        Map<String, String> operationParam = new HashMap<>();
        operationParam.put("type", "string");
        operationParam.put("description", "Operation: 'read' (paste from clipboard) or 'write' (copy to clipboard)");
        this.inputs.put("operation", operationParam);
        this.required.add("operation");

        Map<String, String> contentParam = new HashMap<>();
        contentParam.put("type", "string");
        contentParam.put("description", "Content to write to clipboard (required for 'write' operation)");
        this.inputs.put("content", contentParam);
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        validateParameters(parameters);
        String operation = getParameter(parameters, "operation", "");

        return switch (operation) {
            case "read" -> readClipboard();
            case "write" -> writeClipboard(getParameter(parameters, "content", ""));
            default -> ToolResult.failure("Unknown operation: " + operation + ". Use 'read' or 'write'.");
        };
    }

    private ToolResult readClipboard() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable contents = clipboard.getContents(null);

            if (contents == null) {
                return ToolResult.success(Map.of("content", "", "hasContent", false));
            }

            if (contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String text = (String) contents.getTransferData(DataFlavor.stringFlavor);
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("content", text);
                result.put("hasContent", true);
                result.put("length", text.length());
                return ToolResult.success(result);
            } else {
                return ToolResult.success(Map.of(
                        "content", "",
                        "hasContent", true,
                        "note", "Clipboard contains non-text content"));
            }
        } catch (java.awt.HeadlessException e) {
            return ToolResult.failure("Clipboard not available in headless environment. "
                    + "Set java.awt.headless=false or use a display server.");
        } catch (Exception e) {
            log.error("Error reading clipboard", e);
            return ToolResult.failure("Failed to read clipboard: " + e.getMessage());
        }
    }

    private ToolResult writeClipboard(String content) {
        if (content == null || content.isEmpty()) {
            return ToolResult.failure("Content is required for write operation");
        }

        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection selection = new StringSelection(content);
            clipboard.setContents(selection, null);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("written", true);
            result.put("length", content.length());
            result.put("preview", content.length() > 100 ? content.substring(0, 100) + "..." : content);
            return ToolResult.success(result);
        } catch (java.awt.HeadlessException e) {
            return ToolResult.failure("Clipboard not available in headless environment. "
                    + "Set java.awt.headless=false or use a display server.");
        } catch (Exception e) {
            log.error("Error writing to clipboard", e);
            return ToolResult.failure("Failed to write to clipboard: " + e.getMessage());
        }
    }
}
