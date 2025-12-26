package org.example.aiagent.dto;

import lombok.Data;

@Data
public class ChatRequestDTO {
    private String sessionId;
    private String userPrompt;
}
