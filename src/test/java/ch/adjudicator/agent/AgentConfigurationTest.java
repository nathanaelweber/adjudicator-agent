package ch.adjudicator.agent;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgentConfigurationTest {

    @Test
    void testDefaults() {
        // Use a non-existent file to ensure defaults are loaded
        AgentConfiguration config = new AgentConfiguration(new String[]{}, "non-existent.env");
        assertEquals("grpc.adjudicator.ch", config.getServerAddress());
        assertEquals("1234", config.getApiKey());
        assertEquals("EasyBot", config.getAgentName());
        assertEquals("TRAINING", config.getMode());
        assertEquals("300+0", config.getTimeControl());
    }

    @Test
    void testCliOverrides() {
        String[] args = {
            "--server", "localhost:9090",
            "--key", "secret",
            "--name", "TestBot",
            "--mode", "RANKED",
            "--time", "600+5"
        };
        AgentConfiguration config = new AgentConfiguration(args);
        
        assertEquals("localhost:9090", config.getServerAddress());
        assertEquals("secret", config.getApiKey());
        assertEquals("TestBot", config.getAgentName());
        assertEquals("RANKED", config.getMode());
        assertEquals("600+5", config.getTimeControl());
    }

    @Test
    void testValidationSuccess() {
        AgentConfiguration config = new AgentConfiguration(new String[]{"--key", "123"});
        assertDoesNotThrow(config::validate);
    }
}
