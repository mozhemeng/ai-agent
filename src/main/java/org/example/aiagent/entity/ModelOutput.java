package org.example.aiagent.entity;

import lombok.Data;

@Data
public class ModelOutput {
    private String content;
    private String reasoning;
    private Usage usage;

    @Data
    public static class Usage {
        private long promptTokens;
        private long completionTokens;
        private long totalTokens;
    }
}
