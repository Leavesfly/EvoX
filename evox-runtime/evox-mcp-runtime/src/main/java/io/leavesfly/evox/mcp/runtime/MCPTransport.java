package io.leavesfly.evox.mcp.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.leavesfly.evox.mcp.MCPException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * MCP传输层抽象
 * 支持STDIO、HTTP等多种传输方式
 *
 * @author EvoX Team
 */
public interface MCPTransport {

    /**
     * 启动传输
     */
    void start() throws MCPException;

    /**
     * 停止传输
     */
    void stop();

    /**
     * 发送消息
     */
    void send(String message) throws MCPException;

    /**
     * 发送请求并等待响应
     */
    CompletableFuture<String> sendAndReceive(String message);

    /**
     * 设置消息接收处理器
     */
    void setMessageHandler(Consumer<String> handler);

    /**
     * 设置错误处理器
     */
    void setErrorHandler(Consumer<Throwable> handler);

    /**
     * 检查是否已连接
     */
    boolean isConnected();

    /**
     * 获取传输类型
     */
    TransportType getType();

    /**
     * 传输类型
     */
    enum TransportType {
        STDIO,
        HTTP,
        WEBSOCKET,
        SSE
    }

    // ============= STDIO传输实现 =============

    /**
     * STDIO传输实现
     * 通过标准输入/输出与子进程通信
     */
    @Slf4j
    @Data
    class StdioTransport implements MCPTransport {

        private final String command;
        private final String[] args;
        private final String workingDir;

        private Process process;
        private BufferedReader reader;
        private BufferedWriter writer;
        private Consumer<String> messageHandler;
        private Consumer<Throwable> errorHandler;
        private Thread readerThread;
        private volatile boolean running;
        private final ObjectMapper objectMapper = new ObjectMapper();

        public StdioTransport(String command, String... args) {
            this.command = command;
            this.args = args;
            this.workingDir = null;
        }

        public StdioTransport(String command, String workingDir, String... args) {
            this.command = command;
            this.args = args;
            this.workingDir = workingDir;
        }

        @Override
        public void start() throws MCPException {
            try {
                ProcessBuilder pb = new ProcessBuilder();
                
                // 构建完整命令
                String[] fullCommand = new String[args.length + 1];
                fullCommand[0] = command;
                System.arraycopy(args, 0, fullCommand, 1, args.length);
                pb.command(fullCommand);

                if (workingDir != null) {
                    pb.directory(new File(workingDir));
                }

                pb.redirectErrorStream(false);
                process = pb.start();

                reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                writer = new BufferedWriter(
                        new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));

                running = true;
                startReaderThread();

                log.info("STDIO传输已启动: {}", command);
            } catch (IOException e) {
                throw MCPException.connectionError("启动进程失败: " + command, e);
            }
        }

