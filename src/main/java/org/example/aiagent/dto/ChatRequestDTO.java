package org.example.aiagent.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class ChatRequestDTO {
    @NotEmpty(message = "会话ID不能为空")
    private String sessionId;

    private String userPrompt;
}
