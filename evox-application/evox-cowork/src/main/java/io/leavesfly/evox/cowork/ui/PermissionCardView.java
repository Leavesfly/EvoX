package io.leavesfly.evox.cowork.ui;

import io.leavesfly.evox.cowork.permission.PermissionRequest;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class PermissionCardView extends VBox {

    public PermissionCardView(PermissionRequest request, CoworkServiceBridge serviceBridge) {
        getStyleClass().add("permission-card");
        setMaxWidth(680);

        // Title row
        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label warningIcon = new Label("\u26A0\uFE0F");
        warningIcon.setStyle("-fx-font-size: 16px;");
        Label titleLabel = new Label("Permission Required");
        titleLabel.getStyleClass().add("permission-title");
        titleRow.getChildren().addAll(warningIcon, titleLabel);

        // Tool name
        Label toolLabel = new Label("Tool: " + request.getToolName());
        toolLabel.getStyleClass().add("permission-tool-name");

        // Parameters
        Label paramsTitle = new Label("Parameters:");
        paramsTitle.setStyle("-fx-text-fill: #aaaacc; -fx-font-size: 12px; -fx-font-weight: bold;");

        Label paramsContent = new Label();
        paramsContent.getStyleClass().add("permission-params");
        paramsContent.setWrapText(true);

        StringBuilder paramsText = new StringBuilder();
        Map<String, Object> parameters = request.getParameters();
        if (parameters != null && !parameters.isEmpty()) {
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                String value = String.valueOf(entry.getValue());
                if (value.length() > 200) {
                    value = value.substring(0, 200) + "...";
                }
                paramsText.append(entry.getKey()).append(": ").append(value).append("\n");
            }
        } else {
            paramsText.append("(no parameters)");
        }
        paramsContent.setText(paramsText.toString().trim());

        // Buttons
        HBox buttonRow = new HBox(10);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);
        buttonRow.setPadding(new Insets(8, 0, 0, 0));

        Button denyButton = new Button("Deny");
        denyButton.getStyleClass().add("permission-button-deny");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button allowOnceButton = new Button("Allow Once");
        allowOnceButton.getStyleClass().add("permission-button-allow");

        Button alwaysAllowButton = new Button("Always Allow");
        alwaysAllowButton.getStyleClass().add("permission-button-always");

        denyButton.setOnAction(event -> {
            serviceBridge.replyPermission(request.getRequestId(), PermissionRequest.PermissionReply.REJECT);
            disableButtons(denyButton, allowOnceButton, alwaysAllowButton);
            titleLabel.setText("Permission Denied");
            titleLabel.setStyle("-fx-text-fill: #f87171; -fx-font-size: 13px; -fx-font-weight: bold;");
        });

        allowOnceButton.setOnAction(event -> {
            serviceBridge.replyPermission(request.getRequestId(), PermissionRequest.PermissionReply.ONCE);
            disableButtons(denyButton, allowOnceButton, alwaysAllowButton);
            titleLabel.setText("Permission Granted (Once)");
            titleLabel.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 13px; -fx-font-weight: bold;");
        });

        alwaysAllowButton.setOnAction(event -> {
            serviceBridge.replyPermission(request.getRequestId(), PermissionRequest.PermissionReply.ALWAYS);
            disableButtons(denyButton, allowOnceButton, alwaysAllowButton);
            titleLabel.setText("Permission Granted (Always)");
            titleLabel.setStyle("-fx-text-fill: #60a5fa; -fx-font-size: 13px; -fx-font-weight: bold;");
        });

        buttonRow.getChildren().addAll(denyButton, spacer, allowOnceButton, alwaysAllowButton);

        getChildren().addAll(titleRow, toolLabel, paramsTitle, paramsContent, buttonRow);
    }

    private void disableButtons(Button... buttons) {
        for (Button button : buttons) {
            button.setDisable(true);
            button.setOpacity(0.5);
        }
    }
}