        private void startReaderThread() {
            readerThread = new Thread(() -> {
                try {
                    String line;
                    while (running && (line = reader.readLine()) != null) {
                        final String message = line;
                        if (messageHandler != null) {
                            try {
                                messageHandler.accept(message);
                            } catch (Exception e) {
                                log.error("处理消息时发生错误", e);
                                if (errorHandler != null) {
                                    errorHandler.accept(e);
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    if (running) {
                        log.error("读取消息时发生错误", e);
                        if (errorHandler != null) {
                            errorHandler.accept(e);
                        }
                    }
                }
            }, "MCP-STDIO-Reader");
            readerThread.setDaemon(true);
            readerThread.start();
        }

        @Override
        public void stop() {
            running = false;
            
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                log.warn("关闭writer时发生错误", e);
            }

            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                log.warn("关闭reader时发生错误", e);
            }

            if (process != null) {
                process.destroy();
                try {
                    process.waitFor();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            if (readerThread != null) {
                readerThread.interrupt();
            }

            log.info("STDIO传输已停止");
        }

        @Override
        public void send(String message) throws MCPException {
            if (!isConnected()) {
                throw MCPException.connectionError("传输未连接");
            }

            try {
                writer.write(message);
                writer.newLine();
                writer.flush();
                log.debug("发送消息: {}", message);
            } catch (IOException e) {
                throw MCPException.connectionError("发送消息失败", e);
            }
        }

        @Override
        public CompletableFuture<String> sendAndReceive(String message) {
            CompletableFuture<String> future = new CompletableFuture<>();
            
            // 临时保存原有handler
            Consumer<String> originalHandler = this.messageHandler;
            
            // 设置一次性handler
            this.messageHandler = response -> {
                future.complete(response);
                this.messageHandler = originalHandler;
            };

            try {
                send(message);
            } catch (MCPException e) {
                this.messageHandler = originalHandler;
                future.completeExceptionally(e);
            }

            return future;
        }

        @Override
        public void setMessageHandler(Consumer<String> handler) {
            this.messageHandler = handler;
        }

        @Override
        public void setErrorHandler(Consumer<Throwable> handler) {
            this.errorHandler = handler;
        }

        @Override
        public boolean isConnected() {
            return running && process != null && process.isAlive();
        }

        @Override
        public TransportType getType() {
            return TransportType.STDIO;
        }
    }

    // ============= 内存传输实现（用于测试） =============

    /**
     * 内存传输实现
     * 用于测试场景，直接在内存中传递消息
     */
    @Slf4j
    @Data
    class InMemoryTransport implements MCPTransport {

        private Consumer<String> messageHandler;
        private Consumer<Throwable> errorHandler;
        private InMemoryTransport peer;
        private volatile boolean connected;

        @Override
        public void start() throws MCPException {
            connected = true;
            log.info("内存传输已启动");
        }

        @Override
        public void stop() {
            connected = false;
            log.info("内存传输已停止");
        }

        /**
         * 连接到对端
         */
        public void connectTo(InMemoryTransport peer) {
            this.peer = peer;
            peer.peer = this;
        }

        @Override
        public void send(String message) throws MCPException {
            if (!connected || peer == null) {
                throw MCPException.connectionError("传输未连接");
            }

            log.debug("发送消息: {}", message);
            
            // 直接调用对端的handler
            if (peer.messageHandler != null) {
                try {
                    peer.messageHandler.accept(message);
                } catch (Exception e) {
                    if (peer.errorHandler != null) {
                        peer.errorHandler.accept(e);
                    }
                }
            }
        }

        @Override
        public CompletableFuture<String> sendAndReceive(String message) {
            CompletableFuture<String> future = new CompletableFuture<>();
            
            Consumer<String> originalHandler = this.messageHandler;
            
            this.messageHandler = response -> {
                future.complete(response);
                this.messageHandler = originalHandler;
            };

            try {
                send(message);
            } catch (MCPException e) {
                this.messageHandler = originalHandler;
                future.completeExceptionally(e);
            }

            return future;
        }

        @Override
        public void setMessageHandler(Consumer<String> handler) {
            this.messageHandler = handler;
        }

        @Override
        public void setErrorHandler(Consumer<Throwable> handler) {
            this.errorHandler = handler;
        }

        @Override
        public boolean isConnected() {
            return connected && peer != null && peer.connected;
        }

        @Override
        public TransportType getType() {
            return TransportType.STDIO; // 模拟STDIO行为
        }
    }

    // ============= SSE传输实现 =============

    /**
     * SSE (Server-Sent Events) 传输实现
     * 使用HTTP SSE协议与服务器通信
     * 
     * SSE特点:
     * - 客户端通过GET连接订阅事件流
     * - 服务器通过该连接推送事件
     * - 客户端通过POST发送请求
     */
    @Slf4j
    @Data
    class SSETransport implements MCPTransport {

        private final String sseEndpoint;      // SSE事件流端点
        private final String messageEndpoint;  // 消息发送端点
        private final Map<String, String> headers;

        private HttpClient httpClient;
        private Consumer<String> messageHandler;
        private Consumer<Throwable> errorHandler;
        private volatile boolean connected;
        private Thread sseThread;
        private final ObjectMapper objectMapper = new ObjectMapper();
        private final Map<String, CompletableFuture<String>> pendingRequests = new ConcurrentHashMap<>();
        private final AtomicLong requestIdGenerator = new AtomicLong(1);
        private final BlockingQueue<String> eventQueue = new LinkedBlockingQueue<>();

        public SSETransport(String baseUrl) {
            this(baseUrl + "/sse", baseUrl + "/message", null);
        }

        public SSETransport(String sseEndpoint, String messageEndpoint) {
            this(sseEndpoint, messageEndpoint, null);
        }

        public SSETransport(String sseEndpoint, String messageEndpoint, Map<String, String> headers) {
            this.sseEndpoint = sseEndpoint;
            this.messageEndpoint = messageEndpoint;
            this.headers = headers;
        }

        @Override
        public void start() throws MCPException {
            try {
                httpClient = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(30))
                        .build();

                connected = true;
                startSSEListener();

                log.info("SSE传输已启动: {}", sseEndpoint);
            } catch (Exception e) {
                throw MCPException.connectionError("SSE启动失败", e);
            }
        }

        /**
         * 启动SSE事件监听线程
         */
        private void startSSEListener() {
            sseThread = new Thread(() -> {
                while (connected) {
                    try {
                        connectSSE();
                    } catch (Exception e) {
                        if (connected) {
                            log.warn("SSE连接断开，5秒后重试: {}", e.getMessage());
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                }
            }, "MCP-SSE-Listener");
            sseThread.setDaemon(true);
            sseThread.start();
        }

        /**
         * 连接SSE事件流
         */
        private void connectSSE() throws Exception {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(sseEndpoint))
                    .GET()
                    .header("Accept", "text/event-stream")
                    .header("Cache-Control", "no-cache");

            if (headers != null) {
                headers.forEach(requestBuilder::header);
            }

            HttpRequest request = requestBuilder.build();

            HttpResponse<InputStream> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                throw new IOException("SSE连接失败: HTTP " + response.statusCode());
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                
                StringBuilder eventData = new StringBuilder();
                String eventType = "message";
                String eventId = null;

                String line;
                while (connected && (line = reader.readLine()) != null) {
                    if (line.isEmpty()) {
                        // 空行表示事件结束
                        if (eventData.length() > 0) {
                            processSSEEvent(eventType, eventId, eventData.toString().trim());
                            eventData.setLength(0);
                            eventType = "message";
                            eventId = null;
                        }
                    } else if (line.startsWith("data:")) {
                        String data = line.substring(5).trim();
                        if (eventData.length() > 0) {
                            eventData.append("\n");
                        }
                        eventData.append(data);
                    } else if (line.startsWith("event:")) {
                        eventType = line.substring(6).trim();
                    } else if (line.startsWith("id:")) {
                        eventId = line.substring(3).trim();
                    } else if (line.startsWith("retry:")) {
                        // 可以处理重试时间
                        log.debug("SSE重试时间: {}", line.substring(6).trim());
                    }
                    // 忽略注释行 (以:开头)
                }
            }
        }

        /**
         * 处理SSE事件
         */
        private void processSSEEvent(String eventType, String eventId, String data) {
            log.debug("SSE事件: type={}, id={}, data={}", eventType, eventId, data);

            // 检查是否是响应消息（包含id字段）
            try {
                if (data.contains("\"id\":")) {
                    // 尝试提取请求ID并完成pending请求
                    String idPattern = "\"id\":\\s*";
                    int idIndex = data.indexOf("\"id\":");
                    if (idIndex >= 0) {
                        int start = idIndex + 5;
                        while (start < data.length() && (data.charAt(start) == ' ' || data.charAt(start) == '"')) {
                            start++;
                        }
                        int end = start;
                        while (end < data.length() && data.charAt(end) != '"' && data.charAt(end) != ',' && data.charAt(end) != '}') {
                            end++;
                        }
                        if (start < end) {
                            String requestId = data.substring(start, end);
                            CompletableFuture<String> future = pendingRequests.remove(requestId);
                            if (future != null) {
                                future.complete(data);
                                return;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("解析响应ID失败", e);
            }

            // 如果不是响应消息，调用普通消息处理器
            if (messageHandler != null) {
                try {
                    messageHandler.accept(data);
                } catch (Exception e) {
                    log.error("处理SSE消息时发生错误", e);
                    if (errorHandler != null) {
                        errorHandler.accept(e);
                    }
                }
            }
        }

        @Override
        public void stop() {
            connected = false;

            if (sseThread != null) {
                sseThread.interrupt();
            }

            // 完成所有pending请求
            pendingRequests.values().forEach(f -> 
                f.completeExceptionally(MCPException.connectionError("传输已关闭")));
            pendingRequests.clear();

            log.info("SSE传输已停止");
        }

        @Override
        public void send(String message) throws MCPException {
            if (!connected) {
                throw MCPException.connectionError("SSE传输未连接");
            }

            try {
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(messageEndpoint))
                        .POST(HttpRequest.BodyPublishers.ofString(message, StandardCharsets.UTF_8))
                        .header("Content-Type", "application/json");

                if (headers != null) {
                    headers.forEach(requestBuilder::header);
                }

                HttpResponse<String> response = httpClient.send(requestBuilder.build(),
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 400) {
                    throw new IOException("HTTP错误: " + response.statusCode());
                }

                log.debug("发送SSE消息: {}", message);
            } catch (IOException | InterruptedException e) {
                throw MCPException.connectionError("发送消息失败", e);
            }
        }

        @Override
        public CompletableFuture<String> sendAndReceive(String message) {
            CompletableFuture<String> future = new CompletableFuture<>();

            try {
                // 尝试从消息中提取请求ID
                String requestId = extractRequestId(message);
                if (requestId != null) {
                    pendingRequests.put(requestId, future);
                }

                send(message);

                // 设置超时
                CompletableFuture.delayedExecutor(30, TimeUnit.SECONDS).execute(() -> {
                    if (!future.isDone()) {
                        pendingRequests.remove(requestId);
                        future.completeExceptionally(
                            MCPException.timeout("请求超时"));
                    }
                });

            } catch (MCPException e) {
                future.completeExceptionally(e);
            }

            return future;
        }

        /**
         * 从JSON消息中提取请求ID
         */
        private String extractRequestId(String message) {
            try {
                int idIndex = message.indexOf("\"id\":");
                if (idIndex >= 0) {
                    int start = idIndex + 5;
                    while (start < message.length() && (message.charAt(start) == ' ' || message.charAt(start) == '"')) {
                        start++;
                    }
                    int end = start;
                    while (end < message.length() && message.charAt(end) != '"' && message.charAt(end) != ',' && message.charAt(end) != '}') {
                        end++;
                    }
                    if (start < end) {
                        return message.substring(start, end);
                    }
                }
            } catch (Exception e) {
                log.debug("提取请求ID失败", e);
            }
            return null;
        }

        @Override
        public void setMessageHandler(Consumer<String> handler) {
            this.messageHandler = handler;
        }

        @Override
        public void setErrorHandler(Consumer<Throwable> handler) {
            this.errorHandler = handler;
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public TransportType getType() {
            return TransportType.SSE;
        }
    }

    // ============= HTTP传输实现 =============

    /**
     * HTTP传输实现
     * 使用HTTP POST进行请求/响应模式通信
     */
    @Slf4j
    @Data
    class HttpTransport implements MCPTransport {

        private final String endpoint;
        private final Map<String, String> headers;

        private HttpClient httpClient;
        private Consumer<String> messageHandler;
        private Consumer<Throwable> errorHandler;
        private volatile boolean connected;

        public HttpTransport(String endpoint) {
            this(endpoint, null);
        }

        public HttpTransport(String endpoint, Map<String, String> headers) {
            this.endpoint = endpoint;
            this.headers = headers;
        }

        @Override
        public void start() throws MCPException {
            httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();
            connected = true;
            log.info("HTTP传输已启动: {}", endpoint);
        }

        @Override
        public void stop() {
            connected = false;
            log.info("HTTP传输已停止");
        }

        @Override
        public void send(String message) throws MCPException {
            if (!connected) {
                throw MCPException.connectionError("HTTP传输未连接");
            }

            try {
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .POST(HttpRequest.BodyPublishers.ofString(message, StandardCharsets.UTF_8))
                        .header("Content-Type", "application/json");

                if (headers != null) {
                    headers.forEach(requestBuilder::header);
                }

                HttpResponse<String> response = httpClient.send(requestBuilder.build(),
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 400) {
                    throw new IOException("HTTP错误: " + response.statusCode());
                }

                log.debug("发送HTTP消息: {}", message);
            } catch (IOException | InterruptedException e) {
                throw MCPException.connectionError("发送消息失败", e);
            }
        }

        @Override
        public CompletableFuture<String> sendAndReceive(String message) {
            CompletableFuture<String> future = new CompletableFuture<>();

            if (!connected) {
                future.completeExceptionally(MCPException.connectionError("HTTP传输未连接"));
                return future;
            }

            CompletableFuture.runAsync(() -> {
                try {
                    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                            .uri(URI.create(endpoint))
                            .POST(HttpRequest.BodyPublishers.ofString(message, StandardCharsets.UTF_8))
                            .header("Content-Type", "application/json")
                            .timeout(Duration.ofSeconds(30));

                    if (headers != null) {
                        headers.forEach(requestBuilder::header);
                    }

                    HttpResponse<String> response = httpClient.send(requestBuilder.build(),
                            HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() >= 400) {
                        future.completeExceptionally(
                            new IOException("HTTP错误: " + response.statusCode()));
                    } else {
                        future.complete(response.body());
                    }
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });

            return future;
        }

        @Override
        public void setMessageHandler(Consumer<String> handler) {
            this.messageHandler = handler;
        }

        @Override
        public void setErrorHandler(Consumer<Throwable> handler) {
            this.errorHandler = handler;
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public TransportType getType() {
            return TransportType.HTTP;
        }
    }
}
