package io.leavesfly.evox.cowork.ui;

import io.leavesfly.evox.cowork.session.CoworkSession;
import io.leavesfly.evox.cowork.template.WorkflowTemplate;
import io.leavesfly.evox.cowork.workspace.Workspace;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
public class SidebarPanel extends VBox {

    private final CoworkServiceBridge serviceBridge;
    private final ChatPanel chatPanel;
    private final ListView<CoworkSession> sessionListView;
    private final ObservableList<CoworkSession> sessionItems;
    private final VBox workspaceSection;
    private final VBox templateSection;

    public SidebarPanel(CoworkServiceBridge serviceBridge, ChatPanel chatPanel) {
        this.serviceBridge = serviceBridge;
        this.chatPanel = chatPanel;
        this.sessionItems = FXCollections.observableArrayList();
        this.sessionListView = new ListView<>(sessionItems);

        getStyleClass().add("sidebar");
        setPrefWidth(280);
        setMinWidth(280);
        setMaxWidth(280);

        VBox header = buildHeader();
        VBox buttonContainer = buildNewSessionButtonContainer();
        VBox sessionSection = buildSessionSection();
        this.workspaceSection = buildWorkspaceSection();
        this.templateSection = buildTemplateSection();

        VBox scrollContent = new VBox(8);
        scrollContent.getChildren().addAll(sessionSection, createSeparator(), workspaceSection, createSeparator(), templateSection);
        scrollContent.setPadding(new Insets(0, 0, 16, 0));

        ScrollPane scrollPane = new ScrollPane(scrollContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(header, buttonContainer, scrollPane);
    }

    private VBox buildHeader() {
        VBox header = new VBox(2);
        header.getStyleClass().add("sidebar-header");

        Label title = new Label("\u26A1 EvoX Cowork");
        title.getStyleClass().add("sidebar-title");

        Label subtitle = new Label("Knowledge Work Assistant");
        subtitle.getStyleClass().add("sidebar-subtitle");

        header.getChildren().addAll(title, subtitle);
        return header;
    }

    private VBox buildNewSessionButtonContainer() {
        Button newSessionButton = new Button("\uFF0B  New Session");
        newSessionButton.getStyleClass().add("new-session-button");
        newSessionButton.setMaxWidth(Double.MAX_VALUE);
        newSessionButton.setOnAction(event -> {
            String workingDir = null;
            Workspace activeWorkspace = serviceBridge.getWorkspaceManager().getActiveWorkspace();
            if (activeWorkspace != null) {
                workingDir = activeWorkspace.getDirectory();
            }
            serviceBridge.createSession(workingDir);
        });

        VBox container = new VBox();
        container.setPadding(new Insets(8, 16, 8, 16));
        container.getChildren().add(newSessionButton);
        return container;
    }

    private VBox buildSessionSection() {
        VBox section = new VBox(4);
        section.setPadding(new Insets(0, 8, 0, 8));

        Label label = new Label("SESSIONS");
        label.setStyle("-fx-text-fill: #5555aa; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 8;");

        sessionListView.getStyleClass().add("session-list");
        sessionListView.setPrefHeight(280);
        sessionListView.setStyle("-fx-background-color: transparent;");
        sessionListView.setCellFactory(listView -> new SessionListCell());
        sessionListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                serviceBridge.switchSession(newValue.getSessionId());
                chatPanel.loadSession(newValue);
            }
        });

        section.getChildren().addAll(label, sessionListView);
        return section;
    }

    private VBox buildWorkspaceSection() {
        VBox section = new VBox(4);
        section.setPadding(new Insets(0, 8, 0, 8));

        HBox sectionHeader = new HBox();
        sectionHeader.setAlignment(Pos.CENTER_LEFT);
        sectionHeader.setSpacing(8);
        sectionHeader.setPadding(new Insets(4, 8, 4, 8));

        Label label = new Label("WORKSPACES");
        label.setStyle("-fx-text-fill: #5555aa; -fx-font-size: 11px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addButton = new Button("+");
        addButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #8888aa; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0 4;");
        addButton.setOnAction(event -> showAddWorkspaceDialog());

        sectionHeader.getChildren().addAll(label, spacer, addButton);
        section.getChildren().add(sectionHeader);

        refreshWorkspaceItems();
        return section;
    }

    private VBox buildTemplateSection() {
        VBox section = new VBox(4);
        section.setPadding(new Insets(0, 8, 0, 8));

        HBox sectionHeader = new HBox();
        sectionHeader.setAlignment(Pos.CENTER_LEFT);
        sectionHeader.setSpacing(8);
        sectionHeader.setPadding(new Insets(4, 8, 4, 8));

        Label label = new Label("TEMPLATES");
        label.setStyle("-fx-text-fill: #5555aa; -fx-font-size: 11px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button browseButton = new Button("\u2026");
        browseButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #8888aa; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0 4;");
        browseButton.setOnAction(event -> openTemplateDialog());

        sectionHeader.getChildren().addAll(label, spacer, browseButton);
        section.getChildren().add(sectionHeader);

        refreshTemplateItems();
        return section;
    }

    private void openTemplateDialog() {
        Stage ownerStage = (Stage) getScene().getWindow();
        TemplateDialog dialog = new TemplateDialog(ownerStage, serviceBridge, (templateName, renderedContent) -> {
            chatPanel.setInputText(renderedContent);
        });
        dialog.showAndWait();
    }

    public void refreshSessionList() {
        Platform.runLater(() -> {
            List<CoworkSession> sessions = serviceBridge.listSessions();
            sessionItems.setAll(sessions);
        });
    }

    private void refreshWorkspaceItems() {
        Platform.runLater(() -> {
            List<Workspace> workspaces = serviceBridge.getAllWorkspaces();
            while (workspaceSection.getChildren().size() > 1) {
                workspaceSection.getChildren().remove(workspaceSection.getChildren().size() - 1);
            }

            if (workspaces.isEmpty()) {
                Label emptyLabel = new Label("No workspaces yet");
                emptyLabel.setStyle("-fx-text-fill: #555577; -fx-font-size: 12px; -fx-padding: 4 16;");
                workspaceSection.getChildren().add(emptyLabel);
            } else {
                for (Workspace workspace : workspaces) {
                    HBox item = createWorkspaceItem(workspace);
                    workspaceSection.getChildren().add(item);
                }
            }
        });
    }

    private void refreshTemplateItems() {
        Platform.runLater(() -> {
            List<WorkflowTemplate> templates = serviceBridge.getAllTemplates();
            while (templateSection.getChildren().size() > 1) {
                templateSection.getChildren().remove(templateSection.getChildren().size() - 1);
            }

            if (templates.isEmpty()) {
                Label emptyLabel = new Label("No templates available");
                emptyLabel.setStyle("-fx-text-fill: #555577; -fx-font-size: 12px; -fx-padding: 4 16;");
                templateSection.getChildren().add(emptyLabel);
            } else {
                for (WorkflowTemplate template : templates) {
                    HBox item = createTemplateItem(template);
                    templateSection.getChildren().add(item);
                }
            }
        });
    }

    private HBox createWorkspaceItem(Workspace workspace) {
        HBox item = new HBox(8);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(6, 12, 6, 12));
        item.setStyle("-fx-background-radius: 6; -fx-cursor: hand;");

        item.setOnMouseEntered(event -> item.setStyle("-fx-background-color: #1a1a3e; -fx-background-radius: 6; -fx-cursor: hand;"));
        item.setOnMouseExited(event -> item.setStyle("-fx-background-radius: 6; -fx-cursor: hand;"));

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox nameRow = new HBox(4);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(workspace.getName());
        nameLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 13px;");
        nameRow.getChildren().add(nameLabel);

        if (workspace.isPinned()) {
            Label pinIcon = new Label("\uD83D\uDCCC");
            pinIcon.setStyle("-fx-font-size: 10px;");
            nameRow.getChildren().add(pinIcon);
        }

        String displayPath = shortenPath(workspace.getDirectory());
        Label pathLabel = new Label(displayPath);
        pathLabel.setStyle("-fx-text-fill: #6666aa; -fx-font-size: 11px;");

        info.getChildren().addAll(nameRow, pathLabel);
        item.getChildren().add(info);

        item.setOnMouseClicked(event -> {
            if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                serviceBridge.switchWorkspace(workspace.getWorkspaceId());
                refreshWorkspaceItems();
            }
        });

        ContextMenu contextMenu = new ContextMenu();
        MenuItem pinItem = new MenuItem(workspace.isPinned() ? "Unpin" : "Pin");
        pinItem.setOnAction(event -> {
            if (workspace.isPinned()) {
                serviceBridge.unpinWorkspace(workspace.getWorkspaceId());
            } else {
                serviceBridge.pinWorkspace(workspace.getWorkspaceId());
            }
            refreshWorkspaceItems();
        });
        MenuItem removeItem = new MenuItem("Remove");
        removeItem.setOnAction(event -> {
            serviceBridge.removeWorkspace(workspace.getWorkspaceId());
            refreshWorkspaceItems();
        });
        contextMenu.getItems().addAll(pinItem, removeItem);
        item.setOnContextMenuRequested(event -> contextMenu.show(item, event.getScreenX(), event.getScreenY()));

        return item;
    }

    private HBox createTemplateItem(WorkflowTemplate template) {
        HBox item = new HBox(8);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(6, 12, 6, 12));
        item.setStyle("-fx-background-radius: 6; -fx-cursor: hand;");

        item.setOnMouseEntered(event -> item.setStyle("-fx-background-color: #1a1a3e; -fx-background-radius: 6; -fx-cursor: hand;"));
        item.setOnMouseExited(event -> item.setStyle("-fx-background-radius: 6; -fx-cursor: hand;"));

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);

        String categoryText = template.getCategory() != null ? template.getCategory().toUpperCase() : "GENERAL";
        Label categoryLabel = new Label(categoryText);
        categoryLabel.setStyle("-fx-text-fill: #6c63ff; -fx-font-size: 9px; -fx-font-weight: bold;");

        Label nameLabel = new Label(template.getName());
        nameLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 12px;");

        info.getChildren().addAll(categoryLabel, nameLabel);
        item.getChildren().add(info);

        item.setOnMouseClicked(event -> {
            String rendered = serviceBridge.renderTemplate(template.getTemplateId(), Map.of());
            if (rendered != null) {
                chatPanel.setInputText(rendered);
            }
        });

        return item;
    }

    private void showAddWorkspaceDialog() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Workspace Directory");
        File selectedDir = directoryChooser.showDialog(getScene().getWindow());

        if (selectedDir != null) {
            TextInputDialog nameDialog = new TextInputDialog(selectedDir.getName());
            nameDialog.setTitle("Add Workspace");
            nameDialog.setHeaderText("Enter a name for this workspace");
            nameDialog.setContentText("Name:");
            nameDialog.showAndWait().ifPresent(name -> {
                serviceBridge.addWorkspace(name, selectedDir.getAbsolutePath());
                refreshWorkspaceItems();
            });
        }
    }

    private Separator createSeparator() {
        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: #2a2a4a;");
        separator.setPadding(new Insets(4, 16, 4, 16));
        return separator;
    }

    private String shortenPath(String path) {
        if (path == null) return "";
        String home = System.getProperty("user.home");
        if (path.startsWith(home)) {
            return "~" + path.substring(home.length());
        }
        return path;
    }

    private class SessionListCell extends ListCell<CoworkSession> {
        @Override
        protected void updateItem(CoworkSession session, boolean empty) {
            super.updateItem(session, empty);
            if (empty || session == null) {
                setGraphic(null);
                setText(null);
                setStyle("-fx-background-color: transparent;");
                return;
            }

            setStyle("-fx-background-color: transparent;");

            VBox cellContent = new VBox(4);
            cellContent.setPadding(new Insets(6, 8, 6, 8));
            cellContent.setStyle("-fx-background-color: transparent; -fx-background-radius: 6;");

            cellContent.setOnMouseEntered(event -> cellContent.setStyle("-fx-background-color: #1a1a3e; -fx-background-radius: 6;"));
            cellContent.setOnMouseExited(event -> cellContent.setStyle("-fx-background-color: transparent; -fx-background-radius: 6;"));

            HBox titleRow = new HBox(8);
            titleRow.setAlignment(Pos.CENTER_LEFT);

            String displayTitle = session.getTitle() != null ? session.getTitle() : "New Session";
            if (displayTitle.length() > 28) {
                displayTitle = displayTitle.substring(0, 28) + "...";
            }
            Label titleLabel = new Label(displayTitle);
            titleLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 13px; -fx-font-weight: bold;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button deleteButton = new Button("\u2715");
            deleteButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #555577; -fx-font-size: 10px; -fx-padding: 2 6; -fx-cursor: hand;");
            deleteButton.setOnMouseEntered(event -> deleteButton.setStyle("-fx-background-color: #4a1a1a; -fx-text-fill: #f87171; -fx-font-size: 10px; -fx-padding: 2 6; -fx-cursor: hand; -fx-background-radius: 4;"));
            deleteButton.setOnMouseExited(event -> deleteButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #555577; -fx-font-size: 10px; -fx-padding: 2 6; -fx-cursor: hand;"));
            deleteButton.setOnAction(event -> {
                event.consume();
                serviceBridge.deleteSession(session.getSessionId());
                refreshSessionList();
            });

            titleRow.getChildren().addAll(titleLabel, spacer, deleteButton);

            String statusIcon;
            switch (session.getStatus()) {
                case ACTIVE -> statusIcon = "\uD83D\uDFE2";
                case COMPLETED -> statusIcon = "\u2705";
                case ABORTED -> statusIcon = "\uD83D\uDD34";
                default -> statusIcon = "\u26AA";
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd HH:mm");
            String timeStr = dateFormat.format(new Date(session.getUpdatedAt()));
            Label metaLabel = new Label(statusIcon + "  " + session.getMessageCount() + " msgs \u00B7 " + timeStr);
            metaLabel.setStyle("-fx-text-fill: #7777aa; -fx-font-size: 11px;");

            cellContent.getChildren().addAll(titleRow, metaLabel);
            setGraphic(cellContent);
        }
    }
}
