package io.leavesfly.evox.tools.email;

import io.leavesfly.evox.tools.base.BaseTool;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 邮件发送工具
 * 使用 Java 标准库直接实现 SMTP 协议，无需额外依赖
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class EmailTool extends BaseTool {

    private String smtpHost;
    private int smtpPort;
    private String username;
    private String password;
    private boolean useTls;

    public EmailTool() {
        this(null, 587, null, null, true);
    }

    public EmailTool(String smtpHost, int smtpPort, String username, String password, boolean useTls) {
        super();
        this.name = "email";
        this.description = "Send emails via SMTP. Supports plain text and HTML content, "
                + "CC/BCC recipients, and configurable SMTP settings.";

        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.username = username;
        this.password = password;
        this.useTls = useTls;

        this.inputs = new HashMap<>();
        this.required = new ArrayList<>();

        Map<String, String> toParam = new HashMap<>();
        toParam.put("type", "string");
        toParam.put("description", "Recipient email address(es), comma-separated for multiple");
        this.inputs.put("to", toParam);
        this.required.add("to");

        Map<String, String> subjectParam = new HashMap<>();
        subjectParam.put("type", "string");
        subjectParam.put("description", "Email subject");
        this.inputs.put("subject", subjectParam);
        this.required.add("subject");

        Map<String, String> bodyParam = new HashMap<>();
        bodyParam.put("type", "string");
        bodyParam.put("description", "Email body content");
        this.inputs.put("body", bodyParam);
        this.required.add("body");

        Map<String, String> ccParam = new HashMap<>();
        ccParam.put("type", "string");
        ccParam.put("description", "CC recipients, comma-separated (optional)");
        this.inputs.put("cc", ccParam);

        Map<String, String> bccParam = new HashMap<>();
        bccParam.put("type", "string");
        bccParam.put("description", "BCC recipients, comma-separated (optional)");
        this.inputs.put("bcc", bccParam);

        Map<String, String> htmlParam = new HashMap<>();
        htmlParam.put("type", "boolean");
        htmlParam.put("description", "Whether body is HTML content (default: false)");
        this.inputs.put("html", htmlParam);
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        validateParameters(parameters);

        String to = getParameter(parameters, "to", "");
        String subject = getParameter(parameters, "subject", "");
        String body = getParameter(parameters, "body", "");
        String cc = getParameter(parameters, "cc", "");
        String bcc = getParameter(parameters, "bcc", "");
        Boolean isHtml = getParameter(parameters, "html", false);

        if (smtpHost == null || smtpHost.isEmpty()) {
            String envHost = System.getenv("SMTP_HOST");
            if (envHost != null && !envHost.isEmpty()) {
                this.smtpHost = envHost;
                this.smtpPort = Integer.parseInt(System.getenv().getOrDefault("SMTP_PORT", "587"));
                this.username = System.getenv("SMTP_USERNAME");
                this.password = System.getenv("SMTP_PASSWORD");
                this.useTls = Boolean.parseBoolean(System.getenv().getOrDefault("SMTP_USE_TLS", "true"));
            } else {
                return ToolResult.failure("SMTP not configured. Set smtpHost or SMTP_HOST environment variable.");
            }
        }

        try {
            sendSmtpEmail(to, cc, bcc, subject, body, isHtml);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("sent", true);
            result.put("to", to);
            result.put("subject", subject);
            if (!cc.isEmpty()) result.put("cc", cc);
            if (!bcc.isEmpty()) result.put("bcc", bcc);

            log.info("Email sent to: {}, subject: {}", to, subject);
            return ToolResult.success(result);

        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
            return ToolResult.failure("Failed to send email: " + e.getMessage());
        }
    }

    private void sendSmtpEmail(String to, String cc, String bcc, String subject, String body, boolean isHtml)
            throws IOException {
        Socket socket = new Socket(smtpHost, smtpPort);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

        try {
            readResponse(reader);
            sendCommand(writer, reader, "EHLO localhost");

            if (useTls) {
                sendCommand(writer, reader, "STARTTLS");
                SSLSocketFactory sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                SSLSocket sslSocket = (SSLSocket) sslFactory.createSocket(socket, smtpHost, smtpPort, true);
                sslSocket.startHandshake();
                reader = new BufferedReader(new InputStreamReader(sslSocket.getInputStream(), StandardCharsets.UTF_8));
                writer = new BufferedWriter(new OutputStreamWriter(sslSocket.getOutputStream(), StandardCharsets.UTF_8));
                sendCommand(writer, reader, "EHLO localhost");
            }

            if (username != null && password != null) {
                sendCommand(writer, reader, "AUTH LOGIN");
                sendCommand(writer, reader, Base64.getEncoder().encodeToString(username.getBytes(StandardCharsets.UTF_8)));
                sendCommand(writer, reader, Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8)));
            }

            sendCommand(writer, reader, "MAIL FROM:<" + username + ">");

            for (String recipient : parseRecipients(to)) {
                sendCommand(writer, reader, "RCPT TO:<" + recipient.trim() + ">");
            }
            for (String recipient : parseRecipients(cc)) {
                sendCommand(writer, reader, "RCPT TO:<" + recipient.trim() + ">");
            }
            for (String recipient : parseRecipients(bcc)) {
                sendCommand(writer, reader, "RCPT TO:<" + recipient.trim() + ">");
            }

            sendCommand(writer, reader, "DATA");

            String contentType = isHtml ? "text/html; charset=UTF-8" : "text/plain; charset=UTF-8";
            StringBuilder emailData = new StringBuilder();
            emailData.append("From: ").append(username).append("\r\n");
            emailData.append("To: ").append(to).append("\r\n");
            if (!cc.isEmpty()) emailData.append("Cc: ").append(cc).append("\r\n");
            emailData.append("Subject: =?UTF-8?B?")
                    .append(Base64.getEncoder().encodeToString(subject.getBytes(StandardCharsets.UTF_8)))
                    .append("?=\r\n");
            emailData.append("MIME-Version: 1.0\r\n");
            emailData.append("Content-Type: ").append(contentType).append("\r\n");
            emailData.append("\r\n");
            emailData.append(body).append("\r\n");
            emailData.append(".");

            sendCommand(writer, reader, emailData.toString());
            sendCommand(writer, reader, "QUIT");
        } finally {
            socket.close();
        }
    }

    private List<String> parseRecipients(String recipients) {
        if (recipients == null || recipients.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(recipients.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private void sendCommand(BufferedWriter writer, BufferedReader reader, String command) throws IOException {
        writer.write(command + "\r\n");
        writer.flush();
        readResponse(reader);
    }

    private String readResponse(BufferedReader reader) throws IOException {
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line).append("\n");
            if (line.length() >= 4 && line.charAt(3) == ' ') {
                break;
            }
        }
        String responseStr = response.toString().trim();
        if (!responseStr.isEmpty() && responseStr.charAt(0) >= '4') {
            throw new IOException("SMTP error: " + responseStr);
        }
        return responseStr;
    }
}
