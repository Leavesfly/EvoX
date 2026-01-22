package io.leavesfly.evox.workflow.visualization;

import io.leavesfly.evox.workflow.base.WorkflowNode;
import io.leavesfly.evox.workflow.graph.WorkflowGraph;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 工作流可视化工具
 * 支持导出为多种格式：Mermaid、DOT (Graphviz)、JSON、ASCII
 *
 * @author EvoX Team
 */
@Slf4j
@Data
public class WorkflowVisualizer {

    /**
     * 工作流图
     */
    private final WorkflowGraph graph;

    /**
     * 导出配置
     */
    private ExportConfig config;

    public WorkflowVisualizer(WorkflowGraph graph) {
        this.graph = graph;
        this.config = ExportConfig.builder().build();
    }

    public WorkflowVisualizer(WorkflowGraph graph, ExportConfig config) {
        this.graph = graph;
        this.config = config != null ? config : ExportConfig.builder().build();
    }

    // ============= Mermaid 格式导出 =============

    /**
     * 导出为 Mermaid 格式
     */
    public String toMermaid() {
        StringBuilder sb = new StringBuilder();
        
        // 图类型
        String direction = config.isLeftToRight() ? "LR" : "TB";
        sb.append("graph ").append(direction).append("\n");
        
        // 节点定义
        Map<String, WorkflowNode> nodes = graph.getNodes();
        for (WorkflowNode node : nodes.values()) {
            String nodeShape = getMermaidNodeShape(node);
            String label = getNodeLabel(node);
            sb.append("    ").append(sanitizeId(node.getNodeId()))
              .append(nodeShape.replace("{label}", escapeLabel(label)))
              .append("\n");
        }
        
        sb.append("\n");
        
        // 边定义
        for (WorkflowNode node : nodes.values()) {
            for (String successorId : node.getSuccessors()) {
                String edgeLabel = getEdgeLabel(node, successorId);
                sb.append("    ").append(sanitizeId(node.getNodeId()))
                  .append(" --> ");
                if (edgeLabel != null && !edgeLabel.isEmpty()) {
                    sb.append("|").append(edgeLabel).append("| ");
                }
                sb.append(sanitizeId(successorId)).append("\n");
            }
        }
        
        return sb.toString();
    }

    /**
     * 获取 Mermaid 节点形状
     */
    private String getMermaidNodeShape(WorkflowNode node) {
        return switch (node.getNodeType()) {
            case ACTION -> "[{label}]";           // 方框
            case DECISION -> "{{{label}}}";       // 菱形
            case PARALLEL -> "{{{{label}}}}";     // 六边形
            case LOOP -> "(({label}))";           // 圆形
            case SUBWORKFLOW -> "[[{label}]]";    // 子流程
            default -> "[{label}]";
        };
    }

    // ============= DOT (Graphviz) 格式导出 =============

    /**
     * 导出为 DOT 格式
     */
    public String toDot() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("digraph workflow {\n");
        sb.append("    rankdir=").append(config.isLeftToRight() ? "LR" : "TB").append(";\n");
        sb.append("    node [fontname=\"").append(config.getFontName()).append("\"];\n");
        sb.append("    edge [fontname=\"").append(config.getFontName()).append("\"];\n");
        sb.append("\n");
        
        // 节点定义
        Map<String, WorkflowNode> nodes = graph.getNodes();
        for (WorkflowNode node : nodes.values()) {
            sb.append("    ").append(quoteId(node.getNodeId()));
            sb.append(" [");
            sb.append("label=\"").append(escapeLabel(getNodeLabel(node))).append("\"");
            sb.append(", shape=").append(getDotNodeShape(node));
            
            // 节点颜色
            String color = getNodeColor(node);
            if (color != null) {
                sb.append(", fillcolor=\"").append(color).append("\"");
                sb.append(", style=filled");
            }
            
            sb.append("];\n");
        }
        
        sb.append("\n");
        
        // 边定义
        for (WorkflowNode node : nodes.values()) {
            for (String successorId : node.getSuccessors()) {
                sb.append("    ").append(quoteId(node.getNodeId()));
                sb.append(" -> ").append(quoteId(successorId));
                
                String edgeLabel = getEdgeLabel(node, successorId);
                if (edgeLabel != null && !edgeLabel.isEmpty()) {
                    sb.append(" [label=\"").append(edgeLabel).append("\"]");
                }
                
                sb.append(";\n");
            }
        }
        
