package io.leavesfly.evox.tools.system;

import io.leavesfly.evox.tools.base.BaseTool;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.util.*;

@Slf4j
public class SystemInfoTool extends BaseTool {

    public SystemInfoTool() {
        super();
        this.name = "system_info";
        this.description = "Get system information including CPU, memory, disk, OS, and network details.";

        this.inputs = new HashMap<>();
        this.required = new ArrayList<>();

        Map<String, String> categoryParam = new HashMap<>();
        categoryParam.put("type", "string");
        categoryParam.put("description", "Information category: 'all', 'cpu', 'memory', 'disk', 'os', 'network', 'java' (default: 'all')");
        this.inputs.put("category", categoryParam);
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        String category = getParameter(parameters, "category", "all");

        Map<String, Object> info = new LinkedHashMap<>();

        switch (category) {
            case "cpu" -> info.put("cpu", getCpuInfo());
            case "memory" -> info.put("memory", getMemoryInfo());
            case "disk" -> info.put("disk", getDiskInfo());
            case "os" -> info.put("os", getOsInfo());
            case "network" -> info.put("network", getNetworkInfo());
            case "java" -> info.put("java", getJavaInfo());
            default -> {
                info.put("os", getOsInfo());
                info.put("cpu", getCpuInfo());
                info.put("memory", getMemoryInfo());
                info.put("disk", getDiskInfo());
                info.put("network", getNetworkInfo());
                info.put("java", getJavaInfo());
            }
        }

        return ToolResult.success(info);
    }

    private Map<String, Object> getCpuInfo() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        Map<String, Object> cpu = new LinkedHashMap<>();
        cpu.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        cpu.put("arch", osBean.getArch());
        cpu.put("systemLoadAverage", osBean.getSystemLoadAverage());
        return cpu;
    }

    private Map<String, Object> getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        Map<String, Object> memory = new LinkedHashMap<>();
        memory.put("totalMemoryMB", runtime.totalMemory() / (1024 * 1024));
        memory.put("freeMemoryMB", runtime.freeMemory() / (1024 * 1024));
        memory.put("usedMemoryMB", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
        memory.put("maxMemoryMB", runtime.maxMemory() / (1024 * 1024));
        memory.put("heapUsage", memoryBean.getHeapMemoryUsage().toString());
        memory.put("nonHeapUsage", memoryBean.getNonHeapMemoryUsage().toString());
        return memory;
    }

    private Map<String, Object> getDiskInfo() {
        Map<String, Object> disk = new LinkedHashMap<>();
        File[] roots = File.listRoots();
        List<Map<String, Object>> partitions = new ArrayList<>();
        for (File root : roots) {
            Map<String, Object> partition = new LinkedHashMap<>();
            partition.put("path", root.getAbsolutePath());
            partition.put("totalSpaceGB", root.getTotalSpace() / (1024.0 * 1024 * 1024));
            partition.put("freeSpaceGB", root.getFreeSpace() / (1024.0 * 1024 * 1024));
            partition.put("usableSpaceGB", root.getUsableSpace() / (1024.0 * 1024 * 1024));
            long totalSpace = root.getTotalSpace();
            if (totalSpace > 0) {
                partition.put("usagePercent", String.format("%.1f%%",
                        (1.0 - (double) root.getFreeSpace() / totalSpace) * 100));
            }
            partitions.add(partition);
        }
        disk.put("partitions", partitions);
        return disk;
    }

    private Map<String, Object> getOsInfo() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        Map<String, Object> os = new LinkedHashMap<>();
        os.put("name", System.getProperty("os.name"));
        os.put("version", System.getProperty("os.version"));
        os.put("arch", osBean.getArch());
        os.put("user", System.getProperty("user.name"));
        os.put("homeDir", System.getProperty("user.home"));
        os.put("workDir", System.getProperty("user.dir"));
        os.put("tempDir", System.getProperty("java.io.tmpdir"));
        return os;
    }

    private Map<String, Object> getNetworkInfo() {
        Map<String, Object> network = new LinkedHashMap<>();
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            network.put("hostname", localHost.getHostName());
            network.put("ipAddress", localHost.getHostAddress());
        } catch (Exception e) {
            network.put("error", "Unable to get network info: " + e.getMessage());
        }
        return network;
    }

    private Map<String, Object> getJavaInfo() {
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        Map<String, Object> java = new LinkedHashMap<>();
        java.put("version", System.getProperty("java.version"));
        java.put("vendor", System.getProperty("java.vendor"));
        java.put("vmName", runtimeBean.getVmName());
        java.put("vmVersion", runtimeBean.getVmVersion());
        java.put("uptimeMs", runtimeBean.getUptime());
        java.put("startTime", new Date(runtimeBean.getStartTime()).toString());
        java.put("classpath", runtimeBean.getClassPath());
        return java;
    }
}
