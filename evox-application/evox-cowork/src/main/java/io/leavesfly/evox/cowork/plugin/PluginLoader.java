package io.leavesfly.evox.cowork.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class PluginLoader {
    private final ObjectMapper objectMapper;
    private final YAMLMapper yamlMapper;

    public PluginLoader() {
        this.objectMapper = new ObjectMapper();
        this.yamlMapper = new YAMLMapper();
    }

    // 从指定目录加载插件（支持 yaml/json 格式）
    public List<CoworkPlugin> loadFromDirectory(String directoryPath) {
        List<CoworkPlugin> plugins = new ArrayList<>();
        Path directory = Paths.get(directoryPath);

        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
                log.info("Created plugin directory: {}", directoryPath);
            } catch (IOException e) {
                log.error("Failed to create plugin directory: {}", directoryPath, e);
            }
            return plugins;
        }

        try (Stream<Path> paths = Files.walk(directory, 1)) {
            List<Path> pluginFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileName = path.getFileName().toString().toLowerCase();
                        return fileName.endsWith(".yaml") || fileName.endsWith(".yml") || fileName.endsWith(".json");
                    })
                    .toList();

            for (Path file : pluginFiles) {
                CoworkPlugin plugin = loadFromFile(file);
                if (plugin != null) {
                    plugins.add(plugin);
                }
            }

            log.info("Loaded {} plugins from directory: {}", plugins.size(), directoryPath);
        } catch (IOException e) {
            log.error("Failed to walk plugin directory: {}", directoryPath, e);
        }

        return plugins;
    }

    // 从单个文件加载插件
    public CoworkPlugin loadFromFile(Path filePath) {
        try {
            String fileName = filePath.getFileName().toString().toLowerCase();
            String content = Files.readString(filePath);

            if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
                return yamlMapper.readValue(content, CoworkPlugin.class);
            } else if (fileName.endsWith(".json")) {
                return objectMapper.readValue(content, CoworkPlugin.class);
            }
        } catch (IOException e) {
            log.error("Failed to load plugin from file: {}", filePath, e);
        }
        return null;
    }

    // 加载内置插件
    public List<CoworkPlugin> loadBuiltinPlugins() {
        List<CoworkPlugin> plugins = new ArrayList<>();

        // 生产力插件
        CoworkPlugin productivityPlugin = new CoworkPlugin();
        productivityPlugin.setPluginId("productivity");
        productivityPlugin.setName("Productivity");
        productivityPlugin.setDescription("Manage tasks, calendars, and daily workflows");
        productivityPlugin.setCategory("productivity");
        CoworkPlugin.PluginCommand dailyBriefingCommand = new CoworkPlugin.PluginCommand();
        dailyBriefingCommand.setName("/daily-briefing");
        dailyBriefingCommand.setDescription("Generate a daily briefing from your files and notes");
        dailyBriefingCommand.setPromptTemplate("Review all files in the specified directory and create a comprehensive daily briefing covering priorities, deadlines, and action items.");
        dailyBriefingCommand.setRequiredInputs(List.of("directory"));
        productivityPlugin.setCommands(List.of(dailyBriefingCommand));
        plugins.add(productivityPlugin);

        // 数据分析插件
        CoworkPlugin dataPlugin = new CoworkPlugin();
        dataPlugin.setPluginId("data");
        dataPlugin.setName("Data Analysis");
        dataPlugin.setDescription("Query, visualize, and interpret datasets");
        dataPlugin.setCategory("data");
        CoworkPlugin.PluginCommand analyzeDataCommand = new CoworkPlugin.PluginCommand();
        analyzeDataCommand.setName("/analyze-data");
        analyzeDataCommand.setDescription("Analyze a dataset and generate insights");
        analyzeDataCommand.setPromptTemplate("Load the specified data file, perform statistical analysis including summary statistics, correlations, and outlier detection. Generate a report with key findings and visualizations.");
        analyzeDataCommand.setRequiredInputs(List.of("file"));
        dataPlugin.setCommands(List.of(analyzeDataCommand));
        plugins.add(dataPlugin);

        // 研究插件
        CoworkPlugin researchPlugin = new CoworkPlugin();
        researchPlugin.setPluginId("research");
        researchPlugin.setName("Research & Analysis");
        researchPlugin.setDescription("Search literature, analyze results, and synthesize findings");
        researchPlugin.setCategory("research");
        CoworkPlugin.PluginCommand researchCommand = new CoworkPlugin.PluginCommand();
        researchCommand.setName("/research");
        researchCommand.setDescription("Research a topic and compile findings");
        researchCommand.setPromptTemplate("Conduct comprehensive research on the given topic using web search and available documents. Synthesize findings into a structured report with sources.");
        researchCommand.setRequiredInputs(List.of("topic"));
        researchPlugin.setCommands(List.of(researchCommand));
        plugins.add(researchPlugin);

        // 文档管理插件
        CoworkPlugin documentPlugin = new CoworkPlugin();
        documentPlugin.setPluginId("document");
        documentPlugin.setName("Document Management");
        documentPlugin.setDescription("Create, organize, and manage documents");
        documentPlugin.setCategory("productivity");
        CoworkPlugin.PluginCommand organizeFilesCommand = new CoworkPlugin.PluginCommand();
        organizeFilesCommand.setName("/organize-files");
        organizeFilesCommand.setDescription("Organize files in a directory by type and date");
        organizeFilesCommand.setPromptTemplate("Scan the specified directory, categorize files by type and date, rename with consistent conventions, and move to organized subdirectories.");
        organizeFilesCommand.setRequiredInputs(List.of("directory"));
        
        CoworkPlugin.PluginCommand createReportCommand = new CoworkPlugin.PluginCommand();
        createReportCommand.setName("/create-report");
        createReportCommand.setDescription("Create a formatted report from notes");
        createReportCommand.setPromptTemplate("Read all notes and source materials in the specified directory, synthesize the information, and create a polished report document.");
        createReportCommand.setRequiredInputs(List.of("directory", "title"));
        
        documentPlugin.setCommands(List.of(organizeFilesCommand, createReportCommand));
        plugins.add(documentPlugin);

        return plugins;
    }
}