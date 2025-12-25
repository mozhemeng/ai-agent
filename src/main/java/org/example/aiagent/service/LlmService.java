package org.example.aiagent.service;

import org.example.aiagent.dto.LlmResponseDTO;
import reactor.core.publisher.Flux;

public interface LlmService {
    LlmResponseDTO chat(String prompt);

    Flux<LlmResponseDTO> streamChat(String prompt);
}
