package org.example.aiagent.service;

import org.example.aiagent.dto.ChatRequestDTO;
import org.example.aiagent.dto.ChatResponseDTO;
import reactor.core.publisher.Flux;

public interface ChatService {
    ChatResponseDTO chat(ChatRequestDTO chatRequest);

    Flux<ChatResponseDTO> streamChat(ChatRequestDTO chatRequest);
}
