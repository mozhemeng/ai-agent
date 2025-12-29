package org.example.aiagent.entity;

import lombok.Data;

import java.util.List;

@Data
public class ModelInput {
    private ChatMessage currentChatMessage;
    private List<ChatMessage> historyChatMessageList;
}
