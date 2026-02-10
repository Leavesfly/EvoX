package io.leavesfly.evox.cowork;

import io.leavesfly.evox.cowork.ui.CoworkDesktopApp;
import javafx.application.Application;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * EvoX Cowork Application entry point.
 * Launches Spring Boot backend and JavaFX desktop GUI concurrently.
 * EvoX Cowork 应用程序入口点。
 * 并发启动 Spring Boot 后端和 JavaFX 桌面 GUI。
 */
@SpringBootApplication
public class CoworkApplication {

    private static ConfigurableApplicationContext springContext;

    public static void main(String[] args) {
        // Start Spring Boot in a non-daemon thread with error handling
        Thread springThread = new Thread(() -> {
            try {
                springContext = SpringApplication.run(CoworkApplication.class, args);
                System.out.println("\n========================================");
                System.out.println("  EvoX Cowork - Knowledge Work Assistant");
                System.out.println("  Version: 1.0.0-SNAPSHOT");
                System.out.println("  Backend ready on port 8090");
                System.out.println("========================================\n");
            } catch (Exception e) {
                System.err.println("Failed to start Spring Boot: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }, "spring-boot-thread");
        springThread.setDaemon(false);
        springThread.start();

        // Launch JavaFX on the main thread with fallback
        try {
            Application.launch(CoworkDesktopApp.class, args);
        } catch (Exception e) {
            System.err.println("Failed to start JavaFX desktop app: " + e.getMessage());
            System.err.println("Falling back to REST API only mode.");
        }
    }

    /**
     * Graceful shutdown hook
     */
    public static void shutdown() {
        if (springContext != null && springContext.isActive()) {
            springContext.close();
        }
    }

    public static ConfigurableApplicationContext getSpringContext() {
        return springContext;
    }
}