        sb.append("}\n");
        
        return sb.toString();
    }

    /**
     * 获取 DOT 节点形状
     */
    private String getDotNodeShape(WorkflowNode node) {
        return switch (node.getNodeType()) {
            case ACTION -> "box";
            case DECISION -> "diamond";
            case PARALLEL -> "hexagon";
            case LOOP -> "ellipse";
            case SUBWORKFLOW -> "doubleoctagon";
            default -> "box";
        };
    }

    // ============= JSON 格式导出 =============

    /**
     * 导出为 JSON 格式
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("{\n");
        sb.append("  \"goal\": \"").append(escapeJson(graph.getGoal())).append("\",\n");
        sb.append("  \"nodes\": [\n");
        
        Map<String, WorkflowNode> nodes = graph.getNodes();
        List<WorkflowNode> nodeList = new ArrayList<>(nodes.values());
        for (int i = 0; i < nodeList.size(); i++) {
            WorkflowNode node = nodeList.get(i);
            sb.append("    {\n");
            sb.append("      \"id\": \"").append(node.getNodeId()).append("\",\n");
            sb.append("      \"name\": \"").append(escapeJson(node.getName())).append("\",\n");
            sb.append("      \"type\": \"").append(node.getNodeType()).append("\",\n");
            sb.append("      \"state\": \"").append(node.getState()).append("\",\n");
            sb.append("      \"description\": \"").append(escapeJson(node.getDescription())).append("\"\n");
            sb.append("    }").append(i < nodeList.size() - 1 ? "," : "").append("\n");
        }
        
        sb.append("  ],\n");
        sb.append("  \"edges\": [\n");
        
        List<String[]> edges = new ArrayList<>();
        for (WorkflowNode node : nodes.values()) {
            for (String successorId : node.getSuccessors()) {
                edges.add(new String[]{node.getNodeId(), successorId});
            }
        }
        
        for (int i = 0; i < edges.size(); i++) {
            String[] edge = edges.get(i);
            sb.append("    {\"from\": \"").append(edge[0])
              .append("\", \"to\": \"").append(edge[1]).append("\"}");
            sb.append(i < edges.size() - 1 ? "," : "").append("\n");
        }
        
        sb.append("  ]\n");
        sb.append("}\n");
        
        return sb.toString();
    }

    // ============= ASCII 格式导出 =============

    /**
     * 导出为 ASCII 图
     */
    public String toAscii() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("╔══════════════════════════════════════════════════════════════╗\n");
        sb.append("║  工作流: ").append(padRight(graph.getGoal(), 52)).append("║\n");
        sb.append("╠══════════════════════════════════════════════════════════════╣\n");
        
        // 获取起始节点
        List<WorkflowNode> initialNodes = graph.findInitialNodes();
        Set<String> visited = new HashSet<>();
        
        for (WorkflowNode node : initialNodes) {
            printNodeAscii(sb, node, 0, visited);
        }
        
        sb.append("╚══════════════════════════════════════════════════════════════╝\n");
        
        return sb.toString();
    }

    /**
     * 递归打印节点的 ASCII 表示
     */
    private void printNodeAscii(StringBuilder sb, WorkflowNode node, int depth, Set<String> visited) {
        if (visited.contains(node.getNodeId())) {
            return;
        }
        visited.add(node.getNodeId());
        
        String indent = "║  " + "    ".repeat(depth);
        String nodeIcon = getNodeIcon(node);
        String stateIcon = getStateIcon(node);
        
        sb.append(indent).append(nodeIcon).append(" ")
          .append(node.getName()).append(" ")
          .append(stateIcon);
        
        // 填充到行尾
        int currentLen = indent.length() + nodeIcon.length() + node.getName().length() + stateIcon.length() + 2;
        sb.append(padRight("", 64 - currentLen)).append("║\n");
        
        // 打印后继节点
        for (String successorId : node.getSuccessors()) {
            WorkflowNode successor = graph.getNodes().get(successorId);
            if (successor != null) {
                sb.append(indent).append("    │\n");
                sb.append(indent).append("    ▼\n");
                printNodeAscii(sb, successor, depth + 1, visited);
            }
        }
    }

    /**
     * 获取节点图标
     */
    private String getNodeIcon(WorkflowNode node) {
        return switch (node.getNodeType()) {
            case ACTION -> "[■]";
            case DECISION -> "<◇>";
            case PARALLEL -> "⟨⟩";
            case LOOP -> "(○)";
            case SUBWORKFLOW -> "[[]]";
            default -> "[?]";
        };
    }

    /**
     * 获取状态图标
     */
    private String getStateIcon(WorkflowNode node) {
        return switch (node.getState()) {
            case PENDING -> "⏳";
            case READY -> "🔵";
            case RUNNING -> "🔄";
            case COMPLETED -> "✅";
            case FAILED -> "❌";
            case SKIPPED -> "⏭️";
            default -> "❓";
        };
    }

    // ============= 工具方法 =============

    /**
     * 获取节点标签
     */
    private String getNodeLabel(WorkflowNode node) {
        if (config.isShowDescription() && node.getDescription() != null) {
            return node.getName() + "\\n" + node.getDescription();
        }
        return node.getName();
    }

    /**
     * 获取边标签
     */
    private String getEdgeLabel(WorkflowNode fromNode, String toNodeId) {
        if (fromNode.getNodeType() == WorkflowNode.NodeType.DECISION) {
            if (fromNode.getBranches() != null) {
                for (Map.Entry<String, String> entry : fromNode.getBranches().entrySet()) {
                    if (entry.getValue().equals(toNodeId)) {
                        return entry.getKey();
                    }
                }
            }
        }
        return null;
    }

    /**
     * 获取节点颜色（基于状态）
     */
    private String getNodeColor(WorkflowNode node) {
        if (!config.isShowStateColors()) {
            return null;
        }
        
        return switch (node.getState()) {
            case PENDING -> "#f0f0f0";
            case READY -> "#add8e6";
            case RUNNING -> "#ffd700";
            case COMPLETED -> "#90ee90";
            case FAILED -> "#ff6b6b";
            case SKIPPED -> "#d3d3d3";
            default -> null;
        };
    }

    /**
     * 净化ID（用于Mermaid）
     */
    private String sanitizeId(String id) {
        return id.replaceAll("[^a-zA-Z0-9]", "_");
    }

    /**
     * 引用ID（用于DOT）
     */
    private String quoteId(String id) {
        return "\"" + id.replace("\"", "\\\"") + "\"";
    }

    /**
     * 转义标签
     */
    private String escapeLabel(String label) {
        if (label == null) return "";
        return label.replace("\"", "'")
                   .replace("\n", "\\n")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;");
    }

    /**
     * 转义JSON字符串
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }

    /**
     * 右填充字符串
     */
    private String padRight(String str, int length) {
        if (str == null) str = "";
        if (str.length() >= length) return str.substring(0, length);
        return str + " ".repeat(length - str.length());
    }

    // ============= 文件导出 =============

    /**
     * 导出到文件
     */
    public void exportToFile(String path, ExportFormat format) throws IOException {
        String content = switch (format) {
            case MERMAID -> toMermaid();
            case DOT -> toDot();
            case JSON -> toJson();
            case ASCII -> toAscii();
        };
        
        try (Writer writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8))) {
            writer.write(content);
        }
        
        log.info("工作流已导出到: {} (格式: {})", path, format);
    }

    // ============= 配置和枚举 =============

    /**
     * 导出格式
     */
    public enum ExportFormat {
        MERMAID,  // Mermaid 格式
        DOT,      // Graphviz DOT 格式
        JSON,     // JSON 格式
        ASCII     // ASCII 图
    }

    /**
     * 导出配置
     */
    @Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ExportConfig {
        @lombok.Builder.Default
        private boolean leftToRight = false;  // 方向：false=上到下，true=左到右
        @lombok.Builder.Default
        private boolean showDescription = true;  // 显示描述
        @lombok.Builder.Default
        private boolean showStateColors = true;  // 显示状态颜色
        @lombok.Builder.Default
        private String fontName = "Arial";  // 字体名称
    }
}
