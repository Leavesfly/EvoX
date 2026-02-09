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
        // Start Spring Boot in a daemon thread so JavaFX can own the main thread
        // 在守护线程中启动 Spring Boot，以便 JavaFX 占用主线程
        Thread springThread = new Thread(() -> {
            springContext = SpringApplication.run(CoworkApplication.class, args);
            System.out.println("\n========================================");
            System.out.println("  EvoX Cowork - Knowledge Work Assistant");
            System.out.println("  Version: 1.0.0-SNAPSHOT");
            System.out.println("  Backend ready on port 8090");
            System.out.println("========================================\n");
        }, "spring-boot-thread");
        springThread.setDaemon(true);
        springThread.start();

        // Launch JavaFX on the main thread
        // 在主线程上启动 JavaFX
        Application.launch(CoworkDesktopApp.class, args);
    }

    public static ConfigurableApplicationContext getSpringContext() {
        return springContext;
    }
}