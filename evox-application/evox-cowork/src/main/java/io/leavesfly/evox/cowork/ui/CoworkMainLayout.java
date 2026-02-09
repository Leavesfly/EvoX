package io.leavesfly.evox.cowork.ui;

import javafx.scene.layout.BorderPane;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CoworkMainLayout extends BorderPane {

    private final CoworkServiceBridge serviceBridge;
    private final SidebarPanel sidebarPanel;
    private final ChatPanel chatPanel;

    public CoworkMainLayout(CoworkServiceBridge serviceBridge) {
        this.serviceBridge = serviceBridge;
        getStyleClass().add("main-layout");

        this.chatPanel = new ChatPanel(serviceBridge);
        this.sidebarPanel = new SidebarPanel(serviceBridge, chatPanel);

        setLeft(sidebarPanel);
        setCenter(chatPanel);

        serviceBridge.setOnStreamContent(content -> chatPanel.appendStreamContent(content));
        serviceBridge.setOnPermissionRequest(request -> chatPanel.showPermissionCard(request));
        serviceBridge.setOnSessionCreated(session -> {
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
