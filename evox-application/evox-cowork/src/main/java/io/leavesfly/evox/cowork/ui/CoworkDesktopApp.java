package io.leavesfly.evox.cowork.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class CoworkDesktopApp extends Application {

    private CoworkServiceBridge serviceBridge;

    @Override
    public void start(Stage primaryStage) {
        // 初始化服务桥接器
        serviceBridge = CoworkServiceBridge.initialize();

        // 创建主布局
        CoworkMainLayout mainLayout = new CoworkMainLayout(serviceBridge);

        Scene scene = new Scene(mainLayout, 1200, 800);
        String cssResource = "/styles/cowork-dark.css";
        if (getClass().getResource(cssResource) != null) {
            scene.getStylesheets().add(getClass().getResource(cssResource).toExternalForm());
        }

        primaryStage.setTitle("EvoX Cowork");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        
        // 处理窗口关闭请求
        primaryStage.setOnCloseRequest(event -> {
            serviceBridge.shutdown();
            Platform.exit();
        });

        primaryStage.show();
        // 通知主布局应用已就绪
        mainLayout.onAppReady();
    }

    @Override
    public void stop() {
        // 应用停止时关闭服务
        if (serviceBridge != null) {
            serviceBridge.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}