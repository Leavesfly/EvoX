package io.leavesfly.evox.cowork.connector;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class ConnectorLoader {
    
    public List<Connector> loadBuiltinConnectors() {
        List<Connector> connectors = new ArrayList<>();
        
        connectors.add(new BrowserConnector());
        
        log.info("Loaded {} builtin connectors", connectors.size());
        return connectors;
    }
    
    public List<Connector> loadFromDirectory(String directory) {
        return new ArrayList<>();
    }
    
    private static class BrowserConnector extends BaseConnector {
        
        public BrowserConnector() {
            super("browser", "Web Browser", "Browser automation connector", Connector.ConnectorType.BROWSER);
        }
        
        @Override
        protected void doConnect() throws Exception {
            
        }
        
        @Override
        protected void doDisconnect() {
            
        }
        
        @Override
        protected Map<String, Object> doExecute(String action, Map<String, Object> parameters) {
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("action", action);
            result.put("status", "success");
            return result;
        }
        
        @Override
        public List<String> getSupportedActions() {
            return List.of("navigate", "click", "type", "screenshot", "get_content");
        }
    }
}
