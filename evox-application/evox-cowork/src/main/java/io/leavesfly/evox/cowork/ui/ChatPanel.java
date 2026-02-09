package io.leavesfly.evox.cowork.ui;

import io.leavesfly.evox.cowork.permission.PermissionRequest;
import io.leavesfly.evox.cowork.session.CoworkSession;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Slf4j
public class ChatPanel extends VBox {

    private final CoworkServiceBridge serviceBridge;
    private CoworkSession currentSession;

    private final Label chatTitleLabel;
    private final Label chatStatusLabel;
    private final Button abortButton;
    private final ScrollPane messageScrollPane;
    private final VBox messageContainer;
    private final TextArea inputField;
    private final Button sendButton;
    private final VBox welcomeView;
    private final VBox chatView;

    private VBox streamingMessageBox;
    private TextFlow streamingTextFlow;
    private final StringBuilder streamingBuffer = new StringBuilder();
    private boolean isStreaming = false;

    public ChatPanel(CoworkServiceBridge serviceBridge) {
        this.serviceBridge = serviceBridge;
        getStyleClass().add("chat-area");

        // Chat header
        HBox chatHeader = new HBox(12);
        chatHeader.getStyleClass().add("chat-header");
        chatHeader.setAlignment(Pos.CENTER_LEFT);

        VBox headerInfo = new VBox(2);
        chatTitleLabel = new Label("EvoX Cowork");
        chatTitleLabel.getStyleClass().add("chat-header-title");
        chatStatusLabel = new Label("");
        chatStatusLabel.getStyleClass().add("chat-header-status");
        headerInfo.getChildren().addAll(chatTitleLabel, chatStatusLabel);
        HBox.setHgrow(headerInfo, Priority.ALWAYS);

        abortButton = new Button("\u23F9 Abort");
        abortButton.getStyleClass().addAll("chat-header-button", "chat-header-button-danger");
        abortButton.setVisible(false);
        abortButton.setManaged(false);
        abortButton.setOnAction(event -> {
            if (currentSession != null) {
                serviceBridge.abortSession(currentSession.getSessionId());
                setStreamingState(false);
            }
        });

        chatHeader.getChildren().addAll(headerInfo, abortButton);

        // Message area
        messageContainer = new VBox(8);
        messageContainer.getStyleClass().add("messages-container");

        messageScrollPane = new ScrollPane(messageContainer);
        messageScrollPane.getStyleClass().add("messages-scroll");
        messageScrollPane.setFitToWidth(true);
        messageScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        messageScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(messageScrollPane, Priority.ALWAYS);

        messageContainer.heightProperty().addListener((observable, oldValue, newValue) -> {
            messageScrollPane.setVvalue(1.0);
        });

        // Input area
        HBox inputArea = new HBox(12);
        inputArea.getStyleClass().add("input-area");
        inputArea.setAlignment(Pos.BOTTOM_CENTER);

        inputField = new TextArea();
        inputField.getStyleClass().add("message-input");
        inputField.setPromptText("Type your message... (Shift+Enter for new line)");
        inputField.setPrefRowCount(2);
        inputField.setMaxHeight(120);
        inputField.setWrapText(true);
        HBox.setHgrow(inputField, Priority.ALWAYS);

        inputField.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER && !keyEvent.isShiftDown()) {
                keyEvent.consume();
                sendMessage();
            }
        });

        sendButton = new Button("Send \u27A4");
        sendButton.getStyleClass().add("send-button");
        sendButton.setOnAction(event -> sendMessage());

        inputArea.getChildren().addAll(inputField, sendButton);

        // Chat view
        chatView = new VBox();
        chatView.getChildren().addAll(chatHeader, messageScrollPane, inputArea);
        VBox.setVgrow(messageScrollPane, Priority.ALWAYS);

        // Welcome view
        welcomeView = buildWelcomeView();

        getChildren().add(welcomeView);
        VBox.setVgrow(welcomeView, Priority.ALWAYS);
    }

    private VBox buildWelcomeView() {
        VBox welcome = new VBox(16);
        welcome.getStyleClass().add("welcome-container");
        welcome.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("\u26A1 EvoX Cowork");
        titleLabel.getStyleClass().add("welcome-title");

        Label subtitleLabel = new Label("Your AI-powered knowledge work assistant");
        subtitleLabel.getStyleClass().add("welcome-subtitle");

        VBox featureBox = new VBox(8);
        featureBox.setAlignment(Pos.CENTER);
        featureBox.setPadding(new Insets(24, 0, 0, 0));

        String[] features = {
            "\uD83D\uDCDD  Draft documents, reports, and emails",
            "\uD83D\uDD0D  Research and analyze information",
            "\uD83D\uDCCA  Process and summarize data",
            "\uD83D\uDCC1  Organize files and manage projects",
            "\uD83E\uDD16  Automate repetitive workflows"
        };

        for (String feature : features) {
            Label featureLabel = new Label(feature);
            featureLabel.setStyle("-fx-text-fill: #8888aa; -fx-font-size: 13px;");
            featureBox.getChildren().add(featureLabel);
        }

        Label hintLabel = new Label("Create a new session from the sidebar to get started");
        hintLabel.getStyleClass().add("welcome-hint");
        hintLabel.setPadding(new Insets(16, 0, 0, 0));

        welcome.getChildren().addAll(titleLabel, subtitleLabel, featureBox, hintLabel);
        return welcome;
    }

    public void loadSession(CoworkSession session) {
        Platform.runLater(() -> {
            this.currentSession = session;

            getChildren().clear();
            getChildren().add(chatView);
            VBox.setVgrow(chatView, Priority.ALWAYS);

            String title = session.getTitle() != null ? session.getTitle() : "New Session";
            chatTitleLabel.setText(title);
            updateStatusLabel(session.getStatus());

            messageContainer.getChildren().clear();

            List<CoworkSession.SessionMessage> messages = serviceBridge.getMessages(session.getSessionId());
            if (messages != null) {
                for (CoworkSession.SessionMessage message : messages) {
                    addMessageBubble(message.getRole(), message.getContent(), message.getTimestamp());
                }
            }

            inputField.requestFocus();
        });
    }

    private void sendMessage() {
        String text = inputField.getText();
        if (text == null || text.trim().isEmpty() || currentSession == null || isStreaming) {
            return;
        }

        String message = text.trim();
        inputField.clear();

        addMessageBubble("user", message, System.currentTimeMillis());
        setStreamingState(true);
        beginStreamingMessage();

        serviceBridge.sendPrompt(currentSession.getSessionId(), message, response -> {
            finalizeStreamingMessage(response);
            setStreamingState(false);

            CoworkSession refreshed = serviceBridge.getSession(currentSession.getSessionId());
            if (refreshed != null) {
                currentSession = refreshed;
                String title = refreshed.getTitle() != null ? refreshed.getTitle() : "New Session";
                chatTitleLabel.setText(title);
            }
        });
    }

    private void addMessageBubble(String role, String content, long timestamp) {
        Platform.runLater(() -> {
            VBox bubble = new VBox(4);
            bubble.setPadding(new Insets(12, 16, 12, 16));

            Label roleLabel = new Label();
            roleLabel.getStyleClass().add("message-role");

            TextFlow textFlow = new TextFlow();
            Text messageText = new Text(content);
            messageText.getStyleClass().add("message-content");

            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
            Label timeLabel = new Label(timeFormat.format(new Date(timestamp)));
            timeLabel.getStyleClass().add("message-time");

            HBox wrapper = new HBox();
            wrapper.setPadding(new Insets(2, 0, 2, 0));

            switch (role) {
                case "user" -> {
                    roleLabel.setText("You");
                    bubble.getStyleClass().addAll("message-bubble", "message-user");
                    messageText.setStyle("-fx-fill: #ffffff;");
                    wrapper.setAlignment(Pos.CENTER_RIGHT);
                    bubble.setMaxWidth(600);
                }
                case "assistant" -> {
                    roleLabel.setText("Cowork");
                    bubble.getStyleClass().addAll("message-bubble", "message-assistant");
                    messageText.setStyle("-fx-fill: #e0e0e0;");
                    wrapper.setAlignment(Pos.CENTER_LEFT);
                    bubble.setMaxWidth(700);
                }
                default -> {
                    roleLabel.setText("System");
                    bubble.getStyleClass().addAll("message-bubble", "message-system");
                    messageText.setStyle("-fx-fill: #ccaaff;");
                    wrapper.setAlignment(Pos.CENTER);
                    bubble.setMaxWidth(500);
                }
            }

            textFlow.getChildren().add(messageText);
            bubble.getChildren().addAll(roleLabel, textFlow, timeLabel);
            wrapper.getChildren().add(bubble);

            messageContainer.getChildren().add(wrapper);
        });
    }

    private void beginStreamingMessage() {
        Platform.runLater(() -> {
            streamingBuffer.setLength(0);

            streamingMessageBox = new VBox(4);
            streamingMessageBox.getStyleClass().addAll("message-bubble", "message-assistant");
            streamingMessageBox.setPadding(new Insets(12, 16, 12, 16));
            streamingMessageBox.setMaxWidth(700);

            Label roleLabel = new Label("Cowork");
            roleLabel.getStyleClass().add("message-role");

            streamingTextFlow = new TextFlow();
            Text cursorText = new Text("\u258C");
            cursorText.setStyle("-fx-fill: #6c63ff;");
            streamingTextFlow.getChildren().add(cursorText);

            HBox indicator = new HBox(6);
            indicator.setAlignment(Pos.CENTER_LEFT);
            Label dotLabel = new Label("\u25CF");
            dotLabel.getStyleClass().add("streaming-indicator");
            Label thinkingLabel = new Label("Thinking...");
            thinkingLabel.getStyleClass().add("streaming-indicator");
            indicator.getChildren().addAll(dotLabel, thinkingLabel);

            streamingMessageBox.getChildren().addAll(roleLabel, streamingTextFlow, indicator);

            HBox wrapper = new HBox();
            wrapper.setPadding(new Insets(2, 0, 2, 0));
            wrapper.setAlignment(Pos.CENTER_LEFT);
            wrapper.getChildren().add(streamingMessageBox);

            messageContainer.getChildren().add(wrapper);
        });
    }

    public void appendStreamContent(String content) {
        Platform.runLater(() -> {
            if (streamingTextFlow == null) return;

            streamingBuffer.append(content);

            streamingTextFlow.getChildren().clear();
            Text updatedText = new Text(streamingBuffer.toString() + "\u258C");
            updatedText.setStyle("-fx-fill: #e0e0e0;");
            streamingTextFlow.getChildren().add(updatedText);

            // Remove "Thinking..." indicator after first content arrives
            if (streamingMessageBox != null && streamingMessageBox.getChildren().size() > 2) {
                streamingMessageBox.getChildren().remove(2);
            }
        });
    }

    private void finalizeStreamingMessage(String fullResponse) {
        Platform.runLater(() -> {
            if (streamingMessageBox != null) {
                messageContainer.getChildren().removeIf(node -> {
                    if (node instanceof HBox wrapper) {
                        return wrapper.getChildren().contains(streamingMessageBox);
                    }
                    return false;
                });
                streamingMessageBox = null;
                streamingTextFlow = null;
            }

            String responseContent = fullResponse != null ? fullResponse : streamingBuffer.toString();
            if (!responseContent.isEmpty()) {
                addMessageBubble("assistant", responseContent, System.currentTimeMillis());
            }

            streamingBuffer.setLength(0);
        });
    }

    public void showPermissionCard(PermissionRequest request) {
        Platform.runLater(() -> {
            PermissionCardView card = new PermissionCardView(request, serviceBridge);

            HBox wrapper = new HBox();
            wrapper.setPadding(new Insets(4, 0, 4, 0));
            wrapper.setAlignment(Pos.CENTER);
            wrapper.getChildren().add(card);

            messageContainer.getChildren().add(wrapper);
        });
    }

    public void showSystemMessage(String message) {
        addMessageBubble("system", message, System.currentTimeMillis());
    }

    public void setInputText(String text) {
        Platform.runLater(() -> {
            inputField.setText(text);
            inputField.requestFocus();
            inputField.positionCaret(text.length());
        });
    }

    private void setStreamingState(boolean streaming) {
        Platform.runLater(() -> {
            this.isStreaming = streaming;
            sendButton.setDisable(streaming);
            inputField.setDisable(streaming);
            abortButton.setVisible(streaming);
            abortButton.setManaged(streaming);
            if (currentSession != null) {
                chatStatusLabel.setText(streaming ? "Processing..." : "");
            }
        });
    }

    private void updateStatusLabel(CoworkSession.SessionStatus status) {
        String statusText;
        switch (status) {
            case ACTIVE -> statusText = "\u25CF Active";
            case COMPLETED -> statusText = "\u2713 Completed";
            case ABORTED -> statusText = "\u2715 Aborted";
            default -> statusText = "";
        }
        chatStatusLabel.setText(statusText);
    }
}
