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

        // Title row / 标题行
        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label warningIcon = new Label("\u26A0\uFE0F");
        warningIcon.setStyle("-fx-font-size: 16px;");
        Label titleLabel = new Label("Permission Required"); // 需要权限
        titleLabel.getStyleClass().add("permission-title");
        titleRow.getChildren().addAll(warningIcon, titleLabel);

        // Tool name / 工具名称
        Label toolLabel = new Label("Tool: " + request.getToolName());
        toolLabel.getStyleClass().add("permission-tool-name");

        // Parameters / 参数列表
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
                    value = value.substring(0, 200) + "..."; // 截断过长的参数值
                }
                paramsText.append(entry.getKey()).append(": ").append(value).append("\n");
            }
        } else {
            paramsText.append("(no parameters)");
        }
        paramsContent.setText(paramsText.toString().trim());

        // Buttons / 按钮行
        HBox buttonRow = new HBox(10);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);
        buttonRow.setPadding(new Insets(8, 0, 0, 0));

        // 拒绝按钮
        Button denyButton = new Button("Deny");
        denyButton.getStyleClass().add("permission-button-deny");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 允许一次按钮
        Button allowOnceButton = new Button("Allow Once");
        allowOnceButton.getStyleClass().add("permission-button-allow");

        // 总是允许按钮
        Button alwaysAllowButton = new Button("Always Allow");
        alwaysAllowButton.getStyleClass().add("permission-button-always");

        // 设置拒绝按钮点击事件
        denyButton.setOnAction(event -> {
            serviceBridge.replyPermission(request.getRequestId(), PermissionRequest.PermissionReply.REJECT);
            disableButtons(denyButton, allowOnceButton, alwaysAllowButton);
            titleLabel.setText("Permission Denied");
            titleLabel.setStyle("-fx-text-fill: #f87171; -fx-font-size: 13px; -fx-font-weight: bold;");
        });

        // 设置允许一次按钮点击事件
        allowOnceButton.setOnAction(event -> {
            serviceBridge.replyPermission(request.getRequestId(), PermissionRequest.PermissionReply.ONCE);
            disableButtons(denyButton, allowOnceButton, alwaysAllowButton);
            titleLabel.setText("Permission Granted (Once)");
            titleLabel.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 13px; -fx-font-weight: bold;");
        });

        // 设置总是允许按钮点击事件
        alwaysAllowButton.setOnAction(event -> {
            serviceBridge.replyPermission(request.getRequestId(), PermissionRequest.PermissionReply.ALWAYS);
            disableButtons(denyButton, allowOnceButton, alwaysAllowButton);
            titleLabel.setText("Permission Granted (Always)");
            titleLabel.setStyle("-fx-text-fill: #60a5fa; -fx-font-size: 13px; -fx-font-weight: bold;");
        });

        buttonRow.getChildren().addAll(denyButton, spacer, allowOnceButton, alwaysAllowButton);

        getChildren().addAll(titleRow, toolLabel, paramsTitle, paramsContent, buttonRow);
    }

    // 禁用所有操作按钮
    private void disableButtons(Button... buttons) {
        for (Button button : buttons) {
            button.setDisable(true);
            button.setOpacity(0.5);
        }
    }
}