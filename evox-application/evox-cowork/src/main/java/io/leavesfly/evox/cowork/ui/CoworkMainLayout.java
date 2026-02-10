package io.leavesfly.evox.cowork.ui;

import io.leavesfly.evox.cowork.session.CoworkSession;
import javafx.scene.layout.BorderPane;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CoworkMainLayout extends BorderPane {

    private final CoworkServiceBridge serviceBridge;
    private final UIEventBus uiEventBus;
    private final SidebarPanel sidebarPanel;
    private final ChatPanel chatPanel;

    public CoworkMainLayout(CoworkServiceBridge serviceBridge) {
        this.serviceBridge = serviceBridge;
        this.uiEventBus = new UIEventBus();
        getStyleClass().add("main-layout");

        // Initialize child components without circular dependencies
        this.chatPanel = new ChatPanel(serviceBridge);
        this.sidebarPanel = new SidebarPanel(serviceBridge, uiEventBus);

        setLeft(sidebarPanel);
        setCenter(chatPanel);

        // Wire up UI event bus listeners to decouple SidebarPanel ↔ ChatPanel
        uiEventBus.on(UIEventBus.SESSION_SELECTED, data -> {
            if (data instanceof CoworkSession session) {
                chatPanel.loadSession(session);
            }
        });
        uiEventBus.on(UIEventBus.INPUT_TEXT_SET, data -> {
            if (data instanceof String text) {
                chatPanel.setInputText(text);
            }
        });

        // Wire up service bridge callbacks
        serviceBridge.setOnStreamContent(content -> chatPanel.appendStreamContent(content));
        serviceBridge.setOnPermissionRequest(request -> chatPanel.showPermissionCard(request));
        serviceBridge.setOnSessionCreated(session -> {
            uiEventBus.emit(UIEventBus.SESSION_LIST_REFRESH, null);
            sidebarPanel.refreshSessionList();
            chatPanel.loadSession(session);
        });
        serviceBridge.setOnError(error -> chatPanel.showSystemMessage("Error: " + error));
    }

    public void onAppReady() {
        log.info("EvoX Cowork desktop application ready");
        sidebarPanel.refreshSessionList();
    }
}