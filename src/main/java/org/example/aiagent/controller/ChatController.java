package org.example.aiagent.controller;

import lombok.RequiredArgsConstructor;
import org.example.aiagent.dto.ChatResponseDTO;
import org.example.aiagent.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/hello")
    public String hello() {
        return "Hello, World!";
    }

    @GetMapping("/chat")
    public ChatResponseDTO chat(@RequestParam String prompt) {
        return chatService.chat(prompt);
    }

    @GetMapping(value = "/stream-chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponseDTO> stream(@RequestParam String prompt) {
        return chatService.streamChat(prompt);
    }
}
