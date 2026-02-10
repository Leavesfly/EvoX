package io.leavesfly.evox.cowork.config;

import io.leavesfly.evox.cowork.connector.ConnectorManager;
import io.leavesfly.evox.cowork.event.CoworkEventBus;
import io.leavesfly.evox.cowork.permission.CoworkPermissionManager;
import io.leavesfly.evox.cowork.permission.InteractivePermissionManager;
import io.leavesfly.evox.cowork.plugin.PluginManager;
import io.leavesfly.evox.cowork.session.SessionManager;
import io.leavesfly.evox.cowork.task.TaskDecomposer;
import io.leavesfly.evox.cowork.task.TaskManager;
import io.leavesfly.evox.cowork.template.TemplateManager;
import io.leavesfly.evox.cowork.workspace.WorkspaceManager;
import io.leavesfly.evox.models.factory.LLMFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Paths;

@Configuration
public class CoworkBeanConfig {

    @Bean
    public CoworkConfig coworkConfig() {
        return CoworkConfig.createDefault(System.getProperty("user.dir"));
    }

    @Bean
    public CoworkEventBus coworkEventBus() {
        return new CoworkEventBus();
    }

    @Bean
    public InteractivePermissionManager interactivePermissionManager(CoworkConfig config, CoworkEventBus eventBus) {
        return new InteractivePermissionManager(config, eventBus);
    }

    @Bean
    public SessionManager sessionManager(CoworkConfig config, InteractivePermissionManager permissionManager, CoworkEventBus eventBus) {
        SessionManager sessionManager = new SessionManager(config, permissionManager.getPermissionManager());
        sessionManager.setEventCallback(sessionEvent -> eventBus.emitSessionUpdate(
                sessionEvent.sessionId(),
                sessionEvent.type().name(),
                sessionEvent.data()
        ));
        return sessionManager;
    }

    @Bean
    public TaskManager taskManager(CoworkConfig config) {
        return new TaskManager(new TaskDecomposer(LLMFactory.create(config.getLlmConfig())));
    }

    @Bean
    public PluginManager pluginManager(CoworkConfig config) {
        return new PluginManager(config.getPluginDirectory());
    }

    @Bean
    public ConnectorManager connectorManager(CoworkConfig config) {
        return new ConnectorManager(config.getNetworkAllowlist());
    }

    @Bean
    public TemplateManager templateManager() {
        String templateDir = Paths.get(System.getProperty("user.home"), ".evox", "cowork", "templates").toString();
        return new TemplateManager(templateDir);
    }

    @Bean
    public WorkspaceManager workspaceManager() {
        String workspaceDir = Paths.get(System.getProperty("user.home"), ".evox", "cowork").toString();
        return new WorkspaceManager(workspaceDir);
    }
}
