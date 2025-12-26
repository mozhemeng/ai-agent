package org.example.aiagent.service;

import org.example.aiagent.dto.ChatResponseDTO;
import reactor.core.publisher.Flux;

public interface ChatService {
    ChatResponseDTO chat(String prompt);

    Flux<ChatResponseDTO> streamChat(String prompt);
}
