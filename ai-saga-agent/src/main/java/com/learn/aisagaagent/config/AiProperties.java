package com.learn.aisagaagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai")
@Data
public class AiProperties {

    private String primaryModel = "ollama"; // default
    private ModelConfig ollama;
    private ModelConfig gemini;
    private ModelConfig claude;

    @Data
    public static class ModelConfig {
        private String baseUrl;
        private String modelName;
        private String apiKey;
        private int timeout = 120;
        private boolean thinkEnabled = false;
    }
}