package io.leavesfly.evox.workflow.generation;

import io.leavesfly.evox.core.agent.IAgent;
import io.leavesfly.evox.workflow.base.Workflow;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流模板管理器
 * 预定义常用工作流模板
 *
 * @author EvoX Team
 */
@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTemplateManager {

    /**
     * 工作流模板库
     */
    private Map<String, WorkflowTemplate> templates;

    /**
     * 工作流生成器
     */
    private WorkflowGenerator generator;

    public WorkflowTemplateManager(WorkflowGenerator generator) {
        this.generator = generator;
        this.templates = new HashMap<>();
        initializeDefaultTemplates();
    }

    /**
     * 初始化默认模板
     */
    private void initializeDefaultTemplates() {
        // 数据处理模板
        registerTemplate("data_processing", WorkflowTemplate.builder()
                .name("数据处理")
                .description("ETL数据处理流程")
                .steps(List.of(
                        new TemplateStep("数据提取", "ExtractAgent", "从数据源提取数据"),
                        new TemplateStep("数据转换", "TransformAgent", "转换数据格式"),
                        new TemplateStep("数据加载", "LoadAgent", "加载到目标系统")
                ))
                .build());

        // 审批流程模板
        registerTemplate("approval", WorkflowTemplate.builder()
                .name("审批流程")
                .description("多级审批工作流")
                .steps(List.of(
                        new TemplateStep("提交申请", "SubmitAgent", "提交审批申请"),
                        new TemplateStep("初审", "ReviewAgent", "初步审核"),
                        new TemplateStep("终审", "ApprovalAgent", "最终批准")
                ))
                .build());

        // 问答流程模板
        registerTemplate("qa_flow", WorkflowTemplate.builder()
                .name("问答流程")
                .description("智能问答处理流程")
                .steps(List.of(
                        new TemplateStep("理解问题", "UnderstandAgent", "分析问题意图"),
                        new TemplateStep("检索信息", "RetrieveAgent", "从知识库检索"),
                        new TemplateStep("生成答案", "GenerateAgent", "生成回答")
                ))
                .build());

        log.info("初始化{}个默认模板", templates.size());
    }

    /**
     * 注册模板
     */
    public void registerTemplate(String templateId, WorkflowTemplate template) {
        templates.put(templateId, template);
        log.info("注册模板: {} - {}", templateId, template.getName());
    }

    /**
     * 获取模板
     */
    public WorkflowTemplate getTemplate(String templateId) {
        return templates.get(templateId);
    }

    /**
     * 列出所有模板
     */
    public List<String> listTemplates() {
        return new ArrayList<>(templates.keySet());
    }

    /**
     * 根据模板创建工作流
     *
     * @param templateId 模板ID
     * @param agents Agent列表
     * @return 生成的工作流
     */
    public Workflow createFromTemplate(String templateId, List<IAgent> agents) {
        WorkflowTemplate template = templates.get(templateId);
        if (template == null) {
            throw new IllegalArgumentException("模板不存在: " + templateId);
        }

        log.info("使用模板创建工作流: {}", template.getName());

        // 使用生成器创建工作流
        if (generator != null) {
            generator.setAvailableAgents(agents);
            return generator.generateWorkflow(template.getDescription());
        }

        throw new IllegalStateException("未配置工作流生成器");
    }

    /**
     * 工作流模板
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @lombok.Builder
    public static class WorkflowTemplate {
        /**
         * 模板名称
         */
        private String name;

        /**
         * 模板描述
         */
        private String description;

        /**
         * 步骤列表
         */
        private List<TemplateStep> steps;

        /**
         * 模板元数据
         */
        private Map<String, Object> metadata;
    }

    /**
     * 模板步骤
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateStep {
        /**
         * 步骤名称
         */
        private String name;

        /**
         * Agent类型
         */
        private String agentType;

        /**
         * 步骤描述
         */
        private String description;
    }
}
