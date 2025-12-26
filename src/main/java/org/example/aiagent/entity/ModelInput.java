package org.example.aiagent.entity;

import lombok.Data;

import java.util.List;

@Data
public class ModelInput {
    private String userPrompt;
    private List<ChatMessage> historyChatMessageList;
}
