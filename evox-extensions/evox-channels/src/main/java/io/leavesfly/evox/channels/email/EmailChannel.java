package io.leavesfly.evox.channels.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.leavesfly.evox.channels.core.AbstractChannel;
import io.leavesfly.evox.channels.core.ChannelConfig;
import io.leavesfly.evox.channels.core.ChannelMessage;
import io.leavesfly.evox.channels.core.MessageContentType;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Email 渠道实现
 * 通过 IMAP 轮询收件箱接收邮件，通过 SMTP 发送回复
 * 使用 Java 原生 Socket 实现，不依赖 javax.mail
 *
 * @author EvoX Team
 */
@Slf4j
public class EmailChannel extends AbstractChannel {

    private final EmailChannelConfig config;
    private final ObjectMapper objectMapper;

    private ScheduledExecutorService pollingExecutor;
    private volatile boolean running = false;

    private static final Set<String> PROCESSED_MESSAGE_IDS = Collections.synchronizedSet(new HashSet<>());

    public EmailChannel(EmailChannelConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getChannelId() {
        return config.getChannelId();
    }

    @Override
    public String getChannelName() {
        return config.getChannelName();
    }

    @Override
    public ChannelConfig getConfig() {
        return config;
    }

    @Override
    protected void doStart() throws Exception {
        if (!config.isConsentGranted()) {
            throw new IllegalStateException("Email access not granted. Please set consentGranted=true to enable email access.");
        }

        log.info("Starting EmailChannel [{}]", getChannelId());

        startPolling();

        running = true;
        log.info("EmailChannel [{}] started successfully", getChannelId());
    }

    @Override
    protected void doStop() throws Exception {
        log.info("Stopping EmailChannel [{}]", getChannelId());

        running = false;

        if (pollingExecutor != null) {
            pollingExecutor.shutdown();
            try {
                if (!pollingExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    pollingExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                pollingExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        log.info("EmailChannel [{}] stopped successfully", getChannelId());
    }

    @Override
    public CompletableFuture<Void> sendMessage(String targetId, ChannelMessage message) {
        return CompletableFuture.runAsync(() -> {
            try {
                sendEmail(targetId, message.getContent(), message.getReplyTo());
                log.info("Email sent successfully to: {}", targetId);
            } catch (Exception e) {
                log.error("Failed to send email to: {}", targetId, e);
                throw new RuntimeException("Failed to send email", e);
            }
        });
    }

    /**
     * 启动轮询任务
     */
    private void startPolling() {
        pollingExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "email-polling-" + getChannelId());
            thread.setDaemon(true);
            return thread;
        });

        pollingExecutor.scheduleWithFixedDelay(
                this::pollInbox,
                0,
                config.getPollingIntervalMs(),
                TimeUnit.MILLISECONDS
        );

        log.info("Email polling started with interval: {}ms", config.getPollingIntervalMs());
    }

    /**
     * 轮询收件箱
     */
    private void pollInbox() {
        if (!running) {
            return;
        }

        try {
            List<EmailMessage> messages = fetchUnreadMessages();
            log.debug("Found {} unread messages in INBOX", messages.size());

            for (EmailMessage message : messages) {
                try {
                    processIncomingMessage(message);
                } catch (Exception e) {
                    log.error("Error processing message: {}", message.getSubject(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error polling inbox", e);
        }
    }

    /**
     * 使用 IMAP 协议获取未读邮件
     */
    private List<EmailMessage> fetchUnreadMessages() throws Exception {
        List<EmailMessage> messages = new ArrayList<>();

        SSLSocket socket = null;
        BufferedReader reader = null;
        PrintWriter writer = null;

        try {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            socket = (SSLSocket) factory.createSocket(config.getImapHost(), config.getImapPort());

            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            readResponse(reader);
            sendCommand(writer, "LOGIN " + config.getImapUsername() + " " + config.getImapPassword());
            readResponse(reader);

            sendCommand(writer, "SELECT INBOX");
            readResponse(reader);

            sendCommand(writer, "SEARCH UNSEEN");
            String searchResponse = readResponse(reader);
            List<String> messageIds = parseMessageIds(searchResponse);

            for (String messageId : messageIds) {
                try {
                    sendCommand(writer, "FETCH " + messageId + " (BODY[])");
                    String fetchResponse = readResponse(reader);
                    EmailMessage email = parseEmail(fetchResponse);
                    if (email != null) {
                        messages.add(email);
                    }
                } catch (Exception e) {
                    log.error("Error fetching message: {}", messageId, e);
                }
            }

            sendCommand(writer, "LOGOUT");
            readResponse(reader);

        } finally {
            closeQuietly(reader);
            closeQuietly(writer);
            closeQuietly(socket);
        }

        return messages;
    }

    /**
     * 使用 SMTP 协议发送邮件
     */
    private void sendEmail(String toAddress, String content, ChannelMessage replyTo) throws Exception {
        Socket socket = null;
        BufferedReader reader = null;
        PrintWriter writer = null;

        try {
            socket = new Socket(config.getSmtpHost(), config.getSmtpPort());
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            readResponse(reader);

            sendCommand(writer, "EHLO localhost");
            readResponse(reader);

            sendCommand(writer, "STARTTLS");
            readResponse(reader);

            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket sslSocket = (SSLSocket) factory.createSocket(socket, config.getSmtpHost(), config.getSmtpPort(), true);
            sslSocket.startHandshake();

            reader = new BufferedReader(new InputStreamReader(sslSocket.getInputStream(), StandardCharsets.UTF_8));
            writer = new PrintWriter(new OutputStreamWriter(sslSocket.getOutputStream(), StandardCharsets.UTF_8), true);

            sendCommand(writer, "EHLO localhost");
            readResponse(reader);

            String authString = base64Encode("\0" + config.getSmtpUsername() + "\0" + config.getSmtpPassword());
            sendCommand(writer, "AUTH PLAIN " + authString);
            readResponse(reader);

            String subject;
            if (replyTo != null && replyTo.getMetadata() != null) {
                String originalSubject = (String) replyTo.getMetadata().get("subject");
                subject = "Re: " + (originalSubject != null ? originalSubject : "Your message");
            } else {
                subject = "Re: Your message";
            }

            sendCommand(writer, "MAIL FROM:<" + config.getFromAddress() + ">");
            readResponse(reader);

            sendCommand(writer, "RCPT TO:<" + toAddress + ">");
            readResponse(reader);

            sendCommand(writer, "DATA");
            readResponse(reader);

            String emailBody = buildEmailBody(config.getFromAddress(), toAddress, subject, content);
            writer.println(emailBody);
            writer.println(".");
            readResponse(writer, reader);

            sendCommand(writer, "QUIT");
            readResponse(reader);

        } finally {
            closeQuietly(reader);
            closeQuietly(writer);
            closeQuietly(socket);
        }
    }

    /**
     * 处理收到的邮件
     */
    private void processIncomingMessage(EmailMessage message) {
        String messageId = message.getMessageId();

        if (PROCESSED_MESSAGE_IDS.contains(messageId)) {
            log.debug("Message {} already processed, skipping", messageId);
            return;
        }

        String sender = message.getFrom();

        if (!isSenderAllowed(sender)) {
            log.debug("Sender {} not in allow list, skipping", sender);
            return;
        }

        log.info("Processing email from: {}, subject: {}", sender, message.getSubject());

        ChannelMessage channelMessage = ChannelMessage.builder()
                .channelId(getChannelId())
                .senderId(sender)
                .senderName(extractDisplayName(message.getFrom()))
                .targetId(config.getChannelId())
                .content(buildFullContent(message.getSubject(), message.getContent()))
                .contentType(MessageContentType.TEXT)
                .timestamp(Instant.now())
                .metadata(buildMetadata(message))
                .threadId(messageId)
                .build();

        PROCESSED_MESSAGE_IDS.add(messageId);

        notifyMessage(channelMessage);
    }

    /**
     * 检查发送者是否在白名单中
     */
    private boolean isSenderAllowed(String sender) {
        if (config.getAllowFrom() == null || config.getAllowFrom().isEmpty()) {
            return true;
        }
        return config.getAllowFrom().stream()
                .anyMatch(allowed -> sender.equalsIgnoreCase(allowed) || sender.endsWith("@" + allowed));
    }

    /**
     * 解析邮件 ID
     */
    private List<String> parseMessageIds(String response) {
        List<String> ids = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\* SEARCH\\s+(.*)");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            String[] parts = matcher.group(1).trim().split("\\s+");
            for (String part : parts) {
                if (!part.isEmpty()) {
                    ids.add(part);
                }
            }
        }
        return ids;
    }

    /**
     * 解析邮件内容
     */
    private EmailMessage parseEmail(String response) {
        try {
            EmailMessage email = new EmailMessage();

            Pattern idPattern = Pattern.compile("\\* \\d+ FETCH \\(BODY\\[\\] \\{(\\d+)\\}");
            Matcher idMatcher = idPattern.matcher(response);
            if (!idMatcher.find()) {
                return null;
            }

            String bodyPart = response.substring(idMatcher.end()).trim();

            String[] lines = bodyPart.split("\r?\n");
            boolean inHeaders = true;
            StringBuilder contentBuilder = new StringBuilder();

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];

                if (inHeaders) {
                    if (line.isEmpty()) {
                        inHeaders = false;
                        continue;
                    }

                    if (line.toLowerCase().startsWith("from:")) {
                        email.setFrom(extractEmailAddress(line.substring(5).trim()));
                    } else if (line.toLowerCase().startsWith("subject:")) {
                        email.setSubject(line.substring(8).trim());
                    } else if (line.toLowerCase().startsWith("message-id:")) {
                        email.setMessageId(extractMessageId(line.substring(11).trim()));
                    }
                } else {
                    contentBuilder.append(line).append("\n");
                }
            }

            email.setContent(contentBuilder.toString().trim());
            return email;

        } catch (Exception e) {
            log.error("Error parsing email", e);
            return null;
        }
    }

    /**
     * 提取邮箱地址
     */
    private String extractEmailAddress(String from) {
        Pattern pattern = Pattern.compile("<([^>]+)>");
        Matcher matcher = pattern.matcher(from);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return from.trim();
    }

    /**
     * 提取显示名称
     */
    private String extractDisplayName(String from) {
        Pattern pattern = Pattern.compile("\"?([^\"]+)\"?\\s*<[^>]+>");
        Matcher matcher = pattern.matcher(from);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return from.trim();
    }

    /**
     * 提取消息 ID
     */
    private String extractMessageId(String messageId) {
        return messageId.replaceAll("[<>]", "").trim();
    }

    /**
     * 构建完整内容
     */
    private String buildFullContent(String subject, String content) {
        StringBuilder builder = new StringBuilder();
        if (subject != null && !subject.isEmpty()) {
            builder.append("Subject: ").append(subject).append("\n\n");
        }
        builder.append(content);
        return builder.toString();
    }

    /**
     * 构建邮件体
     */
    private String buildEmailBody(String from, String to, String subject, String content) {
        StringBuilder builder = new StringBuilder();
        builder.append("From: ").append(from).append("\r\n");
        builder.append("To: ").append(to).append("\r\n");
        builder.append("Subject: ").append(subject).append("\r\n");
        builder.append("MIME-Version: 1.0\r\n");
        builder.append("Content-Type: text/plain; charset=UTF-8\r\n");
        builder.append("\r\n");
        builder.append(content);
        return builder.toString();
    }

    /**
     * 构建元数据
     */
    private Map<String, Object> buildMetadata(EmailMessage message) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("subject", message.getSubject());
        metadata.put("from", message.getFrom());
        metadata.put("messageId", message.getMessageId());
        return metadata;
    }

    /**
     * Base64 编码
     */
    private String base64Encode(String input) {
        return Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 发送 IMAP 命令
     */
    private void sendCommand(PrintWriter writer, String command) {
        writer.println("A" + System.currentTimeMillis() % 1000 + " " + command);
    }

    /**
     * 读取 IMAP 响应
     */
    private String readResponse(BufferedReader reader) throws IOException {
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line).append("\n");
            if (line.startsWith("A") && (line.contains("OK") || line.contains("NO") || line.contains("BAD"))) {
                break;
            }
            if (line.startsWith("* OK") || line.startsWith("* ")) {
                continue;
            }
        }
        return response.toString();
    }

    /**
     * 读取 SMTP 响应
     */
    private void readResponse(PrintWriter writer, BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.startsWith("3") && !line.startsWith("2")) {
                throw new IOException("SMTP error: " + line);
            }
            if (!line.startsWith("3-") && !line.startsWith("2-")) {
                break;
            }
        }
    }

    /**
     * 关闭资源
     */
    private void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    /**
     * 邮件消息内部类
     */
    private static class EmailMessage {
        private String messageId;
        private String from;
        private String subject;
        private String content;

        public String getMessageId() {
            return messageId;
        }

        public void setMessageId(String messageId) {
            this.messageId = messageId;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
