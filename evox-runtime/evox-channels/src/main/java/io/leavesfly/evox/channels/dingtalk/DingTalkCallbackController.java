package io.leavesfly.evox.channels.dingtalk;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
public class DingTalkCallbackController {

    private final DingTalkChannel dingTalkChannel;

    public DingTalkCallbackController(DingTalkChannel dingTalkChannel) {
        this.dingTalkChannel = dingTalkChannel;
    }

    @PostMapping("${evox.channels.dingtalk.callback-path:/api/dingtalk/callback}")
    public ResponseEntity<Map<String, String>> handleCallback(@RequestBody String body) {
        log.debug("Received DingTalk callback: {}", body);
        dingTalkChannel.handleCallback(body);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
