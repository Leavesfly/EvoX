package io.leavesfly.evox.cowork.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class TemplateManager {
    private final Map<String, WorkflowTemplate> templates;
    private final String templateDirectory;
    private final ObjectMapper objectMapper;

    public TemplateManager(String templateDirectory) {
        this.templates = new ConcurrentHashMap<>();
        this.templateDirectory = templateDirectory;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        loadTemplates();
        loadBuiltinTemplates();
    }

    // 从指定目录加载 JSON 模板文件
    private void loadTemplates() {
        File dir = new File(templateDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
            log.info("Created template directory: {}", templateDirectory);
            return;
        }

        if (!dir.isDirectory()) {
            log.error("Template path is not a directory: {}", templateDirectory);
            return;
        }

        File[] jsonFiles = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (jsonFiles != null) {
            for (File file : jsonFiles) {
                try {
                    WorkflowTemplate template = objectMapper.readValue(file, WorkflowTemplate.class);
                    templates.put(template.getTemplateId(), template);
                    log.info("Loaded template: {} from {}", template.getName(), file.getName());
                } catch (IOException e) {
                    log.error("Failed to load template from {}: {}", file.getName(), e.getMessage());
                }
            }
        }
        log.info("Loaded {} templates from {}", templates.size(), templateDirectory);
    }

    // 加载内置模板
    private void loadBuiltinTemplates() {
        // daily-briefing template / 每日简报模板
        WorkflowTemplate dailyBriefing = new WorkflowTemplate();
        dailyBriefing.setName("daily-briefing");
        dailyBriefing.setDescription("Create a comprehensive daily briefing");
        dailyBriefing.setCategory("productivity");
        dailyBriefing.setPromptTemplate("Review all files in {{directory}} and create a comprehensive daily briefing covering: 1) Key priorities and deadlines 2) Recent changes and updates 3) Action items for today 4) Important notes and reminders");
        
        WorkflowTemplate.TemplateVariable directoryVar = new WorkflowTemplate.TemplateVariable();
        directoryVar.setName("directory");
        directoryVar.setDescription("Directory to scan");
        directoryVar.setRequired(true);
        dailyBriefing.getVariables().add(directoryVar);
        
        dailyBriefing.getTags().add("productivity");
        dailyBriefing.getTags().add("daily");
        templates.put(dailyBriefing.getTemplateId(), dailyBriefing);

        // file-organizer template / 文件整理模板
        WorkflowTemplate fileOrganizer = new WorkflowTemplate();
        fileOrganizer.setName("file-organizer");
        fileOrganizer.setDescription("Organize files by type and naming conventions");
        fileOrganizer.setCategory("productivity");
        fileOrganizer.setPromptTemplate("Scan {{directory}} and organize files by: 1) Categorize by file type 2) Rename with consistent naming conventions 3) Move to organized subdirectories 4) Generate a summary report of changes made");
        
        WorkflowTemplate.TemplateVariable organizerDirVar = new WorkflowTemplate.TemplateVariable();
        organizerDirVar.setName("directory");
        organizerDirVar.setDescription("Directory to organize");
        organizerDirVar.setRequired(true);
        fileOrganizer.getVariables().add(organizerDirVar);
        
        fileOrganizer.getTags().add("productivity");
        fileOrganizer.getTags().add("files");
        templates.put(fileOrganizer.getTemplateId(), fileOrganizer);

        // research-report template / 研究报告模板
        WorkflowTemplate researchReport = new WorkflowTemplate();
        researchReport.setName("research-report");
        researchReport.setDescription("Create a structured research report");
        researchReport.setCategory("research");
        researchReport.setPromptTemplate("Research the topic '{{topic}}' thoroughly: 1) Search for relevant information 2) Analyze key findings 3) Synthesize into a structured report with sections: Introduction, Key Findings, Analysis, Conclusion 4) Include sources and references");
        
        WorkflowTemplate.TemplateVariable topicVar = new WorkflowTemplate.TemplateVariable();
        topicVar.setName("topic");
        topicVar.setDescription("Research topic");
        topicVar.setRequired(true);
        researchReport.getVariables().add(topicVar);
        
        researchReport.getTags().add("research");
        researchReport.getTags().add("report");
        templates.put(researchReport.getTemplateId(), researchReport);

        // data-analysis template / 数据分析模板
        WorkflowTemplate dataAnalysis = new WorkflowTemplate();
        dataAnalysis.setName("data-analysis");
        dataAnalysis.setDescription("Analyze data file and generate insights");
        dataAnalysis.setCategory("data");
        dataAnalysis.setPromptTemplate("Analyze the data file at {{file}}: 1) Load and inspect the data 2) Compute summary statistics 3) Identify patterns, trends, and outliers 4) Generate a report with key insights and recommendations");
        
        WorkflowTemplate.TemplateVariable fileVar = new WorkflowTemplate.TemplateVariable();
        fileVar.setName("file");
        fileVar.setDescription("Path to data file");
        fileVar.setRequired(true);
        dataAnalysis.getVariables().add(fileVar);
        
        dataAnalysis.getTags().add("data");
        dataAnalysis.getTags().add("analysis");
        templates.put(dataAnalysis.getTemplateId(), dataAnalysis);

        // meeting-notes template / 会议纪要模板
        WorkflowTemplate meetingNotes = new WorkflowTemplate();
        meetingNotes.setName("meeting-notes");
        meetingNotes.setDescription("Process meeting notes into structured summary");
        meetingNotes.setCategory("document");
        meetingNotes.setPromptTemplate("Process the meeting notes in {{file}} and create a structured summary: 1) Meeting overview and attendees 2) Key discussion points 3) Decisions made 4) Action items with owners and deadlines 5) Follow-up items");
        
        WorkflowTemplate.TemplateVariable meetingFileVar = new WorkflowTemplate.TemplateVariable();
        meetingFileVar.setName("file");
        meetingFileVar.setDescription("Path to meeting notes file");
        meetingFileVar.setRequired(true);
        meetingNotes.getVariables().add(meetingFileVar);
        
        meetingNotes.getTags().add("document");
        meetingNotes.getTags().add("meeting");
        templates.put(meetingNotes.getTemplateId(), meetingNotes);

        log.info("Loaded {} builtin templates", 5);
    }

    // 保存模板到文件
    public void saveTemplate(WorkflowTemplate template) {
        try {
            File dir = new File(templateDirectory);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File file = new File(dir, template.getTemplateId() + ".json");
            objectMapper.writeValue(file, template);
            templates.put(template.getTemplateId(), template);
            log.info("Saved template: {} to {}", template.getName(), file.getName());
        } catch (IOException e) {
            log.error("Failed to save template {}: {}", template.getName(), e.getMessage());
            throw new RuntimeException("Failed to save template", e);
        }
    }

    // 删除模板
    public void deleteTemplate(String templateId) {
        WorkflowTemplate template = templates.remove(templateId);
        if (template != null) {
            File file = new File(templateDirectory, templateId + ".json");
            if (file.exists()) {
                file.delete();
                log.info("Deleted template: {}", template.getName());
            }
        }
    }

    public WorkflowTemplate getTemplate(String templateId) {
        return templates.get(templateId);
    }

    public List<WorkflowTemplate> getAllTemplates() {
        return new ArrayList<>(templates.values());
    }

    public List<WorkflowTemplate> getTemplatesByCategory(String category) {
        return templates.values().stream()
                .filter(t -> category.equals(t.getCategory()))
                .collect(Collectors.toList());
    }

    // 搜索模板（按名称、描述或标签）
    public List<WorkflowTemplate> searchTemplates(String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        return templates.values().stream()
                .filter(t -> t.getName().toLowerCase().contains(lowerKeyword) 
                        || t.getDescription().toLowerCase().contains(lowerKeyword)
                        || t.getTags().stream().anyMatch(tag -> tag.toLowerCase().contains(lowerKeyword)))
                .collect(Collectors.toList());
    }

    // 渲染模板（替换变量）
    public String renderTemplate(String templateId, Map<String, String> variables) {
        WorkflowTemplate template = getTemplate(templateId);
        if (template == null) {
            throw new IllegalArgumentException("Template not found: " + templateId);
        }
        template.incrementUsage();
        return template.render(variables);
    }
}