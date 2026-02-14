package io.leavesfly.evox.channels.email;

import io.leavesfly.evox.channels.core.ChannelConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Email 渠道配置
 * 支持 IMAP 收件和 SMTP 发件
 *
 * @author EvoX Team
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class EmailChannelConfig extends ChannelConfig {

    /**
     * IMAP 服务器地址
     */
    private String imapHost;

    /**
     * IMAP 端口
     * 默认使用 993 (SSL)
     */
    @lombok.Builder.Default
    private int imapPort = 993;

    /**
     * IMAP 用户名
     */
    private String imapUsername;

    /**
     * IMAP 密码
     */
    private String imapPassword;

    /**
     * SMTP 服务器地址
     */
    private String smtpHost;

    /**
     * SMTP 端口
     * 默认使用 587 (STARTTLS)
     */
    @lombok.Builder.Default
    private int smtpPort = 587;

    /**
     * SMTP 用户名
     */
    private String smtpUsername;

    /**
     * SMTP 密码
     */
    private String smtpPassword;

    /**
     * 发件人地址
     */
    private String fromAddress;

    /**
     * 允许的发送者白名单
     * 只有在白名单中的发件人发送的邮件才会被处理
     */
    private List<String> allowFrom;

    /**
     * 轮询间隔（毫秒）
     * 默认 30 秒
     */
    @lombok.Builder.Default
    private long pollingIntervalMs = 30000;

    /**
     * 是否启用自动回复
     * 默认启用
     */
    @lombok.Builder.Default
    private boolean autoReplyEnabled = true;

    /**
     * 用户是否授予访问邮箱的同意
     * 安全门控，必须为 true 才能访问邮箱
     */
    @lombok.Builder.Default
    private boolean consentGranted = false;
}
