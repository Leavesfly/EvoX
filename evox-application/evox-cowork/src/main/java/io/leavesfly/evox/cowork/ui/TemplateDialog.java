package io.leavesfly.evox.cowork.ui;

import io.leavesfly.evox.cowork.template.WorkflowTemplate;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@Slf4j
public class TemplateDialog extends Stage {

    private final CoworkServiceBridge serviceBridge;
    private final BiConsumer<String, String> onTemplateSelected;
    private VBox templateListContainer;

    public TemplateDialog(Stage owner, CoworkServiceBridge serviceBridge,
                          BiConsumer<String, String> onTemplateSelected) {
        this.serviceBridge = serviceBridge;
        this.onTemplateSelected = onTemplateSelected;

        initOwner(owner);
        initModality(Modality.WINDOW_MODAL);
        initStyle(StageStyle.UNDECORATED);
        setTitle("Select Template");

        VBox root = buildDialogContent();
        Scene scene = new Scene(root, 560, 520);
        String cssResource = "/styles/cowork-dark.css";
        if (getClass().getResource(cssResource) != null) {
            scene.getStylesheets().add(getClass().getResource(cssResource).toExternalForm());
        }
        setScene(scene);
    }

    private VBox buildDialogContent() {
        VBox root = new VBox();
        root.getStyleClass().add("dialog-content");
        root.setSpacing(16);

        HBox header = buildHeader();
        ScrollPane scrollPane = buildTemplateList();
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        root.getChildren().addAll(header, scrollPane);
        return root;
    }

    private HBox buildHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(12);

        Label titleLabel = new Label("\uD83D\uDCC4 Select Template");
        titleLabel.getStyleClass().add("dialog-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeButton = new Button("\u2715");
        closeButton.getStyleClass().add("dialog-close-button");
        closeButton.setOnAction(event -> close());

        header.getChildren().addAll(titleLabel, spacer, closeButton);
        return header;
    }

