package org.example.aiagent.controller;

import lombok.RequiredArgsConstructor;
import org.example.aiagent.dto.LlmResponseDTO;
import org.example.aiagent.service.LlmService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final LlmService llmService;

    @GetMapping("/hello")
    public String hello() {
        return "Hello, World!";
    }

    @GetMapping("/chat")
    public LlmResponseDTO chat(@RequestParam String prompt) {
        return llmService.chat(prompt);
    }

    @GetMapping(value = "/stream-chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<LlmResponseDTO> stream(@RequestParam String prompt) {
        return llmService.streamChat(prompt);
    }
}
