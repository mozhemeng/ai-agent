package org.example.aiagent.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ChatRequestDTO {
    @NotEmpty(message = "会话ID不能为空")
    private String sessionId;

    private String userPrompt;

    @Size(max = 3, message = "支持添加最多3个附件文件")
    private List<String> attachmentList;
}