    private ScrollPane buildTemplateList() {
        templateListContainer = new VBox(8);
        templateListContainer.setPadding(new Insets(4, 0, 4, 0));

        List<WorkflowTemplate> templates = serviceBridge.getAllTemplates();

        if (templates.isEmpty()) {
            Label emptyLabel = new Label("No templates available.\nAdd templates to ~/.evox/cowork/templates/");
            emptyLabel.setStyle("-fx-text-fill: #7777aa; -fx-font-size: 13px; -fx-text-alignment: center;");
            emptyLabel.setWrapText(true);
            templateListContainer.setAlignment(Pos.CENTER);
            templateListContainer.getChildren().add(emptyLabel);
        } else {
            Map<String, VBox> categoryGroups = new HashMap<>();

            for (WorkflowTemplate template : templates) {
                String category = template.getCategory() != null ? template.getCategory() : "General";
                VBox group = categoryGroups.computeIfAbsent(category, key -> {
                    VBox categoryBox = new VBox(6);
                    categoryBox.setPadding(new Insets(4, 0, 8, 0));

                    Label categoryLabel = new Label(key.toUpperCase());
                    categoryLabel.setStyle("-fx-text-fill: #6c63ff; -fx-font-size: 11px; -fx-font-weight: bold;");
                    categoryBox.getChildren().add(categoryLabel);

                    templateListContainer.getChildren().add(categoryBox);
                    return categoryBox;
                });

                VBox card = buildTemplateCard(template);
                group.getChildren().add(card);
            }
        }

        ScrollPane scrollPane = new ScrollPane(templateListContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return scrollPane;
    }

    private VBox buildTemplateCard(WorkflowTemplate template) {
        VBox card = new VBox(6);
        card.getStyleClass().add("template-card");

        Label nameLabel = new Label(template.getName());
        nameLabel.getStyleClass().add("template-card-name");

        card.getChildren().add(nameLabel);

        if (template.getDescription() != null && !template.getDescription().isEmpty()) {
            Label descLabel = new Label(template.getDescription());
            descLabel.getStyleClass().add("template-card-desc");
            descLabel.setWrapText(true);
            descLabel.setMaxWidth(480);
            card.getChildren().add(descLabel);
        }

        if (template.getTags() != null && !template.getTags().isEmpty()) {
            HBox tagsRow = new HBox(6);
            tagsRow.setAlignment(Pos.CENTER_LEFT);
            for (String tag : template.getTags()) {
                Label tagLabel = new Label(tag);
                tagLabel.setStyle(
                    "-fx-text-fill: #8888cc; -fx-font-size: 10px; " +
                    "-fx-background-color: #2a2a4a; -fx-padding: 2 8; -fx-background-radius: 4;"
                );
                tagsRow.getChildren().add(tagLabel);
            }
            card.getChildren().add(tagsRow);
        }

        List<WorkflowTemplate.TemplateVariable> variables = template.getVariables();
        boolean hasRequiredVariables = variables != null && variables.stream().anyMatch(WorkflowTemplate.TemplateVariable::isRequired);

        if (hasRequiredVariables) {
            card.setOnMouseClicked(event -> showVariableInputForm(template));
        } else {
            card.setOnMouseClicked(event -> {
                String rendered = serviceBridge.renderTemplate(template.getTemplateId(), Map.of());
                if (rendered != null && onTemplateSelected != null) {
                    onTemplateSelected.accept(template.getName(), rendered);
                }
                close();
            });
        }

        return card;
    }

    private void showVariableInputForm(WorkflowTemplate template) {
        templateListContainer.getChildren().clear();

        VBox formContainer = new VBox(12);
        formContainer.setPadding(new Insets(8));

        Button backButton = new Button("\u2190 Back to templates");
        backButton.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #6c63ff; " +
            "-fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 4 0;"
        );
        backButton.setOnAction(event -> refreshTemplateList());

        Label formTitle = new Label(template.getName());
        formTitle.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 16px; -fx-font-weight: bold;");

        formContainer.getChildren().addAll(backButton, formTitle);

        Map<String, TextField> fieldMap = new HashMap<>();

        for (WorkflowTemplate.TemplateVariable variable : template.getVariables()) {
            VBox fieldGroup = new VBox(4);

            String labelText = variable.getName();
            if (variable.isRequired()) {
                labelText += " *";
            }
            Label fieldLabel = new Label(labelText);
            fieldLabel.setStyle("-fx-text-fill: #aaaacc; -fx-font-size: 12px; -fx-font-weight: bold;");

            if (variable.getDescription() != null && !variable.getDescription().isEmpty()) {
                Label descLabel = new Label(variable.getDescription());
                descLabel.setStyle("-fx-text-fill: #7777aa; -fx-font-size: 11px;");
                descLabel.setWrapText(true);
                fieldGroup.getChildren().addAll(fieldLabel, descLabel);
            } else {
                fieldGroup.getChildren().add(fieldLabel);
            }

            TextField textField = new TextField();
            textField.setStyle(
                "-fx-background-color: #1a1a2e; -fx-text-fill: #e0e0e0; " +
                "-fx-border-color: #2a2a5a; -fx-border-width: 1; -fx-border-radius: 6; " +
                "-fx-background-radius: 6; -fx-padding: 8 12; -fx-font-size: 13px;"
            );
            if (variable.getDefaultValue() != null) {
                textField.setText(variable.getDefaultValue());
            }
            textField.setPromptText(variable.isRequired() ? "Required" : "Optional");

            fieldMap.put(variable.getName(), textField);
            fieldGroup.getChildren().add(textField);
            formContainer.getChildren().add(fieldGroup);
        }

        HBox actionRow = new HBox(10);
        actionRow.setAlignment(Pos.CENTER_RIGHT);
        actionRow.setPadding(new Insets(12, 0, 0, 0));

        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle(
            "-fx-background-color: #2a2a4a; -fx-text-fill: #cccccc; " +
            "-fx-font-size: 12px; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;"
        );
        cancelButton.setOnAction(event -> close());

        Button applyButton = new Button("Apply Template");
        applyButton.getStyleClass().add("send-button");
        applyButton.setOnAction(event -> {
            Map<String, String> values = new HashMap<>();
            for (Map.Entry<String, TextField> entry : fieldMap.entrySet()) {
                String value = entry.getValue().getText();
                if (value != null && !value.isEmpty()) {
                    values.put(entry.getKey(), value);
                }
            }

            boolean missingRequired = template.getVariables().stream()
                .filter(WorkflowTemplate.TemplateVariable::isRequired)
                .anyMatch(variable -> {
                    String value = values.get(variable.getName());
                    return value == null || value.isEmpty();
                });

            if (missingRequired) {
                Label errorLabel = new Label("Please fill in all required fields (*)");
                errorLabel.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px;");
                if (!formContainer.getChildren().contains(errorLabel)) {
                    formContainer.getChildren().add(formContainer.getChildren().size() - 1, errorLabel);
                }
                return;
            }

            String rendered = serviceBridge.renderTemplate(template.getTemplateId(), values);
            if (rendered != null && onTemplateSelected != null) {
                onTemplateSelected.accept(template.getName(), rendered);
            }
            close();
        });

        actionRow.getChildren().addAll(cancelButton, applyButton);
        formContainer.getChildren().add(actionRow);

        templateListContainer.getChildren().add(formContainer);
    }

    private void refreshTemplateList() {
        templateListContainer.getChildren().clear();

        List<WorkflowTemplate> templates = serviceBridge.getAllTemplates();
        if (templates.isEmpty()) {
            Label emptyLabel = new Label("No templates available.");
            emptyLabel.setStyle("-fx-text-fill: #7777aa; -fx-font-size: 13px;");
            templateListContainer.getChildren().add(emptyLabel);
        } else {
            Map<String, VBox> categoryGroups = new HashMap<>();
            for (WorkflowTemplate template : templates) {
                String category = template.getCategory() != null ? template.getCategory() : "General";
                VBox group = categoryGroups.computeIfAbsent(category, key -> {
                    VBox categoryBox = new VBox(6);
                    categoryBox.setPadding(new Insets(4, 0, 8, 0));
                    Label categoryLabel = new Label(key.toUpperCase());
                    categoryLabel.setStyle("-fx-text-fill: #6c63ff; -fx-font-size: 11px; -fx-font-weight: bold;");
                    categoryBox.getChildren().add(categoryLabel);
                    templateListContainer.getChildren().add(categoryBox);
                    return categoryBox;
                });
                VBox card = buildTemplateCard(template);
                group.getChildren().add(card);
            }
        }
    }
}
