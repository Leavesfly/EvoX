package io.leavesfly.evox.cowork.api;

import io.leavesfly.evox.cowork.api.exception.CoworkException;
import io.leavesfly.evox.cowork.connector.ConnectorManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/cowork/connectors")
@RequiredArgsConstructor
public class ConnectorController {

    private final ConnectorManager connectorManager;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getConnectors() {
        List<Map<String, Object>> connectors = connectorManager.getAllConnectors().stream()
                .map(connector -> {
                    Map<String, Object> connectorInfo = new HashMap<>();
                    connectorInfo.put("id", connector.getId());
                    connectorInfo.put("name", connector.getName());
                    connectorInfo.put("type", connector.getType());
                    connectorInfo.put("connected", connector.isConnected());
                    connectorInfo.put("actions", connector.getSupportedActions());
                    return connectorInfo;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(connectors);
    }

    @PostMapping("/{connectorId}/connect")
    public ResponseEntity<Map<String, String>> connectConnector(
            @PathVariable String connectorId,
            @RequestBody Map<String, String> credentials) {
        try {
            connectorManager.connectConnector(connectorId, credentials);
            return ResponseEntity.ok(Map.of("message", "Connected: " + connectorId));
        } catch (Exception e) {
            throw new CoworkException("CONNECTOR_ERROR", "Failed to connect: " + e.getMessage(), e);
        }
    }

    @PostMapping("/{connectorId}/disconnect")
    public ResponseEntity<Map<String, String>> disconnectConnector(@PathVariable String connectorId) {
        try {
            connectorManager.disconnectConnector(connectorId);
            return ResponseEntity.ok(Map.of("message", "Disconnected: " + connectorId));
        } catch (Exception e) {
            throw new CoworkException("CONNECTOR_ERROR", "Failed to disconnect: " + e.getMessage(), e);
        }
    }
}
